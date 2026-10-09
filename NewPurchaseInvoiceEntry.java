import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;

public class NewPurchaseInvoiceEntry extends JFrame {
    
    private JComboBox<String> comboVendorName;
    private JTextField txtBarcode, txtTP, txtQty, txtSalePrice, txtItemName, txtInventory;
    private java.util.List<String> vendorNames = new java.util.ArrayList<>();
    private DefaultComboBoxModel<String> fullVendorModel = new DefaultComboBoxModel<>();
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblTotalItems, lblTotalQty, lblTotalAmount;
    private JButton btnSave, btnDeleteRow, btnDeleteAll, btnAddItem, btnEditSelected, btnHold;
    private JDialog itemPickerDialog;
    private JTextField itemSearchField;
    private JList<ItemChoice> itemPickerList;
    private DefaultListModel<ItemChoice> itemPickerModel;
    
    private String url, user, pass;
    private int editingRowIndex = -1;
    private int editingInvoiceId = -1;
    private int editingInvoiceNumber = -1;

    public NewPurchaseInvoiceEntry(JFrame owner, String url, String user, String pass) {
        super("Purchase Invoice System");
        this.url = url;
        this.user = user;
        this.pass = pass;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));
        setWindowIcon();

        // --- TOP PANEL (Vendor & Single Line Entry) ---
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(" Invoice Details "));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Vendor
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        topPanel.add(createLabel("Vendor Name:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 5;
        comboVendorName = new JComboBox<>();
        comboVendorName.setEditable(true);
        comboVendorName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        setupVendorAutoComplete();
        topPanel.add(comboVendorName, gbc);

        // Row 1: Barcode, TP, Qty
        gbc.gridwidth = 1; gbc.gridy = 1;
        
        gbc.gridx = 0; gbc.weightx = 0;
        topPanel.add(createLabel("Barcode:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.5;
        txtBarcode = new JTextField(15);
        txtBarcode.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtBarcode.setBackground(new Color(255, 255, 240));
        topPanel.add(txtBarcode, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        topPanel.add(createLabel("TP:"), gbc);
        
        gbc.gridx = 3; gbc.weightx = 0.3;
        txtTP = new JTextField(8);
        txtTP.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(txtTP, gbc);

        gbc.gridx = 4; gbc.weightx = 0;
        topPanel.add(createLabel("Qty:"), gbc);
        
        gbc.gridx = 5; gbc.weightx = 0.3;
        txtQty = new JTextField(5);
        txtQty.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtQty.setText("1");
        topPanel.add(txtQty, gbc);

        // Row 2: Item Name & Inventory
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0;
        topPanel.add(createLabel("Item Name:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.8;
        txtItemName = new JTextField(18);
        txtItemName.setEditable(false);
        txtItemName.setBackground(new Color(245, 245, 245));
        txtItemName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtItemName.setPreferredSize(new Dimension(220, 30));
        topPanel.add(txtItemName, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        topPanel.add(createLabel("Inv:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.2;
        txtInventory = new JTextField(8);
        txtInventory.setEditable(false);
        txtInventory.setBackground(new Color(250, 250, 240));
        txtInventory.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtInventory.setPreferredSize(new Dimension(90, 30));
        topPanel.add(txtInventory, gbc);

        // Row 3: Sale Price & Add Button
        gbc.gridy = 3;
        gbc.gridx = 0; gbc.weightx = 0;
        topPanel.add(createLabel("Sale Price:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 0.5;
        txtSalePrice = new JTextField(10);
        txtSalePrice.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(txtSalePrice, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.gridwidth = 4;
        btnAddItem = createProfButton("Add Item (Alt+A)", new Color(0, 100, 0));
        btnAddItem.setPreferredSize(new Dimension(180, 40));
        btnAddItem.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(btnAddItem, gbc);
        gbc.gridwidth = 1;

        // Key Bindings and Listeners for Flow
        JTextField txtVendor = (JTextField) comboVendorName.getEditor().getEditorComponent();
        txtVendor.addActionListener(e -> txtBarcode.requestFocusInWindow());
        
        txtBarcode.addActionListener(e -> {
            if (txtBarcode.getText().trim().isEmpty()) {
                txtItemName.setText("");
                txtInventory.setText("");
                txtTP.setText("");
                txtSalePrice.setText("");
                return;
            }
            fetchProductDetails();
            txtTP.requestFocusInWindow();
        });
        
        txtTP.addActionListener(e -> txtQty.requestFocusInWindow());
        txtQty.addActionListener(e -> txtSalePrice.requestFocusInWindow());
        txtSalePrice.addActionListener(e -> btnAddItem.requestFocusInWindow());
        txtSalePrice.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK), "addItem");
        txtSalePrice.getActionMap().put("addItem", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnAddItem.doClick();
            }
        });

        addSelectionOnFocus(txtBarcode);
        addSelectionOnFocus(txtTP);
        addSelectionOnFocus(txtQty);
        addSelectionOnFocus(txtSalePrice);

        btnAddItem.setMnemonic(KeyEvent.VK_A); // Alt+A shortcut
        btnAddItem.addActionListener(e -> {
            addItemToTable();
            txtBarcode.requestFocusInWindow();
        });

        add(topPanel, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Product ID", "Product Name", "Barcode", "TP (Cost)", "Qty", "Sale Price", "Total"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) { return columnIndex >= 3 ? Double.class : String.class; }
        };
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(28);
        table.setSelectionBackground(new Color(173, 216, 230));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnEditSelected.setEnabled(table.getSelectedRow() != -1);
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(new Color(20, 30, 40));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BOTTOM PANEL (Buttons & Totals) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        btnEditSelected = createProfButton("Edit Selected", new Color(0, 123, 255));
        btnEditSelected.setEnabled(false);
        btnEditSelected.addActionListener(e -> editSelectedItem());
        btnPanel.add(btnEditSelected);
        
        btnDeleteRow = createProfButton("Delete Item", new Color(220, 53, 69));
        btnDeleteRow.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) { model.removeRow(row); updateTotals(); } 
            else { JOptionPane.showMessageDialog(this, "Pehle row select karein!"); }
        });

        btnDeleteAll = createProfButton("Clear All", new Color(108, 117, 125));
        btnDeleteAll.addActionListener(e -> { model.setRowCount(0); updateTotals(); });

        btnHold = createProfButton("Hold", new Color(120, 120, 120));
        btnHold.addActionListener(e -> {
            if (model.getRowCount() > 0) {
                saveCurrentInvoiceAsHold();
            } else {
                JOptionPane.showMessageDialog(this, "Hold karne ke liye item add karein!");
            }
            showHoldInvoicesDialog();
        });
        refreshHoldButtonText();

        btnPanel.add(btnDeleteRow);
        btnPanel.add(btnDeleteAll);
        btnPanel.add(btnHold);
        bottomPanel.add(btnPanel, BorderLayout.WEST);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        lblTotalItems = new JLabel("Items: 0");
        lblTotalQty = new JLabel("Qty: 0");
        lblTotalAmount = new JLabel("Total: Rs. 0.00");
        
        lblTotalItems.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalQty.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotalAmount.setForeground(new Color(0, 100, 0));

        statsPanel.add(lblTotalItems);
        statsPanel.add(lblTotalQty);
        
        btnSave = createProfButton("SAVE INVOICE", new Color(0, 120, 60));
        btnSave.setPreferredSize(new Dimension(200, 45));
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSave.addActionListener(e -> saveInvoiceToDB());
        
        statsPanel.add(btnSave);
        statsPanel.add(lblTotalAmount);
        
        bottomPanel.add(statsPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (model.getRowCount() > 0) {
                    saveCurrentInvoiceAsHold();
                }
            }
        });

        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JComponent rootPane = (JComponent) getContentPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveInvoiceShortcut");
        rootPane.getActionMap().put("saveInvoiceShortcut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveInvoiceToDB();
            }
        });

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "openItemPicker");
        rootPane.getActionMap().put("openItemPicker", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showItemPickerDialog();
            }
        });

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "openHoldInvoices");
        rootPane.getActionMap().put("openHoldInvoices", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHoldInvoicesDialog();
            }
        });

        SwingUtilities.invokeLater(() -> {
            comboVendorName.setSelectedIndex(-1);
            txtVendor.setText("");
            txtVendor.requestFocusInWindow();
        });
    }

    public NewPurchaseInvoiceEntry(JFrame owner, String url, String user, String pass, int invoiceId) {
        this(owner, url, user, pass);
        loadInvoiceForEditing(invoiceId);
    }

    public NewPurchaseInvoiceEntry(JFrame owner, String url, String user, String pass, int holdId, boolean loadHold) {
        this(owner, url, user, pass);
        if (loadHold) {
            loadHoldInvoice(holdId);
        }
    }

    private static class ItemChoice {
        private final int productId;
        private final String name;
        private final String barcode;
        private final BigDecimal costPrice;
        private final BigDecimal saleRate;

        private ItemChoice(int productId, String name, String barcode, BigDecimal costPrice, BigDecimal saleRate) {
            this.productId = productId;
            this.name = name;
            this.barcode = barcode;
            this.costPrice = costPrice;
            this.saleRate = saleRate;
        }

        @Override
        public String toString() {
            return name + "  [" + (barcode == null ? "" : barcode) + "]";
        }
    }

    private void showItemPickerDialog() {
        if (itemPickerDialog == null) {
            itemPickerDialog = new JDialog(this, "Item Lookup", Dialog.ModalityType.APPLICATION_MODAL);
            itemPickerDialog.setSize(650, 500);
            itemPickerDialog.setLocationRelativeTo(this);
            itemPickerDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            itemPickerDialog.setResizable(false);

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            mainPanel.setBackground(new Color(248, 250, 252));

            JPanel headerPanel = new JPanel(new BorderLayout(8, 4));
            headerPanel.setOpaque(false);
            JLabel titleLabel = new JLabel("Select Item");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(new Color(20, 40, 80));
            JLabel hintLabel = new JLabel("Search by item name or barcode. Press Enter to add the highlighted item.");
            hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            hintLabel.setForeground(new Color(90, 100, 115));
            headerPanel.add(titleLabel, BorderLayout.NORTH);
            headerPanel.add(hintLabel, BorderLayout.SOUTH);
            mainPanel.add(headerPanel, BorderLayout.NORTH);

            JPanel searchPanel = new JPanel(new BorderLayout());
            searchPanel.setOpaque(false);
            JLabel searchLabel = new JLabel("Search:");
            searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            itemSearchField = new JTextField();
            itemSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            itemSearchField.setPreferredSize(new Dimension(0, 34));
            itemSearchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 190, 205), 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            itemSearchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { refreshItemPicker(itemSearchField.getText()); }
                @Override
                public void removeUpdate(DocumentEvent e) { refreshItemPicker(itemSearchField.getText()); }
                @Override
                public void changedUpdate(DocumentEvent e) { refreshItemPicker(itemSearchField.getText()); }
            });
            itemSearchField.addActionListener(e -> {
                refreshItemPicker(itemSearchField.getText());
                if (!itemPickerModel.isEmpty()) {
                    itemPickerList.setSelectedIndex(0);
                    itemPickerList.requestFocusInWindow();
                }
            });
            searchPanel.add(searchLabel, BorderLayout.WEST);
            searchPanel.add(itemSearchField, BorderLayout.CENTER);
            mainPanel.add(searchPanel, BorderLayout.CENTER);

            itemPickerModel = new DefaultListModel<>();
            itemPickerList = new JList<>(itemPickerModel);
            itemPickerList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            itemPickerList.setSelectionBackground(new Color(173, 216, 230));
            itemPickerList.setSelectionForeground(Color.BLACK);
            itemPickerList.setVisibleRowCount(12);
            itemPickerList.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
            itemPickerList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        selectPickedItem();
                    }
                }
            });
            itemPickerList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "selectItem");
            itemPickerList.getActionMap().put("selectItem", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectPickedItem();
                }
            });
            itemPickerList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
            itemPickerList.getActionMap().put("closeDialog", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    itemPickerDialog.dispose();
                }
            });
            itemPickerList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ItemChoice) {
                        ItemChoice choice = (ItemChoice) value;
                        setText((index + 1) + ". " + choice.name + "   [" + choice.barcode + "]");
                    }
                    return this;
                }
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setOpaque(false);
            JButton btnSelect = new JButton("Add Selected");
            btnSelect.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnSelect.setBackground(new Color(0, 120, 60));
            btnSelect.setForeground(Color.WHITE);
            btnSelect.addActionListener(e -> selectPickedItem());
            JButton btnClose = new JButton("Close");
            btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btnClose.addActionListener(e -> itemPickerDialog.dispose());
            buttonPanel.add(btnSelect);
            buttonPanel.add(btnClose);

            JPanel listPanel = new JPanel(new BorderLayout(0, 8));
            listPanel.setOpaque(false);
            listPanel.add(new JScrollPane(itemPickerList), BorderLayout.CENTER);
            listPanel.add(buttonPanel, BorderLayout.SOUTH);
            mainPanel.add(listPanel, BorderLayout.SOUTH);
            itemPickerDialog.setContentPane(mainPanel);
        }

        refreshItemPicker("");
        itemSearchField.setText("");
        itemSearchField.requestFocusInWindow();
        itemPickerDialog.setVisible(true);
    }

    private void refreshItemPicker(String query) {
        itemPickerModel.clear();
        String term = query == null ? "" : query.trim();
        String likeTerm = "%" + term + "%";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT TOP 100 p.ProductItemId, p.LongName, p.Barcode, CAST(p.CostPrice AS decimal(18,2)) AS CostPrice, CAST(p.SaleRate AS decimal(18,2)) AS SaleRate " +
                         "FROM ProductItem p " +
                         "WHERE (? = '' OR p.LongName LIKE ? OR p.Barcode LIKE ?) " +
                         "ORDER BY p.LongName";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, term);
                pst.setString(2, likeTerm);
                pst.setString(3, likeTerm);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        itemPickerModel.addElement(new ItemChoice(
                                rs.getInt(1),
                                rs.getString(2),
                                rs.getString(3),
                                rs.getBigDecimal(4),
                                rs.getBigDecimal(5)
                        ));
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to load items: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }

        if (!itemPickerModel.isEmpty()) {
            itemPickerList.setSelectedIndex(0);
        } else {
            itemPickerList.clearSelection();
        }
    }

    private void selectPickedItem() {
        ItemChoice selected = itemPickerList.getSelectedValue();
        if (selected == null) {
            return;
        }

        BigDecimal qty = parseDecimal(txtQty.getText());
        if (qty.signum() <= 0) {
            qty = BigDecimal.ONE;
        }

        txtBarcode.setText(selected.barcode);
        txtItemName.setText(selected.name);
        txtTP.setText(formatDecimal(selected.costPrice));
        txtSalePrice.setText(formatDecimal(selected.saleRate));
        txtQty.setText(qty.stripTrailingZeros().toPlainString());

        txtBarcode.requestFocusInWindow();
        if (itemPickerDialog != null) {
            itemPickerDialog.dispose();
        }
    }

    private void setupVendorAutoComplete() {
        vendorNames.clear();
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT [Name] FROM ProductVendor ORDER BY [Name]");
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null && !name.trim().isEmpty()) {
                    vendorNames.add(name.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        fullVendorModel = new DefaultComboBoxModel<>();
        for (String name : vendorNames) {
            fullVendorModel.addElement(name);
        }
        comboVendorName.setModel(fullVendorModel);
        comboVendorName.setMaximumRowCount(10);

        JTextField txtVendor = (JTextField) comboVendorName.getEditor().getEditorComponent();
        txtVendor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateVendorSuggestions();
            }
        });

        comboVendorName.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateVendorSuggestions();
            }
        });

        txtVendor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    comboVendorName.hidePopup();
                    return;
                }
                updateVendorSuggestions();
            }
        });
    }

    private void updateVendorSuggestions() {
        JTextField txtVendor = (JTextField) comboVendorName.getEditor().getEditorComponent();
        String typed = txtVendor.getText() == null ? "" : txtVendor.getText().trim();
        String lowerTyped = typed.toLowerCase();

        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<String> filteredModel = new DefaultComboBoxModel<>();
            boolean hasMatch = false;
            for (int i = 0; i < fullVendorModel.getSize(); i++) {
                String name = (String) fullVendorModel.getElementAt(i);
                if (typed.isEmpty() || name.toLowerCase().contains(lowerTyped)) {
                    filteredModel.addElement(name);
                    hasMatch = true;
                }
            }

            comboVendorName.setModel(filteredModel);
            txtVendor.setText(typed);
            if (!typed.isEmpty() && hasMatch) {
                comboVendorName.showPopup();
            } else {
                comboVendorName.hidePopup();
            }
            if (!typed.isEmpty()) {
                txtVendor.setCaretPosition(typed.length());
            }
        });
    }

    private void setWindowIcon() {
        try {
            String[] candidates = {
                    "Icon/icon.ico",
                    "Icon/icon.png",
                    "icon.ico",
                    "icon.png",
                    "/Icon/icon.ico",
                    "/Icon/icon.png",
                    "/icon.ico",
                    "/icon.png"
            };

            for (String candidate : candidates) {
                java.io.File iconFile = new java.io.File(candidate);
                if (iconFile.exists()) {
                    Image icon = ImageIO.read(iconFile);
                    if (icon != null) {
                        setIconImage(icon);
                        return;
                    }
                }
            }

            URL iconUrl = getClass().getResource("/Icon/icon.png");
            if (iconUrl == null) {
                iconUrl = getClass().getResource("/Icon/icon.ico");
            }
            if (iconUrl == null) {
                iconUrl = getClass().getResource("/icon.png");
            }
            if (iconUrl == null) {
                iconUrl = getClass().getResource("/icon.ico");
            }
            if (iconUrl != null) {
                Image icon = ImageIO.read(iconUrl);
                if (icon != null) {
                    setIconImage(icon);
                }
            }
        } catch (Exception ignored) {}
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(30, 30, 30));
        return label;
    }

    private JButton createProfButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        Color originalBg = bgColor;
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(adjustColor(originalBg, 0.12f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(originalBg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(adjustColor(originalBg, 0.20f));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 70), 2),
                        BorderFactory.createEmptyBorder(7, 13, 7, 13)));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(originalBg);
                btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            }
        });
        return btn;
    }

    private void addSelectionOnFocus(JTextField field) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.selectAll();
            }
        });
    }

    private Color adjustColor(Color color, float factor) {
        int r = Math.max(0, Math.min(255, (int)(color.getRed() * (1f - factor))));
        int g = Math.max(0, Math.min(255, (int)(color.getGreen() * (1f - factor))));
        int b = Math.max(0, Math.min(255, (int)(color.getBlue() * (1f - factor))));
        return new Color(r, g, b);
    }

    private void loadInvoiceForEditing(int invoiceId) {
        editingInvoiceId = invoiceId;
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            PreparedStatement pstHeader = conn.prepareStatement(
                    "SELECT pi.InvoiceNumber, pv.Name FROM PurchaseInvoice pi INNER JOIN ProductVendor pv ON pi.ProductVendorId = pv.ProductVendorId WHERE pi.PurchaseInvoiceId = ?");
            pstHeader.setInt(1, invoiceId);
            ResultSet rsHeader = pstHeader.executeQuery();
            if (rsHeader.next()) {
                editingInvoiceNumber = rsHeader.getInt(1);
                comboVendorName.setSelectedItem(rsHeader.getString(2));
                setTitle("Edit Purchase Invoice #" + editingInvoiceNumber);
            }

            PreparedStatement pstItems = conn.prepareStatement(
                    "SELECT pii.ProductItemId, p.LongName, p.Barcode, pii.Quantity, pii.Price, pii.SaleRate " +
                    "FROM PurchaseInvoiceItem pii INNER JOIN ProductItem p ON pii.ProductItemId = p.ProductItemId " +
                    "WHERE pii.PurchaseInvoiceId = ?");
            pstItems.setInt(1, invoiceId);
            ResultSet rsItems = pstItems.executeQuery();
            while (rsItems.next()) {
                int itemId = rsItems.getInt(1);
                String productName = rsItems.getString(2);
                String barcode = rsItems.getString(3);
                double qty = rsItems.getDouble(4);
                double tp = rsItems.getDouble(5);
                double saleRate = rsItems.getDouble(6);
                model.addRow(new Object[]{itemId, productName, barcode, tp, qty, saleRate, tp * qty});
            }

            updateTotals();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to load invoice for editing: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fetchProductDetails() {
        String input = txtBarcode.getText().trim();
        if (input.isEmpty()) {
            txtItemName.setText("");
            txtInventory.setText("");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                String sql = "SELECT TOP 1 p.ProductItemId, p.LongName, p.Barcode, CAST(p.CostPrice AS float), CAST(p.SaleRate AS float) " +
                             "FROM ProductItem p " +
                             "WHERE p.Barcode = ? OR p.LongName = ? OR (ISNUMERIC(p.Barcode) = 1 AND ISNUMERIC(?) = 1 AND TRY_CAST(p.Barcode AS bigint) = TRY_CAST(? AS bigint))";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, input); pst.setString(2, input); pst.setString(3, input); pst.setString(4, input);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String itemName = rs.getString(2);
                String barcode = rs.getString(3);
                txtItemName.setText(itemName);
                txtTP.setText(String.format("%.2f", rs.getDouble(4)));
                txtSalePrice.setText(String.format("%.2f", rs.getDouble(5)));
                txtInventory.setText(loadInventoryValue(conn, itemName, barcode));
            } else {
                txtItemName.setText("");
                txtInventory.setText("");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatInventoryValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }

        BigDecimal decimalValue = BigDecimal.valueOf(value).stripTrailingZeros();
        return decimalValue.toPlainString();
    }

    private String loadInventoryValue(Connection conn, String itemName, String barcode) {
        try {
            String inventoryTable = null;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'ProductName' INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Inventory'")) {
                if (rs.next()) {
                    inventoryTable = rs.getString(1);
                }
            } catch (Exception ignored) {}

            if (inventoryTable != null && !inventoryTable.trim().isEmpty()) {
                String query = "SELECT TOP 1 [Inventory] FROM [" + inventoryTable + "] WHERE [ProductName] = ? OR [ProductName] = ?";
                try (PreparedStatement pst = conn.prepareStatement(query)) {
                    pst.setString(1, itemName);
                    pst.setString(2, itemName == null ? "" : itemName.trim());
                    try (ResultSet invRs = pst.executeQuery()) {
                        if (invRs.next()) {
                            return formatInventoryValue(invRs.getDouble(1));
                        }
                    }
                } catch (Exception ignored) {}
            }

            try (PreparedStatement stockPst = conn.prepareStatement("SELECT ISNULL(Inventory, 0) FROM StockData WHERE Barcode = ?")) {
                stockPst.setString(1, barcode);
                try (ResultSet stockRs = stockPst.executeQuery()) {
                    if (stockRs.next()) {
                        return formatInventoryValue(stockRs.getDouble(1));
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        return "0";
    }

    private void editSelectedItem() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Pehle row select karein!");
            return;
        }

        int modelRow = viewRow;
        editingRowIndex = modelRow;
        btnAddItem.setText("Update Item (Alt+A)");

        String barcode = model.getValueAt(modelRow, 2) != null ? model.getValueAt(modelRow, 2).toString().trim() : "";
        String itemName = model.getValueAt(modelRow, 1) != null ? model.getValueAt(modelRow, 1).toString().trim() : "";
        txtBarcode.setText(barcode);
        txtItemName.setText(itemName);
        txtTP.setText(formatDecimal(model.getValueAt(modelRow, 3)));
        txtQty.setText(formatDecimal(model.getValueAt(modelRow, 4)));
        txtSalePrice.setText(formatDecimal(model.getValueAt(modelRow, 5)));

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            txtInventory.setText(loadInventoryValue(conn, itemName, barcode));
        } catch (Exception ignored) {
            txtInventory.setText("");
        }

        txtBarcode.requestFocusInWindow();
        txtBarcode.selectAll();
    }

    private void addItemToTable() {
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty()) { txtBarcode.requestFocusInWindow(); return; }

        fetchProductDetails();

        int itemId = -1; String prodName = txtItemName.getText().trim();
        double tp = parseDouble(txtTP.getText());
        double qty = parseDouble(txtQty.getText());
        double saleRate = parseDouble(txtSalePrice.getText());

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                String sql = "SELECT TOP 1 ProductItemId, LongName " +
                             "FROM ProductItem " +
                             "WHERE Barcode = ? OR LongName = ? OR (ISNUMERIC(Barcode) = 1 AND ISNUMERIC(?) = 1 AND TRY_CAST(Barcode AS bigint) = TRY_CAST(? AS bigint))";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, barcode); pst.setString(2, barcode); pst.setString(3, barcode); pst.setString(4, barcode);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                itemId = rs.getInt(1);
                prodName = rs.getString(2);
                Object[] rowData = new Object[]{itemId, prodName, barcode, tp, qty, saleRate, tp * qty};

                if (editingRowIndex >= 0 && editingRowIndex < model.getRowCount()) {
                    for (int i = 0; i < rowData.length; i++) {
                        model.setValueAt(rowData[i], editingRowIndex, i);
                    }
                    updateTotals();
                    JOptionPane.showMessageDialog(this, "Item updated successfully!");
                } else {
                    addItemRowToTable(rowData);
                    updateTotals();
                }

                resetEntryForm();
            } else {
                JOptionPane.showMessageDialog(this, "Product nahi mila!", "Error", JOptionPane.ERROR_MESSAGE);
                txtBarcode.selectAll();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addItemRowToTable(Object[] rowData) {
        model.insertRow(0, rowData);

        if (table.getRowCount() > 0) {
            table.clearSelection();
            table.setRowSelectionInterval(0, 0);
            table.changeSelection(0, 0, false, false);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
            table.repaint();
        }
    }

    private void removeExistingInvoiceStock(Connection conn, int invoiceId) throws SQLException {
        String sql = "SELECT p.LongName, p.Barcode, pii.Quantity " +
                     "FROM PurchaseInvoiceItem pii INNER JOIN ProductItem p ON pii.ProductItemId = p.ProductItemId " +
                     "WHERE pii.PurchaseInvoiceId = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, invoiceId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    adjustInventoryForItem(conn, rs.getString(1), rs.getString(2), rs.getBigDecimal(3).negate());
                }
            }
        }
    }

    private void adjustInventoryForItem(Connection conn, String itemName, String barcode, BigDecimal qtyDelta) throws SQLException {
        if (itemName == null || itemName.trim().isEmpty() || qtyDelta.signum() == 0) {
            return;
        }

        String inventoryTable = null;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'ProductName' INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Inventory'")) {
            if (rs.next()) {
                inventoryTable = rs.getString(1);
            }
        } catch (Exception ignored) {}

        if (inventoryTable != null && !inventoryTable.trim().isEmpty()) {
            try (PreparedStatement pst = conn.prepareStatement("UPDATE [" + inventoryTable + "] SET [Inventory] = ISNULL([Inventory], 0) + CAST(? AS decimal(18,3)) WHERE [ProductName] = ?")) {
                pst.setBigDecimal(1, qtyDelta);
                pst.setString(2, itemName);
                pst.executeUpdate();
            } catch (Exception ignored) {}
        }

        try (PreparedStatement pst = conn.prepareStatement("UPDATE StockData SET Inventory = ISNULL(Inventory, 0) + CAST(? AS decimal(18,3)) WHERE Barcode = ?")) {
            pst.setBigDecimal(1, qtyDelta);
            pst.setString(2, barcode);
            pst.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void updateInventoryForItems(Connection conn, DefaultTableModel model, boolean addToStock) throws SQLException {
        for (int i = 0; i < model.getRowCount(); i++) {
            String itemName = model.getValueAt(i, 1) != null ? model.getValueAt(i, 1).toString().trim() : "";
            String barcode = model.getValueAt(i, 2) != null ? model.getValueAt(i, 2).toString().trim() : "";
            BigDecimal qty = parseDecimal(model.getValueAt(i, 4));
            if (itemName.isEmpty() || qty.signum() == 0) {
                continue;
            }
            adjustInventoryForItem(conn, itemName, barcode, addToStock ? qty : qty.negate());
        }
    }

    private BigDecimal calculateNetAmount() {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < model.getRowCount(); i++) {
            BigDecimal tp = parseDecimal(model.getValueAt(i, 3));
            BigDecimal qty = parseDecimal(model.getValueAt(i, 4));
            totalAmount = totalAmount.add(tp.multiply(qty));
        }
        return totalAmount;
    }

    private void updateTotals() {
        int items = model.getRowCount();
        double totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < items; i++) {
            double qty = parseDouble(model.getValueAt(i, 4));
            totalQty += qty;
            totalAmount = totalAmount.add(parseDecimal(model.getValueAt(i, 3)).multiply(parseDecimal(model.getValueAt(i, 4))));
        }
        lblTotalItems.setText("Items: " + items);
        lblTotalQty.setText("Qty: " + String.format("%.2f", totalQty));
        lblTotalAmount.setText("Total: Rs. " + String.format("%.2f", totalAmount));
    }

    private void resetEntryForm() {
        editingRowIndex = -1;
        editingInvoiceId = -1;
        editingInvoiceNumber = -1;
        btnAddItem.setText("Add Item (Alt+A)");
        txtBarcode.setText("");
        txtItemName.setText("");
        txtInventory.setText("");
        txtTP.setText("");
        txtQty.setText("1");
        txtSalePrice.setText("");
        txtBarcode.requestFocusInWindow();
    }

    private String formatDecimal(Object value) {
        if (value == null) {
            return "";
        }
        BigDecimal decimalValue = parseDecimal(value);
        return decimalValue.stripTrailingZeros().toPlainString();
    }

    private void createHoldTableIfNeeded(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("IF OBJECT_ID(N'dbo.PurchaseInvoiceHold', N'U') IS NULL " +
                    "CREATE TABLE dbo.PurchaseInvoiceHold (HoldId INT IDENTITY(1,1) PRIMARY KEY, HoldNumber INT NOT NULL, VendorName NVARCHAR(200), CreatedDate DATETIME DEFAULT GETDATE(), LastUpdatedDate DATETIME DEFAULT GETDATE(), NetAmount DECIMAL(18,2), ItemData NVARCHAR(MAX))");
        }
    }

    private void refreshHoldButtonText() {
        if (btnHold == null) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            createHoldTableIfNeeded(conn);
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM PurchaseInvoiceHold")) {
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        btnHold.setText(count > 0 ? "Hold (" + count + ")" : "Hold");
                    }
                }
            }
        } catch (Exception ex) {
            btnHold.setText("Hold");
        }
    }

    private void saveCurrentInvoiceAsHold() {
        String vendorName = comboVendorName.getEditor().getItem() != null ? comboVendorName.getEditor().getItem().toString().trim() : "";
        if (model.getRowCount() == 0) {
            return;
        }

        BigDecimal netAmount = calculateNetAmount();
        StringBuilder itemData = new StringBuilder();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (i > 0) {
                itemData.append(";;");
            }
            String productId = model.getValueAt(i, 0) != null ? model.getValueAt(i, 0).toString() : "";
            String productName = model.getValueAt(i, 1) != null ? model.getValueAt(i, 1).toString() : "";
            String barcode = model.getValueAt(i, 2) != null ? model.getValueAt(i, 2).toString() : "";
            String tp = model.getValueAt(i, 3) != null ? model.getValueAt(i, 3).toString() : "";
            String qty = model.getValueAt(i, 4) != null ? model.getValueAt(i, 4).toString() : "";
            String saleRate = model.getValueAt(i, 5) != null ? model.getValueAt(i, 5).toString() : "";
            String total = model.getValueAt(i, 6) != null ? model.getValueAt(i, 6).toString() : "";
            itemData.append(productId).append("|").append(productName).append("|").append(barcode)
                    .append("|").append(tp).append("|").append(qty).append("|").append(saleRate)
                    .append("|").append(total);
        }

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            createHoldTableIfNeeded(conn);
            int holdNumber = 1;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(HoldNumber), 0) + 1 FROM PurchaseInvoiceHold")) {
                if (rs.next()) {
                    holdNumber = rs.getInt(1);
                }
            }

            try (PreparedStatement pst = conn.prepareStatement("INSERT INTO PurchaseInvoiceHold (HoldNumber, VendorName, NetAmount, ItemData) VALUES (?, ?, ?, ?)")) {
                pst.setInt(1, holdNumber);
                pst.setString(2, vendorName);
                pst.setBigDecimal(3, netAmount);
                pst.setString(4, itemData.toString());
                pst.executeUpdate();
            }
            refreshHoldButtonText();
            JOptionPane.showMessageDialog(this, "Invoice hold save ho gaya. Hold No: " + holdNumber, "Hold Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Hold save nahi ho saka: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHoldInvoicesDialog() {
        JDialog dialog = new JDialog(this, "Held Purchase Invoices", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        DefaultTableModel holdModel = new DefaultTableModel(new String[]{"Hold ID", "Hold No", "Vendor", "Date", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable holdTable = new JTable(holdModel);
        holdTable.setRowHeight(24);
        holdTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        refreshHoldButtonText();

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            createHoldTableIfNeeded(conn);
            try (PreparedStatement pst = conn.prepareStatement("SELECT HoldId, HoldNumber, VendorName, CreatedDate, NetAmount FROM PurchaseInvoiceHold ORDER BY HoldId DESC")) {
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        holdModel.addRow(new Object[]{rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getTimestamp(4), rs.getBigDecimal(5)});
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Hold list load nahi ho sakti: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        holdTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = holdTable.getSelectedRow();
                    if (viewRow >= 0) {
                        int holdId = (int) holdModel.getValueAt(viewRow, 0);
                        loadHoldInvoice(holdId);
                        dialog.dispose();
                    }
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnLoad = new JButton("Load Selected");
        btnLoad.addActionListener(e -> {
            int viewRow = holdTable.getSelectedRow();
            if (viewRow >= 0) {
                int holdId = (int) holdModel.getValueAt(viewRow, 0);
                loadHoldInvoice(holdId);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Pehle hold select karein!");
            }
        });
        JButton btnDelete = new JButton("Delete Selected");
        btnDelete.addActionListener(e -> {
            int viewRow = holdTable.getSelectedRow();
            if (viewRow >= 0) {
                int holdId = (int) holdModel.getValueAt(viewRow, 0);
                int confirm = JOptionPane.showConfirmDialog(dialog, "Is hold ko delete karna chahte ho?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                        createHoldTableIfNeeded(conn);
                        try (PreparedStatement pst = conn.prepareStatement("DELETE FROM PurchaseInvoiceHold WHERE HoldId = ?")) {
                            pst.setInt(1, holdId);
                            pst.executeUpdate();
                        }
                        holdModel.removeRow(viewRow);
                        refreshHoldButtonText();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Hold delete nahi ho saka: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Pehle hold select karein!");
            }
        });
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnLoad);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClose);

        dialog.add(new JScrollPane(holdTable), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void loadHoldInvoice(int holdId) {
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            createHoldTableIfNeeded(conn);
            try (PreparedStatement pst = conn.prepareStatement("SELECT VendorName, ItemData FROM PurchaseInvoiceHold WHERE HoldId = ?")) {
                pst.setInt(1, holdId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        model.setRowCount(0);
                        comboVendorName.setSelectedItem(rs.getString(1));
                        String itemData = rs.getString(2);
                        if (itemData != null && !itemData.isEmpty()) {
                            String[] entries = itemData.split(";;");
                            for (String entry : entries) {
                                String[] parts = entry.split("\\|", -1);
                                if (parts.length >= 7) {
                                    Object[] rowData = new Object[]{
                                            parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]),
                                            parts[1],
                                            parts[2],
                                            parts[3].isEmpty() ? 0.0 : Double.parseDouble(parts[3]),
                                            parts[4].isEmpty() ? 0.0 : Double.parseDouble(parts[4]),
                                            parts[5].isEmpty() ? 0.0 : Double.parseDouble(parts[5]),
                                            parts[6].isEmpty() ? 0.0 : Double.parseDouble(parts[6])
                                    };
                                    model.addRow(rowData);
                                }
                            }
                        }
                        updateTotals();
                        resetEntryForm();
                        refreshHoldButtonText();
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Hold invoice load nahi ho saka: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double parseDouble(Object obj) {
        if (obj == null) return 0;
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0; }
    }

    private BigDecimal parseDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        try {
            String text = obj.toString().trim();
            if (text.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void saveInvoiceToDB() {
        String vendorName = comboVendorName.getEditor().getItem() != null ? comboVendorName.getEditor().getItem().toString().trim() : "";
        if (vendorName.isEmpty()) { JOptionPane.showMessageDialog(this, "Pehle Vendor select karein!"); return; }
        if (model.getRowCount() == 0) { JOptionPane.showMessageDialog(this, "Invoice mein koi item nahi hai!"); return; }

        BigDecimal netAmount = calculateNetAmount();

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);

            PreparedStatement pstVendor = conn.prepareStatement("SELECT ProductVendorId FROM ProductVendor WHERE [Name] = ?");
            pstVendor.setString(1, vendorName);
            ResultSet rsVendor = pstVendor.executeQuery();
            if (!rsVendor.next()) { JOptionPane.showMessageDialog(this, "Vendor DB mein nahi mila!"); conn.rollback(); return; }
            int vendorId = rsVendor.getInt(1);

            int targetInvoiceId;
            int invoiceNumberForSave;
            if (editingInvoiceId > 0) {
                targetInvoiceId = editingInvoiceId;
                invoiceNumberForSave = editingInvoiceNumber > 0 ? editingInvoiceNumber : 0;
                removeExistingInvoiceStock(conn, editingInvoiceId);
                try (PreparedStatement deleteItems = conn.prepareStatement("DELETE FROM PurchaseInvoiceItem WHERE PurchaseInvoiceId = ?")) {
                    deleteItems.setInt(1, editingInvoiceId);
                    deleteItems.executeUpdate();
                }
                try (PreparedStatement pstInv = conn.prepareStatement("UPDATE PurchaseInvoice SET InvoiceNumber = ?, ProductVendorId = ?, PurchaseInvoiceDate = GETDATE(), NetAmount = ?, DataEntryStatus = 1 WHERE PurchaseInvoiceId = ?")) {
                    pstInv.setInt(1, invoiceNumberForSave);
                    pstInv.setInt(2, vendorId);
                    pstInv.setBigDecimal(3, netAmount);
                    pstInv.setInt(4, editingInvoiceId);
                    pstInv.executeUpdate();
                }
            } else {
                Statement stMax = conn.createStatement();
                ResultSet rsMax = stMax.executeQuery("SELECT ISNULL(MAX(InvoiceNumber), 0) + 1 FROM PurchaseInvoice");
                rsMax.next();
                int newInvNo = rsMax.getInt(1);
                invoiceNumberForSave = newInvNo;

                String invSql = "INSERT INTO PurchaseInvoice (InvoiceNumber, ProductVendorId, PurchaseInvoiceDate, PaymentModeId, CurrencyId, NetAmount, DataEntryBranchId, UserId, DataEntryDate, DataEntryStatus, IsCalculateInvoiceDiscount, InvoiceDiscount, NetItemDiscount, InvoiceGSTAmount, SoftwareModuleFormId, StockDepartmentId) VALUES (?, ?, GETDATE(), 1, 1, ?, 1, 1, GETDATE(), 1, 0, 0, 0, 0, 419, 0)";
                PreparedStatement pstInv = conn.prepareStatement(invSql, Statement.RETURN_GENERATED_KEYS);
                pstInv.setInt(1, newInvNo); pstInv.setInt(2, vendorId); pstInv.setBigDecimal(3, netAmount);
                pstInv.executeUpdate();

                ResultSet rsInvId = pstInv.getGeneratedKeys();
                rsInvId.next();
                targetInvoiceId = rsInvId.getInt(1);
            }

            PreparedStatement pstItem = conn.prepareStatement("INSERT INTO PurchaseInvoiceItem (PurchaseInvoiceId, ProductItemId, Quantity, Price, DiscountRate, LastPrice, ItemStatus, RetailPrice, TradePrice, SaleRate) VALUES (?, ?, ?, ?, 0, ?, 1, ?, ?, ?)");
            for (int i = 0; i < model.getRowCount(); i++) {
                int itemId = (int) model.getValueAt(i, 0);
                BigDecimal tp = parseDecimal(model.getValueAt(i, 3));
                BigDecimal qty = parseDecimal(model.getValueAt(i, 4));
                BigDecimal saleRate = parseDecimal(model.getValueAt(i, 5));

                pstItem.setInt(1, targetInvoiceId); pstItem.setInt(2, itemId); pstItem.setBigDecimal(3, qty);
                pstItem.setBigDecimal(4, tp); pstItem.setBigDecimal(5, tp); pstItem.setBigDecimal(6, saleRate);
                pstItem.setBigDecimal(7, tp); pstItem.setBigDecimal(8, saleRate);
                pstItem.addBatch();
            }
            pstItem.executeBatch();
            updateInventoryForItems(conn, model, true);
            conn.commit();

            String message = editingInvoiceId > 0
                    ? "Invoice #" + invoiceNumberForSave + " successfully update ho gaya hai!"
                    : "Mubarak Ho! Invoice #" + invoiceNumberForSave + " successfully save ho gaya hai!";
            JOptionPane.showMessageDialog(this, message);
            model.setRowCount(0); updateTotals();
            comboVendorName.setSelectedIndex(-1); comboVendorName.getEditor().setItem("");
            txtBarcode.requestFocusInWindow();
            setTitle("Purchase Invoice System");
            editingInvoiceId = -1;
            editingInvoiceNumber = -1;

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save Error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}