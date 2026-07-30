import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

public class QadriWebServer {

    // --- CONFIGURATION ---
    String serverName = "DESKTOP-18V9L1H"; 
    String dbName = "QadriStore-BE";
    String user = "wasif";
    String pass = "123";
    String url = "jdbc:sqlserver://" + serverName + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";
    
    // Table configurations
    String inventoryTable = ""; 
    String salesTable = ""; 
    
    // Sales table column names
    String salesNameCol = "LongName";
    String salesValueCol = "Quantity";
    
    // Reference table configuration
    String referenceTable = "fitems";
    String refBarcodeCol = "Barcode";
    String refNameCol = "LongName";
    
    // Name-Barcode Maps for exact matching
    Map<String, String> nameToBarcodeMap = new HashMap<>();
    Map<String, String> barcodeToNameMap = new HashMap<>();
    
    // Lock for thread-safe file access
    private static final Object FILE_LOCK = new Object();
    
    // Cache file name for offline backup
    private static final String CACHE_FILE = "inventory_cache.json";

    public static void main(String[] args) throws Exception {
        new QadriWebServer().start();
    }

    public void start() throws IOException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("Connecting to " + serverName + "...");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                detectTableNames(conn);
                loadNameBarcodeMaps(conn);
            }
        } catch (Exception e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Dashboard ke routes
        server.createContext("/", new HomeHandler());
        server.createContext("/sales", new SalesReportHandler()); 
        server.createContext("/data", new DataHandler()); 
        server.createContext("/api/sales-report", new SalesReportDataHandler());
        
        // Doosre modules ke routes yahan register ho rahe hain
        QadriPOS.registerContexts(server);
        QadriStock.registerContexts(server);
        QadriPriceUpdate.registerContexts(server);
        
        server.setExecutor(Executors.newCachedThreadPool()); 
        server.start();
        
        System.out.println("--------------------------------------------------");
        System.out.println("Main Server Started on Port: " + port);
        System.out.println("Dashboard:     http://localhost:" + port);
        System.out.println("POS:           http://localhost:" + port + "/pos");
        System.out.println("Stock:         http://localhost:" + port + "/stock");
        System.out.println("Price Update:  http://localhost:" + port + "/price");
        System.out.println("--------------------------------------------------");
    }

    private void detectTableNames(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'ProductName' " +
            "INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Inventory'")) {
            if (rs.next()) inventoryTable = rs.getString(1);
        } catch(Exception e) {}

        try (ResultSet rs = conn.createStatement().executeQuery(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'LongName' " +
            "INTERSECT SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE COLUMN_NAME = 'Quantity'")) {
            if (rs.next()) salesTable = rs.getString(1);
        } catch(Exception e) {}
        System.out.println("Detected Tables -> Inv: " + inventoryTable + ", Sales: " + salesTable);
    }
    
    private void loadNameBarcodeMaps(Connection conn) {
        try {
            Statement stmt1 = conn.createStatement();
            ResultSet rs1 = stmt1.executeQuery("SELECT [" + refNameCol + "], [" + refBarcodeCol + "] FROM [" + referenceTable + "]");
            while (rs1.next()) {
                String name = rs1.getString(1); 
                String barcode = rs1.getString(2);
                if (name != null && barcode != null) {
                    nameToBarcodeMap.put(name.toLowerCase().trim(), barcode);
                    barcodeToNameMap.put(barcode, name);
                }
            }
            rs1.close();
            System.out.println("Loaded " + nameToBarcodeMap.size() + " items into name-barcode map");
        } catch (Exception e) {
            System.err.println("Error loading maps: " + e.getMessage());
        }
    }
    
    String escape(String s) { 
        if(s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ").replace("\t", " ");
    }

    static class InventoryItem {
        String bc; String nm; double rate; double tp; double inv;
        public InventoryItem(String bc, String nm, double rate, double tp, double inv) {
            this.bc = bc; this.nm = nm; this.rate = rate; this.tp = tp; this.inv = inv;
        }
    }

    // ================================================================
    // DASHBOARD
    // ================================================================
    class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = 
            "<!DOCTYPE html><html><head>" +
            "   <title>Qadri Store</title>" +
            "   <meta charset='UTF-8'>" +
            "   <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "   <style>" +
            "       * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; }" +
            "       body { background: #f4f6f9; height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; }" +
            "       .header { text-align: center; margin-bottom: 40px; }" +
            "       .header h1 { color: #2c3e50; font-size: 28px; margin-bottom: 5px; }" +
            "       .header p { color: #7f8c8d; font-size: 14px; }" +
            "       .menu { width: 100%; max-width: 400px; }" +
            "       a.card, button.card { display: flex; align-items: center; justify-content: flex-start; padding: 20px; background: white; margin-bottom: 20px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); text-decoration: none; transition: transform 0.2s; border-left: 8px solid; width: 100%; cursor: pointer; font-family: inherit; }" +
            "       a.card:active, button.card:active { transform: scale(0.98); }" +
            "       .card-pos { border-color: #3498db; } .card-stock { border-color: #27ae60; } .card-sales { border-color: #9b59b6; } .card-price { border-color: #e74c3c; }" +
            "       .icon-box { width: 60px; height: 60px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-right: 20px; }" +
            "       .icon-pos { background: #ebf5fb; color: #3498db; } .icon-stock { background: #e8f8f0; color: #27ae60; } .icon-sales { background: #f5eef8; color: #9b59b6; } .icon-price { background: #fdedec; color: #e74c3c; }" +
            "       .icon-box span { font-size: 30px; }" +
            "       .text-box h2 { color: #2c3e50; font-size: 20px; margin-bottom: 5px; }" +
            "       .text-box p { color: #95a5a6; font-size: 13px; }" +
            "   </style>" +
            "</head><body>" +
            "   <div class='header'><h1>Qadri Store</h1><p>Management System</p></div>" +
            "   <div class='menu'>" +
            "       <button class='card card-pos' onclick='window.location.href=\"/pos\"'><div class='icon-box icon-pos'><span>🛒</span></div><div class='text-box'><h2>Point of Sale</h2><p>Billing & Invoices</p></div></button>" +
            "       <a href='/stock' class='card card-stock'><div class='icon-box icon-stock'><span>📦</span></div><div class='text-box'><h2>Stock Management</h2><p>Check Inventory & Demand</p></div></a>" +
            "       <a href='/sales' class='card card-sales'><div class='icon-box icon-sales'><span>📊</span></div><div class='text-box'><h2>Sales Report</h2><p>View Saved Sales</p></div></a>" +
            "       <button class='card card-price' onclick='window.location.href=\"/price\"'><div class='icon-box icon-price'><span>💰</span></div><div class='text-box'><h2>Price Update</h2><p>Update Product Prices</p></div></button>" +
            "   </div>" +
            "</body></html>";
            sendResponse(exchange, html);
        }
    }

    // ================================================================
    // SALES REPORT PAGE
    // ================================================================
    class SalesReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = 
            "<!DOCTYPE html><html><head>" +
            "   <title>Sales Report</title><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "   <style>" +
            "       * { box-sizing: border-box; } body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; padding-bottom: 20px; }" +
            "       .navbar { background: #9b59b6; padding: 15px; display: flex; align-items: center; color: white; box-shadow: 0 2px 5px rgba(0,0,0,0.2); }" +
            "       .back-btn { background: rgba(255,255,255,0.2); border-radius: 50%; width: 35px; height: 35px; display: flex; align-items: center; justify-content: center; text-decoration: none; color: white; font-weight: bold; margin-right: 15px; }" +
            "       .navbar h2 { font-size: 18px; margin: 0; }" +
            "       .filter-container { padding: 10px; background: white; display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; border-bottom: 1px solid #ddd; }" +
            "       .filter-btn { flex: 1 0 30%; padding: 10px; border: 1px solid #9b59b6; background: white; color: #9b59b6; border-radius: 5px; font-weight: bold; cursor: pointer; font-size: 14px; } .filter-btn.active { background: #9b59b6; color: white; }" +
            "       .custom-input-box { display: none; width: 100%; padding: 10px; background: #f8f9fa; border-bottom: 1px solid #ddd; align-items: center; justify-content: center; gap: 10px; }" +
            "       .custom-input-box input { width: 80px; padding: 8px; border: 1px solid #ccc; border-radius: 5px; text-align: center; }" +
            "       .custom-input-box button { padding: 8px 15px; background: #27ae60; color: white; border: none; border-radius: 5px; cursor: pointer; }" +
            "       .summary-card { background: white; margin: 15px; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); text-align: center; }" +
            "       .summary-title { color: #7f8c8d; font-size: 14px; margin-bottom: 5px; } .summary-total { color: #9b59b6; font-size: 32px; font-weight: bold; }" +
            "       .table-container { margin: 0 15px; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
            "       .search-area { padding: 15px; background: white; border-bottom: 1px solid #eee; }" +
            "       input.search-field { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 16px; }" +
            "       table { width: 100%; border-collapse: collapse; } th { background: #8e44ad; color: white; padding: 12px; text-align: left; font-size: 14px; } td { padding: 12px; border-bottom: 1px solid #eee; font-size: 14px; }" +
            "       .col-qty { text-align: center; font-weight: bold; color: #e74c3c; } .col-total { text-align: right; font-weight: bold; color: #27ae60; }" +
            "       .no-data { padding: 20px; text-align: center; color: #95a5a6; } .status-bar { padding: 10px; text-align: center; font-size: 12px; color: #7f8c8d; background: white; border-top: 1px solid #eee; margin-top: 10px; }" +
            "   </style>" +
            "</head><body>" +
            "   <div class='navbar'><a href='/' class='back-btn'>←</a><h2>Sales Report</h2></div>" +
            "   <div class='filter-container'>" +
            "       <button class='filter-btn active' id='btn-today' onclick='loadData(\"today\")'>Today</button>" +
            "       <button class='filter-btn' id='btn-yesterday' onclick='loadData(\"yesterday\")'>Yesterday</button>" +
            "       <button class='filter-btn' id='btn-7' onclick='loadData(\"7\")'>7 Days</button>" +
            "       <button class='filter-btn' id='btn-30' onclick='loadData(\"30\")'>30 Days</button>" +
            "       <button class='filter-btn' id='btn-custom' onclick='showCustomInput()'>Last Days</button>" +
            "   </div>" +
            "   <div class='custom-input-box' id='customBox'><span>Enter Days:</span><input type='number' id='customDays' min='1' value='10'><button onclick='loadCustomData()'>Get Record</button></div>" +
            "   <div class='summary-card'><div class='summary-title'>Total Sales Value</div><div class='summary-total' id='grandTotal'>Rs 0</div></div>" +
            "   <div class='search-area'><input type='text' class='search-field' id='searchInput' placeholder='Search product name...' oninput='filterTable()'></div>" +
            "   <div class='table-container'><table id='salesTable'><thead><tr><th>Product Name</th><th style='text-align:center'>Qty Sold</th><th style='text-align:right'>Total Value</th></tr></thead><tbody id='tableBody'></tbody></table><div id='noData' class='no-data' style='display:none;'>No sales recorded.</div></div>" +
            "   <div class='status-bar' id='status'>Loading data...</div>" +
            "   <script>" +
            "       let salesData = [];" +
            "       function setActiveButton(id) { document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active')); document.getElementById(id).classList.add('active'); document.getElementById('customBox').style.display = 'none'; }" +
            "       function showCustomInput() { document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active')); document.getElementById('btn-custom').classList.add('active'); document.getElementById('customBox').style.display = 'flex'; }" +
            "       function loadCustomData() { let d = document.getElementById('customDays').value; if(!d || d < 1) return; loadData('custom', d); }" +
            "       function loadData(mode, val) { setActiveButton('btn-' + (mode === 'custom' ? 'custom' : mode)); if(mode === 'custom') document.getElementById('customBox').style.display = 'flex'; document.getElementById('status').innerText = 'Loading...'; let u = '/api/sales-report?mode=' + mode; if(mode === 'custom') u += '&days=' + val; fetch(u).then(r => r.json()).then(d => { salesData = d; renderTable(salesData); document.getElementById('status').innerText = 'Updated: ' + new Date().toLocaleTimeString(); }).catch(e => document.getElementById('status').innerText = 'Error'); }" +
            "       function renderTable(data) { const t = document.getElementById('tableBody'); const n = document.getElementById('noData'); const g = document.getElementById('grandTotal'); let tv = 0; if(!data.length) { t.innerHTML = ''; n.style.display = 'block'; g.innerText = 'Rs 0'; return; } n.style.display = 'none'; let h = ''; data.sort((a, b) => b.qty - a.qty); for(let i=0;i<data.length;i++) { let it=data[i]; let v=parseFloat(it.total); tv+=v; h+=`<tr><td>${it.nm}</td><td class=\"col-qty\">${it.qty}</td><td class=\"col-total\">Rs ${v.toLocaleString()}</td></tr>`; } t.innerHTML=h; g.innerText='Rs '+tv.toLocaleString(); }" +
            "       function filterTable() { let q = document.getElementById('searchInput').value.toLowerCase(); renderTable(salesData.filter(i => i.nm.toLowerCase().includes(q))); }" +
            "       loadData('today');" +
            "   </script></body></html>";
            sendResponse(exchange, html);
        }
    }
    
    // ================================================================
    // SALES REPORT DATA API
    // ================================================================
    class SalesReportDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String mode = params.getOrDefault("mode", "today");
            int days = 0;
            if (mode.equals("custom")) { try { days = Integer.parseInt(params.getOrDefault("days", "1")); } catch (Exception e) { days = 1; } }

            java.time.Instant now = java.time.Instant.now();
            java.time.Instant start = now;
            if ("today".equals(mode)) start = now.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
            else if ("yesterday".equals(mode)) start = now.truncatedTo(java.time.temporal.ChronoUnit.DAYS).minus(1, java.time.temporal.ChronoUnit.DAYS);
            else if ("7".equals(mode)) start = now.minus(7, java.time.temporal.ChronoUnit.DAYS);
            else if ("30".equals(mode)) start = now.minus(30, java.time.temporal.ChronoUnit.DAYS);
            else if ("custom".equals(mode)) start = now.minus(days, java.time.temporal.ChronoUnit.DAYS);

            java.time.Instant finalStart = start;
            java.time.Instant finalEnd = ("yesterday".equals(mode)) ? now.truncatedTo(java.time.temporal.ChronoUnit.DAYS) : now;

            Map<String, ReportItem> reportMap = new HashMap<>();
            File file = new File("invoices_history.txt");

            if (file.exists()) {
                synchronized (FILE_LOCK) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            int dateIdx = line.indexOf("\"date\":\"");
                            if (dateIdx == -1) continue; 
                            String dateStr = line.substring(dateIdx + 8, line.indexOf("\"", dateIdx + 8));
                            try {
                                java.time.Instant invDate = java.time.Instant.parse(dateStr);
                                if (!invDate.isBefore(finalStart) && invDate.isBefore(finalEnd)) {
                                    int itemsIdx = line.indexOf("\"items\":");
                                    if (itemsIdx == -1) continue;
                                    int arrStart = line.indexOf("[", itemsIdx);
                                    int arrEnd = line.lastIndexOf("]");
                                    if (arrStart == -1 || arrEnd == -1) continue;
                                    String arrayContent = line.substring(arrStart + 1, arrEnd);
                                    if (arrayContent.trim().isEmpty()) continue;
                                    String[] objects = arrayContent.split("\\},\\{");
                                    for (String objStr : objects) {
                                        objStr = objStr.replace("{", "").replace("}", "").trim();
                                        if (objStr.isEmpty()) continue;
                                        Map<String, String> itemData = new HashMap<>();
                                        String[] pairs = objStr.split(",");
                                        for (String pair : pairs) { String[] kv = pair.split(":"); if (kv.length >= 2) itemData.put(kv[0].trim().replace("\"", ""), kv[1].trim().replace("\"", "")); }
                                        String nm = itemData.get("nm"); String qtyStr = itemData.get("qty"); String rateStr = itemData.get("rate");
                                        if (nm != null && qtyStr != null) {
                                            double qty = 0; double rate = 0;
                                            try { qty = Double.parseDouble(qtyStr); if (rateStr != null) rate = Double.parseDouble(rateStr); } catch (NumberFormatException nfe) { continue; }
                                            ReportItem ri = reportMap.getOrDefault(nm.toLowerCase().trim(), new ReportItem(nm, rate));
                                            ri.qty += qty; reportMap.put(nm.toLowerCase().trim(), ri);
                                        }
                                    }
                                }
                            } catch (Exception pe) { }
                        }
                    } catch (Exception e) { }
                }
            }

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, 0); 
            OutputStream os = exchange.getResponseBody();
            PrintStream ps = new PrintStream(os, true, "UTF-8");
            ps.print("[");
            boolean first = true;
            for (ReportItem ri : reportMap.values()) {
                if(!first) ps.print(","); first = false;
                ps.print(String.format("{\"nm\":\"%s\",\"qty\":%.2f,\"rate\":%.2f,\"total\":%.2f}", escape(ri.nm), ri.qty, ri.rate, ri.qty * ri.rate));
            }
            ps.print("]"); ps.close();
        }
        class ReportItem { String nm; double rate; double qty; public ReportItem(String nm, double rate) { this.nm = nm; this.rate = rate; this.qty = 0; } }
    }

    // ================================================================
    // DATA API (Shared by POS, Stock & Price)
    // ================================================================
    class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); 
            exchange.sendResponseHeaders(200, 0); 
            OutputStream os = exchange.getResponseBody();
            PrintStream ps = new PrintStream(os, true, "UTF-8");
            
            List<InventoryItem> items = new ArrayList<>();
            Map<String, Double> salesData = new HashMap<>();
            boolean dbSuccess = false;
            
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                Map<String, InventoryItem> itemMap = new HashMap<>();
                boolean hasPrice = true;
                try { DatabaseMetaData md = conn.getMetaData(); ResultSet rsCols = md.getColumns(null, null, "fitems", "SaleRate"); if (!rsCols.next()) hasPrice = false; rsCols.close(); } catch(Exception e) { hasPrice = false; }
                
                String itemSql = hasPrice ? "SELECT [Barcode], [LongName], [SaleRate], [CostPrice] FROM [fitems]" : "SELECT [Barcode], [LongName] FROM [fitems]";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(itemSql)) {
                    while (rs.next()) {
                        String bc = rs.getString(1); String nm = rs.getString(2);
                        if (bc != null && nm != null) itemMap.put(bc, new InventoryItem(bc, nm, hasPrice ? rs.getDouble(3) : 0.0, hasPrice ? rs.getDouble(4) : 0.0, 0.0));
                    }
                }

                if (!inventoryTable.isEmpty()) {
                    try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT [ProductName], [Inventory] FROM [" + inventoryTable + "]")) {
                        while(rs.next()) { String key = rs.getString(1); if(key != null) { String nameKey = key.toLowerCase().trim(); for(InventoryItem item : itemMap.values()) { if(item.nm.toLowerCase().trim().equals(nameKey)) { item.inv = rs.getDouble(2); break; } } } }
                    }
                }
                
                if (!salesTable.isEmpty() && !itemMap.isEmpty()) {
                    try {
                        StringBuilder inNames = new StringBuilder();
                        for (String bc : itemMap.keySet()) { String name = barcodeToNameMap.get(bc); if (name != null) inNames.append("'").append(name.replace("'", "''")).append("',"); }
                        if (inNames.length() > 0) {
                            inNames.deleteCharAt(inNames.length() - 1);
                            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT [" + salesNameCol + "], [" + salesValueCol + "] FROM [" + salesTable + "] WHERE [" + salesNameCol + "] IN (" + inNames + ")")) {
                                while (rs.next()) { String name = rs.getString(1); String bc = nameToBarcodeMap.get(name.toLowerCase().trim()); if (bc != null) salesData.put(bc, rs.getDouble(2)); }
                            }
                        }
                    } catch (Exception ex) { }
                }
                items = new ArrayList<>(itemMap.values()); dbSuccess = true; saveToCache(items);
            } catch (Exception e) { dbSuccess = false; }
            if (!dbSuccess) items = loadFromCache();

            ps.print("[");
            boolean first = true;
            for (InventoryItem item : items) {
                double sale = salesData.getOrDefault(item.bc, 0.0);
                if(!first) ps.print(","); first = false;
                ps.print(String.format("{\"bc\":\"%s\", \"nm\":\"%s\", \"rate\":\"%.0f\", \"tp\":\"%.0f\", \"inv\":\"%.0f\", \"sale\":\"%.0f\", \"dmd\":\"%.0f\", \"days\":\"%.0f\"}", escape(item.bc), escape(item.nm), item.rate, item.tp, item.inv, sale, item.inv - sale, (sale > 0) ? (item.inv * 30.0) / sale : 999));
            }
            ps.print("]"); ps.close(); os.close();
        }
        
        private void saveToCache(List<InventoryItem> items) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(CACHE_FILE))) {
                pw.print("["); boolean first = true;
                for (InventoryItem item : items) { if (!first) pw.print(","); first = false; pw.print(String.format("{\"bc\":\"%s\",\"nm\":\"%s\",\"rate\":%.2f,\"tp\":%.2f,\"inv\":%.2f}", escape(item.bc), escape(item.nm), item.rate, item.tp, item.inv)); }
                pw.print("]");
            } catch (Exception e) { }
        }
        
        private List<InventoryItem> loadFromCache() {
            List<InventoryItem> list = new ArrayList<>(); File f = new File(CACHE_FILE); if (!f.exists()) return list;
            try {
                String content = new String(Files.readAllBytes(Paths.get(CACHE_FILE))).trim();
                if (content.startsWith("[")) content = content.substring(1); if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
                if (content.isEmpty()) return list;
                for (String obj : content.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "").trim(); String bc="", nm=""; double rate=0, tp=0, inv=0;
                    for(String pair : obj.split(",")) { String[] kv = pair.split("\":", 2); if(kv.length==2) { String k = kv[0].replace("\"", "").trim(); String v = kv[1].trim(); if("bc".equals(k)) bc=v.replace("\"",""); else if("nm".equals(k)) nm=v.replace("\"",""); else if("rate".equals(k)) rate=Double.parseDouble(v); else if("tp".equals(k)) tp=Double.parseDouble(v); } }
                    if(!bc.isEmpty() && !nm.isEmpty()) list.add(new InventoryItem(bc, nm, rate, tp, inv));
                }
            } catch (Exception e) { }
            return list;
        }
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        byte[] bytes = response.getBytes("UTF-8"); exchange.sendResponseHeaders(200, bytes.length); OutputStream os = exchange.getResponseBody(); os.write(bytes); os.close();
    }
    
    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>(); if (query == null) return result;
        for (String param : query.split("&")) { String pair[] = param.split("="); if (pair.length > 1) { try { result.put(pair[0], java.net.URLDecoder.decode(pair[1], "UTF-8")); } catch (Exception e) { result.put(pair[0], pair[1]); } } else result.put(pair[0], ""); }
        return result;
    }
}