import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.math.BigDecimal;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Collections;
import java.util.TreeSet;
import java.io.*;
import javax.swing.border.TitledBorder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.Base64;
import java.nio.file.Files;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class QadriStore extends JFrame {

    // --- CONFIGURATION VARIABLES ---
    String serverName = "Server2025"; 
    String dbName = "QadriStore-BE";
    String user = "wasif";
    String pass = "123";
    
    String configFilePath = "QadriStore_DB_Config.properties";
    String vendorContactsFile = "QadriStore_VendorContacts.cfg";
    String url; 

    private DashboardPanel dashboardPanel;
    private boolean isMainUIInitialized = false;

    // --- AUTO UPDATE CONFIGURATION ---
    private static final String CURRENT_VERSION = "3.9.9.0.1"; 
    
    // Yahan aapka version.txt link hai
    private static final String GITHUB_RAW_VERSION_URL = "https://raw.githubusercontent.com/Wasif00990099/Qadri-Main-Excel-System/main/version.txt";
    
    // Yahan aapki EXE ka link hoga (jab aap EXE upload karenge)
    private static final String GITHUB_RAW_EXE_URL = "https://raw.githubusercontent.com/Wasif00990099/Qadri-Main-Excel-System/main/QadriStore.exe";
    
    private static final String UPDATER_JAR_NAME = "QadriStoreUpdater.jar";

    // --- TABLE CONFIG ---
    String referenceTable = "fitems"; 
    String refBarcodeCol = "Barcode";
    String refNameCol = "LongName";
    
    String inventoryTable = ""; 
    String invNameCol = "ProductName";
    String invValueCol = "Inventory";
    
    String salesTable = ""; 
    String salesNameCol = "LongName";
    String salesValueCol = "Quantity";
    
    String selectionFile = "QadriStore_Selection.cfg";
    String layoutFile = "QadriStore_Layout.cfg"; 
    String localCacheFile = "QadriStore_LocalCache.dat";
    
    String salesModeFile = "QadriStore_SalesMode.cfg";
    String offlineSalesFile = "QadriStore_OfflineSales.dat";
    
    // --- CONFIG BLOCK VARIABLES ---
    String configBlockFile = "QadriStore_ConfigBlock.cfg";
    boolean isConfigBlocked = false;
    StringBuilder hiddenTypingBuffer = new StringBuilder();
    String CONFIG_PASSWORD = "123";
    String UNLOCK_PASSWORD = "wasiftech";
    
    // --- ENCRYPTION KEY ---
    private final byte[] ENCRYPTION_KEY = "QadriStoreSecretKey2025".getBytes();

    // GUI Components
    JTable tableList; 
    JTable resultTable; 
    JTextField txtSearch; 
    JTextField txtProductSearch; 
    JTabbedPane tabbedPane;
    JButton btnLoad, btnToggleView, btnCloseTab, btnAddVendor, btnCompnyNumbers, btnConfig, btnMergeDuplicates, btnOrder, btnPrint, btnClearOrder;
    
    JToggleButton btnLiveSales; 
    JToggleButton btnDragMode; 
    JLabel lblStatus;
    JLabel lblVersion;
    JSplitPane splitPane;
    JPanel leftPanel;
    
    JTable searchPaletteTable;
    JScrollPane searchPaletteScroll;
    JPanel rightWrapperPanel; 
    
    TableColumn orderColumn; 
    TableColumn priceColumn; 
    
    Timer liveUpdateTimer; 
    Timer localCacheTimer;
    
    boolean isVendorInsertMode = false;
    boolean isOfflineMode = false; 
    
    boolean isLiveSalesActive = true;
    long lastSalesFetchTime = 0;
    
    final String VENDOR_MARKER = "VENDOR_ROW_MARKER";

    Map<String, String> nameToBarcodeMap = new HashMap<>();
    Map<String, String> barcodeToNameMap = new HashMap<>();
    Map<String, String[]> vendorContactsMap = new HashMap<>();

    // Thermal Printer Constants (80mm)
    private static final double PAPER_WIDTH_MM = 80.0;
    private static final double PAPER_HEIGHT_MM = 297.0;
    private static final double MARGIN_MM = 3.0;
    private static final double PRINTABLE_WIDTH_MM = PAPER_WIDTH_MM - (2 * MARGIN_MM);
    private static final double MM_TO_POINTS = 72.0 / 25.4;

    public QadriStore() {
        setApplicationIcon();

        // --- DASHBOARD SET KARNA ---
        dashboardPanel = new DashboardPanel(this);
        setContentPane(dashboardPanel);

        setTitle("Qadri Store - Dashboard - v" + CURRENT_VERSION);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowCloseRequest();
            }
        });

        // Background tasks (Data loading)
        loadConfigurationFromDisk();
        updateUrl();
        loadSalesModeState();
        loadConfigBlockState();

        setupTaskbarFeatures();
        
        // YEH LINE ADD KAREIN: Software open hote hi update check karega
        checkForUpdates();
    }

    private void ensureMainUIInitialized() {
        if (isMainUIInitialized) {
            return;
        }

        Container previousContent = getContentPane();
        JPanel temporaryHolder = new JPanel(new BorderLayout());
        setContentPane(temporaryHolder);
        setupMainGUI();
        setContentPane(previousContent != null ? previousContent : dashboardPanel);
        isMainUIInitialized = true;
    }

    public void showMainApplicationUI() {
        ensureMainUIInitialized();

        setContentPane(splitPane);
        setTitle("Qadri Store - Management System - v" + CURRENT_VERSION);
        revalidate();
        repaint();

        initializeAndLoadFast();
    }

    private void showDashboardUI() {
        setContentPane(dashboardPanel);
        setTitle("Qadri Store - Dashboard - v" + CURRENT_VERSION);
        revalidate();
        repaint();
    }

    public void openUnpackUpdaterFromDashboard() {
        ensureMainUIInitialized();
        initializeAndLoadFast();

        Timer timer = new Timer(2000, e -> {
            if (resultTable != null) {
                try {
                    UnpackUpdater unpackUpdater = new UnpackUpdater(this);
                    unpackUpdater.loadUnpackUpdatedState();
                    unpackUpdater.performUnpackUpdate();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Unable to open unpack updater: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Table is still loading. Please try again in a moment.");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void showConfigurationFromDashboard() {
        if (isConfigBlocked) {
            JOptionPane.showMessageDialog(this,
                "<html><div style='text-align:center;'>" +
                "<h2 style='color:red;'>⛔ CONFIGURATION IS BLOCKED</h2>" +
                "<hr>" +
                "<p style='font-size:14px;'><b>Unblock ke liye Muhammed Wasif se contact karein:</b></p>" +
                "<p style='font-size:18px; color:blue;'><b>📞 03131134889</b></p>" +
                "<p style='font-size:18px; color:blue;'><b>📞 03353323497</b></p>" +
                "</div></html>",
                "CONFIGURATION BLOCKED",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!verifyPassword()) return;
        showConfigDialog();
    }

    public void openPurchaseInvoiceWindowFromDashboard() {
        try {
            updateUrl();
            PurchaseInvoiceRecordsWindow recordsWindow = new PurchaseInvoiceRecordsWindow(this, url, user, pass);
            recordsWindow.setVisible(true);
            recordsWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to open purchase invoice window: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openChakkiStockBookFromDashboard() {
        ChakkiStockBookWindow window = new ChakkiStockBookWindow(this);
        window.setVisible(true);
    }

    private class ChakkiStockBookWindow extends JFrame {
        private DefaultTableModel tableModel;
        private JTable table;
        private Timer inventoryRefreshTimer;
        private boolean inventoryLoadErrorShown = false;
        private JTextField chakkiSaleValueField;
        private final JTextField[] vendorNameFieldRef = new JTextField[1];
        private final JTextField[] vendorExtraFieldRef = new JTextField[1];
        private java.util.Map<String, java.util.List<PurchaseEntry>> latestPurchaseEntriesByName = new java.util.HashMap<>();
        private java.util.Map<String, JTextField[]> purchaseInvoiceQtyFields = new java.util.LinkedHashMap<>();
        private static final String SUMMARY_MOJOOD_ITEM = "__SUMMARY_MOJOOD__";
        private final String STOCK_DB_TABLE = "ChakkiStockBookRecord";
        private JButton btnSaveRecord;
        private JTextField balanceField;
        private JTextField saleBalanceTotalField;
        private JTextField shopField;
        private JTextField aliBhaiField;
        private JTextField expensesField;
        private JTextField expensesDeductionTotalField;
        private JTextField kamField;
        private JTextField zyadaField;
        private JTextField mixField;
        private JTextField majoodField;

        private static class PurchaseEntry {
            private final String productName;
            private final String vendorName;
            private final double quantity;

            private PurchaseEntry(String productName, String vendorName, double quantity) {
                this.productName = productName;
                this.vendorName = vendorName;
                this.quantity = quantity;
            }
        }

        ChakkiStockBookWindow(Frame owner) {
            super("Chakki Stock Book");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            setIconImage(QadriStore.this.getIconImage());
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    stopAutoRefresh();
                }
            });

            JPanel headerPanel = new JPanel(new BorderLayout(12, 12));
            headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

            JLabel titleLabel = new JLabel("CHAKKI STOCK BOOK");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            titleLabel.setForeground(new Color(191, 39, 38));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

            JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            datePanel.setOpaque(false);
            datePanel.add(new JLabel("Date:"));
            JTextField txtDate = new JTextField(12);
            txtDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtDate.setText(new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date()));
            txtDate.setEditable(false);
            datePanel.add(txtDate);

            headerPanel.add(titleLabel, BorderLayout.WEST);
            headerPanel.add(datePanel, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            String[] columns = {"Item Name", "New Stock", "Yesterday", "Today", "Sale"};
            String[][] data = {
                {"Atta Chakki 10kg", "", "", "", ""},
                {"Atta Chakki 5kg", "", "", "", ""},
                {"Atta 2.5# 10kg", "", "", "", ""},
                {"Atta 2.5# 5kg", "", "", "", ""},
                {"Atta Super Fine 10kg", "", "", "", ""},
                {"Atta Super Fine 5kg", "", "", "", ""},
                {"Atta 2.5# 50kg", "", "", "", ""},
                {"Atta Super Fine 50kg", "", "", "", ""},
                {"Meda 50kg", "", "", "", ""},
                {"Atta Makai 1kg", "", "", "", ""},
                {"Atta Makai 500gm", "", "", "", ""},
                {"Atta Chawal 1kg", "", "", "", ""},
                {"Atta Chawal 500gm", "", "", "", ""},
                {"Gandum Dalia 500gm", "", "", "", ""},
                {"Atta Jaw 500gm", "", "", "", ""},
                {"Dalia Jaw 500gm", "", "", "", ""},
                {"Atta Chakki 2kg", "", "", "", ""},
                {"Atta Super Fine 2kg", "", "", "", ""},
                {"Meda 1kg", "", "", "", ""},
                {"Meda 5kg", "", "", "", ""},
                {"Atta Bajra 500gm", "", "", "", ""},
                {"Atta Kala Chana 500gm", "", "", "", ""},
                {"Daal Chana 50kg", "", "", "", ""},
                {"Gandum 50kg", "", "", "", ""},
                {"Makai 50kg", "", "", "", ""},
                {"Jaw 50kg", "", "", "", ""},
                {"Chawal 50kg", "", "", "", ""},
                {"Bajra 50kg", "", "", "", ""},
                {"Kala Chana 50kg", "", "", "", ""}
            };

            tableModel = new DefaultTableModel(data, columns) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            table = new JTable(tableModel);
            table.setRowHeight(32);
            table.setShowGrid(true);
            table.setGridColor(new Color(200, 200, 200));
            table.setIntercellSpacing(new Dimension(1, 1));
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 18));
            table.getTableHeader().setBackground(new Color(242, 242, 242));
            table.getTableHeader().setReorderingAllowed(false);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            JTableHeader header = table.getTableHeader();
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                    setForeground(new Color(0, 83, 156));
                    return c;
                }
            });
            table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            for (int i = 1; i < columns.length; i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
            for (int i = 0; i < columns.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 420 : 110);
            }
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));

            JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
            rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1), "Daily Exp", TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)));
            JPanel valuesPanel = new JPanel(new GridBagLayout());
            valuesPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.25;

            String[][] rightRows = {
                {"Big", ""}, {"100", ""}, {"50", ""}, {"20", ""}, {"10", ""}, {"Damage", ""}, {"Coin", ""},
                {"Mix", ""}, {"Total", ""}, {"Balance", ""}, {"Sale", ""}, {"Total", ""}, {"Shop", ""},
                {"Ali Bhai", ""}, {"Expenses", ""}, {"Total", ""}, {"Mojood", ""}, {"Kam", ""}, {"Zyada", ""},
                {"Purchase Invoice", ""}, {"Vendor Name", ""}, {"Atta 2.5# 10kg", ""}, {"Atta 2.5# 5kg", ""}, {"Atta Super Fine 10kg", ""}, {"Atta Super Fine 5kg", ""},
                {"Atta 2.5# 50kg", ""}, {"Atta Super Fine 50kg", ""}, {"Meda 50kg", ""}, {"Soji", ""}
            };
            java.util.List<JTextField> editableCashFields = new java.util.ArrayList<>();
            final JTextField[] summaryTotalFieldRef = new JTextField[1];
            java.util.List<JTextField> summaryInputFields = new java.util.ArrayList<>();
            for (int i = 0; i < rightRows.length; i++) {
                String[] rowData = rightRows[i];
                gbc.gridx = 0;
                gbc.gridy = i;
                gbc.gridwidth = 2;
                if ("Purchase Invoice".equals(rowData[0])) {
                    JLabel sectionLabel = new JLabel(rowData[0], SwingConstants.CENTER);
                    sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    sectionLabel.setForeground(new Color(0, 83, 156));
                    sectionLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
                    valuesPanel.add(sectionLabel, gbc);
                    continue;
                }

                if ("Vendor Name".equals(rowData[0])) {
                    gbc.gridwidth = 1;
                    JLabel vendorLabel = new JLabel(rowData[0]);
                    vendorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    vendorLabel.setPreferredSize(new Dimension(90, 20));
                    valuesPanel.add(vendorLabel, gbc);

                    gbc.gridx = 1;
                    JTextField vendorField = new JTextField(rowData[1]);
                    vendorField.setEditable(false);
                    vendorField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    vendorField.setPreferredSize(new Dimension(90, 22));
                    vendorField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                    valuesPanel.add(vendorField, gbc);
                    vendorNameFieldRef[0] = vendorField;

                    gbc.gridx = 2;
                    JTextField vendorExtraField = new JTextField();
                    vendorExtraField.setEditable(false);
                    vendorExtraField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    vendorExtraField.setPreferredSize(new Dimension(90, 22));
                    vendorExtraField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                    valuesPanel.add(vendorExtraField, gbc);
                    vendorExtraFieldRef[0] = vendorExtraField;
                    continue;
                }

                gbc.gridwidth = 1;
                JLabel label = new JLabel(rowData[0]);
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setPreferredSize(new Dimension(90, 20));
                valuesPanel.add(label, gbc);

                gbc.gridx = 1;
                JTextField valueField = new JTextField(rowData[1]);
                valueField.setEditable(false);
                valueField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                valueField.setPreferredSize(new Dimension(70, 22));
                valueField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                valuesPanel.add(valueField, gbc);

                if ("Atta 2.5# 10kg".equals(rowData[0]) || "Atta 2.5# 5kg".equals(rowData[0]) || "Atta Super Fine 10kg".equals(rowData[0]) || "Atta Super Fine 5kg".equals(rowData[0]) || "Atta 2.5# 50kg".equals(rowData[0]) || "Atta Super Fine 50kg".equals(rowData[0]) || "Meda 50kg".equals(rowData[0]) || "Soji".equals(rowData[0])) {
                    gbc.gridx = 2;
                    JTextField secondValueField = new JTextField();
                    secondValueField.setEditable(false);
                    secondValueField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    secondValueField.setPreferredSize(new Dimension(70, 22));
                    secondValueField.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                    valuesPanel.add(secondValueField, gbc);
                    purchaseInvoiceQtyFields.put(normalizeItemName(rowData[0]), new JTextField[]{valueField, secondValueField});
                }
                if ("Sale".equals(rowData[0])) {
                    chakkiSaleValueField = valueField;
                }
                if (java.util.Arrays.asList("Big", "100", "50", "20", "10", "Damage", "Coin", "Mix").contains(rowData[0])) {
                    valueField.setEditable(true);
                    editableCashFields.add(valueField);
                    summaryInputFields.add(valueField);
                    valueField.addActionListener(e -> {
                        int currentIndex = summaryInputFields.indexOf(valueField);
                        if (currentIndex >= 0 && currentIndex < summaryInputFields.size() - 1) {
                            JTextField nextField = summaryInputFields.get(currentIndex + 1);
                            nextField.requestFocusInWindow();
                            nextField.selectAll();
                        } else if (shopField != null) {
                            shopField.requestFocusInWindow();
                            shopField.selectAll();
                        } else {
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        }
                    });
                    valueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        @Override
                        public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSummaryTotal(summaryTotalFieldRef[0], editableCashFields, majoodField); }
                        @Override
                        public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSummaryTotal(summaryTotalFieldRef[0], editableCashFields, majoodField); }
                        @Override
                        public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSummaryTotal(summaryTotalFieldRef[0], editableCashFields, majoodField); }
                    });
                }
                if ("Total".equals(rowData[0])) {
                    if (i == 8) {
                        summaryTotalFieldRef[0] = valueField;
                    } else if (i == 11) {
                        saleBalanceTotalField = valueField;
                    } else if (i == 15) {
                        expensesDeductionTotalField = valueField;
                    }
                    valueField.setEditable(false);
                }
                if ("Balance".equals(rowData[0])) {
                    balanceField = valueField;
                    valueField.setEditable(false);
                }
                if ("Shop".equals(rowData[0])) {
                    shopField = valueField;
                    valueField.setEditable(true);
                    valueField.addActionListener(e -> {
                        if (aliBhaiField != null) {
                            aliBhaiField.requestFocusInWindow();
                            aliBhaiField.selectAll();
                        } else {
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        }
                    });
                    valueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                    });
                }
                if ("Ali Bhai".equals(rowData[0])) {
                    aliBhaiField = valueField;
                    valueField.setEditable(true);
                    valueField.addActionListener(e -> {
                        if (expensesField != null) {
                            expensesField.requestFocusInWindow();
                            expensesField.selectAll();
                        } else {
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        }
                    });
                    valueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                    });
                }
                if ("Expenses".equals(rowData[0])) {
                    expensesField = valueField;
                    valueField.setEditable(true);
                    valueField.addActionListener(e -> {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                    });
                    valueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateExpensesAfterDeductions(); }
                    });
                }
                if ("Mojood".equals(rowData[0])) {
                    majoodField = valueField;
                    valueField.setEditable(false);
                }
                if ("Kam".equals(rowData[0])) {
                    kamField = valueField;
                    valueField.setEditable(false);
                }
                if ("Zyada".equals(rowData[0])) {
                    zyadaField = valueField;
                    valueField.setEditable(false);
                }
            }
            updateSummaryTotal(summaryTotalFieldRef[0], editableCashFields, majoodField);
            updateSaleBalanceTotal();
            updateExpensesAfterDeductions();

            table.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                // Ab vendor name auto-fill ho raha hai loadPurchase mein, isliye yahan kuch nahi karna
            });
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }

            rightPanel.add(new JScrollPane(valuesPanel), BorderLayout.CENTER);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, rightPanel);
            splitPane.setResizeWeight(0.75);
            splitPane.setDividerSize(6);
            add(splitPane, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            
            JButton btnRefresh = new JButton("Refresh Inventory");
            btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnRefresh.setBackground(new Color(0, 120, 215));
            btnRefresh.setForeground(Color.BLACK);
            btnRefresh.setFocusPainted(false);
            btnRefresh.addActionListener(e -> refreshChakkiData());
            
            btnSaveRecord = new JButton("Save Record");
            btnSaveRecord.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnSaveRecord.setBackground(new Color(40, 167, 69));
            btnSaveRecord.setForeground(Color.BLACK);
            btnSaveRecord.setFocusPainted(false);
            btnSaveRecord.addActionListener(e -> saveCurrentRecordToDB());
            
            JButton btnRecords = new JButton("Records");
            btnRecords.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnRecords.setBackground(new Color(255, 193, 7));
            btnRecords.setForeground(Color.BLACK);
            btnRecords.setFocusPainted(false);
            btnRecords.addActionListener(e -> openRecordsHistoryWindow());
            
            footer.add(btnRefresh);
            footer.add(btnSaveRecord);
            footer.add(btnRecords);
            add(footer, BorderLayout.SOUTH);

            refreshChakkiData();
            startAutoRefresh();
        }

        private void startAutoRefresh() {
            if (inventoryRefreshTimer != null) {
                inventoryRefreshTimer.stop();
            }
            inventoryRefreshTimer = new Timer(5000, e -> refreshChakkiData());
            inventoryRefreshTimer.setRepeats(true);
            inventoryRefreshTimer.start();
        }

        private void stopAutoRefresh() {
            if (inventoryRefreshTimer != null) {
                inventoryRefreshTimer.stop();
            }
        }

        private boolean ensureInventoryTableInfo() {
            if (inventoryTable != null && !inventoryTable.isEmpty()) {
                return true;
            }
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'ProductName' INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Inventory'")) {
                    if (rs.next()) {
                        inventoryTable = rs.getString(1);
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
            return false;
        }

        private void refreshChakkiData() {
            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            if (!isTodayRecordPresent(currentDate)) {
                loadYesterdayFromLastSavedRecord(currentDate);
            }
            int selectedRow = table.getSelectedRow();
            loadInventoryIntoTodayColumn();
            loadPurchaseIntoNewStockColumn();
            loadSalesIntoSaleColumn();
            if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
                table.setRowSelectionInterval(selectedRow, selectedRow);
            } else if (table.getRowCount() > 0 && table.getSelectedRow() < 0) {
                table.setRowSelectionInterval(0, 0);
            }
        }

        private boolean isTodayRecordPresent(String currentDate) {
            String sql = "SELECT COUNT(*) FROM " + STOCK_DB_TABLE + " WHERE RecordDate = ?";
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentDate);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            } catch (Exception ignored) {
            }
            return false;
        }

        private void loadYesterdayFromLastSavedRecord(String currentDate) {
            // Clear the Yesterday column first so stale values don't remain from any previous refresh.
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                tableModel.setValueAt("", row, 2);
            }
            if (balanceField != null) {
                balanceField.setText("");
            }

            String sql = "SELECT ItemName, Yesterday FROM " + STOCK_DB_TABLE + " WHERE RecordDate = (SELECT MAX(RecordDate) FROM " + STOCK_DB_TABLE + " WHERE RecordDate < ?)";
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentDate);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String itemName = rs.getString("ItemName");
                        double yesterdayVal = rs.getDouble("Yesterday");
                        if (SUMMARY_MOJOOD_ITEM.equals(itemName)) {
                            if (balanceField != null) {
                                balanceField.setText(yesterdayVal == 0 ? "" : formatInventoryValue(yesterdayVal));
                            }
                            continue;
                        }
                        String normalizedItem = normalizeItemName(itemName);
                        for (int row = 0; row < tableModel.getRowCount(); row++) {
                            if (normalizeItemName(tableModel.getValueAt(row, 0).toString()).equals(normalizedItem)) {
                                tableModel.setValueAt(yesterdayVal == 0 ? "" : formatInventoryValue(yesterdayVal), row, 2);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to load yesterday from last saved record: " + ex.getMessage());
            }
        }

        private void updateSummaryTotal(JTextField totalField, java.util.List<JTextField> fields, JTextField copyTargetField) {
            if (totalField == null) {
                return;
            }
            double total = 0.0;
            for (JTextField field : fields) {
                if (field == null) {
                    continue;
                }
                total += parseDouble(field.getText());
            }
            String formattedTotal = formatInventoryValue(total);
            totalField.setText(formattedTotal);
            if (copyTargetField != null) {
                copyTargetField.setText(formattedTotal);
            }
            updateSaleBalanceTotal();
        }

        private void updateSaleBalanceTotal() {
            if (saleBalanceTotalField == null) {
                return;
            }
            double balanceValue = balanceField != null ? parseDouble(balanceField.getText()) : 0.0;
            double saleValue = chakkiSaleValueField != null ? parseDouble(chakkiSaleValueField.getText()) : 0.0;
            saleBalanceTotalField.setText(formatInventoryValue(balanceValue + saleValue));
            updateExpensesAfterDeductions();
        }

        private void updateExpensesAfterDeductions() {
            if (expensesDeductionTotalField == null) {
                return;
            }
            double baseTotal = saleBalanceTotalField != null ? parseDouble(saleBalanceTotalField.getText()) : 0.0;
            double shopValue = shopField != null ? parseDouble(shopField.getText()) : 0.0;
            double aliValue = aliBhaiField != null ? parseDouble(aliBhaiField.getText()) : 0.0;
            double expensesValue = expensesField != null ? parseDouble(expensesField.getText()) : 0.0;
            double result = baseTotal - shopValue - aliValue - expensesValue;
            expensesDeductionTotalField.setText(formatInventoryValue(result));
            updateKamZyada(result);
        }

        private void updateKamZyada(double expensesResult) {
            if (kamField == null || zyadaField == null || majoodField == null) {
                return;
            }
            double mojoodValue = parseDouble(majoodField.getText());
            double diff = expensesResult - mojoodValue;
            if (diff > 0) {
                // expensesResult greater than mojood -> shortage (Kam)
                kamField.setText(formatInventoryValue(diff));
                zyadaField.setText("");
            } else if (diff < 0) {
                // expensesResult less than mojood -> excess (Zyada)
                zyadaField.setText(formatInventoryValue(-diff));
                kamField.setText("");
            } else {
                kamField.setText("");
                zyadaField.setText("");
            }
        }

        private void updateSaleSummaryValue(Map<String, Double> salesAmountMap) {
            if (chakkiSaleValueField == null) {
                return;
            }
            double totalSaleAmount = 0.0;
            for (Double value : salesAmountMap.values()) {
                totalSaleAmount += value;
            }
            chakkiSaleValueField.setText(formatInventoryValue(totalSaleAmount));
        }

        private PurchaseEntry findBestPurchaseEntry(String targetItemName, java.util.List<PurchaseEntry> purchaseEntries) {
            if (targetItemName == null || targetItemName.trim().isEmpty() || purchaseEntries == null || purchaseEntries.isEmpty()) {
                return null;
            }
            String normalizedTarget = normalizeItemName(targetItemName);
            if (normalizedTarget.isEmpty()) {
                return null;
            }

            PurchaseEntry bestMatch = null;
            int bestScore = -1;
            for (PurchaseEntry entry : purchaseEntries) {
                if (entry == null || entry.productName == null || entry.productName.trim().isEmpty()) {
                    continue;
                }
                String normalizedCandidate = normalizeItemName(entry.productName);
                int score = scoreNameSimilarity(normalizedTarget, normalizedCandidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = entry;
                }
            }
            return bestScore >= 25 ? bestMatch : null;
        }

        private String normalizeItemName(String name) {
            if (name == null) {
                return "";
            }
            return name.toLowerCase().replaceAll("[^a-z0-9]+", "").trim();
        }

        private java.util.List<PurchaseEntry> findMatchingPurchaseEntries(String normalizedTarget, java.util.Map<String, java.util.List<PurchaseEntry>> purchaseEntriesByName) {
            if (normalizedTarget == null || normalizedTarget.isEmpty() || purchaseEntriesByName == null || purchaseEntriesByName.isEmpty()) {
                return null;
            }
            java.util.List<PurchaseEntry> exact = purchaseEntriesByName.get(normalizedTarget);
            if (exact != null && !exact.isEmpty()) {
                return exact;
            }
            for (Map.Entry<String, java.util.List<PurchaseEntry>> entry : purchaseEntriesByName.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) continue;
                if (normalizedTarget.contains(key) || key.contains(normalizedTarget)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        private int scoreNameSimilarity(String target, String candidate) {
            if (target.isEmpty() || candidate.isEmpty()) {
                return 0;
            }
            if (target.equals(candidate)) {
                return 100;
            }
            if (target.contains(candidate) || candidate.contains(target)) {
                return 80;
            }

            String[] targetTokens = target.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");
            String[] candidateTokens = candidate.split("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)");
            int overlap = 0;
            for (String t : targetTokens) {
                if (t.isEmpty()) {
                    continue;
                }
                for (String c : candidateTokens) {
                    if (t.equals(c)) {
                        overlap++;
                        break;
                    }
                }
            }
            return overlap >= 2 ? 25 + overlap : 0;
        }

        private void loadInventoryIntoTodayColumn() {
            try {
                updateUrl();
                if (!ensureInventoryTableInfo()) {
                    return;
                }

                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    Map<String, Double> inventoryMap = new LinkedHashMap<>();
                    try (PreparedStatement ps = conn.prepareStatement("SELECT [" + invNameCol + "], [" + invValueCol + "] FROM [" + inventoryTable + "]")) {
                        try (ResultSet rs = ps.executeQuery()) {
                                    while (rs.next()) {
                                String name = rs.getString(1);
                                double value = rs.getDouble(2);
                                if (name != null && !name.trim().isEmpty()) {
                                    inventoryMap.put(normalizeItemName(name), value);
                                }
                            }
                        }
                    }

                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String itemName = normalizeItemName(tableModel.getValueAt(row, 0).toString());
                        Double invValue = inventoryMap.get(itemName);
                        Object displayValue = invValue != null ? formatInventoryValue(invValue) : "";
                        tableModel.setValueAt(displayValue, row, 3);
                    }
                    inventoryLoadErrorShown = false;
                }
            } catch (Exception ex) {
                if (!inventoryLoadErrorShown) {
                    System.err.println("Inventory refresh failed: " + ex.getMessage());
                    inventoryLoadErrorShown = true;
                }
            }
        }

        private void loadPurchaseIntoNewStockColumn() {
            try {
                updateUrl();
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    String purchaseSql = """
                        SELECT 
                            P.LongName AS ProductName,
                            V.Name AS VendorName,
                            SUM(PI.Quantity) AS TotalPurchaseQty
                        FROM PurchaseInvoiceItem PI
                        INNER JOIN PurchaseInvoice PINV ON PI.PurchaseInvoiceId = PINV.PurchaseInvoiceId
                        INNER JOIN ProductItem P ON PI.ProductItemId = P.ProductItemId
                        INNER JOIN ProductVendor V ON PINV.ProductVendorId = V.ProductVendorId
                        WHERE CAST(PINV.PurchaseInvoiceDate AS DATE) = CAST(GETDATE() AS DATE)
                        AND PINV.DeletedByUserId IS NULL
                        AND PI.ItemStatus = 1
                        GROUP BY P.LongName, V.Name
                        ORDER BY TotalPurchaseQty DESC
                        """;

                    Map<String, java.util.List<PurchaseEntry>> purchaseEntriesByName = new LinkedHashMap<>();
                    Map<String, Double> purchaseQtyTotals = new LinkedHashMap<>();
                    try (PreparedStatement ps = conn.prepareStatement(purchaseSql)) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String name = rs.getString("ProductName");
                                String vendorName = rs.getString("VendorName");
                                double qtyValue = rs.getDouble("TotalPurchaseQty");
                                if (name != null && !name.trim().isEmpty()) {
                                    String normalizedName = normalizeItemName(name);
                                    purchaseEntriesByName.computeIfAbsent(normalizedName, k -> new java.util.ArrayList<>())
                                            .add(new PurchaseEntry(name.trim(), vendorName, qtyValue));
                                    purchaseQtyTotals.put(normalizedName, purchaseQtyTotals.getOrDefault(normalizedName, 0.0) + qtyValue);
                                }
                            }
                        }
                    }

                    latestPurchaseEntriesByName = purchaseEntriesByName;
                    for (Map.Entry<String, JTextField[]> entry : purchaseInvoiceQtyFields.entrySet()) {
                        String itemName = entry.getKey();
                        JTextField[] fields = entry.getValue();
                        java.util.List<PurchaseEntry> entries = findMatchingPurchaseEntries(itemName, purchaseEntriesByName);
                        if (entries != null && !entries.isEmpty()) {
                            fields[0].setText(formatInventoryValue(entries.get(0).quantity));
                            if (entries.size() > 1) {
                                fields[1].setText(formatInventoryValue(entries.get(1).quantity));
                            } else {
                                fields[1].setText("");
                            }
                        } else {
                            fields[0].setText("");
                            fields[1].setText("");
                        }
                    }
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String itemName = normalizeItemName(tableModel.getValueAt(row, 0).toString());
                        Double totalQty = purchaseQtyTotals.get(itemName);
                        Object displayValue = totalQty != null ? formatInventoryValue(totalQty) : "";
                        tableModel.setValueAt(displayValue, row, 1);
                    }

                    // =====================================================================
                    // YEH NAYA FIXED LOGIC HAI: Vendor Ke Hisaab Se Quantities Align Karna
                    // =====================================================================
                    
                    // Step 1: Aaj ki Purchases mein Kaunse Vendors hain, unki list banao (Ordered)
                    java.util.Set<String> todayVendorOrder = new java.util.LinkedHashSet<>();
                    for (java.util.List<PurchaseEntry> entries : purchaseEntriesByName.values()) {
                        if (entries != null && !entries.isEmpty() && entries.get(0).vendorName != null) {
                            todayVendorOrder.add(entries.get(0).vendorName.trim());
                        }
                    }

                    // Step 2: Vendor Name aur Extra Vendor Box mein Pehle 2 Vendors Set Karo
                    String firstVendorName = "";
                    String secondVendorName = "";
                    if (todayVendorOrder.size() > 0) firstVendorName = todayVendorOrder.iterator().next();
                    
                    int vendorCount = 0;
                    for (String vName : todayVendorOrder) {
                        vendorCount++;
                        if (vendorCount == 2) {
                            secondVendorName = vName;
                            break;
                        }
                    }

                    if (vendorNameFieldRef[0] != null) {
                        vendorNameFieldRef[0].setText(firstVendorName);
                    }
                    if (vendorExtraFieldRef[0] != null) {
                        vendorExtraFieldRef[0].setText(secondVendorName);
                    }

                    // Step 3: Right Side Ke Quantity Boxes Ko Us Vendor Se Match Karo
                    for (Map.Entry<String, JTextField[]> entry : purchaseInvoiceQtyFields.entrySet()) {
                        String boxItemName = entry.getKey();
                        JTextField[] fields = entry.getValue();
                        
                        java.util.List<PurchaseEntry> matchedEntries = purchaseEntriesByName.get(boxItemName);
                        
                        String qtyForFirstVendor = "";
                        String qtyForSecondVendor = "";
                        
                        if (matchedEntries != null && !matchedEntries.isEmpty()) {
                            for (PurchaseEntry pe : matchedEntries) {
                                String peVendor = pe.vendorName != null ? pe.vendorName.trim() : "";
                                if (peVendor.equals(firstVendorName)) {
                                    qtyForFirstVendor = formatInventoryValue(pe.quantity);
                                } else if (peVendor.equals(secondVendorName)) {
                                    qtyForSecondVendor = formatInventoryValue(pe.quantity);
                                }
                            }
                        }
                        
                        fields[0].setText(qtyForFirstVendor);
                        fields[1].setText(qtyForSecondVendor);
                    }
                    // =====================================================================

                }
            } catch (Exception ex) {
                System.err.println("Purchase refresh failed: " + ex.getMessage());
            }
        }

        private void loadSalesIntoSaleColumn() {
            try {
                updateUrl();
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    String sql = """
                        SELECT 
                            P.LongName AS ProductName,
                            ISNULL(SaleData.TotalSaleQty, 0) - ISNULL(ReturnData.TotalReturnQty, 0) AS NetQuantityBiki,
                            (ISNULL(SaleData.TotalSaleAmount, 0) - ISNULL(ReturnData.TotalReturnAmount, 0)) AS NetTotalAmount
                        FROM ProductItem P
                        LEFT JOIN (
                            SELECT 
                                SI.ProductItemId,
                                SUM(SI.Quantity) AS TotalSaleQty,
                                SUM(SI.Quantity * SI.Price) AS TotalSaleAmount
                            FROM SaleInvoiceItem SI
                            INNER JOIN SaleInvoice INV ON SI.SaleInvoiceId = INV.SaleInvoiceId
                            WHERE CAST(INV.InvoiceDate AS DATE) = CAST(GETDATE() AS DATE)
                            AND INV.DeletedByUserId IS NULL
                            AND SI.DeletedBy IS NULL
                            AND SI.ItemStatus = 1
                            AND INV.InvoiceNumber > 0
                            GROUP BY SI.ProductItemId
                        ) SaleData ON P.ProductItemId = SaleData.ProductItemId
                        LEFT JOIN (
                            SELECT 
                                RI.ProductItemId,
                                SUM(RI.Quantity) AS TotalReturnQty,
                                SUM(RI.Quantity * RI.Price) AS TotalReturnAmount
                            FROM ReturnInvoiceItem RI
                            INNER JOIN ReturnInvoice RINV ON RI.ReturnInvoiceId = RINV.ReturnInvoiceId
                            WHERE CAST(RINV.InvoiceDate AS DATE) = CAST(GETDATE() AS DATE)
                            AND RINV.DataEntryStatus = 1
                            AND RINV.DeletedByUserId IS NULL
                            GROUP BY RI.ProductItemId
                        ) ReturnData ON P.ProductItemId = ReturnData.ProductItemId
                        WHERE (ISNULL(SaleData.TotalSaleQty, 0) - ISNULL(ReturnData.TotalReturnQty, 0)) <> 0
                        AND (ISNULL(SaleData.TotalSaleQty, 0) + ISNULL(ReturnData.TotalReturnQty, 0)) > 0
                        ORDER BY NetQuantityBiki DESC
                        """;

                    Map<String, Double> salesMap = new LinkedHashMap<>();
                    Map<String, Double> salesAmountMap = new LinkedHashMap<>();
                    Map<String, Double> discardMap = new LinkedHashMap<>();
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String name = rs.getString("ProductName");
                                double qtyValue = rs.getDouble("NetQuantityBiki");
                                double amountValue = rs.getDouble("NetTotalAmount");
                                if (name != null && !name.trim().isEmpty()) {
                                    String normalizedName = normalizeItemName(name);
                                    salesMap.put(normalizedName, qtyValue);
                                    salesAmountMap.put(normalizedName, amountValue);
                                }
                            }
                        }
                    }

                    String discardSql = """
                        SELECT 
                            P.LongName AS ProductName,
                            SI.Quantity AS DiscardQuantity
                        FROM StockIssuenceItem SI
                        INNER JOIN StockIssuence SINV ON SI.StockIssuenceId = SINV.StockIssuenceId
                        INNER JOIN ProductItem P ON SI.ProductItemId = P.ProductItemId
                        WHERE CAST(SINV.IssuenceDate AS DATE) = CAST(GETDATE() AS DATE)
                        AND SINV.ProductDiscardReasonId IS NOT NULL
                        AND SINV.DeleteByUserId IS NULL
                        AND SINV.DataEntryStatus = 1
                        ORDER BY DiscardQuantity DESC
                        """;
                    try (PreparedStatement ps = conn.prepareStatement(discardSql)) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                String name = rs.getString("ProductName");
                                double discardQty = rs.getDouble("DiscardQuantity");
                                if (name != null && !name.trim().isEmpty()) {
                                    discardMap.put(normalizeItemName(name), discardQty);
                                }
                            }
                        }
                    }

                    updateSaleSummaryValue(salesAmountMap);
                    updateSaleBalanceTotal();

                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String itemName = normalizeItemName(tableModel.getValueAt(row, 0).toString());
                        Double saleQty = salesMap.get(itemName);
                        Double discardQty = discardMap.get(itemName);
                        String displayValue = "";
                        if (saleQty != null && discardQty != null) {
                            displayValue = formatInventoryValue(saleQty) + "+" + formatInventoryValue(discardQty);
                        } else if (saleQty != null) {
                            displayValue = formatInventoryValue(saleQty);
                        } else if (discardQty != null) {
                            displayValue = "+" + formatInventoryValue(discardQty);
                        }
                        tableModel.setValueAt(displayValue, row, 4);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Sale refresh failed: " + ex.getMessage());
            }
        }

        private void saveCurrentRecordToDB() {
            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()); // SQL Date Format
            String url = QadriStore.this.url;
            String user = QadriStore.this.user;
            String pass = QadriStore.this.pass;
            
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                
                String checkSql = "SELECT COUNT(*) FROM " + STOCK_DB_TABLE + " WHERE RecordDate = ? AND ItemName = ?";
                String insertSql = "INSERT INTO " + STOCK_DB_TABLE + " (RecordDate, ItemName, NewStock, Yesterday, Today, Sale, Vendor1Qty, Vendor2Qty) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                String updateSql = "UPDATE " + STOCK_DB_TABLE + " SET NewStock=?, Yesterday=?, Today=?, Sale=?, Vendor1Qty=?, Vendor2Qty=? WHERE RecordDate=? AND ItemName=?";

                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    String itemName = tableModel.getValueAt(row, 0).toString().trim();
                    String todayValue = tableModel.getValueAt(row, 3).toString();
                    tableModel.setValueAt(todayValue, row, 2);
                    double newStock = parseDouble(tableModel.getValueAt(row, 1));
                    double yesterday = parseDouble(tableModel.getValueAt(row, 2));
                    double today = parseDouble(tableModel.getValueAt(row, 3));
                    double sale = parseDouble(tableModel.getValueAt(row, 4));
                    
                    double vendor1Qty = 0.0;
                    double vendor2Qty = 0.0;
                    
                    // Right Panel Quantity dhundhein
                    String normalizedItem = normalizeItemName(itemName);
                    JTextField[] qtyFields = purchaseInvoiceQtyFields.get(normalizedItem);
                    if (qtyFields != null) {
                        vendor1Qty = parseDouble(qtyFields[0].getText());
                        vendor2Qty = parseDouble(qtyFields[1].getText());
                    }

                    // Check if record exists for today
                    boolean recordExists = false;
                    try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                        checkPs.setString(1, currentDate);
                        checkPs.setString(2, itemName);
                        try (ResultSet rs = checkPs.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) recordExists = true;
                        }
                    }

                    // Insert or Update
                    if (recordExists) {
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setDouble(1, newStock);
                            updatePs.setDouble(2, yesterday);
                            updatePs.setDouble(3, today);
                            updatePs.setDouble(4, sale);
                            updatePs.setDouble(5, vendor1Qty);
                            updatePs.setDouble(6, vendor2Qty);
                            updatePs.setString(7, currentDate);
                            updatePs.setString(8, itemName);
                            updatePs.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                            insertPs.setString(1, currentDate);
                            insertPs.setString(2, itemName);
                            insertPs.setDouble(3, newStock);
                            insertPs.setDouble(4, yesterday);
                            insertPs.setDouble(5, today);
                            insertPs.setDouble(6, sale);
                            insertPs.setDouble(7, vendor1Qty);
                            insertPs.setDouble(8, vendor2Qty);
                            insertPs.executeUpdate();
                        }
                    }
                }

                double lastMojoodValue = 0.0;
                if (majoodField != null) {
                    lastMojoodValue = parseDouble(majoodField.getText());
                }

                String summaryItemName = SUMMARY_MOJOOD_ITEM;
                String summaryCheckSql = "SELECT COUNT(*) FROM " + STOCK_DB_TABLE + " WHERE RecordDate = ? AND ItemName = ?";
                String summaryInsertSql = "INSERT INTO " + STOCK_DB_TABLE + " (RecordDate, ItemName, NewStock, Yesterday, Today, Sale, Vendor1Qty, Vendor2Qty) VALUES (?, ?, 0, ?, 0, 0, 0, 0)";
                String summaryUpdateSql = "UPDATE " + STOCK_DB_TABLE + " SET Yesterday = ? WHERE RecordDate = ? AND ItemName = ?";
                boolean summaryExists = false;
                try (PreparedStatement summaryCheckPs = conn.prepareStatement(summaryCheckSql)) {
                    summaryCheckPs.setString(1, currentDate);
                    summaryCheckPs.setString(2, summaryItemName);
                    try (ResultSet rs = summaryCheckPs.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            summaryExists = true;
                        }
                    }
                }
                if (summaryExists) {
                    try (PreparedStatement summaryUpdatePs = conn.prepareStatement(summaryUpdateSql)) {
                        summaryUpdatePs.setDouble(1, lastMojoodValue);
                        summaryUpdatePs.setString(2, currentDate);
                        summaryUpdatePs.setString(3, summaryItemName);
                        summaryUpdatePs.executeUpdate();
                    }
                } else {
                    try (PreparedStatement summaryInsertPs = conn.prepareStatement(summaryInsertSql)) {
                        summaryInsertPs.setString(1, currentDate);
                        summaryInsertPs.setString(2, summaryItemName);
                        summaryInsertPs.setDouble(3, lastMojoodValue);
                        summaryInsertPs.executeUpdate();
                    }
                }
                
                btnSaveRecord.setText("Saved!");
                btnSaveRecord.setBackground(new Color(100, 100, 100));
                Timer timer = new Timer(2000, e -> {
                    btnSaveRecord.setText("Save Record");
                    btnSaveRecord.setBackground(new Color(40, 167, 69));
                });
                timer.setRepeats(false);
                timer.start();

                JOptionPane.showMessageDialog(this, "Record for " + currentDate + " saved to DB successfully!", "DB Save Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving record to DB: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void openRecordsHistoryWindow() {
            RecordsHistoryWindow historyWindow = new RecordsHistoryWindow();
            historyWindow.setVisible(true);
        }

        private class RecordsHistoryWindow extends JFrame {
            private JList<String> dateList;
            private DefaultListModel<String> listModel;

            RecordsHistoryWindow() {
                super("Chakki Stock Book - Saved Records");
                setIconImage(QadriStore.this.getIconImage());
                setSize(500, 400);
                setLocationRelativeTo(ChakkiStockBookWindow.this);
                setLayout(new BorderLayout(10, 10));

                listModel = new DefaultListModel<>();
                loadDatesFromDB();
                
                dateList = new JList<>(listModel);
                dateList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                JScrollPane scrollPane = new JScrollPane(dateList);
                add(scrollPane, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
                
                JButton btnEdit = new JButton("Edit / View");
                btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnEdit.setBackground(new Color(0, 120, 215));
                btnEdit.setForeground(Color.BLACK);
                btnEdit.addActionListener(e -> openEditWindowForSelectedDate());
                
                JButton btnDelete = new JButton("Delete Date");
                btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnDelete.setBackground(new Color(220, 53, 69));
                btnDelete.setForeground(Color.BLACK);
                btnDelete.addActionListener(e -> deleteSelectedDateFromDB());
                
                JButton btnClose = new JButton("Close");
                btnClose.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnClose.addActionListener(e -> dispose());
                
                btnPanel.add(btnEdit);
                btnPanel.add(btnDelete);
                btnPanel.add(btnClose);
                add(btnPanel, BorderLayout.SOUTH);
            }

            private void loadDatesFromDB() {
                listModel.clear();
                String url = QadriStore.this.url;
                String user = QadriStore.this.user;
                String pass = QadriStore.this.pass;
                
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    String sql = "SELECT DISTINCT RecordDate FROM " + STOCK_DB_TABLE + " ORDER BY RecordDate DESC";
                    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            listModel.addElement(rs.getString("RecordDate"));
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error loading dates: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            private void openEditWindowForSelectedDate() {
                String selectedDate = dateList.getSelectedValue();
                if (selectedDate == null) {
                    JOptionPane.showMessageDialog(this, "Please select a date first!");
                    return;
                }
                new RecordEditWindow(selectedDate).setVisible(true);
            }

            private void deleteSelectedDateFromDB() {
                String selectedDate = dateList.getSelectedValue();
                if (selectedDate == null) {
                    JOptionPane.showMessageDialog(this, "Please select a date to delete!");
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(this, "Delete ALL records of " + selectedDate + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String url = QadriStore.this.url;
                    String user = QadriStore.this.user;
                    String pass = QadriStore.this.pass;
                    
                    try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                        String sql = "DELETE FROM " + STOCK_DB_TABLE + " WHERE RecordDate = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, selectedDate);
                            ps.executeUpdate();
                        }
                        listModel.removeElement(selectedDate);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error deleting: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        private class RecordEditWindow extends JFrame {
            private JTable editTable;
            private DefaultTableModel editTableModel;
            private final String recordDate;
            private java.util.Map<String, JTextField[]> editFieldsMap = new java.util.LinkedHashMap<>();

            RecordEditWindow(String date) {
                super("Edit Record - " + date);
                this.recordDate = date;
                setIconImage(QadriStore.this.getIconImage());
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                setLocationRelativeTo(ChakkiStockBookWindow.this);
                setLayout(new BorderLayout(8, 8));

                JPanel headerPanel = new JPanel(new BorderLayout());
                JLabel titleLbl = new JLabel("EDITING RECORD: " + date);
                titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
                titleLbl.setForeground(Color.RED);
                headerPanel.add(titleLbl, BorderLayout.WEST);
                add(headerPanel, BorderLayout.NORTH);

                String[] columns = {"Item Name", "New Stock", "Yesterday", "Today", "Sale"};
                editTableModel = new DefaultTableModel(columns, 0) {
                    @Override public boolean isCellEditable(int row, int column) { return column > 0; }
                };
                editTable = new JTable(editTableModel);
                editTable.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                editTable.setRowHeight(28);
                editTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                for (int i = 0; i < columns.length; i++) {
                    editTable.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 400 : 100);
                }
                JScrollPane tableScroll = new JScrollPane(editTable);
                
                JPanel rightPanel = new JPanel(new GridBagLayout());
                rightPanel.setBorder(BorderFactory.createTitledBorder("Purchase Invoice Data (Editable)"));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(4, 4, 4, 4);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                loadRecordDataFromDB(rightPanel, gbc);

                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, rightPanel);
                splitPane.setResizeWeight(0.70);
                add(splitPane, BorderLayout.CENTER);

                JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton btnSaveChanges = new JButton("Save Changes to DB");
                btnSaveChanges.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnSaveChanges.setBackground(new Color(40, 167, 69));
                btnSaveChanges.setForeground(Color.BLACK);
                btnSaveChanges.addActionListener(e -> saveEditedRecordToDB());
                
                JButton btnCancel = new JButton("Cancel");
                btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                btnCancel.addActionListener(e -> dispose());
                
                footer.add(btnSaveChanges);
                footer.add(btnCancel);
                add(footer, BorderLayout.SOUTH);
            }

            private void loadRecordDataFromDB(JPanel rightPanel, GridBagConstraints gbc) {
                String url = QadriStore.this.url;
                String user = QadriStore.this.user;
                String pass = QadriStore.this.pass;
                
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    String sql = "SELECT ItemName, NewStock, Yesterday, Today, Sale, Vendor1Qty, Vendor2Qty FROM " + STOCK_DB_TABLE + " WHERE RecordDate = ? ORDER BY RecordId";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, recordDate);
                        try (ResultSet rs = ps.executeQuery()) {
                            int rightRow = 0;
                            while (rs.next()) {
                                String itemName = rs.getString("ItemName");
                                double newStock = rs.getDouble("NewStock");
                                double yesterday = rs.getDouble("Yesterday");
                                double today = rs.getDouble("Today");
                                double sale = rs.getDouble("Sale");
                                double vendor1Qty = rs.getDouble("Vendor1Qty");
                                double vendor2Qty = rs.getDouble("Vendor2Qty");

                                Object[] row = new Object[]{
                                    itemName, 
                                    newStock == 0 ? "" : formatInventoryValue(newStock),
                                    yesterday == 0 ? "" : formatInventoryValue(yesterday),
                                    today == 0 ? "" : formatInventoryValue(today),
                                    sale == 0 ? "" : formatInventoryValue(sale)
                                };
                                editTableModel.addRow(row);

                                String normalizedItem = normalizeItemName(itemName);
                                if (purchaseInvoiceQtyFields.containsKey(normalizedItem)) {
                                    gbc.gridx = 0; gbc.gridy = rightRow; gbc.gridwidth = 1;
                                    JLabel lbl = new JLabel(itemName);
                                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                                    rightPanel.add(lbl, gbc);

                                    gbc.gridx = 1;
                                    JTextField txt1 = new JTextField(vendor1Qty == 0 ? "" : formatInventoryValue(vendor1Qty));
                                    txt1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                                    rightPanel.add(txt1, gbc);

                                    gbc.gridx = 2;
                                    JTextField txt2 = new JTextField(vendor2Qty == 0 ? "" : formatInventoryValue(vendor2Qty));
                                    txt2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                                    rightPanel.add(txt2, gbc);

                                    editFieldsMap.put(normalizedItem, new JTextField[]{txt1, txt2});
                                    rightRow++;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error loading record: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            private void saveEditedRecordToDB() {
                String url = QadriStore.this.url;
                String user = QadriStore.this.user;
                String pass = QadriStore.this.pass;
                
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    String updateSql = "UPDATE " + STOCK_DB_TABLE + " SET NewStock=?, Yesterday=?, Today=?, Sale=?, Vendor1Qty=?, Vendor2Qty=? WHERE RecordDate=? AND ItemName=?";

                    for (int row = 0; row < editTableModel.getRowCount(); row++) {
                        String itemName = editTableModel.getValueAt(row, 0).toString().trim();
                        double newStock = parseDouble(editTableModel.getValueAt(row, 1));
                        double yesterday = parseDouble(editTableModel.getValueAt(row, 2));
                        double today = parseDouble(editTableModel.getValueAt(row, 3));
                        double sale = parseDouble(editTableModel.getValueAt(row, 4));

                        double vendor1Qty = 0.0;
                        double vendor2Qty = 0.0;
                        String normalizedItem = normalizeItemName(itemName);
                        if (editFieldsMap.containsKey(normalizedItem)) {
                            JTextField[] fields = editFieldsMap.get(normalizedItem);
                            vendor1Qty = parseDouble(fields[0].getText());
                            vendor2Qty = parseDouble(fields[1].getText());
                        }

                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setDouble(1, newStock);
                            updatePs.setDouble(2, yesterday);
                            updatePs.setDouble(3, today);
                            updatePs.setDouble(4, sale);
                            updatePs.setDouble(5, vendor1Qty);
                            updatePs.setDouble(6, vendor2Qty);
                            updatePs.setString(7, recordDate);
                            updatePs.setString(8, itemName);
                            updatePs.executeUpdate();
                        }
                    }

                    JOptionPane.showMessageDialog(this, "Changes saved to DB successfully for " + recordDate, "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error saving changes: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    }

    private String formatInventoryValue(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void openNewPurchaseInvoiceEntryWindow() {
        try {
            updateUrl();
            NewPurchaseInvoiceEntry invoiceWindow = new NewPurchaseInvoiceEntry(this, url, user, pass);
            invoiceWindow.setVisible(true);
            invoiceWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to open purchase invoice form: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditPurchaseInvoiceEntryWindow(int invoiceId) {
        try {
            updateUrl();
            NewPurchaseInvoiceEntry invoiceWindow = new NewPurchaseInvoiceEntry(this, url, user, pass, invoiceId);
            invoiceWindow.setVisible(true);
            invoiceWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to open purchase invoice form for editing: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openPurchaseInvoiceUIFromDashboard() {
        try {
            updateUrl();
            PurchaseInvoiceWindow invoiceUI = new PurchaseInvoiceWindow(this, url, user, pass);
            invoiceUI.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to open purchase invoice UI: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class PurchaseInvoiceRecordsWindow extends JFrame {
        private JTable recordsTable;
        private DefaultTableModel recordsModel;
        private final String dbUrl;
        private final String dbUser;
        private final String dbPass;
        private final JTextField txtDays;
        private final JButton btnToday;
        private final JButton btnYesterday;
        private final JButton btn7Days;
        private final JButton btn30Days;
        private final JButton btnGetRecords;

        PurchaseInvoiceRecordsWindow(Frame owner, String url, String user, String pass) {
            super("Purchase Invoice Records");
            this.dbUrl = url;
            this.dbUser = user;
            this.dbPass = pass;

            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(10, 10));
            setSize(1000, 700);
            setLocationRelativeTo(owner);

            File logoFile = getSavedLogoFile();
            if (logoFile != null && logoFile.exists()) {
                try {
                    Image icon = ImageIO.read(logoFile);
                    if (icon != null) {
                        setIconImage(icon);
                    }
                } catch (Exception ignored) {}
            } else {
                setIconImage(QadriStore.this.getIconImage());
            }

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
            JLabel titleLabel = new JLabel("Saved Purchase Invoices");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            topPanel.add(titleLabel, BorderLayout.WEST);

            JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnToday = new JButton("Today");
            btnYesterday = new JButton("Yesterday");
            btn7Days = new JButton("7 Days");
            btn30Days = new JButton("30 Days");
            txtDays = new JTextField(4);
            txtDays.setToolTipText("Custom days");
            btnGetRecords = new JButton("Get Record");

            for (JButton btn : new JButton[]{btnToday, btnYesterday, btn7Days, btn30Days, btnGetRecords}) {
                btn.setFocusPainted(false);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setOpaque(true);
                btn.setBorderPainted(true);
                btn.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));
                btn.setBackground(new Color(245, 245, 245));
                btn.setForeground(new Color(20, 20, 20));
            }

            btnToday.setBackground(new Color(240, 240, 240));
            btnToday.setForeground(new Color(20, 20, 20));
            btnYesterday.setBackground(new Color(240, 240, 240));
            btnYesterday.setForeground(new Color(20, 20, 20));
            btn7Days.setBackground(new Color(240, 240, 240));
            btn7Days.setForeground(new Color(20, 20, 20));
            btn30Days.setBackground(new Color(240, 240, 240));
            btn30Days.setForeground(new Color(20, 20, 20));
            btnGetRecords.setBackground(new Color(230, 230, 230));
            btnGetRecords.setForeground(new Color(20, 20, 20));

            btnToday.addActionListener(e -> {
                setFilterButtons(btnToday);
                loadInvoices(1);
            });
            btnYesterday.addActionListener(e -> {
                setFilterButtons(btnYesterday);
                loadInvoices(2);
            });
            btn7Days.addActionListener(e -> {
                setFilterButtons(btn7Days);
                loadInvoices(7);
            });
            btn30Days.addActionListener(e -> {
                setFilterButtons(btn30Days);
                loadInvoices(30);
            });
            btnGetRecords.addActionListener(e -> {
                try {
                    int days = Integer.parseInt(txtDays.getText().trim());
                    if (days > 0) {
                        setFilterButtons(btnGetRecords);
                        loadInvoices(days);
                    } else {
                        JOptionPane.showMessageDialog(this, "Days must be greater than 0", "Input Error", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number of days", "Input Error", JOptionPane.WARNING_MESSAGE);
                }
            });

            JButton btnNew = new JButton("New");
            btnNew.setBackground(new Color(240, 240, 240));
            btnNew.setForeground(new Color(20, 20, 20));
            btnNew.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnNew.setFocusPainted(false);
            btnNew.setBorderPainted(true);
            btnNew.setOpaque(true);
            btnNew.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));
            btnNew.addActionListener(e -> openNewPurchaseInvoiceEntryWindow());

            JButton btnEdit = new JButton("Edit");
            btnEdit.setBackground(new Color(240, 240, 240));
            btnEdit.setForeground(new Color(20, 20, 20));
            btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnEdit.setFocusPainted(false);
            btnEdit.setBorderPainted(true);
            btnEdit.setOpaque(true);
            btnEdit.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));
            btnEdit.addActionListener(e -> {
                int selectedRow = recordsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Pehle invoice select karein!");
                    return;
                }
                int invoiceId = (int) recordsModel.getValueAt(selectedRow, 0);
                openEditPurchaseInvoiceEntryWindow(invoiceId);
            });

            rightControls.add(btnToday);
            rightControls.add(btnYesterday);
            rightControls.add(btn7Days);
            rightControls.add(btn30Days);
            rightControls.add(new JLabel("Days:"));
            rightControls.add(txtDays);
            rightControls.add(btnGetRecords);
            rightControls.add(btnNew);
            rightControls.add(btnEdit);
            topPanel.add(rightControls, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);

            String[] columns = {"Invoice ID", "Invoice No", "Date", "Vendor", "Net Amount"};
            recordsModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            recordsTable = new JTable(recordsModel);
            recordsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            recordsTable.setRowHeight(24);
            recordsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            add(new JScrollPane(recordsTable), BorderLayout.CENTER);

            loadInvoices(1);
        }

        private void setFilterButtons(JButton selectedButton) {
            btnToday.setBackground(new Color(240, 240, 240));
            btnToday.setForeground(new Color(20, 20, 20));
            btnYesterday.setBackground(new Color(240, 240, 240));
            btnYesterday.setForeground(new Color(20, 20, 20));
            btn7Days.setBackground(new Color(240, 240, 240));
            btn7Days.setForeground(new Color(20, 20, 20));
            btn30Days.setBackground(new Color(240, 240, 240));
            btn30Days.setForeground(new Color(20, 20, 20));
            btnGetRecords.setBackground(new Color(230, 230, 230));
            btnGetRecords.setForeground(new Color(20, 20, 20));

            selectedButton.setBackground(new Color(220, 220, 220));
            selectedButton.setForeground(new Color(20, 20, 20));
        }

        private void loadInvoices(int days) {
            recordsModel.setRowCount(0);
            String sql = "SELECT pi.PurchaseInvoiceId, pi.InvoiceNumber, pi.PurchaseInvoiceDate, pv.Name, " +
                         "COALESCE(pi.NetAmount, (SELECT SUM(pii.Quantity * pii.Price) FROM PurchaseInvoiceItem pii WHERE pii.PurchaseInvoiceId = pi.PurchaseInvoiceId)) AS NetAmount " +
                         "FROM PurchaseInvoice pi " +
                         "INNER JOIN ProductVendor pv ON pi.ProductVendorId = pv.ProductVendorId " +
                         "WHERE pi.PurchaseInvoiceDate >= DATEADD(day, -?, GETDATE()) " +
                         "ORDER BY pi.PurchaseInvoiceId DESC";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, days);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal netAmount = rs.getBigDecimal("NetAmount");
                        if (netAmount == null) {
                            netAmount = BigDecimal.ZERO;
                        }
                        recordsModel.addRow(new Object[]{
                                rs.getInt("PurchaseInvoiceId"),
                                rs.getInt("InvoiceNumber"),
                                rs.getString("PurchaseInvoiceDate"),
                                rs.getString("Name"),
                                formatCurrency(netAmount)
                        });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Unable to load invoices: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleWindowCloseRequest() {
        if (getContentPane() == dashboardPanel) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to exit?",
                    "Exit System",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                saveTableLayout();
                saveFullDataToLocalCache();
                saveSalesModeState();
                if (liveUpdateTimer != null) liveUpdateTimer.stop();
                if (localCacheTimer != null) localCacheTimer.stop();
                System.exit(0);
            }
            return;
        }

        saveTableLayout();
        saveFullDataToLocalCache();
        saveSalesModeState();
        if (liveUpdateTimer != null) liveUpdateTimer.stop();
        if (localCacheTimer != null) localCacheTimer.stop();
        showDashboardUI();
    }

    private void setupMainGUI() {
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);

        // --- LEFT PANEL ---
        leftPanel = new JPanel(new BorderLayout());
        
        JPanel searchPanel = new JPanel(new BorderLayout());
        txtSearch = new JTextField();
        searchPanel.add(new JLabel("Search Tables: "), BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        
        tableList = new JTable();
        tableList.setModel(new DefaultTableModel(new Object[]{"Select", "Table Name"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 0) ? Boolean.class : String.class;
            }
        });
        
        tableList.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableList.getColumnModel().getColumn(1).setPreferredWidth(200);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tableList.getModel());
        tableList.setRowSorter(sorter);
        
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String text = txtSearch.getText();
                sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 1));
            }
        });

        leftPanel.add(searchPanel, BorderLayout.NORTH);
        
        JScrollPane leftScroll = new JScrollPane(tableList);
        styleScrollPane(leftScroll);
        leftPanel.add(leftScroll, BorderLayout.CENTER);

        btnLoad = new JButton("Save & Load Selected");
        btnLoad.setBackground(new Color(173, 216, 230));
        leftPanel.add(btnLoad, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // --- RIGHT PANEL ---
        rightWrapperPanel = new JPanel(new BorderLayout());
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JLabel lblProductSearch = new JLabel("Search:");
        lblProductSearch.setFont(new Font("Arial", Font.BOLD, 16)); 
        topPanel.add(lblProductSearch);

        txtProductSearch = new JTextField();
        txtProductSearch.setFont(new Font("Arial", Font.BOLD, 20)); 
        txtProductSearch.setForeground(Color.BLACK); 
        txtProductSearch.setPreferredSize(new Dimension(300, 40)); 
        topPanel.add(txtProductSearch);
        
        btnToggleView = new JButton("Show List");
        btnCloseTab = new JButton("Close Tab");
        
        btnAddVendor = new JButton("Add Vendor Name");
        btnAddVendor.setBackground(new Color(255, 200, 100)); 
        btnAddVendor.setOpaque(true); btnAddVendor.setBorderPainted(false);
        
        btnCompnyNumbers = new JButton("Compny Numbers");
        btnCompnyNumbers.setBackground(Color.ORANGE);
        btnCompnyNumbers.setForeground(Color.BLACK);
        btnCompnyNumbers.setFont(new Font("Arial", Font.BOLD, 12));
        btnCompnyNumbers.setOpaque(true); btnCompnyNumbers.setBorderPainted(false);
        
        btnLiveSales = new JToggleButton("Live Sales: ON");
        btnLiveSales.setBackground(new Color(0, 150, 0));
        btnLiveSales.setForeground(Color.WHITE);
        btnLiveSales.setFont(new Font("Arial", Font.BOLD, 12));
        btnLiveSales.setOpaque(true);
        btnLiveSales.setBorderPainted(false);
        btnLiveSales.setToolTipText("Toggle ON to fetch 30 Days Sale from DB, OFF to keep current values.");
        
        btnOrder = new JButton("Order");
        btnOrder.setBackground(new Color(102, 178, 255));
        btnOrder.setForeground(Color.BLACK);
        btnOrder.setFont(new Font("Arial", Font.BOLD, 12));
        btnOrder.setOpaque(true);
        btnOrder.setBorderPainted(false);
        btnOrder.setToolTipText("Show/Hide Order, Price and Print Options");
        
        btnClearOrder = new JButton("Clear Order/Price");
        btnClearOrder.setBackground(new Color(220, 53, 69)); 
        btnClearOrder.setForeground(Color.WHITE);
        btnClearOrder.setFont(new Font("Arial", Font.BOLD, 12));
        btnClearOrder.setOpaque(true);
        btnClearOrder.setBorderPainted(false);
        btnClearOrder.setToolTipText("Clear all Order and Price values from the list");
        
        btnPrint = new JButton("Print");
        btnPrint.setBackground(new Color(0, 150, 0));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Arial", Font.BOLD, 12));
        btnPrint.setOpaque(true);
        btnPrint.setBorderPainted(false);
        btnPrint.setVisible(false);
        btnPrint.setToolTipText("Open Print Preview for Current Order");
        
        btnConfig = new JButton("CONFIGURATION");
        btnConfig.setBackground(new Color(70, 130, 180));
        btnConfig.setForeground(Color.WHITE);
        btnConfig.setFont(new Font("Arial", Font.BOLD, 12));
        btnConfig.setMargin(new Insets(5, 15, 5, 15));
        btnConfig.setOpaque(true);
        btnConfig.setBorderPainted(false);
        
        btnDragMode = new JToggleButton("Drag Mode: OFF");
        btnDragMode.setBackground(new Color(255, 255, 255));
        btnDragMode.setToolTipText("Turn ON to drag rows up/down");

        btnMergeDuplicates = new JButton("Merge Same Names");
        btnMergeDuplicates.setBackground(new Color(199, 21, 133)); 
        btnMergeDuplicates.setForeground(Color.WHITE);
        btnMergeDuplicates.setFont(new Font("Arial", Font.BOLD, 12));
        btnMergeDuplicates.setOpaque(true);
        btnMergeDuplicates.setBorderPainted(false);

        topPanel.add(btnToggleView);
        topPanel.add(btnCloseTab);
        topPanel.add(btnAddVendor);
        topPanel.add(btnCompnyNumbers);
        topPanel.add(btnLiveSales); 
        topPanel.add(btnOrder);
        topPanel.add(btnClearOrder); 
        topPanel.add(btnPrint); 
        topPanel.add(btnDragMode); 
        topPanel.add(btnMergeDuplicates);
        
        rightWrapperPanel.add(topPanel, BorderLayout.NORTH);
        rightWrapperPanel.add(tabbedPane = new JTabbedPane(), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        statusPanel.add(lblVersion = new JLabel("Version " + CURRENT_VERSION), BorderLayout.WEST);
        statusPanel.add(lblStatus = new JLabel(" Loading data..."), BorderLayout.CENTER);
        rightWrapperPanel.add(statusPanel, BorderLayout.SOUTH);

        searchPaletteTable = new JTable(new DefaultTableModel(new Object[]{"Name", "Barcode"}, 0));
        searchPaletteTable.setAutoCreateRowSorter(true);
        searchPaletteTable.setDragEnabled(true);
        searchPaletteTable.setTransferHandler(new PaletteTransferHandler()); 
        searchPaletteTable.setBackground(new Color(240, 240, 255));
        
        searchPaletteScroll = new JScrollPane(searchPaletteTable);
        styleScrollPane(searchPaletteScroll);
        searchPaletteScroll.setBorder(BorderFactory.createTitledBorder("Search Results (Drag to Add)"));
        searchPaletteScroll.setPreferredSize(new Dimension(300, 0)); 
        
        splitPane.setRightComponent(rightWrapperPanel);
        add(splitPane);

        splitPane.setDividerLocation(0);
        leftPanel.setVisible(false);
        btnCloseTab.setVisible(false); 

        // --- HIDDEN KEYBOARD LISTENER FOR UNBLOCKING CONFIG ---
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (isConfigBlocked && e.getID() == KeyEvent.KEY_TYPED) {
                    char c = e.getKeyChar();
                    if (Character.isLetterOrDigit(c)) {
                        hiddenTypingBuffer.append(c);
                        
                        // Keep buffer length manageable
                        if (hiddenTypingBuffer.length() > UNLOCK_PASSWORD.length() + 10) {
                            String current = hiddenTypingBuffer.toString();
                            hiddenTypingBuffer = new StringBuilder(current.substring(current.length() - UNLOCK_PASSWORD.length()));
                        }
                        
                        // Check if unlock password typed
                        if (hiddenTypingBuffer.toString().endsWith(UNLOCK_PASSWORD)) {
                            unblockConfigButton();
                            hiddenTypingBuffer.setLength(0);
                        }
                    }
                }
                return false; // Don't consume the event, let it pass through
            }
        });

        // --- APPLY CONFIG BLOCK STATE TO BUTTON AND SEARCH ---
        applyConfigBlockState();

        btnLoad.addActionListener(e -> loadAndMergeSelectedTables());
        btnConfig.addActionListener(e -> {
            if (isConfigBlocked) {
                JOptionPane.showMessageDialog(this,
                    "<html><div style='text-align:center;'>" +
                    "<h2 style='color:red;'>⛔ CONFIGURATION IS BLOCKED</h2>" +
                    "<hr>" +
                    "<p style='font-size:14px;'><b>Unblock ke liye Muhammed Wasif se contact karein:</b></p>" +
                    "<p style='font-size:18px; color:blue;'><b>📞 03131134889</b></p>" +
                    "<p style='font-size:18px; color:blue;'><b>📞 03353323497</b></p>" +
                    "</div></html>",
                    "CONFIGURATION BLOCKED",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!verifyPassword()) return;
            showConfigDialog();
        });
        btnMergeDuplicates.addActionListener(e -> { if (!verifyPassword()) return; mergeDuplicateRows(); });
        btnOrder.addActionListener(e -> toggleOrderPriceAndPrint());
        btnPrint.addActionListener(e -> showPrintPreview());
        
        btnLiveSales.addActionListener(e -> {
            if (!verifyPassword()) {
                btnLiveSales.setSelected(!btnLiveSales.isSelected());
                return;
            }
            toggleLiveSalesMode();
        });
        
        btnClearOrder.addActionListener(e -> {
            if (resultTable == null) {
                JOptionPane.showMessageDialog(this, "Table not loaded.");
                return;
            }
            if (!verifyPassword()) return; 
            
            DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt("", i, 2); 
                model.setValueAt("", i, 3); 
            }
            saveTableLayout();
            lblStatus.setText("Order and Price columns cleared.");
        });
        
        txtProductSearch.addActionListener(e -> performSearchJump());
        
        txtProductSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { if (btnDragMode.isSelected()) updateSearchPalette(txtProductSearch.getText()); }
        });
        
        btnCompnyNumbers.addActionListener(e -> showVendorNumbersDialog());
        
        btnToggleView.addActionListener(e -> {
            if (!verifyPassword()) return; 
            if (leftPanel.isVisible()) {
                leftPanel.setVisible(false); splitPane.setDividerLocation(0);
                btnToggleView.setText("Show List"); btnCloseTab.setVisible(false);
            } else {
                leftPanel.setVisible(true); splitPane.setDividerLocation(250);
                btnToggleView.setText("Hide List"); btnCloseTab.setVisible(true);
            }
        });
        
        btnCloseTab.addActionListener(e -> {
            if (tabbedPane.getSelectedIndex() != -1) {
                tabbedPane.remove(tabbedPane.getSelectedIndex());
                if (liveUpdateTimer != null) liveUpdateTimer.stop();
                if (localCacheTimer != null) localCacheTimer.stop();
            }
        });

        btnAddVendor.addActionListener(e -> {
            if (!verifyPassword()) return; 
            isVendorInsertMode = !isVendorInsertMode;
            if (isVendorInsertMode) {
                btnAddVendor.setText("Click Table Row"); btnAddVendor.setBackground(Color.RED);
                lblStatus.setText("Mode: Click on table to insert Vendor Row");
            } else {
                btnAddVendor.setText("Add Vendor Name"); btnAddVendor.setBackground(new Color(255, 200, 100));
                lblStatus.setText("Mode: Normal");
            }
        });

        btnDragMode.addActionListener(e -> {
            if (!verifyPassword()) { btnDragMode.setSelected(!btnDragMode.isSelected()); return; }
            if (btnDragMode.isSelected()) {
                btnDragMode.setText("Drag Mode: ON"); btnDragMode.setBackground(Color.GREEN);
                rightWrapperPanel.add(searchPaletteScroll, BorderLayout.EAST);
                rightWrapperPanel.revalidate(); rightWrapperPanel.repaint();
                if (resultTable != null) {
                    resultTable.setDragEnabled(true);
                    resultTable.setDropMode(DropMode.INSERT_ROWS);
                    resultTable.setTransferHandler(new TableRowTransferHandler(resultTable));
                }
                updateSearchPalette(txtProductSearch.getText());
                lblStatus.setText("Drag Mode ON.");
            } else {
                btnDragMode.setText("Drag Mode: OFF"); btnDragMode.setBackground(new Color(255, 255, 255));
                rightWrapperPanel.remove(searchPaletteScroll);
                rightWrapperPanel.revalidate(); rightWrapperPanel.repaint();
                if (resultTable != null) {
                    resultTable.setDragEnabled(false);
                    resultTable.setTransferHandler(null);
                }
                lblStatus.setText("Drag Mode OFF.");
            }
        });
        
        updateLiveSalesButtonUI();
    }

    // ==================== CONFIG BLOCK & ENCRYPTION METHODS ====================

    private String encrypt(String data) {
        try {
            byte[] bytes = data.getBytes("UTF-8");
            byte[] encrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                encrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            }
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return "";
        }
    }

    private String decrypt(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            byte[] decrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                decrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            }
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private void loadConfigBlockState() {
        File f = new File(configBlockFile);
        if (f.exists()) {
            try {
                String encryptedContent = new String(Files.readAllBytes(f.toPath()));
                String decryptedContent = decrypt(encryptedContent.trim());
                // FIX: Use .equals() instead of .contains() because "UNBLOCKED" contains the word "BLOCKED"
                isConfigBlocked = decryptedContent.trim().equals("BLOCKED");
            } catch (Exception e) {
                isConfigBlocked = false;
            }
        } else {
            isConfigBlocked = false;
        }
    }

    private void saveConfigBlockState() {
        try {
            String plainText = isConfigBlocked ? "BLOCKED" : "UNBLOCKED";
            String encrypted = encrypt(plainText);
            try (PrintWriter writer = new PrintWriter(new FileWriter(configBlockFile))) {
                writer.print(encrypted);
            }
        } catch (Exception e) {
            System.err.println("Error saving config block state.");
        }
    }

    private void applyConfigBlockState() {
        if (isConfigBlocked) {
            btnConfig.setText("⛔ BLOCKED");
            btnConfig.setBackground(new Color(139, 0, 0));
            btnConfig.setForeground(Color.WHITE);
            btnConfig.setFont(new Font("Arial", Font.BOLD, 12));
            btnConfig.setToolTipText("CONFIGURATION IS BLOCKED - Contact Muhammed Wasif to unblock");
            
            txtProductSearch.setEnabled(false);
            txtProductSearch.setBackground(new Color(220, 220, 220));
        } else {
            btnConfig.setText("CONFIGURATION");
            btnConfig.setBackground(new Color(70, 130, 180));
            btnConfig.setForeground(Color.WHITE);
            btnConfig.setFont(new Font("Arial", Font.BOLD, 12));
            btnConfig.setToolTipText("Open Database Configuration");
            
            txtProductSearch.setEnabled(true);
            txtProductSearch.setBackground(Color.WHITE);
        }
    }

    private void blockConfigButton() {
        isConfigBlocked = true;
        saveConfigBlockState();
        applyConfigBlockState();

        JOptionPane.showMessageDialog(this,
            "<html><div style='text-align:center;'>" +
            "<h2 style='color:red;'>⛔ CONFIGURATION BLOCKED!</h2>" +
            "<hr>" +
            "<p style='font-size:14px;'>Galat password ki wajah se Configuration block ho gaya hai.</p>" +
            "<p style='font-size:14px;'><b>Unblock ke liye Muhammed Wasif se contact karein:</b></p>" +
            "<p style='font-size:20px; color:blue;'><b>📞 03131134889</b></p>" +
            "<p style='font-size:20px; color:blue;'><b>📞 03353323497</b></p>" +
            "</div></html>",
            "CONFIGURATION BLOCKED",
            JOptionPane.ERROR_MESSAGE);
    }

    private void unblockConfigButton() {
        isConfigBlocked = false;
        saveConfigBlockState();
        applyConfigBlockState();

        JOptionPane.showMessageDialog(this,
            "<html><div style='text-align:center;'>" +
            "<h2 style='color:green;'>✅ CONFIGURATION UNBLOCKED!</h2>" +
            "<p style='font-size:14px;'>Configuration button aur Search box ab active hain.</p>" +
            "</div></html>",
            "UNBLOCKED",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean verifyConfigPassword() {
        JDialog dialog = new JDialog(this, "Configuration Authentication", true);
        dialog.setIconImage(this.getIconImage());
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 245, 255));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(5, 5, 15, 5);
        
        JLabel iconLabel = new JLabel("🔒");
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 40));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(iconLabel, gbc);
        
        gbc.gridy = 1;
        JLabel label = new JLabel("Enter Configuration Password:");
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(label, gbc);
        
        gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Arial", Font.BOLD, 18));
        passField.setHorizontalAlignment(JPasswordField.CENTER);
        centerPanel.add(passField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(new Color(245, 245, 255));
        JButton btnOk = new JButton("VERIFY");
        btnOk.setBackground(new Color(0, 120, 60));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(new Font("Arial", Font.BOLD, 13));
        btnOk.setOpaque(true);
        btnOk.setBorderPainted(false);
        btnOk.setPreferredSize(new Dimension(120, 38));
        
        JButton btnCancel = new JButton("CANCEL");
        btnCancel.setBackground(new Color(150, 150, 150));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Arial", Font.BOLD, 13));
        btnCancel.setOpaque(true);
        btnCancel.setBorderPainted(false);
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnPanel.add(btnOk);
        btnPanel.add(btnCancel);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        final boolean[] result = {false};

        btnOk.addActionListener(e -> {
            char[] password = passField.getPassword();
            String passStr = new String(password);
            if (CONFIG_PASSWORD.equals(passStr)) {
                result[0] = true;
                dialog.dispose();
            } else {
                dialog.dispose(); // Close password dialog first
                // BLOCK the config button and search box
                blockConfigButton();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());
        passField.addActionListener(e -> btnOk.doClick());
        SwingUtilities.invokeLater(() -> passField.requestFocusInWindow());
        dialog.setVisible(true);
        return result[0];
    }

    private void checkForUpdates() {
        if (!isInternetAvailable()) {
            return;
        }

        new SwingWorker<Void, Void>() {
            String latestVersion = "";
            boolean updateAvailable = false;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    URL url = new URL(GITHUB_RAW_VERSION_URL);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    latestVersion = reader.readLine().trim();
                    reader.close();

                    if (!latestVersion.equals(CURRENT_VERSION)) {
                        updateAvailable = true;
                    }
                } catch (Exception e) {
                    System.err.println("GitHub Error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                if (updateAvailable) {
                    // Yahan se JOptionPane (Popup) hata diya gaya hai
                    // Update direct chup chap download hone lagega
                    downloadAndPrepareUpdate();
                }
            }
        }.execute();
    }

    private boolean isInternetAvailable() {
        try {
            URL url = new URL("http://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            return (connection.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }

    private void downloadAndPrepareUpdate() {
        new SwingWorker<Void, Void>() {
            boolean success = false;
            File tempExe;
            String errorMsg = "";

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    URL url = new URL(GITHUB_RAW_EXE_URL);
                    InputStream in = url.openStream();
                    
                    String tempDir = System.getProperty("java.io.tmpdir");
                    tempExe = new File(tempDir, "QadriStore_new.exe");
                    
                    FileOutputStream out = new FileOutputStream(tempExe);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.close();
                    in.close();
                    
                    // Check karna ke file download hui ya khali (corrupt) aayi
                    if (tempExe.length() > 0) {
                        success = true;
                    } else {
                        errorMsg = "Downloaded file is empty or corrupt!";
                    }
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    try {
                        // Current EXE ka path
                        File currentExeFile = new File(QadriStore.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                        File appDir = currentExeFile.getParentFile();
                        
                        if (appDir == null) {
                            JOptionPane.showMessageDialog(null, "Error: App directory nahi mil rahi.");
                            return;
                        }

                        // Bat file jo replace karegi
                        File batFile = new File(appDir, "QadriUpdater.bat");
                        
                        String batchContent = "@echo off\n" +
                                "timeout /t 3 /nobreak >nul\n" + // 3 second wait
                                "del \"" + currentExeFile.getAbsolutePath() + "\"\n" +
                                "move /Y \"" + tempExe.getAbsolutePath() + "\" \"" + currentExeFile.getAbsolutePath() + "\"\n" +
                                "start \"\" \"" + currentExeFile.getAbsolutePath() + "\"\n" +
                                "del \"" + batFile.getAbsolutePath() + "\"";
                                
                        try (PrintWriter batWriter = new PrintWriter(new FileWriter(batFile))) {
                            batWriter.print(batchContent);
                        }

                        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", batFile.getAbsolutePath());
                        pb.directory(appDir);
                        pb.start();
                        
                        // App band ho rahi hai
                        System.exit(0);
                        
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "Replace Error: " + e.getMessage());
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Update Download Fail Hua: " + errorMsg);
                }
            }
        }.execute();
    }

    // ==================== END CONFIG BLOCK METHODS ====================
    
    private void toggleLiveSalesMode() {
        isLiveSalesActive = !isLiveSalesActive;
        
        if (isLiveSalesActive) {
            lastSalesFetchTime = System.currentTimeMillis();
            saveSalesModeState();
            lblStatus.setText("Live Sales Mode ON. Fetching 30 Days Sale from DB...");
            if (resultTable != null) {
                updateSalesColumnFromDB();
            }
        } else {
            saveCurrentSalesToLocalFile();
            lastSalesFetchTime = System.currentTimeMillis(); 
            saveSalesModeState();
            lblStatus.setText("Live Sales Mode OFF. 30 Days Sale frozen. Inventory still live.");
            checkSalesExpiry();
        }
        
        updateLiveSalesButtonUI();
    }
    
    private void updateLiveSalesButtonUI() {
        if (isLiveSalesActive) {
            btnLiveSales.setText("Live Sales: ON");
            btnLiveSales.setBackground(new Color(0, 150, 0));
            btnLiveSales.setSelected(false);
        } else {
            btnLiveSales.setText("Live Sales: OFF");
            btnLiveSales.setBackground(new Color(255, 0, 0));
            btnLiveSales.setSelected(true);
            checkSalesExpiry();
        }
    }
    
    private void saveSalesModeState() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(salesModeFile))) {
            writer.println(isLiveSalesActive);
            writer.println(lastSalesFetchTime);
        } catch (Exception e) {
            System.err.println("Error saving sales mode state.");
        }
    }
    
    private void loadSalesModeState() {
        File f = new File(salesModeFile);
        if (f.exists()) {
            try (Scanner sc = new Scanner(f)) {
                if (sc.hasNextBoolean()) isLiveSalesActive = sc.nextBoolean();
                if (sc.hasNextLong()) lastSalesFetchTime = sc.nextLong();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            isLiveSalesActive = true;
            lastSalesFetchTime = System.currentTimeMillis();
        }
    }
    
    private void saveCurrentSalesToLocalFile() {
        if (resultTable == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineSalesFile))) {
            DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String bc = (String) model.getValueAt(i, 0);
                if (VENDOR_MARKER.equals(bc)) continue;
                
                Object saleObj = model.getValueAt(i, 5);
                double sale = parseDouble(saleObj);
                
                writer.println(bc + "|" + sale);
            }
            lblStatus.setText("Sales data saved to local file.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadSalesFromLocalFile() {
        if (resultTable == null) return;
        
        Map<String, Double> localSales = new HashMap<>();
        File f = new File(offlineSalesFile);
        if (f.exists()) {
            try (Scanner sc = new Scanner(f)) {
                while (sc.hasNextLine()) {
                    String[] parts = sc.nextLine().split("\\|");
                    if (parts.length >= 2) {
                        localSales.put(parts[0], parseDouble(parts[1]));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String bc = (String) model.getValueAt(i, 0);
            if (VENDOR_MARKER.equals(bc)) continue;
            
            double sale = localSales.getOrDefault(bc, 0.0);
            model.setValueAt(sale, i, 5);
            
            double inv = parseDouble(model.getValueAt(i, 4));
            double demand = inv - sale;
            double daysStock = (sale > 0) ? Math.round((inv * 30.0) / sale) : 999;
            model.setValueAt(demand, i, 6);
            model.setValueAt(daysStock, i, 7);
        }
        lblStatus.setText("Sales loaded from local file.");
    }
    
    private void checkSalesExpiry() {
        if (isLiveSalesActive) return;
        
        long diffMillis = System.currentTimeMillis() - lastSalesFetchTime;
        long days = diffMillis / (1000 * 60 * 60 * 24);
        
        if (days >= 35) {
            JOptionPane.showMessageDialog(this, 
                "<html><b>Warning:</b><br>30 Days Sale data has been OFF for " + days + " days.<br>Data is being loaded from the local file and may be outdated.<br>Please turn ON Live Sales to update from Database.</html>", 
                "Sales Data Outdated", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void updateSalesColumnFromDB() {
        if (isOfflineMode) {
            lblStatus.setText("Cannot fetch sales: Offline Mode.");
            return;
        }
        
        new SwingWorker<Map<String, Double>, Void>() {
            @Override
            protected Map<String, Double> doInBackground() throws Exception {
                Map<String, Double> salesData = new HashMap<>();
                DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                Set<String> barcodes = new HashSet<>();
                Map<String, String> bcToNameLocal = new HashMap<>();
                
                for (int i = 0; i < model.getRowCount(); i++) {
                    String bc = (String) model.getValueAt(i, 0);
                    if (!VENDOR_MARKER.equals(bc)) {
                        barcodes.add(bc);
                        bcToNameLocal.put(bc, (String) model.getValueAt(i, 1));
                    }
                }
                
                if (barcodes.isEmpty()) return salesData;

                StringBuilder inNames = new StringBuilder();
                for (String bc : barcodes) {
                    String name = bcToNameLocal.get(bc);
                    if (name != null) inNames.append("'").append(name.replace("'", "''")).append("',");
                }
                
                if (inNames.length() == 0) return salesData;
                
                String inClause = inNames.substring(0, inNames.length() - 1);
                
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    if (!salesTable.isEmpty()) {
                        String sql = "SELECT [" + salesNameCol + "], [" + salesValueCol + "] FROM [" + salesTable + "] WHERE [" + salesNameCol + "] IN (" + inClause + ")";
                        ResultSet rs = conn.createStatement().executeQuery(sql);
                        while (rs.next()) {
                            String name = rs.getString(1);
                            double val = rs.getDouble(2);
                            String bc = nameToBarcodeMap.get(name.toLowerCase().trim());
                            if (bc != null) salesData.put(bc, val);
                        }
                        rs.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return salesData;
            }
            
            @Override
            protected void done() {
                try {
                    Map<String, Double> salesData = get();
                    DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                    
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String bc = (String) model.getValueAt(i, 0);
                        if (VENDOR_MARKER.equals(bc)) continue;
                        
                        double sale = salesData.getOrDefault(bc, 0.0);
                        model.setValueAt(sale, i, 5);
                        
                        double inv = parseDouble(model.getValueAt(i, 4));
                        double demand = inv - sale;
                        double daysStock = (sale > 0) ? Math.round((inv * 30.0) / sale) : 999;
                        
                        model.setValueAt(demand, i, 6);
                        model.setValueAt(daysStock, i, 7);
                    }
                    lblStatus.setText("30 Days Sales updated from Database.");
                } catch (Exception e) {
                    lblStatus.setText("Error updating sales: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    public class CustomRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            int modelColumn = table.convertColumnIndexToModel(column);
            String barcode = (String) table.getValueAt(row, 0); 
            
            setHorizontalAlignment(SwingConstants.CENTER); 
            setFont(new Font("Arial", Font.PLAIN, 16));

            if (VENDOR_MARKER.equals(barcode)) {
                if (modelColumn == 1) {
                    setText(value != null ? value.toString() : "");
                    setFont(new Font("Arial", Font.BOLD, 20)); 
                } else {
                    setText("");
                }
                setBackground(new Color(0, 100, 0)); 
                setForeground(Color.WHITE);
            } 
            else {
                setForeground(Color.BLACK);
                
                if (modelColumn == 2 || modelColumn == 3) { 
                    setBackground(new Color(255, 255, 224)); 
                    if (value == null || value.toString().isEmpty() || "0.0".equals(value.toString()) || "0".equals(value.toString())) {
                        setText("");
                    } else {
                        setText(value.toString());
                    }
                }
                else if (modelColumn == 4) setBackground(new Color(50, 205, 50)); 
                else if (modelColumn == 5) setBackground(new Color(255, 182, 193)); 
                else if (modelColumn == 6) { 
                    double val = parseDouble(value);
                    if (val < 0) { setBackground(new Color(128, 0, 128)); setForeground(Color.WHITE); } 
                    else setBackground(Color.WHITE);
                }
                else if (modelColumn == 7) { 
                    double val = parseDouble(value);
                    if (val <= 15 && val != 999) { setBackground(Color.RED); setForeground(Color.WHITE); } 
                    else setBackground(Color.WHITE);
                }
                else if (modelColumn == 0) setBackground(new Color(255, 215, 0)); 
                else setBackground(Color.WHITE);
                
                if (modelColumn > 3) {
                    setText(formatDisplayNumber(value));
                }
            }

            if (isSelected) {
                setBorder(new LineBorder(Color.BLUE, 3));
            } else {
                setBorder(noFocusBorder);
            }
            
            return c;
        }
    }

    private void setupTaskbarFeatures() {
        // JNA/Shell32 code removed to ensure compilation without external dependencies.
    }
    
    private void toggleOrderPriceAndPrint() {
        if (resultTable == null || orderColumn == null || priceColumn == null) {
            JOptionPane.showMessageDialog(this, "Table not loaded yet.");
            return;
        }
        TableColumnModel tcm = resultTable.getColumnModel();
        boolean isOrderVisible = false;
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            if (tcm.getColumn(i) == orderColumn) { isOrderVisible = true; break; }
        }
        if (isOrderVisible) {
            tcm.removeColumn(orderColumn); tcm.removeColumn(priceColumn);
            btnPrint.setVisible(false);
            lblStatus.setText("Order and Price columns hidden.");
        } else {
            tcm.addColumn(orderColumn); tcm.addColumn(priceColumn);
            tcm.moveColumn(tcm.getColumnCount() - 2, 2);
            tcm.moveColumn(tcm.getColumnCount() - 1, 3);
            btnPrint.setVisible(true);
            resizeColumnWidth(resultTable);
            lblStatus.setText("Order and Price columns shown.");
        }
    }
    
    private void showPrintPreview() {
        if (resultTable == null) return;

        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        ArrayList<Object[]> billItems = new ArrayList<>();
        double grandTotal = 0.0;

        for (int i = 0; i < model.getRowCount(); i++) {
            String barcode = (String) model.getValueAt(i, 0);
            if (VENDOR_MARKER.equals(barcode)) continue;

            Object orderObj = model.getValueAt(i, 2);
            Object priceObj = model.getValueAt(i, 3);

            double order = parseDouble(orderObj);
            double price = parseDouble(priceObj);
            
            if (order > 0 && price == 0) {
                double tp = parseDouble(model.getValueAt(i, 9)); 
                if (tp > 0) {
                    model.setValueAt(tp, i, 3);
                    lblStatus.setText("Auto-filled Price from TP for row " + (i+1));
                }
            }
        }
        saveTableLayout();

        for (int i = 0; i < model.getRowCount(); i++) {
            String barcode = (String) model.getValueAt(i, 0);
            if (VENDOR_MARKER.equals(barcode)) continue;

            String name = (String) model.getValueAt(i, 1);
            Object orderObj = model.getValueAt(i, 2);
            Object priceObj = model.getValueAt(i, 3); 

            double order = parseDouble(orderObj);
            double price = parseDouble(priceObj);

            if (order > 0) {
                double lineTotal = order * price;
                grandTotal += lineTotal;
                billItems.add(new Object[]{name, order, price, lineTotal});
            }
        }

        if (billItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items with Order quantity found.", "Empty Order", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel receiptPanel = createModernReceiptPanel(billItems, grandTotal);
        
        JDialog previewDialog = new JDialog(this, "Receipt Preview - 80mm Thermal", false);
        previewDialog.setIconImage(this.getIconImage());
        previewDialog.setSize(400, 750);
        previewDialog.setLocationRelativeTo(this);
        previewDialog.setLayout(new BorderLayout(5, 5));
        previewDialog.setBackground(new Color(60, 60, 60));

        JPanel receiptWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        receiptWrapper.setBackground(new Color(60, 60, 60));
        receiptWrapper.add(receiptPanel);

        JScrollPane scrollPane = new JScrollPane(receiptWrapper);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        previewDialog.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        btnPanel.setBackground(new Color(50, 50, 50));
        
        JButton btnPrintNow = new JButton("🖨️  PRINT NOW");
        btnPrintNow.setBackground(new Color(0, 120, 215));
        btnPrintNow.setForeground(Color.WHITE);
        btnPrintNow.setFont(new Font("Arial", Font.BOLD, 14));
        btnPrintNow.setOpaque(true);
        btnPrintNow.setBorderPainted(false);
        btnPrintNow.setPreferredSize(new Dimension(150, 45));
        btnPrintNow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton btnCancelPrint = new JButton("✕  CANCEL");
        btnCancelPrint.setBackground(new Color(100, 100, 100));
        btnCancelPrint.setForeground(Color.WHITE);
        btnCancelPrint.setFont(new Font("Arial", Font.BOLD, 12));
        btnCancelPrint.setOpaque(true);
        btnCancelPrint.setBorderPainted(false);
        btnCancelPrint.setPreferredSize(new Dimension(120, 45));
        btnCancelPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnPanel.add(btnPrintNow);
        btnPanel.add(btnCancelPrint);
        previewDialog.add(btnPanel, BorderLayout.SOUTH);

        btnPrintNow.addActionListener(ev -> {
            printReceipt(receiptPanel);
            previewDialog.dispose();
        });

        btnCancelPrint.addActionListener(ev -> previewDialog.dispose());

        previewDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                receiptPanel.revalidate();
                receiptPanel.repaint();
            }
        });
        
        previewDialog.setVisible(true);
    }
    
    private JPanel createModernReceiptPanel(ArrayList<Object[]> billItems, double grandTotal) {
        double receiptWidthPoints = PRINTABLE_WIDTH_MM * MM_TO_POINTS;
        final int receiptWidth = (int) receiptWidthPoints;
        
        JPanel receipt = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension superSize = super.getPreferredSize();
                return new Dimension(receiptWidth, Math.max(superSize.height, 100));
            }
            
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(receiptWidth, Integer.MAX_VALUE);
            }
        };
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBackground(Color.WHITE);
        
        int padding = 8;
        
        JLabel topLine = new JLabel("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        topLine.setFont(new Font("Consolas", Font.PLAIN, 8));
        topLine.setForeground(new Color(40, 40, 40));
        topLine.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(topLine);
        
        receipt.add(Box.createVerticalStrut(6));
        
        JLabel storeName = new JLabel("QADRI STORE");
        storeName.setFont(new Font("Arial", Font.BOLD, 16));
        storeName.setForeground(new Color(20, 20, 80));
        storeName.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(storeName);
        
        receipt.add(Box.createVerticalStrut(2));
        
        JLabel tagline = new JLabel("Quality Products, Best Prices");
        tagline.setFont(new Font("Arial", Font.ITALIC, 8));
        tagline.setForeground(new Color(100, 100, 100));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(tagline);
        
        receipt.add(Box.createVerticalStrut(6));
        
        JLabel doubleLine1 = new JLabel("════════════════════════════");
        doubleLine1.setFont(new Font("Consolas", Font.PLAIN, 8));
        doubleLine1.setForeground(new Color(40, 40, 40));
        doubleLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(doubleLine1);
        
        receipt.add(Box.createVerticalStrut(4));
        
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm:ss a");
        java.util.Date now = new java.util.Date();
        
        JLabel dateLabel = new JLabel("📅 Date: " + dateFormat.format(now));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        dateLabel.setForeground(new Color(60, 60, 60));
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(dateLabel);
        
        JLabel timeLabel = new JLabel("🕐 Time: " + timeFormat.format(now));
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        timeLabel.setForeground(new Color(60, 60, 60));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(timeLabel);
        
        String receiptNo = "QS-" + System.currentTimeMillis() % 100000;
        JLabel receiptNoLabel = new JLabel("📄 Receipt #: " + receiptNo);
        receiptNoLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        receiptNoLabel.setForeground(new Color(60, 60, 60));
        receiptNoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(receiptNoLabel);
        
        receipt.add(Box.createVerticalStrut(4));
        
        JLabel dashLine1 = new JLabel("──────────────────────────────");
        dashLine1.setFont(new Font("Consolas", Font.PLAIN, 8));
        dashLine1.setForeground(new Color(80, 80, 80));
        dashLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(dashLine1);
        
        receipt.add(Box.createVerticalStrut(4));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setMaximumSize(new Dimension(receiptWidth - padding*2, 20));
        headerPanel.setBackground(new Color(240, 240, 245));
        headerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(180, 180, 200)));
        
        JLabel hItem = new JLabel("ITEM");
        hItem.setFont(new Font("Arial", Font.BOLD, 9));
        hItem.setForeground(new Color(40, 40, 80));
        headerPanel.add(hItem, BorderLayout.WEST);
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerRight.setBackground(new Color(240, 240, 245));
        headerRight.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        JLabel hQty = new JLabel("QTY");
        hQty.setFont(new Font("Arial", Font.BOLD, 9));
        hQty.setForeground(new Color(40, 40, 80));
        hQty.setPreferredSize(new Dimension(30, 15));
        hQty.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel hPrice = new JLabel("PRICE");
        hPrice.setFont(new Font("Arial", Font.BOLD, 9));
        hPrice.setForeground(new Color(40, 40, 80));
        hPrice.setPreferredSize(new Dimension(42, 15));
        hPrice.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel hTotal = new JLabel("TOTAL");
        hTotal.setFont(new Font("Arial", Font.BOLD, 9));
        hTotal.setForeground(new Color(40, 40, 80));
        hTotal.setPreferredSize(new Dimension(48, 15));
        hTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        headerRight.add(hQty);
        headerRight.add(hPrice);
        headerRight.add(hTotal);
        headerPanel.add(headerRight, BorderLayout.EAST);
        receipt.add(headerPanel);
        
        receipt.add(Box.createVerticalStrut(2));
        
        for (int i = 0; i < billItems.size(); i++) {
            Object[] item = billItems.get(i);
            String name = (String) item[0];
            double qty = (double) item[1];
            double price = (double) item[2];
            double total = (double) item[3];
            
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setMaximumSize(new Dimension(receiptWidth - padding*2, 40));
            itemPanel.setBackground(Color.WHITE);
            
            JLabel itemName = new JLabel("<html><div style='width:130px'>" + name + "</div></html>");
            itemName.setFont(new Font("Arial", Font.PLAIN, 9));
            itemName.setForeground(new Color(30, 30, 30));
            itemPanel.add(itemName, BorderLayout.CENTER);
            
            JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            valuePanel.setMaximumSize(new Dimension(receiptWidth - padding*2, 40));
            valuePanel.setBackground(Color.WHITE);
            valuePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
            
            JLabel vQty = new JLabel(String.format("%.0f", qty));
            vQty.setFont(new Font("Arial", Font.PLAIN, 9));
            vQty.setForeground(new Color(50, 50, 50));
            vQty.setPreferredSize(new Dimension(30, 15));
            vQty.setHorizontalAlignment(SwingConstants.CENTER);
            
            JLabel vPrice = new JLabel(String.format("%.0f", price));
            vPrice.setFont(new Font("Arial", Font.PLAIN, 9));
            vPrice.setForeground(new Color(50, 50, 50));
            vPrice.setPreferredSize(new Dimension(42, 15));
            vPrice.setHorizontalAlignment(SwingConstants.RIGHT);
            
            JLabel vTotal = new JLabel(String.format("%.0f", total));
            vTotal.setFont(new Font("Arial", Font.BOLD, 9));
            vTotal.setForeground(new Color(20, 20, 60));
            vTotal.setPreferredSize(new Dimension(48, 15));
            vTotal.setHorizontalAlignment(SwingConstants.RIGHT);
            
            valuePanel.add(vQty);
            valuePanel.add(vPrice);
            valuePanel.add(vTotal);
            
            receipt.add(itemPanel);
            receipt.add(valuePanel);
            
            if (i < billItems.size() - 1) {
                JLabel thinSep = new JLabel("· · · · · · · · · · · · · · · · · · · ·");
                thinSep.setFont(new Font("Arial", Font.PLAIN, 6));
                thinSep.setForeground(new Color(200, 200, 200));
                thinSep.setAlignmentX(Component.CENTER_ALIGNMENT);
                receipt.add(thinSep);
            }
        }
        
        receipt.add(Box.createVerticalStrut(4));
        
        JLabel dashLine2 = new JLabel("──────────────────────────────");
        dashLine2.setFont(new Font("Consolas", Font.PLAIN, 8));
        dashLine2.setForeground(new Color(80, 80, 80));
        dashLine2.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(dashLine2);
        
        receipt.add(Box.createVerticalStrut(4));
        
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setMaximumSize(new Dimension(receiptWidth - padding*2, 35));
        totalPanel.setBackground(new Color(245, 245, 250));
        totalPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(40, 40, 100)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        
        JLabel totalLabel = new JLabel("GRAND TOTAL");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setForeground(new Color(20, 20, 80));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        
        JLabel totalValue = new JLabel("Rs. " + String.format("%,.0f", grandTotal));
        totalValue.setFont(new Font("Arial", Font.BOLD, 16));
        totalValue.setForeground(new Color(180, 0, 0));
        totalValue.setHorizontalAlignment(SwingConstants.RIGHT);
        totalPanel.add(totalValue, BorderLayout.EAST);
        
        receipt.add(totalPanel);
        
        receipt.add(Box.createVerticalStrut(4));
        
        JLabel itemCount = new JLabel("Total Items: " + billItems.size());
        itemCount.setFont(new Font("Arial", Font.PLAIN, 9));
        itemCount.setForeground(new Color(80, 80, 80));
        itemCount.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(itemCount);
        
        receipt.add(Box.createVerticalStrut(6));
        
        JLabel doubleLine2 = new JLabel("════════════════════════════");
        doubleLine2.setFont(new Font("Consolas", Font.PLAIN, 8));
        doubleLine2.setForeground(new Color(40, 40, 40));
        doubleLine2.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(doubleLine2);
        
        receipt.add(Box.createVerticalStrut(8));
        
        JLabel thankYou = new JLabel("Thank You!");
        thankYou.setFont(new Font("Arial", Font.BOLD, 14));
        thankYou.setForeground(new Color(60, 60, 120));
        thankYou.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(thankYou);
        
        receipt.add(Box.createVerticalStrut(3));
        
        JLabel visitAgain = new JLabel("Visit Us Again");
        visitAgain.setFont(new Font("Arial", Font.ITALIC, 10));
        visitAgain.setForeground(new Color(100, 100, 100));
        visitAgain.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(visitAgain);
        
        receipt.add(Box.createVerticalStrut(8));
        
        JLabel phone = new JLabel("📞 Contact: 0300-1234567");
        phone.setFont(new Font("Arial", Font.PLAIN, 8));
        phone.setForeground(new Color(80, 80, 80));
        phone.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(phone);
        
        receipt.add(Box.createVerticalStrut(6));
        
        JLabel bottomLine = new JLabel("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        bottomLine.setFont(new Font("Consolas", Font.PLAIN, 8));
        bottomLine.setForeground(new Color(40, 40, 40));
        bottomLine.setAlignmentX(Component.CENTER_ALIGNMENT);
        receipt.add(bottomLine);
        
        receipt.add(Box.createVerticalStrut(15));
        
        receipt.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        
        receipt.doLayout();
        
        return receipt;
    }
    
    private void printReceipt(JPanel receiptPanel) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("QadriStore_Receipt_80mm");

        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();
        
        double widthPoints = PAPER_WIDTH_MM * MM_TO_POINTS;
        double heightPoints = PAPER_HEIGHT_MM * MM_TO_POINTS;
        double marginPoints = MARGIN_MM * MM_TO_POINTS;
        
        paper.setSize(widthPoints, heightPoints);
        paper.setImageableArea(marginPoints, marginPoints, 
                               widthPoints - (2 * marginPoints), 
                               heightPoints - (2 * marginPoints));
        
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat format, int pageIndex) throws PrinterException {
                Dimension panelSize = receiptPanel.getPreferredSize();
                double panelWidth = panelSize.getWidth();
                double panelHeight = panelSize.getHeight();
                double printableWidth = format.getImageableWidth();
                double printableHeight = format.getImageableHeight();

                double scale = 1.0;
                if (panelWidth > printableWidth) {
                    scale = printableWidth / panelWidth;
                }

                double totalHeight = panelHeight * scale;
                int totalPages = (int) Math.ceil(totalHeight / printableHeight);
                if (pageIndex >= totalPages) {
                    return NO_SUCH_PAGE;
                }

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                double scaledWidth = panelWidth * scale;
                double xPos = format.getImageableX() + ((printableWidth - scaledWidth) / 2);

                g2d.translate(xPos, format.getImageableY());
                g2d.scale(scale, scale);
                g2d.translate(0, -pageIndex * printableHeight / scale);

                receiptPanel.printAll(g2d);
                return PAGE_EXISTS;
            }
        }, pf);

        try {
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "✅ Print Sent Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "❌ Printing Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void setApplicationIcon() {
        try {
            // Current working directory (jahan se EXE chal rahi hai) nikalna
            String currentDir = System.getProperty("user.dir");
            
            // "Icon" folder ke andar "Icon.png" ka path banana
            File iconFile = new File(currentDir + File.separator + "Icon" + File.separator + "Icon.png");
            
            if (iconFile.exists()) {
                // Agar file mil gayi toh usko read karke icon set karein
                Image icon = ImageIO.read(iconFile);
                setIconImage(icon);
            } else {
                System.err.println("Icon file nahi mili: " + iconFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Icon set karne mein error: " + e.getMessage());
        }
    }

    public void selectAndSaveLogo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select App Logo");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg", "gif", "ico", "bmp"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists()) {
                saveLogoPath(selectedFile);
                applyIconToAllWindows(selectedFile);
                JOptionPane.showMessageDialog(this, "Logo save ho gaya aur app ke har jagah lag jaega.", "Logo Updated", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void saveLogoPath(File logoFile) {
        try {
            String encrypted = encryptText(logoFile.getAbsolutePath());
            Files.write(new File("QadriStore_Logo.cfg").toPath(), encrypted.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Logo save karne mein problem: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File getSavedLogoFile() {
        File cfg = new File("QadriStore_Logo.cfg");
        if (!cfg.exists()) {
            return null;
        }

        try {
            String encryptedContent = new String(Files.readAllBytes(cfg.toPath()), StandardCharsets.UTF_8);
            String decryptedContent = decryptText(encryptedContent.trim());
            if (!decryptedContent.isEmpty()) {
                File logoFile = new File(decryptedContent);
                if (logoFile.exists()) {
                    return logoFile;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void applyIconToAllWindows(File logoFile) {
        applyIconFromFile(logoFile);
        for (Window window : Window.getWindows()) {
            try {
                if (window instanceof Frame || window instanceof Dialog) {
                    window.setIconImage(getIconImage());
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void applyIconFromFile(File logoFile) {
        if (logoFile == null || !logoFile.exists()) {
            return;
        }

        try {
            Image img = ImageIO.read(logoFile);
            if (img != null) {
                setIconImage(img);
            }
        } catch (Exception ignored) {
        }
    }

    private String encryptText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            encrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
        }
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptText(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            byte[] decrypted = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                decrypted[i] = (byte) (bytes[i] ^ ENCRYPTION_KEY[i % ENCRYPTION_KEY.length]);
            }
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    private void performSearchJump() {
        if (resultTable == null) return;
        String query = txtProductSearch.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        int currentRow = resultTable.getSelectedRow();
        int startRow = (currentRow < 0) ? 0 : currentRow + 1;
        
        for (int i = startRow; i < resultTable.getRowCount(); i++) {
            if (checkRowMatch(i, query)) {
                selectAndScrollToTop(i); 
                return;
            }
        }
        
        for (int i = 0; i < startRow; i++) {
            if (checkRowMatch(i, query)) {
                selectAndScrollToTop(i); 
                return;
            }
        }
        
        lblStatus.setText("No match found for: " + query);
    }
    
    private boolean checkRowMatch(int viewRowIndex, String query) {
        Object nameObj = resultTable.getValueAt(viewRowIndex, 1);
        Object barcodeObj = resultTable.getValueAt(viewRowIndex, 0);
        
        String name = (nameObj != null) ? nameObj.toString().toLowerCase() : "";
        String barcode = (barcodeObj != null) ? barcodeObj.toString().toLowerCase() : "";
        
        return name.contains(query) || barcode.contains(query);
    }
    
    private void selectAndScrollToTop(int viewRowIndex) {
        resultTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);
        Container parent = resultTable.getParent();
        if (parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            Rectangle rect = resultTable.getCellRect(viewRowIndex, 0, true);
            viewport.setViewPosition(new Point(0, rect.y));
        }
        lblStatus.setText("Match found at row " + (viewRowIndex + 1));
    }
    
    private void mergeDuplicateRows() {
        if (resultTable == null) {
            JOptionPane.showMessageDialog(this, "No table loaded.");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        int originalCount = model.getRowCount();
        if (originalCount == 0) return;

        Map<String, Object[]> mergedMap = new LinkedHashMap<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String barcode = (String) model.getValueAt(i, 0);
            String name = (String) model.getValueAt(i, 1);
            
            if (name == null) name = "";
            
            boolean isVendor = VENDOR_MARKER.equals(barcode);
            String key = name.toLowerCase().trim() + (isVendor ? "_VENDOR" : "_PRODUCT");

            double order = parseDouble(model.getValueAt(i, 2));
            double price = parseDouble(model.getValueAt(i, 3)); 
            double inv = parseDouble(model.getValueAt(i, 4));
            double sale = parseDouble(model.getValueAt(i, 5));
            double rate = parseDouble(model.getValueAt(i, 8));
            double tp = parseDouble(model.getValueAt(i, 9));

            if (mergedMap.containsKey(key)) {
                Object[] existing = mergedMap.get(key);
                String existingBarcode = (String) existing[0];
                if (!existingBarcode.contains(barcode)) {
                    existing[0] = existingBarcode + ", " + barcode;
                }
                existing[2] = (double) existing[2] + order;
                if((double)existing[3] == 0 && price != 0) existing[3] = price;
                
                existing[4] = (double) existing[4] + inv;
                existing[5] = (double) existing[5] + sale;
            } else {
                Object[] rowData = new Object[]{ barcode, name, order, price, inv, sale, 0.0, 0.0, rate, tp };
                mergedMap.put(key, rowData);
            }
        }

        model.setRowCount(0);
        
        for (Object[] row : mergedMap.values()) {
            double order = (double) row[2];
            double inv = (double) row[4];
            double sale = (double) row[5];
            double demand = inv - sale;
            double daysStock = (sale > 0) ? Math.round((inv * 30.0) / sale) : 999;
            
            row[6] = demand;
            row[7] = daysStock;
            
            row[2] = (order == 0.0) ? "" : order; 
            
            model.addRow(row);
            
            if (VENDOR_MARKER.equals(row[0])) {
                int lastRow = model.getRowCount() - 1;
                resultTable.setRowHeight(lastRow, 40);
            }
        }
        
        saveTableLayout();
        resizeColumnWidth(resultTable);
        lblStatus.setText("Merged: " + originalCount + " rows -> " + model.getRowCount() + " rows.");
    }
    
    private String formatDisplayNumber(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Number) {
            BigDecimal decimalValue = BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros();
            if (decimalValue.compareTo(BigDecimal.ZERO) == 0) {
                return "0";
            }
            return decimalValue.toPlainString();
        }

        if (value instanceof CharSequence) {
            String text = value.toString().trim();
            if (text.isEmpty()) {
                return "";
            }

            try {
                BigDecimal decimalValue = new BigDecimal(text.replace(",", ""));
                if (decimalValue.compareTo(BigDecimal.ZERO) == 0) {
                    return "0";
                }
                return decimalValue.stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ex) {
                double parsedValue = parseDouble(value);
                if (Double.isNaN(parsedValue) || Double.isInfinite(parsedValue)) {
                    return "";
                }
                BigDecimal decimalValue = BigDecimal.valueOf(parsedValue).stripTrailingZeros();
                if (decimalValue.compareTo(BigDecimal.ZERO) == 0) {
                    return "0";
                }
                return decimalValue.toPlainString();
            }
        }

        return value.toString();
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        return "Rs. " + String.format("%,.2f", value.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            String str = obj.toString().trim();
            if (str.isEmpty()) return 0.0;
            // normalize common formatting like commas and support values such as .2462
            str = str.replaceAll(",", "");
            if (str.startsWith(".")) {
                str = "0" + str;
            } else if (str.startsWith("-.")) {
                str = "-0" + str.substring(1);
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?").matcher(str);
            if (m.find()) return Double.parseDouble(m.group());
            return 0.0;
        } catch (Exception e) { return 0.0; }
    }

    // Parse weight in grams from item name. Supports patterns like "1kg", "500gm", "250 g", "unpack 100", "unpack:100g", "unpack100"
    private double parseWeightGramsFromName(String name) {
        if (name == null) return 0.0;
        try {
            String s = name.toLowerCase();
            java.util.regex.Matcher m;
            java.util.regex.Pattern pKg = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(kg|kgs)\\b");
            java.util.regex.Pattern pG = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(g|gm|gr|grams?)\\b");
            java.util.regex.Pattern pUnpack = java.util.regex.Pattern.compile("(?:unpack|pack)\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?)(?:\\s*(kg|kgs|g|gm|gr|grams?))?\\b");

            // 1) explicit kg
            m = pKg.matcher(s);
            if (m.find()) return Double.parseDouble(m.group(1)) * 1000.0;
            // 2) explicit grams
            m = pG.matcher(s);
            if (m.find()) return Double.parseDouble(m.group(1));
            // 3) unpack/pack forms (unpack100, unpack 100, unpack:100g, pack-100)
            m = pUnpack.matcher(s);
            if (m.find()) {
                double val = Double.parseDouble(m.group(1));
                String unit = null;
                try { unit = m.group(2); } catch (Exception ex) { unit = null; }
                if (unit != null) {
                    if (unit.startsWith("kg")) return val * 1000.0;
                    return val; // grams provided
                }
                // if no unit, assume grams (as per existing convention)
                return val;
            }

            // fallback: any number followed by unit
            java.util.regex.Pattern pAny = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)(kg|kgs|g|gm|gr)\\b");
            m = pAny.matcher(s);
            if (m.find()) {
                String unit = m.group(2);
                double val = Double.parseDouble(m.group(1));
                if (unit.startsWith("kg")) return val * 1000.0;
                return val;
            }

            // fallback 2: any standalone number in the name (assume grams)
            java.util.regex.Pattern pNumberOnly = java.util.regex.Pattern.compile("\\b([0-9]+(?:\\.[0-9]+)?)\\b");
            m = pNumberOnly.matcher(s);
            if (m.find()) {
                try { return Double.parseDouble(m.group(1)); } catch (Exception ex) { }
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }

    // Show a professional modal dialog with total kg and pack-size breakdown
    private void showVendorWeightDialog(Object nameObj, double totalKg, int[] packSizes, double[] quantities) {
        String titleText = (nameObj != null) ? nameObj.toString() : "Vendor";
        JDialog dlg = new JDialog(this, "Vendor Weight — " + titleText, true);
        dlg.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("Total: " + String.format("%.3f", totalKg) + " kg", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        dlg.add(title, BorderLayout.NORTH);

        DefaultTableModel tm = new DefaultTableModel(new Object[]{"Pack Size","Qty (approx)"}, 0);
        for (int i = 0; i < packSizes.length; i++) {
            tm.addRow(new Object[]{packSizes[i] + " kg", String.format("%.3f", quantities[i])});
        }
        JTable table = new JTable(tm);
        table.setEnabled(false);
        table.setFont(new Font("Arial", Font.PLAIN, 18));
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        dlg.add(sp, BorderLayout.CENTER);

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Arial", Font.BOLD, 16));
        btnClose.addActionListener(ae -> dlg.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(btnClose);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(420, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
    
    private void loadConfigurationFromDisk() {
        Properties props = new Properties();
        File f = new File(configFilePath); 
        if (f.exists()) {
            try {
                String fileContent = new String(Files.readAllBytes(f.toPath()));
                String decryptedContent = decrypt(fileContent.trim());
                
                if (!decryptedContent.isEmpty()) {
                    props.load(new ByteArrayInputStream(decryptedContent.getBytes("UTF-8")));
                }
            } catch (Exception ex) { 
                // Decryption failed, maybe it's an old plain-text file
                try (FileInputStream in = new FileInputStream(f)) {
                    props.load(in);
                } catch (Exception ex2) {}
            }
            
            if (props.getProperty("serverName") != null) serverName = props.getProperty("serverName");
            if (props.getProperty("dbName") != null) dbName = props.getProperty("dbName");
            if (props.getProperty("user") != null) user = props.getProperty("user");
            if (props.getProperty("pass") != null) pass = props.getProperty("pass");
        }
    }
    
    private void loadVendorContactsFromFile() {
        vendorContactsMap.clear();
        File f = new File(vendorContactsFile); 
        if (f.exists()) {
            try (Scanner sc = new Scanner(f)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if(line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|", -1); 
                    if (parts.length >= 5) {
                        String name = parts[0];
                        String[] nums = new String[]{parts[1], parts[2], parts[3], parts[4]};
                        vendorContactsMap.put(name, nums);
                    }
                }
            } catch (Exception e) { }
        }
    }
    
    private void saveVendorContactsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(vendorContactsFile))) { 
            for (Map.Entry<String, String[]> entry : vendorContactsMap.entrySet()) {
                String name = entry.getKey();
                String[] nums = entry.getValue();
                writer.println(name + "|" + (nums[0] != null ? nums[0] : "") + "|" + (nums[1] != null ? nums[1] : "") + "|" + (nums[2] != null ? nums[2] : "") + "|" + (nums[3] != null ? nums[3] : ""));
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error saving contacts file: " + e.getMessage()); }
    }
    
    private void updateUrl() {
        url = "jdbc:sqlserver://" + serverName + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";
    }
    
    private void showConfigDialog() {
        JDialog dialog = new JDialog(this, "Database Configuration", true);
        dialog.setIconImage(this.getIconImage());
        dialog.setSize(450, 300); 
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField txtServer = new JTextField(serverName);
        JTextField txtDb = new JTextField(dbName);
        JTextField txtUser = new JTextField(user);
        JPasswordField txtPass = new JPasswordField(pass);
        
        fieldsPanel.add(new JLabel("Server Name:")); fieldsPanel.add(txtServer);
        fieldsPanel.add(new JLabel("Database Name:")); fieldsPanel.add(txtDb);
        fieldsPanel.add(new JLabel("Username:")); fieldsPanel.add(txtUser);
        fieldsPanel.add(new JLabel("Password:")); fieldsPanel.add(txtPass);
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnSave = new JButton("Save Configuration");
        btnSave.setBackground(new Color(50, 205, 50));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Arial", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(180, 35));
        btnSave.setOpaque(true);
        btnSave.setBorderPainted(false);
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setPreferredSize(new Dimension(100, 35));
        buttonsPanel.add(btnSave);
        buttonsPanel.add(btnCancel);
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        
        btnSave.addActionListener(e -> {
            // --- CONFIG PASSWORD CHECK ---
            if (!verifyConfigPassword()) {
                // Password was wrong - button is now blocked, search box disabled, close dialog
                dialog.dispose();
                return;
            }
            
            serverName = txtServer.getText();
            dbName = txtDb.getText();
            user = txtUser.getText();
            pass = new String(txtPass.getPassword());
            updateUrl();
            Properties props = new Properties();
            props.setProperty("serverName", serverName);
            props.setProperty("dbName", dbName);
            props.setProperty("user", user);
            props.setProperty("pass", pass);
            
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                props.store(baos, "Qadri Store Configuration");
                String plainText = baos.toString();
                String encrypted = encrypt(plainText);
                
                try (FileOutputStream out = new FileOutputStream(configFilePath)) {
                    out.write(encrypted.getBytes());
                }
                
                JOptionPane.showMessageDialog(dialog, "Configuration Saved!\nPlease restart application to apply changes.");
                dialog.dispose();
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Error saving config: " + ex.getMessage()); }
        });
        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }
    
    private void showVendorNumbersDialog() {
        JDialog dialog = new JDialog(this, "Company Numbers Directory", false);
        dialog.setIconImage(this.getIconImage());
        dialog.setSize(800, 600); 
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField txtVendorSearch = new JTextField();
        txtVendorSearch.setFont(new Font("Arial", Font.PLAIN, 18));
        searchPanel.add(new JLabel("Search Vendor: "), BorderLayout.WEST);
        searchPanel.add(txtVendorSearch, BorderLayout.CENTER);
        dialog.add(searchPanel, BorderLayout.NORTH);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(300);
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        Set<String> allVendors = new TreeSet<>(); 
        
        if (resultTable != null) {
            DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object val = model.getValueAt(i, 0);
                if (val != null && VENDOR_MARKER.equals(val.toString())) {
                    String name = (String) model.getValueAt(i, 1);
                    if (name != null && !name.trim().isEmpty()) allVendors.add(name);
                }
            }
        }
        allVendors.addAll(vendorContactsMap.keySet());
        for (String name : allVendors) listModel.addElement(name);
        
        JList<String> vendorList = new JList<>(listModel);
        vendorList.setFont(new Font("Arial", Font.PLAIN, 18));
        JScrollPane listScroll = new JScrollPane(vendorList);
        styleScrollPane(listScroll);
        split.setLeftComponent(listScroll);
        
        JPanel detailPanel = new JPanel(new BorderLayout(10, 10));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        detailPanel.setBackground(Color.WHITE);
        
        JPanel fieldsGrid = new JPanel(new GridLayout(4, 2, 10, 15));
        fieldsGrid.setBackground(Color.WHITE);
        Font labelFont = new Font("Arial", Font.BOLD, 18);
        Font fieldFont = new Font("Arial", Font.PLAIN, 18);
        Color labelColor = new Color(50, 50, 50);
        
        JLabel lblBookerTitle = new JLabel("Booker Number:"); lblBookerTitle.setFont(labelFont); lblBookerTitle.setForeground(labelColor);
        JTextField txtBooker = new JTextField(); txtBooker.setFont(fieldFont);
        JLabel lblSuperTitle = new JLabel("Supervisor Number:"); lblSuperTitle.setFont(labelFont); lblSuperTitle.setForeground(labelColor);
        JTextField txtSuper = new JTextField(); txtSuper.setFont(fieldFont);
        JLabel lblRsmTitle = new JLabel("RSM Number:"); lblRsmTitle.setFont(labelFont); lblRsmTitle.setForeground(labelColor);
        JTextField txtRsm = new JTextField(); txtRsm.setFont(fieldFont);
        JLabel lblHelpTitle = new JLabel("Helpline:"); lblHelpTitle.setFont(labelFont); lblHelpTitle.setForeground(labelColor);
        JTextField txtHelp = new JTextField(); txtHelp.setFont(fieldFont);
        
        fieldsGrid.add(lblBookerTitle); fieldsGrid.add(txtBooker);
        fieldsGrid.add(lblSuperTitle); fieldsGrid.add(txtSuper);
        fieldsGrid.add(lblRsmTitle); fieldsGrid.add(txtRsm);
        fieldsGrid.add(lblHelpTitle); fieldsGrid.add(txtHelp);
        detailPanel.add(fieldsGrid, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnSaveContact = new JButton("Save Contact Info");
        btnSaveContact.setFont(new Font("Arial", Font.BOLD, 16));
        btnSaveContact.setBackground(new Color(0, 100, 150));
        btnSaveContact.setForeground(Color.WHITE);
        btnSaveContact.setOpaque(true);
        btnSaveContact.setBorderPainted(false);
        JButton btnDelete = new JButton("Delete Vendor");
        btnDelete.setFont(new Font("Arial", Font.BOLD, 16));
        btnDelete.setBackground(new Color(220, 53, 69)); 
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setOpaque(true);
        btnDelete.setBorderPainted(false);
        btnPanel.add(btnSaveContact);
        btnPanel.add(btnDelete);
        detailPanel.add(btnPanel, BorderLayout.SOUTH);
        split.setRightComponent(detailPanel);
        dialog.add(split, BorderLayout.CENTER);
        
        txtVendorSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String query = txtVendorSearch.getText().toLowerCase();
                listModel.clear();
                for (String name : allVendors) { if (name.toLowerCase().contains(query)) listModel.addElement(name); }
            }
        });
        
        vendorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedName = vendorList.getSelectedValue();
                if (selectedName != null) {
                    if (vendorContactsMap.containsKey(selectedName)) {
                        String[] nums = vendorContactsMap.get(selectedName);
                        txtBooker.setText(nums[0] != null ? nums[0] : "");
                        txtSuper.setText(nums[1] != null ? nums[1] : "");
                        txtRsm.setText(nums[2] != null ? nums[2] : "");
                        txtHelp.setText(nums[3] != null ? nums[3] : "");
                    } else { txtBooker.setText(""); txtSuper.setText(""); txtRsm.setText(""); txtHelp.setText(""); }
                }
            }
        });
        
        btnSaveContact.addActionListener(ev -> {
            String selectedVendor = vendorList.getSelectedValue();
            if (selectedVendor == null || selectedVendor.trim().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Please select a vendor from the list first."); return; }
            String[] data = new String[]{txtBooker.getText(), txtSuper.getText(), txtRsm.getText(), txtHelp.getText()};
            vendorContactsMap.put(selectedVendor, data);
            saveVendorContactsToFile();
            JOptionPane.showMessageDialog(dialog, "Saved Successfully to Local File!");
            if (!allVendors.contains(selectedVendor)) { allVendors.add(selectedVendor); listModel.addElement(selectedVendor); }
        });
        
        btnDelete.addActionListener(ev -> {
            String selectedVendor = vendorList.getSelectedValue();
            if (selectedVendor == null || selectedVendor.trim().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Please select a vendor to delete."); return; }
            int confirm = JOptionPane.showConfirmDialog(dialog, "Are you sure you want to delete contact for:\n" + selectedVendor + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                vendorContactsMap.remove(selectedVendor);
                listModel.removeElement(selectedVendor);
                allVendors.remove(selectedVendor);
                saveVendorContactsToFile();
                txtBooker.setText(""); txtSuper.setText(""); txtRsm.setText(""); txtHelp.setText("");
                lblStatus.setText("Deleted vendor: " + selectedVendor);
            }
        });
        dialog.setVisible(true);
    }

    boolean verifyPassword() {
        JDialog dialog = new JDialog(this, "Authentication Required", true);
        dialog.setIconImage(this.getIconImage());
        dialog.setLayout(new BorderLayout(10, 10));
        JPanel centerPanel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("Enter Password:");
        JPasswordField passField = new JPasswordField(15);
        Font f = new Font("Arial", Font.BOLD, 16);
        label.setFont(f);
        passField.setFont(f);
        centerPanel.add(label);
        centerPanel.add(passField);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        btnOk.setPreferredSize(new Dimension(100, 35));
        btnCancel.setPreferredSize(new Dimension(100, 35));
        btnPanel.add(btnOk);
        btnPanel.add(btnCancel);
        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        final boolean[] result = {false};
        btnOk.addActionListener(e -> {
             char[] password = passField.getPassword();
             String passStr = new String(password);
             if ("123".equals(passStr)) { result[0] = true; dialog.dispose(); }
             else { JOptionPane.showMessageDialog(dialog, "Incorrect Password!", "Error", JOptionPane.ERROR_MESSAGE); passField.setText(""); passField.requestFocusInWindow(); }
        });
        btnCancel.addActionListener(e -> dialog.dispose());
        passField.addActionListener(e -> btnOk.doClick());
        SwingUtilities.invokeLater(() -> passField.requestFocusInWindow());
        dialog.setVisible(true);
        return result[0];
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(28, Integer.MAX_VALUE));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(Integer.MAX_VALUE, 28));
    }

    class ModernScrollBarUI extends BasicScrollBarUI {
        private static final int SCROLL_BAR_WIDTH = 28; 
        private final Color TRACK_COLOR = new Color(230, 230, 230); 
        private final Color THUMB_COLOR = new Color(100, 100, 150); 
        private final Color THUMB_HOVER_COLOR = new Color(60, 60, 120); 
        
        @Override
        protected void configureScrollBarColors() { this.trackColor = TRACK_COLOR; this.thumbColor = THUMB_COLOR; }
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() { JButton button = new JButton(); button.setPreferredSize(new Dimension(0, 0)); return button; }
        @Override
        protected Dimension getMinimumThumbSize() { return new Dimension(SCROLL_BAR_WIDTH, 40); }
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(TRACK_COLOR);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g.setColor(new Color(200, 200, 200));
            g.drawRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1);
        }
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? THUMB_HOVER_COLOR : THUMB_COLOR);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
            g2.dispose();
        }
        @Override
        public Dimension getPreferredSize(JComponent c) { return new Dimension(SCROLL_BAR_WIDTH, SCROLL_BAR_WIDTH); }
    }

    private void updateSearchPalette(String query) {
        DefaultTableModel model = (DefaultTableModel) searchPaletteTable.getModel();
        model.setRowCount(0);
        if (query.trim().isEmpty()) { model.addRow(new Object[]{"Type to search...", ""}); return; }
        String lowerQuery = query.toLowerCase();
        int count = 0;
        for (Map.Entry<String, String> entry : nameToBarcodeMap.entrySet()) {
            String name = entry.getKey();
            String barcode = entry.getValue();
            if (name.contains(lowerQuery) || barcode.toLowerCase().contains(lowerQuery)) {
                String displayName = barcodeToNameMap.get(barcode);
                if (displayName == null) displayName = name;
                model.addRow(new Object[]{displayName, barcode});
                count++;
                if (count > 100) break;
            }
        }
    }

    private void saveFullDataToLocalCache() {
        if (resultTable == null) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(localCacheFile))) {
            DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object val = model.getValueAt(i, j);
                    String str = (val == null) ? "" : val.toString();
                    str = str.replace("|", " ");
                    if (j > 0) sb.append("|");
                    sb.append(str);
                }
                writer.println(sb.toString());
            }
        } catch (Exception e) {
            System.err.println("Error saving local cache: " + e.getMessage());
        }
    }

    private void loadFullDataFromLocalCache() {
        File f = new File(localCacheFile);
        if (!f.exists()) return;

        try {
            DefaultTableModel model = new DefaultTableModel() {
                @Override public boolean isCellEditable(int row, int column) {
                    String bc = (String) getValueAt(row, 0);
                    if (VENDOR_MARKER.equals(bc)) return column == 1 && btnDragMode.isSelected();
                    if(column == 2 || column == 3) return true;
                    return false;
                }
                @Override public Class<?> getColumnClass(int columnIndex) { return Object.class; }
            };
            
            model.addColumn("Barcode"); model.addColumn("Name"); 
            model.addColumn("Order"); model.addColumn("Price");
            model.addColumn("Inventory"); model.addColumn("30 Days Sale"); model.addColumn("Demand"); 
            model.addColumn("Days Stock"); model.addColumn("Sale Rate"); model.addColumn("TP");

            try (Scanner sc = new Scanner(f)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 10) {
                        Object[] row = new Object[10];
                        row[0] = parts[0]; 
                        row[1] = parts[1]; 
                        row[2] = parts[2]; 
                        row[3] = parts[3]; 
                        row[4] = parseDouble(parts[4]); 
                        row[5] = parseDouble(parts[5]); 
                        row[6] = parseDouble(parts[6]); 
                        row[7] = parseDouble(parts[7]); 
                        row[8] = parseDouble(parts[8]); 
                        row[9] = parseDouble(parts[9]); 
                        model.addRow(row);
                    }
                }
            }

            resultTable = new JTable(model);
            
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            for (int i = 0; i < model.getColumnCount(); i++) sorter.setSortable(i, false);
            resultTable.setRowSorter(sorter); 
            
            Font headerFont = new Font("Arial", Font.BOLD, 20); 
            JTableHeader header = resultTable.getTableHeader();
            header.setFont(headerFont); header.setBackground(new Color(200, 200, 200)); header.setForeground(Color.BLACK);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40)); 
            
            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(SwingConstants.CENTER); setFont(headerFont);
                    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.BLACK), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                    return this;
                }
            };
            for (int i = 0; i < resultTable.getColumnCount(); i++) resultTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

            resultTable.setDefaultRenderer(Object.class, new CustomRenderer());

            resultTable.setRowHeight(25);
            resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
            
            for (int i = 0; i < model.getRowCount(); i++) {
                if (VENDOR_MARKER.equals(model.getValueAt(i, 0))) {
                    resultTable.setRowHeight(i, 40);
                }
            }

            resizeColumnWidth(resultTable);
            
            orderColumn = resultTable.getColumnModel().getColumn(2);
            priceColumn = resultTable.getColumnModel().getColumn(3);
            
            resultTable.removeColumn(orderColumn);
            resultTable.removeColumn(priceColumn);
            btnPrint.setVisible(false);

            resultTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isVendorInsertMode) {
                        int row = resultTable.rowAtPoint(e.getPoint());
                        if (row != -1) insertVendorRow(row);
                        isVendorInsertMode = false;
                        btnAddVendor.setText("Add Vendor Name"); btnAddVendor.setBackground(new Color(255, 200, 100));
                    }
                }
            });

            resultTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                        int viewCol = resultTable.columnAtPoint(e.getPoint());
                        if (viewCol >= 0 && viewCol < resultTable.getColumnCount()) {
                            int modelCol = resultTable.convertColumnIndexToModel(viewCol);
                            if (modelCol == 2 || modelCol == 3) { 
                                int row = resultTable.rowAtPoint(e.getPoint());
                                if (row != -1) {
                                    if (resultTable.getModel().isCellEditable(row, modelCol)) {
                                        resultTable.editCellAt(row, viewCol);
                                    }
                                }
                            }
                        }
                    }
                }
            });

            // Double-click on vendor row: show total weight (kg) for that vendor
            resultTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                        int viewRow = resultTable.rowAtPoint(e.getPoint());
                        if (viewRow == -1) return;
                        int modelRow = resultTable.convertRowIndexToModel(viewRow);
                        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                        Object bcObj = model.getValueAt(modelRow, 0);
                        // (debug removed) -- proceed to compute total weight
                        if (bcObj != null && VENDOR_MARKER.equals(bcObj.toString())) {
                            Object nameObj = model.getValueAt(modelRow, 1);
                            String vname = (nameObj != null) ? nameObj.toString().trim().toLowerCase() : "";
                            if (vname.isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Vendor name is empty.");
                                return;
                            }
                            double totalGrams = 0.0;
                            StringBuilder dbg = new StringBuilder();
                            int rowsScanned = 0;
                            // collect contiguous item rows after this vendor row until next vendor marker
                            for (int r = modelRow + 1; r < model.getRowCount(); r++) {
                                Object bco = model.getValueAt(r, 0);
                                String barcodeStr = (bco != null) ? bco.toString() : "";
                                if (VENDOR_MARKER.equals(barcodeStr)) break;
                                Object nmObj = model.getValueAt(r, 1);
                                String iname = (nmObj != null) ? nmObj.toString().toLowerCase().trim() : "";
                                double perUnitGrams = parseWeightGramsFromName(iname);
                                double inv = parseDouble(model.getValueAt(r, 4));
                                double rowTotal;
                                if (perUnitGrams <= 0 && iname.endsWith("unpack")) {
                                    // For '... unpack' rows the Inventory column stores kilograms total — treat as kg
                                    rowTotal = inv * 1000.0;
                                    dbg.append("row=" + r + " name='" + iname + "' UNPACK-> invKg=" + inv + " => " + rowTotal + "g\n");
                                } else {
                                    rowTotal = perUnitGrams * inv;
                                    dbg.append("row=" + r + " name='" + iname + "' perUnit=" + perUnitGrams + "g inv=" + inv + " => " + rowTotal + "g\n");
                                }
                                totalGrams += rowTotal;
                                rowsScanned++;
                            }
                            double totalKg = totalGrams / 1000.0;
                            double kg = Math.round(totalKg * 1000.0) / 1000.0;
                            // console debug removed
                            int[] packSizes = new int[]{50,25};
                            double[] quantities = new double[packSizes.length];
                            for (int i = 0; i < packSizes.length; i++) quantities[i] = Math.round((totalKg / packSizes[i]) * 1000.0) / 1000.0;
                            showVendorWeightDialog(nameObj, totalKg, packSizes, quantities);
                        }
                    }
                }
            });
            JScrollPane sp = new JScrollPane(resultTable);
            styleScrollPane(sp);
            
            int idx = tabbedPane.indexOfTab("Offline Mode");
            if (idx != -1) tabbedPane.remove(idx);
            tabbedPane.addTab("Offline Mode", sp);
            tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("Offline Mode"));
            
            lblStatus.setText("Loaded from Local Cache (Offline Mode).");

        } catch (Exception ex) {
            lblStatus.setText("Error loading local cache: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void initializeAndLoadFast() {
        new SwingWorker<Void, String>() {
            boolean connectionSuccess = false;
            
            @Override
            protected Void doInBackground() throws Exception {
                publish("Connecting to " + serverName + "...");
                try {
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    Connection conn = DriverManager.getConnection(url, user, pass);
                    connectionSuccess = true;
                    
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE", "VIEW"});
                    DefaultTableModel model = (DefaultTableModel) tableList.getModel();
                    model.setRowCount(0);
                    Set<String> savedSelections = loadSelectionFromFile();
                    while (rs.next()) {
                        String nameWithType = rs.getString("TABLE_NAME") + " (" + rs.getString("TABLE_TYPE") + ")";
                        model.addRow(new Object[]{savedSelections.contains(nameWithType), nameWithType});
                    }
                    rs.close();

                    publish("Loading Maps...");
                    Statement stmt1 = conn.createStatement();
                    ResultSet rs1 = stmt1.executeQuery("SELECT [" + refNameCol + "], [" + refBarcodeCol + "] FROM [" + referenceTable + "]");
                    while (rs1.next()) {
                        String name = rs1.getString(1); String barcode = rs1.getString(2);
                        if (name != null && barcode != null) {
                            nameToBarcodeMap.put(name.toLowerCase().trim(), barcode);
                            barcodeToNameMap.put(barcode, name);
                        }
                    }
                    rs1.close();
                    
                    publish("Loading Vendor Contacts...");
                    loadVendorContactsFromFile();
                    
                    publish("Detecting Tables...");
                    try {
                        ResultSet rsInv = conn.createStatement().executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'ProductName' INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Inventory'");
                        if (rsInv.next()) inventoryTable = rsInv.getString(1);
                        rsInv.close();
                    } catch(Exception ex) {}
                    
                    try {
                        ResultSet rsSales = conn.createStatement().executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'LongName' INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Quantity'");
                        if (rsSales.next()) salesTable = rsSales.getString(1);
                        rsSales.close();
                    } catch(Exception ex) {}
                    
                    conn.close();
                } catch (Exception e) {
                    connectionSuccess = false;
                    publish("Connection Failed. Loading Local Cache...");
                }
                return null;
            }
            
            @Override protected void process(List<String> chunks) { lblStatus.setText(chunks.get(chunks.size()-1)); }
            
            @Override protected void done() {
                if (connectionSuccess) {
                    lblStatus.setText("Ready. Connected to: " + dbName);
                    isOfflineMode = false;
                    if (!loadSelectionFromFile().isEmpty()) loadAndMergeSelectedTables();
                } else {
                    loadFullDataFromLocalCache();
                    isOfflineMode = true;
                    startLiveUpdates(); 
                    startLocalCacheTimer(); 
                }
                
                if (lblVersion != null) {
                    lblVersion.setVisible(false);
                }
                
                if (!isLiveSalesActive) {
                    checkSalesExpiry();
                }
            }
        }.execute();
    }

    private Set<String> loadSelectionFromFile() {
        Set<String> set = new HashSet<>();
        File f = new File(selectionFile);
        if (f.exists()) { try (Scanner sc = new Scanner(f)) { while (sc.hasNextLine()) set.add(sc.nextLine().trim()); } catch (Exception e) { } }
        return set;
    }

    private void saveSelectionToFile(List<String> items) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(selectionFile))) { for (String item : items) writer.println(item); } catch (Exception e) { }
    }

    void loadAndMergeSelectedTables() {
        DefaultTableModel listModel = (DefaultTableModel) tableList.getModel();
        List<String> selectedTables = new ArrayList<>();
        for (int i = 0; i < listModel.getRowCount(); i++) {
            if ((Boolean) listModel.getValueAt(i, 0)) selectedTables.add((String) listModel.getValueAt(i, 1));
        }
        if (selectedTables.isEmpty()) return;
        saveSelectionToFile(selectedTables);
        lblStatus.setText("Fetching...");

        new SwingWorker<Map<String, Object[]>, String>() {
            @Override
            protected Map<String, Object[]> doInBackground() throws Exception {
                publish("Scanning...");
                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    
                    Set<String> neededBarcodes = new HashSet<>();
                    Map<String, Double> saleRateData = new HashMap<>(); 
                    Map<String, Double> costPriceData = new HashMap<>(); 
                    
                    for (String item : selectedTables) {
                        String tableName = item.split(" \\(")[0];
                        try {
                            ResultSet rs = conn.createStatement().executeQuery("SELECT TOP 1 * FROM [" + tableName + "]");
                            ResultSetMetaData md = rs.getMetaData(); rs.close();
                            String keyCol = ""; int keyType = 0; String srCol = ""; String cpCol = "";
                            for (int i = 1; i <= md.getColumnCount(); i++) {
                                String colName = md.getColumnName(i);
                                if (colName.equalsIgnoreCase("Barcode")) { keyCol = colName; keyType = 1; }
                                else if (colName.equalsIgnoreCase("LongName") || colName.equalsIgnoreCase("ProductName") || colName.equalsIgnoreCase("Name")) { if(keyType==0) {keyCol = colName; keyType = 2;}}
                                else if (colName.equalsIgnoreCase("SaleRate")) srCol = colName;
                                else if (colName.equalsIgnoreCase("CostPrice")) cpCol = colName;
                            }
                            if (keyCol.isEmpty()) continue;

                            StringBuilder dataSql = new StringBuilder("SELECT [" + keyCol + "]");
                            if(!srCol.isEmpty()) dataSql.append(", [").append(srCol).append("]");
                            if(!cpCol.isEmpty()) dataSql.append(", [").append(cpCol).append("]");
                            dataSql.append(" FROM [").append(tableName).append("]");
                            
                            ResultSet dataRs = conn.createStatement().executeQuery(dataSql.toString());
                            while (dataRs.next()) {
                                String val = dataRs.getString(1); if (val == null || val.trim().isEmpty()) continue;
                                String bc = (keyType == 1) ? val.trim() : nameToBarcodeMap.get(val.toLowerCase().trim());
                                if (bc != null) {
                                    neededBarcodes.add(bc);
                                    int colIndex = 2;
                                    if(!srCol.isEmpty()) { try { saleRateData.put(bc, dataRs.getDouble(colIndex)); } catch(Exception ex) {} colIndex++; }
                                    if(!cpCol.isEmpty()) { try { costPriceData.put(bc, dataRs.getDouble(colIndex)); } catch(Exception ex) {} }
                                }
                            }
                            dataRs.close();
                        } catch(Exception ex) {}
                        publish("Scanning " + tableName + "...");
                    }

                    Map<String, Double> invData = new HashMap<>();
                    Map<String, Double> salesData = new HashMap<>(); 
                    
                    if (isLiveSalesActive && !salesTable.isEmpty() && !neededBarcodes.isEmpty()) {
                        try {
                            StringBuilder inNames = new StringBuilder();
                            for (String bc : neededBarcodes) { String name = barcodeToNameMap.get(bc); if (name != null) inNames.append("'").append(name.replace("'", "''")).append("',"); }
                            if(inNames.length() > 0) {
                                inNames.deleteCharAt(inNames.length() - 1);
                                ResultSet rs = conn.createStatement().executeQuery("SELECT [" + salesNameCol + "], [" + salesValueCol + "] FROM [" + salesTable + "] WHERE [" + salesNameCol + "] IN (" + inNames + ")");
                                while (rs.next()) { String bc = nameToBarcodeMap.get(rs.getString(1).toLowerCase().trim()); if (bc != null) salesData.put(bc, rs.getDouble(2)); }
                                rs.close();
                            }
                        } catch (Exception ex) {}
                    } else if (!isLiveSalesActive) {
                        File f = new File(offlineSalesFile);
                        if (f.exists()) {
                            try (Scanner sc = new Scanner(f)) {
                                while (sc.hasNextLine()) {
                                    String[] parts = sc.nextLine().split("\\|");
                                    if (parts.length >= 2) {
                                        salesData.put(parts[0], parseDouble(parts[1]));
                                    }
                                }
                            } catch (Exception ex) {}
                        }
                    }
                    
                    if (!inventoryTable.isEmpty() && !neededBarcodes.isEmpty()) {
                        try {
                            StringBuilder inNames = new StringBuilder();
                            for (String bc : neededBarcodes) { String name = barcodeToNameMap.get(bc); if (name != null) inNames.append("'").append(name.replace("'", "''")).append("',"); }
                            if(inNames.length() > 0) {
                                inNames.deleteCharAt(inNames.length() - 1);
                                ResultSet rs = conn.createStatement().executeQuery("SELECT [" + invNameCol + "], [" + invValueCol + "] FROM [" + inventoryTable + "] WHERE [" + invNameCol + "] IN (" + inNames + ")");
                                while (rs.next()) { String bc = nameToBarcodeMap.get(rs.getString(1).toLowerCase().trim()); if (bc != null) invData.put(bc, rs.getDouble(2)); }
                                rs.close();
                            }
                        } catch (Exception ex) {}
                    }

                    Map<String, Object[]> dataMap = new HashMap<>();
                    for (String bc : neededBarcodes) {
                        String name = barcodeToNameMap.get(bc);
                        double inv = invData.getOrDefault(bc, 0.0);
                        double sale = salesData.getOrDefault(bc, 0.0);
                        double demand = inv - sale;
                        double daysStock = (sale > 0) ? Math.round((inv * 30.0) / sale) : 999;
                        
                        dataMap.put(bc, new Object[]{ bc, name != null ? name : "N/A", "", "", inv, sale, demand, daysStock, saleRateData.getOrDefault(bc, 0.0), costPriceData.getOrDefault(bc, 0.0) });
                    }
                    return dataMap;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override protected void process(List<String> chunks) { lblStatus.setText(chunks.get(chunks.size()-1)); }

            @Override
            protected void done() {
                try {
                    Map<String, Object[]> dataMap = get();
                    
                    if (dataMap == null) {
                        lblStatus.setText("Failed to load from DB. Loading Local Cache...");
                        loadFullDataFromLocalCache();
                        isOfflineMode = true;
                        startLiveUpdates();
                        startLocalCacheTimer();
                        return;
                    }
                    
                    isOfflineMode = false;
                    
                    DefaultTableModel model = new DefaultTableModel() {
                        @Override public boolean isCellEditable(int row, int column) {
                            String bc = (String) getValueAt(row, 0);
                            if (VENDOR_MARKER.equals(bc)) return column == 1 && btnDragMode.isSelected();
                            if(column == 2 || column == 3) return true;
                            return false;
                        }
                        @Override public Class<?> getColumnClass(int columnIndex) { return Object.class; }
                    };
                    
                    model.addColumn("Barcode"); model.addColumn("Name"); 
                    model.addColumn("Order"); model.addColumn("Price");
                    model.addColumn("Inventory"); model.addColumn("30 Days Sale"); model.addColumn("Demand"); 
                    model.addColumn("Days Stock"); model.addColumn("Sale Rate"); model.addColumn("TP");

                    resultTable = new JTable(model);
                    
                    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                    for (int i = 0; i < model.getColumnCount(); i++) sorter.setSortable(i, false);
                    resultTable.setRowSorter(sorter); 
                    
                    Font headerFont = new Font("Arial", Font.BOLD, 20); 

                    JTableHeader header = resultTable.getTableHeader();
                    header.setFont(headerFont); header.setBackground(new Color(200, 200, 200)); header.setForeground(Color.BLACK);
                    header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40)); 
                    
                    DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            setHorizontalAlignment(SwingConstants.CENTER); setFont(headerFont);
                            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.BLACK), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                            return this;
                        }
                    };
                    for (int i = 0; i < resultTable.getColumnCount(); i++) resultTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

                    resultTable.setDefaultRenderer(Object.class, new CustomRenderer());

                    resultTable.setRowHeight(25);
                    resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
                    
                    loadTableLayout(model, dataMap);
                    resizeColumnWidth(resultTable);
                    
                    orderColumn = resultTable.getColumnModel().getColumn(2);
                    priceColumn = resultTable.getColumnModel().getColumn(3);
                    
                    resultTable.removeColumn(orderColumn);
                    resultTable.removeColumn(priceColumn);
                    btnPrint.setVisible(false);

                    resultTable.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (isVendorInsertMode) {
                                int row = resultTable.rowAtPoint(e.getPoint());
                                if (row != -1) insertVendorRow(row);
                                isVendorInsertMode = false;
                                btnAddVendor.setText("Add Vendor Name"); btnAddVendor.setBackground(new Color(255, 200, 100));
                            }
                        }
                    });

                    // Double-click on vendor row: show total weight (kg) for that vendor (also for this table instance)
                    resultTable.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                                int viewRow = resultTable.rowAtPoint(e.getPoint());
                                if (viewRow == -1) return;
                                int modelRow = resultTable.convertRowIndexToModel(viewRow);
                                DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                                Object bcObj = model.getValueAt(modelRow, 0);
                                // debug message removed
                                if (bcObj != null && VENDOR_MARKER.equals(bcObj.toString())) {
                                    Object nameObj = model.getValueAt(modelRow, 1);
                                    String vname = (nameObj != null) ? nameObj.toString().trim().toLowerCase() : "";
                                    if (vname.isEmpty()) { JOptionPane.showMessageDialog(null, "Vendor name is empty."); return; }
                                    double totalGrams = 0.0;
                                    StringBuilder dbg = new StringBuilder();
                                    int rowsScanned = 0;
                                    // collect contiguous item rows after this vendor row until next vendor marker
                                    for (int r = modelRow + 1; r < model.getRowCount(); r++) {
                                        Object bco = model.getValueAt(r, 0);
                                        String barcodeStr = (bco != null) ? bco.toString() : "";
                                        if (VENDOR_MARKER.equals(barcodeStr)) break;
                                        Object nmObj = model.getValueAt(r, 1);
                                        String iname = (nmObj != null) ? nmObj.toString().toLowerCase().trim() : "";
                                        double perUnitGrams = parseWeightGramsFromName(iname);
                                        double inv = parseDouble(model.getValueAt(r, 4));
                                        double rowTotal;
                                        if (perUnitGrams <= 0 && iname.endsWith("unpack")) {
                                            rowTotal = inv * 1000.0; // inv is already in kg for unpack rows
                                            dbg.append("row=" + r + " name='" + iname + "' UNPACK-> invKg=" + inv + " => " + rowTotal + "g\n");
                                        } else {
                                            rowTotal = perUnitGrams * inv;
                                            dbg.append("row=" + r + " name='" + iname + "' perUnit=" + perUnitGrams + "g inv=" + inv + " => " + rowTotal + "g\n");
                                        }
                                        totalGrams += rowTotal;
                                        rowsScanned++;
                                    }
                                    double totalKg = totalGrams / 1000.0;
                                    double kg = Math.round(totalKg * 1000.0) / 1000.0;
                                    // console debug removed
                                    int[] packSizes = new int[]{50,25};
                                    double[] quantities = new double[packSizes.length];
                                    for (int i = 0; i < packSizes.length; i++) quantities[i] = Math.round((totalKg / packSizes[i]) * 1000.0) / 1000.0;
                                    showVendorWeightDialog(nameObj, totalKg, packSizes, quantities);
                                }
                            }
                        }
                        });

                    resultTable.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                                int viewCol = resultTable.columnAtPoint(e.getPoint());
                                if (viewCol >= 0 && viewCol < resultTable.getColumnCount()) {
                                    int modelCol = resultTable.convertColumnIndexToModel(viewCol);
                                    if (modelCol == 2 || modelCol == 3) { 
                                        int row = resultTable.rowAtPoint(e.getPoint());
                                        if (row != -1) {
                                            if (resultTable.getModel().isCellEditable(row, modelCol)) {
                                                resultTable.editCellAt(row, viewCol);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

                    JScrollPane sp = new JScrollPane(resultTable);
                    styleScrollPane(sp);
                    
                    int idx = tabbedPane.indexOfTab("Live Monitor");
                    if (idx != -1) tabbedPane.remove(idx);
                    tabbedPane.addTab("Live Monitor", sp);
                    tabbedPane.setSelectedIndex(tabbedPane.indexOfTab("Live Monitor"));
                    
                    lblStatus.setText("Ready. Items: " + model.getRowCount());
                    startLiveUpdates();
                    startLocalCacheTimer();

                } catch (Exception ex) { 
                    lblStatus.setText("Error: " + ex.getMessage()); 
                    loadFullDataFromLocalCache();
                    isOfflineMode = true;
                    startLiveUpdates();
                    startLocalCacheTimer();
                }
            }
        }.execute();
    }
    
    private void loadTableLayout(DefaultTableModel model, Map<String, Object[]> dataMap) {
        File f = new File(layoutFile);
        Set<String> addedBarcodes = new HashSet<>();
        if (f.exists()) {
            try (Scanner sc = new Scanner(f)) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if (line.startsWith("VENDOR|")) {
                        String name = line.substring(7);
                        model.addRow(new Object[]{VENDOR_MARKER, name, "", "", 0, 0, 0, 0, 0, 0});
                        resultTable.setRowHeight(model.getRowCount() - 1, 40);
                    } else if (line.startsWith("ITEM|")) {
                        String content = line.substring(5);
                        String[] parts = content.split("\\|", -1);
                        String bc = parts[0];
                        String orderVal = "";
                        String priceVal = "";
                        if(parts.length > 1) orderVal = parts[1];
                        if(parts.length > 2) priceVal = parts[2]; 
                        if (dataMap.containsKey(bc)) {
                            Object[] original = dataMap.get(bc);
                            original[2] = orderVal; 
                            original[3] = priceVal; 
                            model.addRow(original);
                            addedBarcodes.add(bc);
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    private void saveTableLayout() {
        if (resultTable == null) return;
        if (resultTable.isEditing()) resultTable.getCellEditor().stopCellEditing();
        try (PrintWriter writer = new PrintWriter(new FileWriter(layoutFile))) {
            DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String bc = (String) model.getValueAt(i, 0);
                if (VENDOR_MARKER.equals(bc)) writer.println("VENDOR|" + model.getValueAt(i, 1));
                else {
                    Object orderVal = model.getValueAt(i, 2);
                    Object priceVal = model.getValueAt(i, 3);
                    String orderStr = (orderVal == null) ? "" : orderVal.toString();
                    String priceStr = (priceVal == null) ? "" : priceVal.toString();
                    writer.println("ITEM|" + bc + "|" + orderStr + "|" + priceStr);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void startLocalCacheTimer() {
        if (localCacheTimer != null) localCacheTimer.stop();
        localCacheTimer = new Timer(10000, e -> {
            saveFullDataToLocalCache();
        });
        localCacheTimer.start();
    }
    
    private void insertVendorRow(int index) {
        DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
        int modelIndex = resultTable.convertRowIndexToModel(index);
        model.insertRow(modelIndex, new Object[]{VENDOR_MARKER, "New Vendor", "", "", 0, 0, 0, 0, 0, 0});
        resultTable.setRowHeight(index, 40);
        saveTableLayout();
    }
    
    private void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 30; 
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 5, width);
            }
            TableCellRenderer hr = columnModel.getColumn(column).getHeaderRenderer();
            if (hr == null) hr = table.getTableHeader().getDefaultRenderer();
            Component hc = hr.getTableCellRendererComponent(table, columnModel.getColumn(column).getHeaderValue(), false, false, 0, column);
            width = Math.max(hc.getPreferredSize().width + 10, width);
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    private void startLiveUpdates() {
        if (liveUpdateTimer != null) liveUpdateTimer.stop();
        liveUpdateTimer = new Timer(5000, e -> { 
            if (isOfflineMode) return;
            
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    DefaultTableModel model = (DefaultTableModel) resultTable.getModel();
                    Set<String> barcodes = new HashSet<>();
                    Map<String, String> bcToNameLocal = new HashMap<>();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String bc = (String) model.getValueAt(i, 0);
                        if (VENDOR_MARKER.equals(bc)) continue;
                        barcodes.add(bc); bcToNameLocal.put(bc, (String) model.getValueAt(i, 1));
                    }
                    Map<String, Double> invData = new HashMap<>(), salesData = new HashMap<>();
                    StringBuilder inNames = new StringBuilder();
                    for (String bc : barcodes) { String name = bcToNameLocal.get(bc); if (name != null) inNames.append("'").append(name.replace("'", "''")).append("',"); }
                    if (inNames.length() == 0) return null; String inClause = inNames.substring(0, inNames.length() - 1);
                    try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                        if (!inventoryTable.isEmpty()) { 
                            try { 
                                ResultSet rs = conn.createStatement().executeQuery("SELECT [" + invNameCol + "], [" + invValueCol + "] FROM [" + inventoryTable + "] WHERE [" + invNameCol + "] IN (" + inClause + ")"); 
                                while (rs.next()) { String bc = nameToBarcodeMap.get(rs.getString(1).toLowerCase().trim()); if (bc != null) invData.put(bc, rs.getDouble(2)); } 
                                rs.close(); 
                            } catch(Exception ex) {} 
                        }
                        
                        if (isLiveSalesActive && !salesTable.isEmpty()) { 
                            try { 
                                ResultSet rs = conn.createStatement().executeQuery("SELECT [" + salesNameCol + "], [" + salesValueCol + "] FROM [" + salesTable + "] WHERE [" + salesNameCol + "] IN (" + inClause + ")"); 
                                while (rs.next()) { String bc = nameToBarcodeMap.get(rs.getString(1).toLowerCase().trim()); if (bc != null) salesData.put(bc, rs.getDouble(2)); } 
                                rs.close(); 
                            } catch(Exception ex) {} 
                        }
                    } catch (Exception ex) { 
                        SwingUtilities.invokeLater(() -> lblStatus.setText("Connection Lost. Retrying..."));
                        return null;
                    }
                    SwingUtilities.invokeLater(() -> {
                        DefaultTableModel m = (DefaultTableModel) resultTable.getModel();
                        for (int i = 0; i < m.getRowCount(); i++) {
                            String bc = (String) m.getValueAt(i, 0);
                            if (VENDOR_MARKER.equals(bc)) continue;
                            
                            double inv = invData.getOrDefault(bc, 0.0);
                            
                            double sale;
                            if (isLiveSalesActive) {
                                sale = salesData.getOrDefault(bc, 0.0);
                            } else {
                                sale = parseDouble(m.getValueAt(i, 5));
                            }
                            
                            double demand = inv - sale;
                            double daysStock = (sale > 0) ? Math.round((inv * 30.0) / sale) : 999;
                            
                            m.setValueAt(inv, i, 4); 
                            m.setValueAt(sale, i, 5); 
                            m.setValueAt(demand, i, 6); 
                            m.setValueAt(daysStock, i, 7);
                        }
                        resultTable.repaint();
                        lblStatus.setText("Updated: " + new java.util.Date());
                    });
                    return null;
                }
            }.execute();
        });
        liveUpdateTimer.start();
    }
    
    public class PaletteTransferHandler extends TransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            int row = table.getSelectedRow();
            if (row < 0) return null;
            String barcode = (String) table.getValueAt(row, 1);
            return new StringSelection(barcode);
        }
        @Override public int getSourceActions(JComponent c) { return COPY; }
    }
    
    public class TableRowTransferHandler extends TransferHandler {
        private final JTable table;
        public TableRowTransferHandler(JTable table) { this.table = table; }
        @Override protected Transferable createTransferable(JComponent c) { return new StringSelection("move"); }
        @Override public int getSourceActions(JComponent c) { return MOVE; }
        @Override public boolean canImport(TransferHandler.TransferSupport info) { return info.isDataFlavorSupported(DataFlavor.stringFlavor); }
        
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) return false;
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
            int targetRowView = dl.getRow();
            if (targetRowView < 0) targetRowView = 0;
            if (targetRowView > table.getRowCount()) targetRowView = table.getRowCount();
            try {
                String data = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
                if ("move".equals(data)) { boolean success = performInternalMove(model, targetRowView); if(success) saveTableLayout(); return success; }
                else { boolean success = performImport(model, targetRowView, data); if(success) saveTableLayout(); return success; }
            } catch (Exception ex) { return false; }
        }
        
        private boolean performInternalMove(DefaultTableModel model, int targetRowView) {
            int[] selectedRowsView = table.getSelectedRows();
            if (selectedRowsView.length == 0) return false;
            List<Integer> selectedModelIndices = new ArrayList<>();
            List<Object[]> movedData = new ArrayList<>();
            List<Integer> rowHeights = new ArrayList<>();
            for (int viewRow : selectedRowsView) {
                int modelRow = table.convertRowIndexToModel(viewRow);
                selectedModelIndices.add(modelRow);
                Object[] rowData = new Object[model.getColumnCount()];
                for (int col = 0; col < model.getColumnCount(); col++) rowData[col] = model.getValueAt(modelRow, col);
                movedData.add(rowData);
                rowHeights.add(table.getRowHeight(viewRow));
            }
            Collections.sort(selectedModelIndices, Collections.reverseOrder());
            int targetRowModel;
            if (targetRowView >= table.getRowCount()) targetRowModel = model.getRowCount();
            else targetRowModel = table.convertRowIndexToModel(targetRowView);
            int removedAboveTarget = 0;
            for (int mRow : selectedModelIndices) if (mRow < targetRowModel) removedAboveTarget++;
            for (int mRow : selectedModelIndices) model.removeRow(mRow);
            int insertAtModel = targetRowModel - removedAboveTarget;
            for (int i = 0; i < movedData.size(); i++) {
                if (insertAtModel > model.getRowCount()) insertAtModel = model.getRowCount();
                model.insertRow(insertAtModel, movedData.get(i));
                int newViewIndex = table.convertRowIndexToView(insertAtModel);
                if (newViewIndex >= 0) table.setRowHeight(newViewIndex, rowHeights.get(i));
                insertAtModel++;
            }
            table.clearSelection();
            int firstNewRowModel = insertAtModel - movedData.size();
            for (int i = 0; i < movedData.size(); i++) { int viewIdx = table.convertRowIndexToView(firstNewRowModel + i); if (viewIdx >= 0) table.addRowSelectionInterval(viewIdx, viewIdx); }
            return true;
        }
        
        private boolean performImport(DefaultTableModel model, int targetRowView, String barcode) {
            int existingModelIndex = -1;
            Object[] existingData = null;
            int existingHeight = 25;
            for (int i = 0; i < model.getRowCount(); i++) {
                String bc = (String) model.getValueAt(i, 0);
                if (barcode.equals(bc)) {
                    existingModelIndex = i;
                    existingData = new Object[model.getColumnCount()];
                    for(int col=0; col<model.getColumnCount(); col++) existingData[col] = model.getValueAt(i, col);
                    int viewIdx = table.convertRowIndexToView(i);
                    if(viewIdx >= 0) existingHeight = table.getRowHeight(viewIdx);
                    break;
                }
            }
            int targetModel = (targetRowView < table.getRowCount()) ? table.convertRowIndexToModel(targetRowView) : model.getRowCount();
            if (existingModelIndex != -1) { model.removeRow(existingModelIndex); if (existingModelIndex < targetModel) targetModel--; }
            Object[] rowData; int rowHeight;
            if (existingData != null) { rowData = existingData; rowHeight = existingHeight; }
            else {
                String name = barcodeToNameMap.get(barcode);
                if (name == null) name = "Unknown Item";
                rowData = new Object[]{barcode, name, "", "", 0, 0, 0, 999, 0, 0};
                rowHeight = 25;
            }
            model.insertRow(targetModel, rowData);
            int newView = table.convertRowIndexToView(targetModel);
            if(newView >= 0) table.setRowHeight(newView, rowHeight);
            lblStatus.setText(existingData != null ? "Moved: " + rowData[1] : "Added: " + rowData[1]);
            return true;
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new QadriStore().setVisible(true));
    }
}