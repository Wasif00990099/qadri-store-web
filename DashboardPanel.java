import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardPanel extends JPanel {

    private JFrame mainFrame;
    private String typedLogoBuffer = "";

    public DashboardPanel(JFrame frame) {
        this.mainFrame = frame;

        setLayout(new BorderLayout());
        setOpaque(false);
        setFocusable(true);

        // Background with Modern Gradient
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 23, 42), 
                                                   0, getHeight(), new Color(30, 41, 82));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.setOpaque(false);

        // Header
        JPanel headerPanel = createHeader();
        backgroundPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 60, 80, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(25, 30, 25, 30);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Buttons Row 1
        JButton btnStockManagement = createModernButton("Stock Management", new Color(59, 130, 246));
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(btnStockManagement, gbc);

        JButton btnUpdateUnpack = createModernButton("Update Unpack", new Color(16, 185, 129));
        gbc.gridx = 1;
        gbc.gridy = 0;
        centerPanel.add(btnUpdateUnpack, gbc);

        JButton btnConfiguration = createModernButton("Configuration", new Color(245, 158, 11));
        gbc.gridx = 2;
        gbc.gridy = 0;
        centerPanel.add(btnConfiguration, gbc);

        JButton btnAliBhai = createModernButton("Invoice Import Export", new Color(236, 72, 153));
        gbc.gridx = 3;
        gbc.gridy = 0;
        centerPanel.add(btnAliBhai, gbc);

        // Buttons Row 2 (Naya Buttons Yahan Add Hue Hain)
        JButton btnPurchaseInvoice = createModernButton("Purchase Invoice", new Color(147, 51, 234)); // Purple color
        gbc.gridx = 1; // Isko beech mein rakhne ke liye
        gbc.gridy = 1; // Neeche wali row
        centerPanel.add(btnPurchaseInvoice, gbc);

        JButton btnChakki = createModernButton("Chakki", new Color(234, 88, 12)); // Orange color
        gbc.gridx = 2; // Saath mein rakhein
        gbc.gridy = 1;
        centerPanel.add(btnChakki, gbc);

        backgroundPanel.add(centerPanel, BorderLayout.CENTER);

        add(backgroundPanel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char ch = Character.toLowerCase(e.getKeyChar());
                if (Character.isLetter(ch)) {
                    typedLogoBuffer += ch;
                    if (typedLogoBuffer.length() > 10) {
                        typedLogoBuffer = typedLogoBuffer.substring(typedLogoBuffer.length() - 10);
                    }
                    if ("logo".equals(typedLogoBuffer)) {
                        typedLogoBuffer = "";
                        if (mainFrame instanceof QadriStore) {
                            ((QadriStore) mainFrame).selectAndSaveLogo();
                        }
                    }
                } else {
                    typedLogoBuffer = "";
                }
            }
        });

        // Action Listeners
        btnStockManagement.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).showMainApplicationUI();
            }
        });

        btnUpdateUnpack.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).openUnpackUpdaterFromDashboard();
            }
        });

        btnConfiguration.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).showConfigurationFromDashboard();
            }
        });

        btnAliBhai.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).openPurchaseInvoiceUIFromDashboard(); // Ye purana wala function hai
            }
        });

        // Naye Button Ka Action Listener
        btnPurchaseInvoice.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).openPurchaseInvoiceWindowFromDashboard();
            }
        });

        btnChakki.addActionListener(e -> {
            if (mainFrame instanceof QadriStore) {
                ((QadriStore) mainFrame).openChakkiStockBookFromDashboard();
            }
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(45, 60, 30, 60));

        JLabel title = new JLabel("QADRI STORE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 48));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Inventory & Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        subtitle.setForeground(new Color(148, 163, 184));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        return header;
    }

    private JButton createModernButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded background
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.setForeground(Color.WHITE);
        btn.setBackground(baseColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(280, 140));

        // Hover Effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(baseColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
            }
        });

        return btn;
    }
}