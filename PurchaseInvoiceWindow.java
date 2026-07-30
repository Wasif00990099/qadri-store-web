import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.nio.file.Files;

public class PurchaseInvoiceWindow extends JFrame {

    private String dbUrl;
    private String dbUser;
    private String dbPass;

    private JComboBox<String> comboVendor;
    private FilterableComboBoxModel vendorModel;
    private List<String> dbVendors;

    private JTable tableItems;
    private DefaultTableModel tableModel;
    private JButton btnSelectFile;
    private JButton btnStartImport;

    private JTextField txtInvoiceNumber;
    private JButton btnExport;
    private JButton btnGetRecords;

    private JLabel lblStatus;

    private File selectedFile;

    private boolean adjusting = false;

    private final byte[] ENCRYPTION_KEY = "QadriStoreSecretKey2025".getBytes();

    public PurchaseInvoiceWindow(Frame owner, String url, String user, String pass) {
        super("Import / Export Purchase Data");
        this.dbUrl = url;
        this.dbUser = user;
        this.dbPass = pass;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        Color bgColor = new Color(240, 240, 240);
        getContentPane().setBackground(bgColor);

        setLayout(new BorderLayout(10, 10));

        loadAndApplyLogo();

        JPanel topMainPanel = new JPanel(new BorderLayout(15, 10));
        topMainPanel.setBackground(Color.WHITE);
        topMainPanel.setBorder(new EmptyBorder(10, 10, 0, 10));

        JPanel vendorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        vendorPanel.setBackground(Color.WHITE);
        JLabel lblVendor = new JLabel("Vendor Name:");
        lblVendor.setFont(new Font("Segoe UI", Font.BOLD, 14));

        dbVendors = loadVendorsFromDB();
        vendorModel = new FilterableComboBoxModel(dbVendors);
        comboVendor = new JComboBox<>(vendorModel);
        comboVendor.setEditable(true);
        comboVendor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        comboVendor.setPreferredSize(new Dimension(250, 30));

        JTextComponent editorComp = (JTextComponent) comboVendor.getEditor().getEditorComponent();
        editorComp.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateComboFilter(); }
            public void removeUpdate(DocumentEvent e) { updateComboFilter(); }
            public void changedUpdate(DocumentEvent e) { updateComboFilter(); }
        });

        btnGetRecords = new JButton("GET RECORDS");
        btnGetRecords.setBackground(new Color(100, 0, 150));
        btnGetRecords.setForeground(Color.WHITE);
        btnGetRecords.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGetRecords.setOpaque(true);
        btnGetRecords.setBorderPainted(false);
        btnGetRecords.setCursor(new Cursor(Cursor.HAND_CURSOR));

        vendorPanel.add(lblVendor);
        vendorPanel.add(comboVendor);
        vendorPanel.add(btnGetRecords);

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        exportPanel.setBackground(Color.WHITE);

        JLabel lblInvoice = new JLabel("Invoice No (Export):");
        lblInvoice.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtInvoiceNumber = new JTextField(12);
        txtInvoiceNumber.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInvoiceNumber.setBorder(BorderFactory.createCompoundBorder(
            txtInvoiceNumber.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        txtInvoiceNumber.setToolTipText("Leave empty to export ALL invoices");

        btnExport = new JButton("EXPORT TO EXCEL");
        btnExport.setBackground(new Color(0, 128, 0));
        btnExport.setForeground(Color.WHITE);
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnExport.setOpaque(true);
        btnExport.setBorderPainted(false);
        btnExport.setCursor(new Cursor(Cursor.HAND_CURSOR));

        exportPanel.add(lblInvoice);
        exportPanel.add(txtInvoiceNumber);
        exportPanel.add(btnExport);

        topMainPanel.add(vendorPanel, BorderLayout.WEST);
        topMainPanel.add(exportPanel, BorderLayout.EAST);
        add(topMainPanel, BorderLayout.NORTH);

        String[] columns = {"Item Name", "Qty", "Purchase Price", "Retail Price"};
        tableModel = new DefaultTableModel(columns, 0);
        tableItems = new JTable(tableModel);
        tableItems.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableItems.setRowHeight(25);
        tableItems.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(tableItems);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomMainPanel = new JPanel(new BorderLayout());
        bottomMainPanel.setBackground(bgColor);
        bottomMainPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(bgColor);

        btnSelectFile = new JButton("IMPORT FILE");
        btnSelectFile.setBackground(new Color(0, 70, 180));
        btnSelectFile.setForeground(Color.WHITE);
        btnSelectFile.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSelectFile.setOpaque(true);
        btnSelectFile.setBorderPainted(false);
        btnSelectFile.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnStartImport = new JButton("START IMPORT TO DB");
        btnStartImport.setBackground(new Color(180, 100, 0));
        btnStartImport.setForeground(Color.WHITE);
        btnStartImport.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStartImport.setOpaque(true);
        btnStartImport.setBorderPainted(false);
        btnStartImport.setEnabled(false);
        btnStartImport.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnPanel.add(btnSelectFile);
        btnPanel.add(btnStartImport);

        lblStatus = new JLabel(" Pehle Vendor select karein, phir File Import karein ya Export karein.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblStatus.setForeground(new Color(80, 80, 80));
        lblStatus.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 0, 0, 0)
        ));

        bottomMainPanel.add(btnPanel, BorderLayout.NORTH);
        bottomMainPanel.add(lblStatus, BorderLayout.SOUTH);
        add(bottomMainPanel, BorderLayout.SOUTH);

        btnExport.addActionListener(e -> exportDataToExcel());
        btnSelectFile.addActionListener(e -> selectAndReadFile());
        btnStartImport.addActionListener(e -> startDatabaseImport());

        btnGetRecords.addActionListener(e -> {
            RecordViewerDialog dialog = new RecordViewerDialog(this, dbUrl, dbUser, dbPass);
            dialog.setVisible(true);
        });
    }

    private void loadAndApplyLogo() {
        String logoConfigFile = "QadriStore_Logo.cfg";
        File f = new File(logoConfigFile);
        if (f.exists()) {
            try {
                String encryptedContent = new String(Files.readAllBytes(f.toPath()));
                String decryptedContent = decrypt(encryptedContent.trim());
                if (!decryptedContent.isEmpty()) {
                    File logoFile = new File(decryptedContent);
                    if (logoFile.exists()) {
                        try {
                            Image img = ImageIO.read(logoFile);
                            if (img != null) {
                                setIconImage(img);
                            }
                        } catch (Exception e) {}
                    }
                }
            } catch (Exception e) {}
        }
    }

    private String decrypt(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            byte[] decrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) decrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) { return ""; }
    }

    private void updateComboFilter() {
        if (adjusting) return;

        SwingUtilities.invokeLater(() -> {
            adjusting = true;
            JTextComponent editor = (JTextComponent) comboVendor.getEditor().getEditorComponent();
            String text = editor.getText().trim();

            vendorModel.filter(text);
            comboVendor.setSelectedItem(null);

            editor.setText(text);
            editor.setCaretPosition(text.length());

            if (text.isEmpty() || vendorModel.getSize() == 0) {
                comboVendor.hidePopup();
            } else {
                comboVendor.showPopup();
            }
            adjusting = false;
        });
    }

    class FilterableComboBoxModel extends AbstractListModel<String> implements MutableComboBoxModel<String> {
        private List<String> items;
        private List<String> filteredItems;
        private String selectedItem;

        public FilterableComboBoxModel(List<String> items) {
            this.items = new ArrayList<>(items);
            this.filteredItems = new ArrayList<>(items);
        }

        public void filter(String text) {
            filteredItems.clear();
            String lowerText = text.toLowerCase();
            for (String item : items) {
                if (item.toLowerCase().contains(lowerText)) {
                    filteredItems.add(item);
                }
            }
            fireContentsChanged(this, 0, filteredItems.size() - 1);
        }

        @Override public int getSize() { return filteredItems.size(); }
        @Override public String getElementAt(int index) { return filteredItems.get(index); }
        @Override public void setSelectedItem(Object anItem) { selectedItem = (String) anItem; fireContentsChanged(this, -1, -1); }
        @Override public Object getSelectedItem() { return selectedItem; }
        @Override public void addElement(String item) { items.add(item); filter(""); }
        @Override public void removeElement(Object obj) { items.remove(obj); filter(""); }
        @Override public void insertElementAt(String item, int index) { items.add(index, item); filter(""); }
        @Override public void removeElementAt(int index) { items.remove(index); filter(""); }
    }

    private List<String> loadVendorsFromDB() {
        List<String> vendors = new ArrayList<>();
        String sql = "SELECT Name FROM ProductVendor ORDER BY Name";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vendors.add(rs.getString("Name"));
            }
        } catch (Exception ex) {
            System.err.println("Vendor load error: " + ex.getMessage());
        }
        return vendors;
    }

    private Set<String> loadItemNamesFromDB() {
        Set<String> itemNames = new TreeSet<>();
        String sql = "SELECT ShortName, LongName FROM ProductItem";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String shortName = rs.getString("ShortName");
                String longName = rs.getString("LongName");
                if (shortName != null && !shortName.trim().isEmpty()) itemNames.add(shortName.trim());
                if (longName != null && !longName.trim().isEmpty()) itemNames.add(longName.trim());
            }
        } catch (Exception ex) {
            System.err.println("Item names load error: " + ex.getMessage());
        }
        return itemNames;
    }

    private void exportDataToExcel() {
        String invoiceNum = txtInvoiceNumber.getText().trim();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");
        queryBuilder.append("si.InvoiceNumber, si.InvoiceDate, pi.LongName AS ItemName, sii.Barcode, ");
        queryBuilder.append("sii.Quantity, sii.Price, sii.DiscountRate, (sii.Quantity * sii.Price) AS TotalAmount ");
        queryBuilder.append("FROM SaleInvoice si ");
        queryBuilder.append("INNER JOIN SaleInvoiceItem sii ON si.SaleInvoiceId = sii.SaleInvoiceId ");
        queryBuilder.append("INNER JOIN ProductItem pi ON sii.ProductItemId = pi.ProductItemId ");

        if (!invoiceNum.isEmpty()) {
            queryBuilder.append("WHERE si.InvoiceNumber = '").append(invoiceNum).append("'");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Excel File");
        String defaultFileName = invoiceNum.isEmpty() ? "All_Invoices_Export.xlsx" : "Invoice_" + invoiceNum + ".xlsx";
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File tempFile = fileChooser.getSelectedFile();
            final File fileToSave;
            if (!tempFile.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(tempFile.getParentFile(), tempFile.getName() + ".xlsx");
            } else {
                fileToSave = tempFile;
            }

            btnExport.setEnabled(false); btnSelectFile.setEnabled(false); btnStartImport.setEnabled(false);
            lblStatus.setText(" Exporting data... Please wait.");
            lblStatus.setForeground(Color.BLUE);

            SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(queryBuilder.toString())) {
                        writeXlsx(fileToSave, rs);
                        publish("Export successful! Saved to: " + fileToSave.getName());
                    } catch (Exception ex) {
                        publish("Export Error: " + ex.getMessage());
                    }
                    return null;
                }
                @Override
                protected void process(java.util.List<String> chunks) {
                    lblStatus.setText(chunks.get(chunks.size() - 1));
                    lblStatus.setForeground(new Color(0, 128, 0));
                    btnExport.setEnabled(true); btnSelectFile.setEnabled(true);
                    if(selectedFile != null) btnStartImport.setEnabled(true);
                }
            };
            worker.execute();
        }
    }

    private void writeXlsx(File fileToSave, ResultSet rs) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileToSave))) {
            writeZipEntry(zos, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n</Types>");
            writeZipEntry(zos, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n</Relationships>");
            writeZipEntry(zos, "xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n  <sheets>\n    <sheet name=\"Invoice Data\" sheetId=\"1\" r:id=\"rId1\"/>\n  </sheets>\n</workbook>");
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n</Relationships>");

            zos.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            StringBuilder sheetXml = new StringBuilder();
            sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n<sheetData>\n");

            int rowNum = 1;
            sheetXml.append("<row r=\"").append(rowNum).append("\">");
            String[] headers = {"InvoiceNumber", "InvoiceDate", "ItemName", "Barcode", "Quantity", "Price", "DiscountRate", "TotalAmount"};
            for (int i = 0; i < headers.length; i++) {
                sheetXml.append("<c r=\"").append(getColumnName(i + 1)).append(rowNum).append("\" t=\"inlineStr\"><is><t>").append(escapeXml(headers[i])).append("</t></is></c>");
            }
            sheetXml.append("</row>\n");

            while (rs.next()) {
                rowNum++;
                sheetXml.append("<row r=\"").append(rowNum).append("\">");
                String invNum = rs.getString("InvoiceNumber");
                Date invDate = rs.getDate("InvoiceDate");
                String itemName = rs.getString("ItemName");
                String barcode = rs.getString("Barcode");
                double qty = rs.getDouble("Quantity");
                double price = rs.getDouble("Price");
                double disc = rs.getDouble("DiscountRate");
                double total = rs.getDouble("TotalAmount");

                String dateStr = (invDate != null) ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(invDate) : "";
                sheetXml.append(createCellStringXml(1, rowNum, invNum));
                sheetXml.append(createCellStringXml(2, rowNum, dateStr));
                sheetXml.append(createCellStringXml(3, rowNum, itemName));
                sheetXml.append(createCellStringXml(4, rowNum, barcode));
                sheetXml.append(createCellNumberXml(5, rowNum, qty));
                sheetXml.append(createCellNumberXml(6, rowNum, price));
                sheetXml.append(createCellNumberXml(7, rowNum, disc));
                sheetXml.append(createCellNumberXml(8, rowNum, total));
                sheetXml.append("</row>\n");

                if (rowNum % 1000 == 0) {
                    zos.write(sheetXml.toString().getBytes("UTF-8"));
                    sheetXml.setLength(0);
                }
            }
            sheetXml.append("</sheetData>\n</worksheet>");
            zos.write(sheetXml.toString().getBytes("UTF-8"));
            zos.closeEntry();
        }
    }

    private void writeZipEntry(ZipOutputStream zos, String entryName, String content) throws Exception {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private String createCellStringXml(int col, int row, String value) {
        if (value == null) value = "";
        return "<c r=\"" + getColumnName(col) + row + "\" t=\"inlineStr\"><is><t>" + escapeXml(value) + "</t></is></c>";
    }

    private String createCellNumberXml(int col, int row, double value) {
        return "<c r=\"" + getColumnName(col) + row + "\"><v>" + value + "</v></c>";
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String getColumnName(int index) {
        StringBuilder sb = new StringBuilder();
        while (index > 0) {
            index--;
            char c = (char) ('A' + (index % 26));
            sb.append(c);
            index /= 26;
        }
        return sb.reverse().toString();
    }

    private List<String[]> parseXlsx(File xlsxFile) throws Exception {
        List<String[]> data = new ArrayList<>();
        java.util.Map<Integer, String> sharedStrings = new java.util.HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(xlsxFile))) {
            ZipEntry entry;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("xl/sharedStrings.xml")) {
                    Document doc = builder.parse(new NonClosingInputStream(zis));
                    NodeList siNodes = doc.getElementsByTagName("si");
                    for (int i = 0; i < siNodes.getLength(); i++) {
                        Element si = (Element) siNodes.item(i);
                        NodeList tNodes = si.getElementsByTagName("t");
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < tNodes.getLength(); j++) {
                            sb.append(tNodes.item(j).getTextContent());
                        }
                        sharedStrings.put(i, sb.toString());
                    }
                } else if (entry.getName().equals("xl/worksheets/sheet1.xml")) {
                    Document doc = builder.parse(new NonClosingInputStream(zis));
                    NodeList rowNodes = doc.getElementsByTagName("row");

                    for (int i = 0; i < rowNodes.getLength(); i++) {
                        Element row = (Element) rowNodes.item(i);
                        NodeList cNodes = row.getElementsByTagName("c");
                        String[] rowData = new String[20];
                        java.util.Arrays.fill(rowData, "");

                        for (int j = 0; j < cNodes.getLength(); j++) {
                            Element c = (Element) cNodes.item(j);
                            String r = c.getAttribute("r");
                            int colIndex = getColumnIndex(r);
                            if (colIndex >= 20) continue;

                            String type = c.getAttribute("t");
                            String value = "";

                            if ("s".equals(type)) {
                                NodeList vNodes = c.getElementsByTagName("v");
                                if (vNodes.getLength() > 0) {
                                    int sIdx = Integer.parseInt(vNodes.item(0).getTextContent());
                                    value = sharedStrings.getOrDefault(sIdx, "");
                                }
                            } else if ("inlineStr".equals(type)) {
                                NodeList isNodes = c.getElementsByTagName("is");
                                if (isNodes.getLength() > 0) {
                                    NodeList tNodes = ((Element) isNodes.item(0)).getElementsByTagName("t");
                                    if (tNodes.getLength() > 0) value = tNodes.item(0).getTextContent();
                                }
                            } else {
                                NodeList vNodes = c.getElementsByTagName("v");
                                if (vNodes.getLength() > 0) {
                                    value = vNodes.item(0).getTextContent();
                                }
                            }
                            rowData[colIndex] = value;
                        }
                        data.add(rowData);
                    }
                }
            }
        }
        return data;
    }

    class NonClosingInputStream extends java.io.FilterInputStream {
        NonClosingInputStream(InputStream in) { super(in); }
        @Override public void close() throws IOException { /* Do nothing */ }
    }

    private int getColumnIndex(String cellRef) {
        StringBuilder letters = new StringBuilder();
        for (char c : cellRef.toCharArray()) {
            if (Character.isLetter(c)) letters.append(c);
            else break;
        }
        String colStr = letters.toString();
        int index = 0;
        for (int i = 0; i < colStr.length(); i++) {
            index *= 26;
            index += (colStr.charAt(i) - 'A' + 1);
        }
        return index - 1;
    }

    private void selectAndReadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Excel File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files (.xlsx)", "xlsx"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            lblStatus.setText(" File read ho rahi hai...");
            lblStatus.setForeground(Color.BLUE);

            try {
                List<String[]> parsedData = parseXlsx(selectedFile);
                tableModel.setRowCount(0);

                if (parsedData.isEmpty()) {
                    lblStatus.setText(" File mein koi data nahi mila.");
                    lblStatus.setForeground(Color.RED);
                    return;
                }

                String[] headers = parsedData.get(0);
                for (int i = 0; i < headers.length; i++) {
                    if(headers[i] != null) headers[i] = headers[i].trim().toLowerCase().replace(" ", "").replace("_", "");
                    else headers[i] = "";
                }

                int colItemName = -1;
                int colQty = -1;
                int colPrice = -1;

                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i];
                    if(h.contains("itemname") || h.equals("item") || h.contains("longname")) colItemName = i;
                    else if(h.contains("quantity") || h.equals("qty")) colQty = i;
                    else if(h.contains("price")) colPrice = i;
                }

                if(colItemName == -1) colItemName = 0;
                if(colQty == -1) colQty = 2;
                if(colPrice == -1) colPrice = 3;

                int importCount = 0;

                for (int i = 1; i < parsedData.size(); i++) {
                    String[] row = parsedData.get(i);

                    String itemName = (colItemName < row.length && row[colItemName] != null) ? row[colItemName].trim() : "";
                    String qty = (colQty < row.length && row[colQty] != null) ? row[colQty].trim().replace(",", "") : "0";
                    String priceVal = (colPrice < row.length && row[colPrice] != null) ? row[colPrice].trim().replace(",", "") : "0";

                    if(itemName.isEmpty()) continue;

                    tableModel.addRow(new Object[]{itemName, qty, priceVal, priceVal});
                    importCount++;
                }

                btnStartImport.setEnabled(true);
                lblStatus.setText(" File ready! " + importCount + " items table mein load ho gaye.");
                lblStatus.setForeground(new Color(0, 128, 0));

            } catch (Exception ex) {
                lblStatus.setText(" File read error: " + ex.getMessage());
                lblStatus.setForeground(Color.RED);
            }
        }
    }

    private void startDatabaseImport() {
        JTextComponent editor = (JTextComponent) comboVendor.getEditor().getEditorComponent();
        String vendorName = editor.getText().trim();

        if (vendorName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vendor Name select karein!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String matchedVendorName = null;
        for (String v : dbVendors) {
            if (v.equalsIgnoreCase(vendorName)) {
                matchedVendorName = v;
                break;
            }
        }

        if (matchedVendorName == null) {
            JOptionPane.showMessageDialog(this, "Ye Vendor DB mein nahi mila! List se select karein.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final String finalVendorName = matchedVendorName;

        List<String[]> tableData = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String itemName = tableModel.getValueAt(i, 0) != null ? tableModel.getValueAt(i, 0).toString().trim() : "";
            String qtyStr = tableModel.getValueAt(i, 1) != null ? tableModel.getValueAt(i, 1).toString().trim().replace(",", "") : "0";
            String purchasePriceStr = tableModel.getValueAt(i, 2) != null ? tableModel.getValueAt(i, 2).toString().trim().replace(",", "") : "0";
            String retailPriceStr = tableModel.getValueAt(i, 3) != null ? tableModel.getValueAt(i, 3).toString().trim().replace(",", "") : "0";
            tableData.add(new String[]{itemName, qtyStr, purchasePriceStr, retailPriceStr});
        }

        Set<String> dbItemNames = loadItemNamesFromDB();
        List<String> mismatchedItems = new ArrayList<>();
        List<String[]> validDataToImport = new ArrayList<>();

        for (String[] row : tableData) {
            String itemName = row[0];
            if (itemName.isEmpty()) continue;

            if (dbItemNames.contains(itemName)) {
                validDataToImport.add(row);
            } else {
                mismatchedItems.add(itemName);
            }
        }

        if (!mismatchedItems.isEmpty()) {
            StringBuilder listBuilder = new StringBuilder();
            for (String item : mismatchedItems) {
                listBuilder.append("• ").append(item).append(" (DB mein nahi hai)\n");
            }

            JTextArea textArea = new JTextArea(listBuilder.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            textArea.setBackground(new Color(255, 240, 240));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(420, 240));

            JPanel panel = new JPanel(new BorderLayout(0, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(new JLabel("Ye exact item names database mein nahi mile:"), BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(new JLabel("Yes: Skip karke baaki items import karo | No: Pura import cancel karo"), BorderLayout.SOUTH);

            int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Items NOT Found in Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (option == JOptionPane.NO_OPTION || option == JOptionPane.CLOSED_OPTION) {
                lblStatus.setText(" Import cancel ho gaya.");
                lblStatus.setForeground(Color.RED);
                return;
            }
        }

        if (validDataToImport.isEmpty()) {
            lblStatus.setText(" Koi valid item import ke liye nahi mila.");
            lblStatus.setForeground(Color.RED);
            return;
        }

        btnSelectFile.setEnabled(false);
        btnStartImport.setEnabled(false);
        btnExport.setEnabled(false);
        lblStatus.setText(" Database mein import ho raha hai...");
        lblStatus.setForeground(Color.BLUE);

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            int successCount = 0;
            int failCount = mismatchedItems.size();

            @Override
            protected Void doInBackground() throws Exception {
                Connection conn = null;
                try {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                    conn.setAutoCommit(false);

                    int vendorId = -1;
                    try (PreparedStatement pvStmt = conn.prepareStatement("SELECT ProductVendorId FROM ProductVendor WHERE Name = ?")) {
                        pvStmt.setString(1, finalVendorName);
                        ResultSet pvRs = pvStmt.executeQuery();
                        if (pvRs.next()) vendorId = pvRs.getInt(1);
                    }
                    if (vendorId == -1) throw new Exception("Vendor ID nahi mila");

                    String yearPrefix = "PI" + java.time.LocalDate.now().getYear();
                    int maxInvNum = 1;
                    try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT ISNULL(MAX(TRY_CAST(REPLACE(TransactionNumber, '" + yearPrefix + "', '') AS INT)), 0) + 1 FROM PurchaseInvoice WHERE TransactionNumber LIKE '" + yearPrefix + "%'")) {
                        if (rs.next()) maxInvNum = rs.getInt(1);
                    }
                    String transNum = yearPrefix + maxInvNum;

                    int newInvoiceId = -1;
                    String insertInvSql = "INSERT INTO PurchaseInvoice (InvoiceNumber, TransactionNumber, PurchaseInvoiceDate, ProductVendorId, NetAmount, DataEntryDate, DataEntryStatus, UserId, DataEntryBranchId, PaymentModeId) " +
                                          "VALUES (?, ?, GETDATE(), ?, 0, GETDATE(), 1, 1, 1, 1)";
                    try (PreparedStatement invStmt = conn.prepareStatement(insertInvSql, Statement.RETURN_GENERATED_KEYS)) {
                        invStmt.setInt(1, maxInvNum);
                        invStmt.setString(2, transNum);
                        invStmt.setInt(3, vendorId);
                        invStmt.executeUpdate();
                        try (ResultSet generatedKeys = invStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                newInvoiceId = generatedKeys.getInt(1);
                            } else {
                                throw new Exception("Invoice ID generate nahi hui");
                            }
                        }
                    }

                    double totalNetAmount = 0;
                    String insertItemSql = "INSERT INTO PurchaseInvoiceItem (PurchaseInvoiceId, ProductItemId, Quantity, Price, TradePrice, SaleRate, ItemStatus) VALUES (?, ?, ?, ?, ?, ?, 1)";
                    String updateStockSql = "UPDATE StockData SET Inventory = ISNULL(Inventory, 0) + ? WHERE Barcode = ?";

                    try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql);
                         PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {

                        for (String[] row : validDataToImport) {
                            String itemName = row[0];
                            if (itemName.isEmpty()) continue;

                            double qty = Double.parseDouble(row[1].isEmpty() ? "0" : row[1]);
                            double purchasePrice = Double.parseDouble(row[2].isEmpty() ? "0" : row[2]);
                            double retailPrice = Double.parseDouble(row[3].isEmpty() ? "0" : row[3]);

                            int productId = -1;
                            String barcode = "";
                            try (PreparedStatement pStmt = conn.prepareStatement("SELECT ProductItemId, ISNULL(Barcode, '') FROM ProductItem WHERE ShortName = ? OR LongName = ?")) {
                                pStmt.setString(1, itemName);
                                pStmt.setString(2, itemName);
                                try (ResultSet pRs = pStmt.executeQuery()) {
                                    if (pRs.next()) {
                                        productId = pRs.getInt(1);
                                        barcode = pRs.getString(2);
                                    }
                                }
                            }

                            if (productId == -1) {
                                failCount++;
                                continue;
                            }

                            itemStmt.setInt(1, newInvoiceId);
                            itemStmt.setInt(2, productId);
                            itemStmt.setDouble(3, qty);
                            itemStmt.setDouble(4, purchasePrice);
                            itemStmt.setDouble(5, purchasePrice);
                            itemStmt.setDouble(6, retailPrice);
                            itemStmt.addBatch();

                            if (!barcode.isEmpty()) {
                                stockStmt.setDouble(1, qty);
                                stockStmt.setString(2, barcode);
                                stockStmt.addBatch();
                            }

                            totalNetAmount += (qty * purchasePrice);
                            successCount++;
                            publish("Importing... Success: " + successCount + " | Failed/Skipped: " + failCount);
                        }

                        itemStmt.executeBatch();
                        stockStmt.executeBatch();
                    }

                    try (PreparedStatement updateInvStmt = conn.prepareStatement("UPDATE PurchaseInvoice SET NetAmount = ? WHERE PurchaseInvoiceId = ?")) {
                        updateInvStmt.setDouble(1, totalNetAmount);
                        updateInvStmt.setInt(2, newInvoiceId);
                        updateInvStmt.executeUpdate();
                    }

                    conn.commit();
                    publish("Import Complete! Success: " + successCount + " | Failed/Skipped: " + failCount);

                } catch (Exception ex) {
                    if (conn != null) try { conn.rollback(); } catch (SQLException e) {}
                    publish("DB Error: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                lblStatus.setText(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                btnSelectFile.setEnabled(true);
                btnStartImport.setEnabled(true);
                btnExport.setEnabled(true);
                lblStatus.setForeground(new Color(0, 128, 0));
            }
        };
        worker.execute();
    }

    class RecordViewerDialog extends JDialog {
        private JTable invoiceTable;
        private DefaultTableModel invoiceTableModel;
        private JTable itemsTable;
        private DefaultTableModel itemsTableModel;
        private JLabel lblRecordStatus;

        private String dbUrl, dbUser, dbPass;

        public RecordViewerDialog(Frame owner, String url, String user, String pass) {
            super(owner, "Purchase Records - View / Edit / Delete", true);
            this.dbUrl = url;
            this.dbUser = user;
            this.dbPass = pass;

            setSize(900, 600);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            Color bgColor = new Color(245, 245, 245);
            getContentPane().setBackground(bgColor);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBorder(BorderFactory.createTitledBorder("Imported Purchase Invoices (1 Row = 1 Import)"));
            String[] invCols = {"Invoice ID", "Transaction No", "Date", "Vendor", "Net Amount"};
            invoiceTableModel = new DefaultTableModel(invCols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            invoiceTable = new JTable(invoiceTableModel);
            invoiceTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            invoiceTable.setRowHeight(22);
            JScrollPane invScroll = new JScrollPane(invoiceTable);
            invScroll.setPreferredSize(new Dimension(850, 200));
            topPanel.add(invScroll, BorderLayout.CENTER);
            add(topPanel, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setBorder(BorderFactory.createTitledBorder("Items inside selected Invoice"));
            String[] itemCols = {"Item ID (Hidden)", "Product ID (Hidden)", "Barcode (Hidden)", "Item Name", "Qty", "Price", "Total"};
            itemsTableModel = new DefaultTableModel(itemCols, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            itemsTable = new JTable(itemsTableModel);
            itemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            itemsTable.setRowHeight(22);
            itemsTable.getColumnModel().getColumn(0).setMinWidth(0);
            itemsTable.getColumnModel().getColumn(0).setMaxWidth(0);
            itemsTable.getColumnModel().getColumn(1).setMinWidth(0);
            itemsTable.getColumnModel().getColumn(1).setMaxWidth(0);
            itemsTable.getColumnModel().getColumn(2).setMinWidth(0);
            itemsTable.getColumnModel().getColumn(2).setMaxWidth(0);

            JScrollPane itemScroll = new JScrollPane(itemsTable);
            centerPanel.add(itemScroll, BorderLayout.CENTER);
            add(centerPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
            bottomPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

            JButton btnEditQty = new JButton("EDIT QTY");
            btnEditQty.setBackground(new Color(0, 120, 180));
            btnEditQty.setForeground(Color.WHITE);
            btnEditQty.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnEditQty.setOpaque(true);
            btnEditQty.setBorderPainted(false);

            JButton btnDeleteItem = new JButton("DELETE ITEM");
            btnDeleteItem.setBackground(new Color(200, 50, 50));
            btnDeleteItem.setForeground(Color.WHITE);
            btnDeleteItem.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnDeleteItem.setOpaque(true);
            btnDeleteItem.setBorderPainted(false);

            JButton btnRefresh = new JButton("REFRESH");
            btnRefresh.setBackground(new Color(100, 100, 100));
            btnRefresh.setForeground(Color.WHITE);
            btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnRefresh.setOpaque(true);
            btnRefresh.setBorderPainted(false);

            btnPanel.add(btnEditQty);
            btnPanel.add(btnDeleteItem);
            btnPanel.add(btnRefresh);

            lblRecordStatus = new JLabel("Select an invoice to view items.");
            lblRecordStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblRecordStatus.setForeground(new Color(80, 80, 80));

            bottomPanel.add(btnPanel, BorderLayout.NORTH);
            bottomPanel.add(lblRecordStatus, BorderLayout.SOUTH);
            add(bottomPanel, BorderLayout.SOUTH);

            invoiceTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && invoiceTable.getSelectedRow() != -1) {
                    loadItemsForInvoice();
                }
            });

            btnEditQty.addActionListener(e -> editItemQuantity());
            btnDeleteItem.addActionListener(e -> deleteSelectedItem());
            btnRefresh.addActionListener(e -> loadInvoices());

            loadInvoices();
        }

        private void loadInvoices() {
            invoiceTableModel.setRowCount(0);
            itemsTableModel.setRowCount(0);
            String sql = "SELECT pi.PurchaseInvoiceId, pi.TransactionNumber, pi.PurchaseInvoiceDate, pv.Name, pi.NetAmount " +
                         "FROM PurchaseInvoice pi " +
                         "INNER JOIN ProductVendor pv ON pi.ProductVendorId = pv.ProductVendorId " +
                         "ORDER BY pi.PurchaseInvoiceId DESC";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    invoiceTableModel.addRow(new Object[]{
                        rs.getInt("PurchaseInvoiceId"),
                        rs.getString("TransactionNumber"),
                        rs.getString("PurchaseInvoiceDate"),
                        rs.getString("Name"),
                        rs.getDouble("NetAmount")
                    });
                }
                if(invoiceTableModel.getRowCount() > 0) {
                    invoiceTable.setRowSelectionInterval(0, 0);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading invoices: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void loadItemsForInvoice() {
            itemsTableModel.setRowCount(0);
            int selectedRow = invoiceTable.getSelectedRow();
            if (selectedRow == -1) return;

            int invoiceId = (int) invoiceTableModel.getValueAt(selectedRow, 0);
            String sql = "SELECT pii.PurchaseInvoiceItemId, pii.ProductItemId, pi.Barcode, pi.LongName, pii.Quantity, pii.Price " +
                         "FROM PurchaseInvoiceItem pii " +
                         "INNER JOIN ProductItem pi ON pii.ProductItemId = pi.ProductItemId " +
                         "WHERE pii.PurchaseInvoiceId = " + invoiceId;
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    double qty = rs.getDouble("Quantity");
                    double price = rs.getDouble("Price");
                    itemsTableModel.addRow(new Object[]{
                        rs.getInt("PurchaseInvoiceItemId"),
                        rs.getInt("ProductItemId"),
                        rs.getString("Barcode"),
                        rs.getString("LongName"),
                        qty,
                        price,
                        qty * price
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading items: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void editItemQuantity() {
            int itemRow = itemsTable.getSelectedRow();
            if (itemRow == -1) {
                JOptionPane.showMessageDialog(this, "Pehle item select karein!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int invoiceItemId = (int) itemsTableModel.getValueAt(itemRow, 0);
            int invoiceId = (int) invoiceTableModel.getValueAt(invoiceTable.getSelectedRow(), 0);
            String barcode = (String) itemsTableModel.getValueAt(itemRow, 2);
            String itemName = (String) itemsTableModel.getValueAt(itemRow, 3);
            double oldQty = (double) itemsTableModel.getValueAt(itemRow, 4);

            String newQtyStr = JOptionPane.showInputDialog(this, "Item: " + itemName + "\nCurrent Qty: " + oldQty + "\n\nNayi Qty darj karein:", "Edit Quantity", JOptionPane.PLAIN_MESSAGE);
            if (newQtyStr == null || newQtyStr.trim().isEmpty()) return;

            try {
                double newQty = Double.parseDouble(newQtyStr.trim());
                if (newQty < 0) {
                    JOptionPane.showMessageDialog(this, "Qty negative nahi ho sakti!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double diff = newQty - oldQty;
                Connection conn = null;
                try {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                    conn.setAutoCommit(false);

                    try (PreparedStatement pstmt = conn.prepareStatement("UPDATE PurchaseInvoiceItem SET Quantity = ? WHERE PurchaseInvoiceItemId = ?")) {
                        pstmt.setDouble(1, newQty);
                        pstmt.setInt(2, invoiceItemId);
                        pstmt.executeUpdate();
                    }

                    if (barcode != null && !barcode.trim().isEmpty()) {
                        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE StockData SET Inventory = ISNULL(Inventory, 0) + ? WHERE Barcode = ?")) {
                            pstmt.setDouble(1, diff);
                            pstmt.setString(2, barcode);
                            pstmt.executeUpdate();
                        }
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement("UPDATE PurchaseInvoice SET NetAmount = (SELECT ISNULL(SUM(Quantity * Price), 0) FROM PurchaseInvoiceItem WHERE PurchaseInvoiceId = ?) WHERE PurchaseInvoiceId = ?")) {
                        pstmt.setInt(1, invoiceId);
                        pstmt.setInt(2, invoiceId);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Qty update ho gayi hai! Stock adjust ho gaya hai.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadItemsForInvoice();
                    loadInvoices();

                } catch (Exception dbEx) {
                    if (conn != null) try { conn.rollback(); } catch (SQLException e) {}
                    JOptionPane.showMessageDialog(this, "Update Error (Check Console for details):\n" + dbEx.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    dbEx.printStackTrace();
                } finally {
                    if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Sirf number darj karein!", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteSelectedItem() {
            int itemRow = itemsTable.getSelectedRow();
            if (itemRow == -1) {
                JOptionPane.showMessageDialog(this, "Pehle delete karne ke liye item select karein!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int invoiceItemId = (int) itemsTableModel.getValueAt(itemRow, 0);
            int invoiceId = (int) invoiceTableModel.getValueAt(invoiceTable.getSelectedRow(), 0);
            String barcode = (String) itemsTableModel.getValueAt(itemRow, 2);
            String itemName = (String) itemsTableModel.getValueAt(itemRow, 3);
            double oldQty = (double) itemsTableModel.getValueAt(itemRow, 4);

            int confirm = JOptionPane.showConfirmDialog(this,
                "Kya aap ye item delete karna chahte hain?\n\nItem: " + itemName + "\nQty: " + oldQty + "\n\nStock se bhi ye Qty minus ho jayegi!",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                Connection conn = null;
                try {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                    conn.setAutoCommit(false);

                    try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM PurchaseInvoiceItem WHERE PurchaseInvoiceItemId = ?")) {
                        pstmt.setInt(1, invoiceItemId);
                        pstmt.executeUpdate();
                    }

                    if (barcode != null && !barcode.trim().isEmpty()) {
                        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE StockData SET Inventory = ISNULL(Inventory, 0) - ? WHERE Barcode = ?")) {
                            pstmt.setDouble(1, oldQty);
                            pstmt.setString(2, barcode);
                            pstmt.executeUpdate();
                        }
                    }

                    try (PreparedStatement pstmt = conn.prepareStatement("UPDATE PurchaseInvoice SET NetAmount = (SELECT ISNULL(SUM(Quantity * Price), 0) FROM PurchaseInvoiceItem WHERE PurchaseInvoiceId = ?) WHERE PurchaseInvoiceId = ?")) {
                        pstmt.setInt(1, invoiceId);
                        pstmt.setInt(2, invoiceId);
                        pstmt.executeUpdate();
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Item delete ho gaya hai! Stock se minus ho gayi hai.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadItemsForInvoice();
                    loadInvoices();

                } catch (Exception dbEx) {
                    if (conn != null) try { conn.rollback(); } catch (SQLException e) {}
                    JOptionPane.showMessageDialog(this, "Delete Error (Check Console for details):\n" + dbEx.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    dbEx.printStackTrace();
                } finally {
                    if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
                }
            }
        }
    }
}
