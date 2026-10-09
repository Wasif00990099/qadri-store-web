import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.List;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class UnpackUpdater {

    private final QadriStore app;

    // --- UNPACK UPDATE TRACKING (Tracks PurchaseInvoiceItem IDs) ---
    Set<String> processedInvoiceItemIds = new HashSet<>();
    String unpackUpdatedFile = "QadriStore_UnpackVendorUpdated.cfg";

    // --- ENCRYPTION KEY ---
    private final byte[] ENCRYPTION_KEY = "QadriStoreSecretKey2025".getBytes();

    // --- DEBUG MODE ---
    private static final String DEBUG_FILE = "QadriStore_Debug.log";
    // Set to true to enable console debug output (false for production)
    private static final boolean ENABLE_CONSOLE_DEBUG = false;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private void logDebug(String message) {
        if (ENABLE_CONSOLE_DEBUG) System.out.println("[UNPACK_DEBUG] " + message);
        try (PrintWriter writer = new PrintWriter(new FileWriter(DEBUG_FILE, true))) {
            writer.println(new java.util.Date() + " | " + message);
        } catch (Exception e) { if (ENABLE_CONSOLE_DEBUG) e.printStackTrace(); }
    }

    private void logError(String message, Exception ex) {
        if (ENABLE_CONSOLE_DEBUG) System.err.println("[UNPACK_ERROR] " + message);
        try (PrintWriter writer = new PrintWriter(new FileWriter(DEBUG_FILE, true))) {
            writer.println(new java.util.Date() + " | ERROR: " + message);
            if (ex != null) {
                writer.println("Exception: " + ex.getMessage());
                for (StackTraceElement el : ex.getStackTrace()) writer.println("    at " + el.toString());
            }
            writer.println("--------------------------------------------------");
        } catch (Exception e) { if (ENABLE_CONSOLE_DEBUG) e.printStackTrace(); }

        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(app, "<html><b>Debug Error:</b><br>" + message + "<br><br>Check <b>" + DEBUG_FILE + "</b> for details.</html>", "Debug Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    public UnpackUpdater(QadriStore app) {
        this.app = app;
    }

    // ==================== ENCRYPTION / DECRYPTION ====================
    private String encrypt(String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            byte[] encrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) encrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) { return ""; }
    }

    private String decrypt(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            byte[] decrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) decrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) { return ""; }
    }

    public void loadUnpackUpdatedState() {
        File f = new File(unpackUpdatedFile);
        if (f.exists()) {
            try {
                String encryptedContent = new String(Files.readAllBytes(f.toPath()));
                String decryptedContent = decrypt(encryptedContent.trim());
                if (!decryptedContent.isEmpty()) {
                    String[] lines = decryptedContent.split("\\n");
                    for (String line : lines) { line = line.trim(); if (!line.isEmpty()) processedInvoiceItemIds.add(line); }
                }
            } catch (Exception e) { logError("Failed to load unpack state", e); }
        }
    }

    private void saveUnpackUpdatedState() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String id : processedInvoiceItemIds) sb.append(id).append("\n");
            String encrypted = encrypt(sb.toString());
            try (PrintWriter writer = new PrintWriter(new FileWriter(unpackUpdatedFile))) { writer.print(encrypted); }
        } catch (Exception e) { logError("Failed to save unpack state", e); }
    }

    // ==================== SIZE & BASE NAME LOGIC ====================
    private double parseSizeFromName(String name) {
        if (name == null) return 0;
        String lower = name.toLowerCase().trim();

        Pattern kgPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg\\s*$");
        Matcher kgMatcher = kgPattern.matcher(lower);
        if (kgMatcher.find()) return Double.parseDouble(kgMatcher.group(1));

        Pattern gmPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*gm\\s*$");
        Matcher gmMatcher = gmPattern.matcher(lower);
        if (gmMatcher.find()) return Double.parseDouble(gmMatcher.group(1)) / 1000.0;

        Pattern gPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*g\\b\\s*$");
        Matcher gMatcher = gPattern.matcher(lower);
        if (gMatcher.find()) return Double.parseDouble(gMatcher.group(1)) / 1000.0;

        Pattern ltrPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ltr\\s*$");
        Matcher ltrMatcher = ltrPattern.matcher(lower);
        if (ltrMatcher.find()) return Double.parseDouble(ltrMatcher.group(1));

        Pattern mlPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*ml\\s*$");
        Matcher mlMatcher = mlPattern.matcher(lower);
        if (mlMatcher.find()) return Double.parseDouble(mlMatcher.group(1)) / 1000.0;

        return 0;
    }

    private String getBaseName(String name) {
        if (name == null) return "";
        String base = name.trim();
        base = base.replaceAll("(?i)\\s*\\d+(?:\\.\\d+)?\\s*(kg|gm|g|ltr|ml)\\s*$", "");
        base = base.replaceAll("(?i)\\s*unpack\\s*$", "");
        return base.trim().toLowerCase();
    }

    private boolean isUnpackItem(String name) {
        if (name == null) return false;
        return name.toLowerCase().trim().endsWith("unpack");
    }

    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { String str = obj.toString().trim(); if (str.isEmpty()) return 0.0; return Double.parseDouble(str); }
        catch (Exception e) { return 0.0; }
    }

    // ==================== MAIN UPDATE METHOD ====================
    public void performUnpackUpdate() {
        if (app.resultTable == null) {
            int choice = JOptionPane.showConfirmDialog(app,
                "<html><b>Table is not loaded yet!</b><br>Do you still want to proceed?<br>(Data will be fetched from DB)</html>",
                "Table Not Loaded", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        if (app.isOfflineMode) {
            JOptionPane.showMessageDialog(app, "<html><b>Cannot update in Offline Mode!</b></html>", "Offline Mode", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Skip password verification and vendor-search popup. Open the vendor update dialog directly.
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    VendorUpdateDialog dialog = new VendorUpdateDialog("");
                    dialog.setVisible(true);
                } catch (Exception ex) { logError("Failed to open VendorUpdateDialog", ex); }
            });
        } catch (Exception ex) {
            logError("Error in performUnpackUpdate", ex);
        }
    }

    // ==================== VENDOR SEARCH DIALOG ====================
    class VendorSearchDialog extends JDialog {
        private JTextField txtSearch;
        private JList<String> listVendors;
        private DefaultListModel<String> listModel;
        private javax.swing.Timer searchTimer;
        public String selectedVendor = null;

        public VendorSearchDialog() {
            super(app, "🔍 Search Vendor", true);
            setSize(400, 500);
            setLocationRelativeTo(app);
            setLayout(new BorderLayout(10, 10));

            txtSearch = new JTextField();
            txtSearch.setFont(new Font("Arial", Font.BOLD, 18));

            listModel = new DefaultListModel<>();
            listVendors = new JList<>(listModel);
            listVendors.setFont(new Font("Arial", Font.PLAIN, 16));

            JButton btnSelect = new JButton("SELECT VENDOR");
            btnSelect.setBackground(new Color(0, 120, 60));
            btnSelect.setForeground(Color.WHITE);
            btnSelect.setFont(new Font("Arial", Font.BOLD, 14));
            btnSelect.setOpaque(true);
            btnSelect.setBorderPainted(false);
            btnSelect.addActionListener(e -> selectVendor());

            listVendors.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && listVendors.getSelectedIndex() != -1) txtSearch.setText(listVendors.getSelectedValue()); });
            listVendors.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent evt) { if (evt.getClickCount() == 2) selectVendor(); } });

            searchTimer = new javax.swing.Timer(300, e -> searchVendors(txtSearch.getText().trim()));
            searchTimer.setRepeats(false);

            txtSearch.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) { selectVendor(); return; }
                    searchTimer.restart();
                }
            });

            add(txtSearch, BorderLayout.NORTH);
            add(new JScrollPane(listVendors), BorderLayout.CENTER);
            add(btnSelect, BorderLayout.SOUTH);

            SwingUtilities.invokeLater(() -> txtSearch.requestFocusInWindow());
            searchVendors("");
        }

        private void selectVendor() {
            if (listVendors.getSelectedIndex() != -1) selectedVendor = listVendors.getSelectedValue();
            else if (!txtSearch.getText().trim().isEmpty()) selectedVendor = txtSearch.getText().trim();
            dispose();
        }

        private void searchVendors(String query) {
            new SwingWorker<List<String>, Void>() {
                @Override
                protected List<String> doInBackground() {
                    List<String> names = new ArrayList<>();
                    String sql = "SELECT DISTINCT Name FROM ProductVendor";
                    if (!query.isEmpty()) sql += " WHERE Name LIKE ?";
                    sql += " ORDER BY Name";
                    try (Connection conn = DriverManager.getConnection(app.url, app.user, app.pass);
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        if (!query.isEmpty()) pstmt.setString(1, "%" + query + "%");
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) { String name = rs.getString(1); if (name != null) names.add(name); }
                    } catch (Exception ex) { logError("DB Error fetching vendors", ex); }
                    return names;
                }
                @Override
                protected void done() {
                    try { listModel.clear(); for (String n : get()) listModel.addElement(n); if (!listModel.isEmpty()) listVendors.setSelectedIndex(0); }
                    catch (Exception ex) { logError("Error populating vendor list", ex); }
                }
            }.execute();
        }
    }

    // ==================== INVOICE RECORD HELPER ====================
    class InvoiceRecord {
        int purchaseInvoiceId;
        String invoiceNo;
        String invoiceDateStr;
        String vendorName;
        List<ItemRecord> items = new ArrayList<>();
        Map<String, Double> unpackDeductions = new LinkedHashMap<>();
        Map<String, List<String>> unpackDetailsHtml = new LinkedHashMap<>();
        boolean isFullyUpdated = true;

        public String getItemsHtml() {
            StringBuilder sb = new StringBuilder("<html>");
            if (!unpackDetailsHtml.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : unpackDetailsHtml.entrySet()) {
                    sb.append("<b style='color:navy;'>📦 ").append(entry.getKey().toUpperCase()).append(" UNPACK:</b><br>");
                    for (String detail : entry.getValue()) sb.append("&nbsp;&nbsp;").append(detail).append("<br>");
                    double totalDeduction = unpackDeductions.get(entry.getKey());
                    sb.append(String.format("&nbsp;&nbsp;➡️ <b>Total Minus: %.3f kg</b><br>", totalDeduction));
                }
            }
            for (ItemRecord item : items) {
                if (parseSizeFromName(item.itemName) == 0) {
                    sb.append("&nbsp;&nbsp;• ").append(item.itemName == null ? "N/A" : item.itemName).append(" (No deduction)<br>");
                }
            }
            if (sb.toString().equals("<html>")) sb.append("No packed items to deduct");
            return sb.append("</html>").toString();
        }
    }

    class ItemRecord {
        int piiId;
        String itemName;
        double qty;
        double price;
    }

    // ==================== VENDOR UPDATE DIALOG ====================
    class VendorUpdateDialog extends JDialog {
        private String vendorName;
        private LocalDate currentStartDate = null;
        private LocalDate currentEndDate = null;
        private JComboBox<String> cmbVendors = null;
        private JTable recordTable;
        private DefaultTableModel tableModel;
        private JTextField txtDays;
        private JButton btnToday, btnYesterday, btn7Days, btn30Days, btnCustom;
        private Map<Integer, InvoiceRecord> invoiceDataMap = new LinkedHashMap<>();

        public VendorUpdateDialog(String vendorName) {
            super(app, "📦 Vendor Unpack Update: " + vendorName, true);
            this.vendorName = vendorName;
            setSize(1100, 600);
            setLocationRelativeTo(app);
            setLayout(new BorderLayout(10, 10));

            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            filterPanel.setBackground(new Color(240, 240, 245));

            // Vendor selector combobox (populated from ProductVendor table)
            cmbVendors = new JComboBox<>();
            cmbVendors.setFont(new Font("Arial", Font.PLAIN, 14));
            cmbVendors.setPreferredSize(new Dimension(300, 28));
            cmbVendors.addActionListener(e -> {
                try {
                    String sel = (String) cmbVendors.getSelectedItem();
                    if (sel == null) return;
                    sel = sel.trim();
                    if (sel.isEmpty() || sel.equals(VendorUpdateDialog.this.vendorName) || "-- Select Vendor --".equals(sel)) return;
                    VendorUpdateDialog.this.vendorName = sel;
                    // reload records for current date range if available; only if controls initialized
                    if (currentStartDate != null && currentEndDate != null && VendorUpdateDialog.this.btnToday != null) {
                        loadRecords(currentStartDate, currentEndDate);
                    } else if (VendorUpdateDialog.this.btnToday != null) {
                        VendorUpdateDialog.this.btnToday.doClick();
                    }
                    setTitle("📦 Vendor Unpack Update: " + VendorUpdateDialog.this.vendorName);
                } catch (Exception ex) {
                    logDebug("Vendor combo action error: " + ex.getMessage());
                }
            });
            filterPanel.add(new JLabel("Vendor: "));
            filterPanel.add(cmbVendors);
            populateVendorCombo();
            cmbVendors.setSelectedItem(vendorName);

            btnToday = createFilterButton("📅 Today", FilterType.TODAY);
            btnYesterday = createFilterButton("📆 Yesterday", FilterType.YESTERDAY);
            btn7Days = createFilterButton("📊 7 Days", FilterType.DAYS_7);
            btn30Days = createFilterButton("📈 30 Days", FilterType.DAYS_30);

            txtDays = new JTextField(5);
            txtDays.setFont(new Font("Arial", Font.BOLD, 14));
            txtDays.setHorizontalAlignment(JTextField.CENTER);
            btnCustom = new JButton("🔍 Get Record");
            btnCustom.setBackground(new Color(0, 120, 215));
            btnCustom.setForeground(Color.WHITE);
            btnCustom.setFont(new Font("Arial", Font.BOLD, 12));
            btnCustom.setOpaque(true);
            btnCustom.setBorderPainted(false);
            btnCustom.addActionListener(e -> {
                String text = txtDays.getText().trim();
                if (text.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter number of days.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    int days = Integer.parseInt(text);
                    if (days <= 0) {
                        JOptionPane.showMessageDialog(this, "Please enter a valid positive number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    highlightFilterButton(null);
                    LocalDate today = LocalDate.now();
                    LocalDate startDate = today.minusDays(days - 1);
                    loadRecords(startDate, today);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                }
            });

            txtDays.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        btnCustom.doClick();
                    }
                }
            });

            JLabel lblInfo = new JLabel(" ← Last N days from today");
            lblInfo.setFont(new Font("Arial", Font.ITALIC, 11));
            lblInfo.setForeground(Color.GRAY);

            filterPanel.add(new JLabel("Filter: "));
            filterPanel.add(btnToday);
            filterPanel.add(btnYesterday);
            filterPanel.add(btn7Days);
            filterPanel.add(btn30Days);
            filterPanel.add(Box.createHorizontalStrut(10));
            filterPanel.add(new JLabel("Last"));
            filterPanel.add(txtDays);
            filterPanel.add(new JLabel("Days"));
            filterPanel.add(btnCustom);
            filterPanel.add(lblInfo);

            add(filterPanel, BorderLayout.NORTH);

            String[] columns = {"InvId", "Invoice No", "Invoice Date", "Vendor Name", "Deduction Details", "Status", "Action"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return column == 6; }
            };
            recordTable = new JTable(tableModel);
            recordTable.setRowHeight(60);
            recordTable.setFont(new Font("Arial", Font.PLAIN, 13));

            recordTable.getColumnModel().getColumn(0).setMinWidth(0);
            recordTable.getColumnModel().getColumn(0).setMaxWidth(0);
            recordTable.getColumnModel().getColumn(0).setWidth(0);

            recordTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
            recordTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
            recordTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

            JScrollPane scrollPane = new JScrollPane(recordTable);
            add(scrollPane, BorderLayout.CENTER);

            btnToday.doClick();
        }

        private enum FilterType {
            TODAY, YESTERDAY, DAYS_7, DAYS_30
        }

        private JButton createFilterButton(String text, FilterType type) {
            JButton btn = new JButton(text);
            btn.setBackground(new Color(70, 130, 180));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Arial", Font.BOLD, 12));
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                LocalDate today = LocalDate.now();
                LocalDate startDate;
                LocalDate endDate;

                switch (type) {
                    case YESTERDAY:
                        startDate = today.minusDays(1);
                        endDate = today.minusDays(1);
                        txtDays.setText("1");
                        break;
                    case DAYS_7:
                        startDate = today.minusDays(6);
                        endDate = today;
                        txtDays.setText("7");
                        break;
                    case DAYS_30:
                        startDate = today.minusDays(29);
                        endDate = today;
                        txtDays.setText("30");
                        break;
                    case TODAY:
                    default:
                        startDate = today;
                        endDate = today;
                        txtDays.setText("1");
                        break;
                }

                highlightFilterButton(btn);
                loadRecords(startDate, endDate);
            });
            return btn;
        }

        private void highlightFilterButton(JButton activeBtn) {
            Color defaultColor = new Color(70, 130, 180);
            Color activeColor = new Color(0, 100, 0);

            btnToday.setBackground(defaultColor);
            btnYesterday.setBackground(defaultColor);
            btn7Days.setBackground(defaultColor);
            btn30Days.setBackground(defaultColor);

            if (activeBtn != null) {
                activeBtn.setBackground(activeColor);
            }
        }

        private void populateVendorCombo() {
            cmbVendors.removeAllItems();
            List<String> names = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(app.url, app.user, app.pass)) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT Name FROM ProductVendor WHERE Name IS NOT NULL ORDER BY Name")) {
                    while (rs.next()) {
                        String n = rs.getString(1);
                        if (n != null && !n.trim().isEmpty()) names.add(n.trim());
                    }
                }
                // fallback: if vendor table seems empty, get distinct vendor names from PurchaseInvoice
                if (names.size() < 2) {
                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery("SELECT DISTINCT PV.Name FROM PurchaseInvoice PI LEFT JOIN ProductVendor PV ON PI.ProductVendorId = PV.ProductVendorId WHERE PV.Name IS NOT NULL ORDER BY PV.Name")) {
                        while (rs2.next()) {
                            String n = rs2.getString(1);
                            if (n != null && !n.trim().isEmpty() && !names.contains(n.trim())) names.add(n.trim());
                        }
                    }
                }
            } catch (Exception ex) {
                logDebug("Failed to populate vendor combo: " + ex.getMessage());
            }

            // Ensure at least the current vendorName is present
            if ((vendorName != null) && !vendorName.trim().isEmpty() && !names.contains(vendorName.trim())) names.add(0, vendorName.trim());

            // add a placeholder and then all names
            cmbVendors.addItem("-- Select Vendor --");
            for (String n : names) cmbVendors.addItem(n);
            // select vendorName if available, otherwise keep placeholder
            if (vendorName != null && !vendorName.trim().isEmpty()) cmbVendors.setSelectedItem(vendorName.trim());
        }

        private void loadRecords(LocalDate startDate, LocalDate endDate) {
            tableModel.setRowCount(0);
            invoiceDataMap.clear();
            app.lblStatus.setText("Fetching records...");

            final LocalDate finalStartDate = startDate;
            final LocalDate finalEndDate = endDate;
            final String displayRange = startDate.format(DATE_FORMAT) + " to " + endDate.format(DATE_FORMAT);
            // remember current range so vendor combobox can reload same range
            currentStartDate = startDate;
            currentEndDate = endDate;

            logDebug("loadRecords: filterRange=" + displayRange + " | startDate=" + startDate + " | endDate=" + endDate);

            new SwingWorker<Map<Integer, InvoiceRecord>, Void>() {
                @Override
                protected Map<Integer, InvoiceRecord> doInBackground() {
                    Map<Integer, InvoiceRecord> map = new LinkedHashMap<>();

                    String sql = "SELECT PI.PurchaseInvoiceId, PI.TransactionNumber, PI.PurchaseInvoiceDate, PV.Name, PItem.LongName, " +
                                 "PII.PurchaseInvoiceItemId, PII.Quantity, PII.Price " +
                                 "FROM PurchaseInvoice PI " +
                                 "INNER JOIN PurchaseInvoiceItem PII ON PI.PurchaseInvoiceId = PII.PurchaseInvoiceId " +
                                 "LEFT JOIN ProductVendor PV ON PI.ProductVendorId = PV.ProductVendorId " +
                                 "LEFT JOIN ProductItem PItem ON PII.ProductItemId = PItem.ProductItemId " +
                                 "WHERE PV.Name LIKE ? " +
                                 "AND CONVERT(DATE, PI.PurchaseInvoiceDate) >= ? " +
                                 "AND CONVERT(DATE, PI.PurchaseInvoiceDate) <= ? " +
                                 "ORDER BY PI.PurchaseInvoiceDate DESC";

                    try (Connection conn = DriverManager.getConnection(app.url, app.user, app.pass);
                         PreparedStatement pstmt = conn.prepareStatement(sql)) {

                        pstmt.setString(1, "%" + vendorName + "%");
                        java.sql.Date sqlStart = java.sql.Date.valueOf(finalStartDate);
                        java.sql.Date sqlEnd = java.sql.Date.valueOf(finalEndDate);
                        pstmt.setDate(2, sqlStart);
                        pstmt.setDate(3, sqlEnd);

                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            int invId = rs.getInt("PurchaseInvoiceId");
                            String invNo = rs.getString("TransactionNumber");

                            if (!map.containsKey(invId)) {
                                InvoiceRecord inv = new InvoiceRecord();
                                inv.purchaseInvoiceId = invId;
                                inv.invoiceNo = invNo == null ? "N/A" : invNo;

                                try {
                                    Timestamp ts = rs.getTimestamp("PurchaseInvoiceDate");
                                    if (ts != null) {
                                        LocalDate localDate = ts.toLocalDateTime().toLocalDate();
                                        inv.invoiceDateStr = localDate.format(DATE_FORMAT);
                                    } else {
                                        inv.invoiceDateStr = "N/A";
                                    }
                                } catch (Exception dateEx) {
                                    String rawDate = rs.getString("PurchaseInvoiceDate");
                                    inv.invoiceDateStr = rawDate != null ? rawDate : "N/A";
                                }

                                inv.vendorName = rs.getString("Name");
                                map.put(invId, inv);
                            }

                            ItemRecord item = new ItemRecord();
                            item.piiId = rs.getInt("PurchaseInvoiceItemId");
                            item.itemName = rs.getString("LongName");
                            item.qty = rs.getDouble("Quantity");
                            item.price = rs.getDouble("Price");

                            if (!processedInvoiceItemIds.contains("PII_" + item.piiId)) {
                                map.get(invId).isFullyUpdated = false;
                            }
                            map.get(invId).items.add(item);

                            double sizeKg = parseSizeFromName(item.itemName);
                            if (sizeKg > 0) {
                                String baseName = getBaseName(item.itemName);
                                if (!baseName.isEmpty()) {
                                    double weight = sizeKg * item.qty;
                                    map.get(invId).unpackDeductions.merge(baseName, weight, Double::sum);
                                    map.get(invId).unpackDetailsHtml.computeIfAbsent(baseName, k -> new ArrayList<>())
                                        .add(String.format("• %s (%.0f pcs × %.3f kg = <b>%.3f kg</b>)", item.itemName, item.qty, sizeKg, weight));
                                }
                            }
                        }
                    } catch (Exception ex) { logError("DB Error loading records", ex); }
                    return map;
                }

                @Override
                protected void done() {
                    try {
                        Map<Integer, InvoiceRecord> map = get();
                        invoiceDataMap = map;

                        for (Map.Entry<Integer, InvoiceRecord> entry : map.entrySet()) {
                            InvoiceRecord inv = entry.getValue();
                            String status = inv.isFullyUpdated ? "Updated" : "Pending";
                            tableModel.addRow(new Object[]{entry.getKey(), inv.invoiceNo, inv.invoiceDateStr, inv.vendorName, inv.getItemsHtml(), status, "Update"});
                        }

                        if (finalStartDate.equals(finalEndDate)) {
                            app.lblStatus.setText("📅 " + finalStartDate.format(DATE_FORMAT) + " — Loaded " + map.size() + " invoice(s).");
                        } else {
                            app.lblStatus.setText("📅 " + displayRange + " — Loaded " + map.size() + " invoice(s).");
                        }
                    } catch (Exception ex) { logError("UI Error populating table", ex); }
                }
            }.execute();
        }

        class StatusRenderer extends JLabel implements TableCellRenderer {
            public StatusRenderer() { setOpaque(true); setHorizontalAlignment(SwingConstants.CENTER); setFont(new Font("Arial", Font.BOLD, 13)); }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                setText(value == null ? "" : value.toString());
                if ("Updated".equals(value)) { setBackground(new Color(144, 238, 144)); setForeground(new Color(0, 100, 0)); }
                else { setBackground(new Color(255, 182, 193)); setForeground(Color.RED); }
                return this;
            }
        }

        class ButtonRenderer extends JButton implements TableCellRenderer {
            public ButtonRenderer() { setOpaque(true); setBorderPainted(false); setFont(new Font("Arial", Font.BOLD, 12)); }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String status = table.getModel().getValueAt(row, 5).toString();
                if ("Updated".equals(status)) { setText("✅ Done"); setEnabled(false); setBackground(Color.LIGHT_GRAY); setForeground(Color.DARK_GRAY); }
                else { setText("📦 Update All"); setEnabled(true); setBackground(new Color(255, 140, 0)); setForeground(Color.WHITE); }
                return this;
            }
        }

        class ButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
            private JButton button;
            private int currentRow;

            public ButtonEditor(JCheckBox checkBox) {
                button = new JButton();
                button.setOpaque(true);
                button.setBorderPainted(false);
                button.setFont(new Font("Arial", Font.BOLD, 12));
                button.addActionListener(this);
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                currentRow = row;
                String status = table.getModel().getValueAt(row, 5).toString();
                if ("Updated".equals(status)) { button.setText("✅ Done"); button.setEnabled(false); button.setBackground(Color.LIGHT_GRAY); button.setForeground(Color.DARK_GRAY); }
                else { button.setText("📦 Update All"); button.setEnabled(true); button.setBackground(new Color(255, 140, 0)); button.setForeground(Color.WHITE); }
                return button;
            }

            @Override
            public Object getCellEditorValue() { return "Update"; }

            @Override
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
                int invId = (int) tableModel.getValueAt(currentRow, 0);
                InvoiceRecord inv = invoiceDataMap.get(invId);
                if (inv == null) return;

                int confirm = JOptionPane.showConfirmDialog(VendorUpdateDialog.this,
                        "<html>Update all pending Unpack items for Invoice: <b>" + inv.invoiceNo + "</b>?<br>" + inv.getItemsHtml() + "</html>",
                        "Confirm Update", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    app.lblStatus.setText("Updating invoice: " + inv.invoiceNo + "...");
                    new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() {
                            boolean allSuccess = true;
                            DefaultTableModel appModel = (DefaultTableModel) app.resultTable.getModel();

                            for (Map.Entry<String, Double> deduction : inv.unpackDeductions.entrySet()) {
                                String baseName = deduction.getKey();
                                double totalWeight = deduction.getValue();

                                String unpackBarcode = "";
                                double unpackCostPrice = 0.0;
                                String unpackName = baseName + " unpack";

                                for (int i = 0; i < appModel.getRowCount(); i++) {
                                    String name = (String) appModel.getValueAt(i, 1);
                                    if (name != null && isUnpackItem(name) && getBaseName(name).equals(baseName)) {
                                        unpackBarcode = (String) appModel.getValueAt(i, 0);
                                        unpackCostPrice = parseDouble(appModel.getValueAt(i, 9));
                                        unpackName = name;
                                        break;
                                    }
                                }

                                boolean success = executeUnpackDeduction(unpackBarcode, unpackName, totalWeight, unpackCostPrice);
                                if (success) {
                                    for (ItemRecord item : inv.items) {
                                        if (item.itemName != null && getBaseName(item.itemName).equals(baseName)) {
                                            processedInvoiceItemIds.add("PII_" + item.piiId);
                                        }
                                    }
                                } else {
                                    allSuccess = false;
                                }
                            }
                            return allSuccess;
                        }

                        @Override
                        protected void done() {
                            try {
                                boolean success = get();
                                if (success) {
                                    saveUnpackUpdatedState();
                                    tableModel.setValueAt("Updated", currentRow, 5);
                                    recordTable.repaint();
                                    app.lblStatus.setText("✅ Successfully updated invoice: " + inv.invoiceNo);
                                    app.loadAndMergeSelectedTables();
                                } else {
                                    saveUnpackUpdatedState();
                                    JOptionPane.showMessageDialog(VendorUpdateDialog.this, "Some items failed to update.", "Warning", JOptionPane.WARNING_MESSAGE);
                                }
                            } catch (Exception ex) { logError("Error in deduction done()", ex); }
                        }
                    }.execute();
                }
            }
        }
    }

    // ==================== DB EXECUTION (WAZAN MINUS LOGIC) ====================
    private boolean executeUnpackDeduction(String barcode, String productName, double qtyKg, double costPrice) {
        String escapedBarcode = barcode.replace("'", "''");
        String escapedName = productName.replace("'", "''");

        String sql =
            "SET NOCOUNT ON; " +
            "DECLARE @ProductSearch NVARCHAR(200) = '" + escapedName + "'; " +
            "DECLARE @Qty DECIMAL(18,3) = " + String.format("%.3f", qtyKg) + "; " +
            "DECLARE @Rate MONEY = 0.00; " +
            "DECLARE @CostPrice MONEY = " + String.format("%.2f", costPrice) + "; " +
            "DECLARE @BranchID INT = 1; " +
            "DECLARE @UserID INT = 4; " +
            "DECLARE @CounterID INT = 3; " +
            "DECLARE @PayModeID INT = 1; " +
            "DECLARE @SaleTypeID INT = 1; " +
            "DECLARE @ClientID INT = 1; " +

            "BEGIN TRY " +
            "BEGIN TRANSACTION; " +
            "DECLARE @PID INT; DECLARE @Bar NVARCHAR(30); DECLARE @PName NVARCHAR(200); " +
            "SELECT TOP 1 @PID=ProductItemId, @Bar=ISNULL(Barcode,''), @PName=LongName FROM ProductItem WHERE LongName=@ProductSearch OR Barcode='" + escapedBarcode + "'; " +
            "IF @PID IS NULL BEGIN ROLLBACK; RETURN; END " +

            "DECLARE @PosID INT; " +
            "SELECT TOP 1 @PosID=POSSessionActivityId FROM POSSessionActivity WHERE UserId=@UserID AND CompanyBranchId=@BranchID AND CAST(DataEntryDate AS DATE)=CAST(GETDATE() AS DATE) ORDER BY POSSessionActivityId DESC; " +
            "IF @PosID IS NULL BEGIN INSERT INTO POSSessionActivity (POSActivityType,Amount,CounterId,CompanyBranchId,UserId,DataEntryDate,DataEntryStatus,Remarks,RecordLockStatus,TransactionNumber) VALUES (6,0,@CounterID,@BranchID,@UserID,GETDATE(),1,'Day Start',0,0); SET @PosID = SCOPE_IDENTITY(); END " +

            "DECLARE @InvNum INT = (SELECT ISNULL(MAX(InvoiceNumber),0)+1 FROM SaleInvoice); " +
            "INSERT INTO SaleInvoice (InvoiceNumber,InvoiceDate,NetAmount,CompanyBranchId,UserId,CounterId,ClientInformationId,CreditPeriod,DataEntryDate,DataEntryStatus,Remarks,NotePaid,InvoiceDiscount,NetItemDiscount,RoundOffValue,ReferenceNumber,AccountsPost,SaleTypeId,POSSessionActivityId,RawFilter,RecordLockStatus,SlipNumber,NetOtherCharges,DeliveryPersonId,SalesmanId,CurrencyRateId,CurrencyRate,CashBack) " +
            "VALUES (@InvNum,GETDATE(),(@Qty*@Rate),@BranchID,@UserID,@CounterID,@ClientID,0,GETDATE(),1,'UNPACK UPDATE',(@Qty*@Rate),0,0,0,'',1,@SaleTypeID,@PosID,0,0,'',0,NULL,NULL,NULL,1.00,0.00); " +
            "DECLARE @SID INT = SCOPE_IDENTITY(); " +

            "INSERT INTO SaleInvoiceItem (SaleInvoiceId,ProductItemId,Quantity,Price,SaleRate,DataEntryDate,UserId,ItemStatus,CostPrice,Barcode,DiscountRate,RowSortId) " +
            "VALUES (@SID,@PID,@Qty,@Rate,@Rate,GETDATE(),@UserID,1,@CostPrice,@Bar,0,1); " +
            "DECLARE @IID INT = SCOPE_IDENTITY(); " +

            "INSERT INTO SaleInvoicePaymentMode (SaleInvoiceId,PaymentModeId,PaymentModeReferanceId,PaymentModeAmount) VALUES (@SID,@PayModeID,0,(@Qty*@Rate)); " +
            "INSERT INTO COGS (TransactionDate,ProductItemId,SaleInvoiceItemId,Quantity,CostRate,OutRate,CompanyBranchId,UserId,DataEntryDate,RawFilter) VALUES (GETDATE(),@PID,@IID,@Qty,@CostPrice,@Rate,@BranchID,@UserID,GETDATE(),0); " +

            "DECLARE @Bal DECIMAL(18,3) = ISNULL((SELECT TOP 1 BalanceQuantity FROM InventoryTracking WHERE ProductItemId=@PID ORDER BY InventoryTrackingId DESC),0); " +
            "INSERT INTO InventoryTracking (ProductItemId,Quantity,Rate,InventoryType,ReferanceType,BalanceQuantity,SaleInvoiceItemId,CompanyBranchId,DataEntryDate,DataEntryStatus) VALUES (@PID,(@Qty*-1),@CostPrice,2,2,(@Bal-@Qty),@IID,@BranchID,GETDATE(),1); " +

            "IF EXISTS (SELECT 1 FROM DailySale WHERE CAST(StartDate AS DATE)=CAST(GETDATE() AS DATE) AND CompanyBranchId=@BranchID) " +
            "BEGIN UPDATE DailySale SET InvoiceCount=InvoiceCount+1, NetAmount=NetAmount+(@Qty*@Rate), DataEntryDate=GETDATE() WHERE CAST(StartDate AS DATE)=CAST(GETDATE() AS DATE) AND CompanyBranchId=@BranchID; END " +
            "ELSE BEGIN INSERT INTO DailySale (TransactionDate,StartDate,EndDate,TransactionTypeId,CompanyBranchId,ClientInformationId,SalesmanId,PaymentModeIds,SaleTypeId,CounterId,InvoiceCount,NetAmount,InvoiceDiscount,NetItemDiscount,RoundOffValue,NetOtherCharges,DinePersons,DataEntryDate,DataEntryStatus) VALUES (GETDATE(),GETDATE(),GETDATE(),1,@BranchID,@ClientID,NULL,CAST(@PayModeID AS VARCHAR),@SaleTypeID,@CounterID,1,(@Qty*@Rate),0,0,0,0,0,GETDATE(),1); END " +

            "COMMIT TRANSACTION; " +
            "SET NOCOUNT OFF; SELECT 'SALE_DONE' AS S, @SID AS InvID, @InvNum AS InvNo, @PName AS Product, @Qty AS Qty, @Rate AS Rate, GETDATE() AS Time; " +
            "END TRY " +
            "BEGIN CATCH IF @@TRANCOUNT > 0 ROLLBACK; SET NOCOUNT OFF; SELECT 'ERROR' AS S, ERROR_MESSAGE() AS M; END CATCH";

        try (Connection conn = DriverManager.getConnection(app.url, app.user, app.pass);
             Statement stmt = conn.createStatement()) {
            boolean hasResult = stmt.execute(sql);
            String status = "";
            while (true) {
                if (hasResult) { try { ResultSet rs = stmt.getResultSet(); if (rs != null && rs.next()) { String s = rs.getString(1); if (s != null) status = s; } } catch (Exception ignored) {} }
                try { hasResult = stmt.getMoreResults(); if (!hasResult && stmt.getUpdateCount() == -1) break; } catch (Exception e) { break; }
            }
            return status.equals("SALE_DONE");
        } catch (Exception ex) {
            logError("SQL Error in executeUnpackDeduction", ex);
            return false;
        }
    }
}
