import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class QadriStock {
    private static final String VENDOR_FILE_PATH = "QadriStock_Vendors.cfg";
    private static final String ORDER_FILE_PATH = "QadriStock_Order.cfg";
    private static final Object fileLock = new Object();
   
    public static void registerContexts(HttpServer server) {
        server.createContext("/stock", new StockHandler());
        server.createContext("/api/vendors", new GetVendorsHandler());
        server.createContext("/api/add-vendor", new AddVendorHandler());
        server.createContext("/api/update-vendor-name", new UpdateVendorNameHandler());
        server.createContext("/api/delete-vendor", new DeleteVendorHandler());
        server.createContext("/api/save-order", new SaveOrderHandler());
        server.createContext("/api/get-order", new GetOrderHandler());
        System.out.println("Stock Module Loaded (Contexts: /stock, /api/vendors, order sync...)");
    }
   
    // ==================== FILE OPERATIONS ====================
    private static List<String[]> readVendorsFromFile() {
        List<String[]> vendors = new ArrayList<>();
        synchronized (fileLock) {
            File file = new File(VENDOR_FILE_PATH);
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("\\|", -1);
                        if (parts.length >= 2) {
                            vendors.add(new String[]{parts[0], parts[1]});
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        return vendors;
    }
   
    private static void appendVendorToFile(String bc, String name) {
        synchronized (fileLock) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(VENDOR_FILE_PATH, true))) {
                pw.println(bc + "|" + name);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
   
    private static void updateVendorNameInFile(String bc, String newName) {
        synchronized (fileLock) {
            List<String[]> vendors = readVendorsFromFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(VENDOR_FILE_PATH, false))) {
                for (String[] v : vendors) {
                    if (v[0].equals(bc)) {
                        pw.println(v[0] + "|" + newName);
                    } else {
                        pw.println(v[0] + "|" + v[1]);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
   
    private static void deleteVendorFromFile(String bc) {
        synchronized (fileLock) {
            List<String[]> vendors = readVendorsFromFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(VENDOR_FILE_PATH, false))) {
                for (String[] v : vendors) {
                    if (!v[0].equals(bc)) {
                        pw.println(v[0] + "|" + v[1]);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
   
    // ==================== ORDER FILE OPERATIONS ====================
    private static void saveOrderToFile(List<String> order) {
        synchronized (fileLock) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(ORDER_FILE_PATH, false))) {
                for (String bc : order) {
                    pw.println(bc);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
   
    private static List<String> readOrderFromFile() {
        List<String> order = new ArrayList<>();
        synchronized (fileLock) {
            File file = new File(ORDER_FILE_PATH);
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) order.add(line);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        return order;
    }
   
    private static String buildVendorsJson(List<String[]> vendors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vendors.size(); i++) {
            if (i > 0) sb.append(",");
            String[] v = vendors.get(i);
            sb.append("{\"bc\":\"").append(escapeJson(v[0])).append("\",");
            sb.append("\"nm\":\"").append(escapeJson(v[1])).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
   
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
        return bos.toString("UTF-8");
    }
   
    private static String extractJsonVal(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\\\"", "\"");
    }
   
    // ==================== HANDLERS ====================
    static class GetVendorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String[]> vendors = readVendorsFromFile();
            String json = buildVendorsJson(vendors);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = json.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }
   
    static class AddVendorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String bc = extractJsonVal(body, "bc");
            String nm = extractJsonVal(body, "nm");
            if (bc != null && nm != null) {
                appendVendorToFile(bc, nm);
            }
            sendSuccess(exchange);
        }
    }
   
    static class UpdateVendorNameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String bc = extractJsonVal(body, "bc");
            String nm = extractJsonVal(body, "nm");
            if (bc != null && nm != null) {
                updateVendorNameInFile(bc, nm);
            }
            sendSuccess(exchange);
        }
    }
   
    static class DeleteVendorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String bc = extractJsonVal(body, "bc");
            if (bc != null) {
                deleteVendorFromFile(bc);
            }
            sendSuccess(exchange);
        }
    }
   
    static class SaveOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            try {
                List<String> order = new ArrayList<>();
                String clean = body.replace("[", "").replace("]", "").replace("\"", "");
                for (String s : clean.split(",")) {
                    s = s.trim();
                    if (!s.isEmpty()) order.add(s);
                }
                saveOrderToFile(order);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sendSuccess(exchange);
        }
    }
   
    static class GetOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> order = readOrderFromFile();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < order.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(order.get(i))).append("\"");
            }
            sb.append("]");
            String json = sb.toString();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = json.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }
   
    private static void sendSuccess(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"success\"}";
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
   
    // ==================== MAIN STOCK HANDLER ====================
    static class StockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html =
            "<html><head>" +
            "<title>Stock Status</title><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "*{box-sizing:border-box;margin:0;padding:0}body{font-family:Arial,sans-serif;margin:0;background:#f4f6f9}" +
            ".navbar{background:white;padding:10px 15px;display:flex;align-items:center;justify-content:space-between;box-shadow:0 2px 5px rgba(0,0,0,0.1);position:sticky;top:0;z-index:100;gap:10px}" +
            ".nav-left{display:flex;align-items:center;flex-shrink:0}.back-btn{background:#ecf0f1;border-radius:50%;width:35px;height:35px;display:flex;align-items:center;justify-content:center;text-decoration:none;color:#2c3e50;font-weight:bold;margin-right:15px}" +
            ".navbar h2{font-size:18px;color:#2c3e50;margin:0;white-space:nowrap}" +
            ".nav-center{flex:1;display:flex;justify-content:center;padding:0 15px}" +
            ".nav-center input{width:100%;max-width:450px;padding:10px 15px;border:2px solid #bdc3c7;border-radius:8px;font-size:16px;font-weight:bold;outline:none;transition:border-color 0.3s}" +
            ".nav-center input:focus{border-color:#3498db}" +
            ".nav-right{display:flex;gap:8px;align-items:center;flex-wrap:wrap;justify-content:flex-end;flex-shrink:0}" +
            ".btn-icon{background:#95a5a6;color:white;border:none;padding:8px 12px;border-radius:20px;font-size:12px;cursor:pointer}.awake-on{background:#27ae60!important}.btn-icon.drag-on{background:#27ae60;color:white}" +
            ".btn-vendor{background:#ffc864;color:#000;font-weight:bold;border:none;padding:8px 12px;border-radius:5px;cursor:pointer;font-size:12px}.btn-vendor.active{background:red;color:white}" +
            ".btn-del-vendor{background:#e74c3c;color:white;font-weight:bold;border:none;padding:8px 12px;border-radius:5px;cursor:pointer;font-size:12px}.btn-del-vendor.active{background:#c0392b;color:#fff;box-shadow:0 0 10px rgba(231,76,60,0.8)}" +
            ".alert-popup{position:fixed;top:70px;left:10px;right:10px;background:#c0392b;color:white;padding:15px;border-radius:10px;z-index:2000;display:none;animation:slideDown 0.5s}.alert-popup h3{margin:0 0 10px 0;font-size:16px}.alert-popup p{margin:2px 0;font-size:13px}.close-x{position:absolute;top:5px;right:10px;background:none;border:none;color:white;font-size:20px;cursor:pointer}@keyframes slideDown{from{transform:translateY(-100%);opacity:0}to{transform:translateY(0);opacity:1}}" +
            ".main-container{display:flex;position:relative;height:calc(100vh - 60px)}" +
            ".table-section{flex:1;overflow-x:auto;overflow-y:auto;height:calc(100vh - 60px);position:relative}" +
            ".table-wrapper{position:relative;overflow-x:visible;overflow-y:visible;-webkit-overflow-scrolling:touch}" +
            ".palette-panel{width:300px;background:#f0f0ff;border-left:2px solid #3498db;display:none;flex-direction:column;overflow:hidden;transition:all 0.3s;box-shadow:-2px 0 10px rgba(0,0,0,0.1)}" +
            ".palette-panel.visible{display:flex}" +
            ".palette-header{background:#34495e;color:white;padding:12px;font-weight:bold;text-align:center;font-size:14px}" +
            ".palette-search{padding:10px;background:#e8e8f0;border-bottom:1px solid #ccc}" +
            ".palette-search input{width:100%;padding:8px;border:1px solid #999;border-radius:4px;font-size:14px}" +
            ".palette-list{flex:1;overflow-y:auto;padding:5px}" +
            ".palette-item{padding:10px;margin-bottom:3px;background:white;border:1px solid #ddd;border-radius:4px;cursor:grab;font-size:13px;user-select:none;transition:all 0.2s;display:flex;justify-content:space-between;align-items:center}" +
            ".palette-item:hover{background:#e3f2fd;border-color:#2196F3;transform:translateX(3px)}.palette-item:active{cursor:grabbing;background:#bbdefb}" +
            ".palette-item .item-name{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.palette-item .item-barcode{color:#666;font-size:11px;margin-left:8px}" +
            ".palette-empty{padding:20px;text-align:center;color:#999;font-style:italic}" +
            "table{width:100%;border-collapse:collapse;min-width:900px}" +
            "thead{position:sticky;top:0;z-index:20}" +
            "th{padding:12px;text-align:center;border:1px solid #000;font-size:13px;font-weight:bold;background:#34495e;color:white;position:sticky;top:0;z-index:20}" +
            "td{padding:10px;text-align:center;border:1px solid #000;font-size:13px}" +
            "tr{cursor:pointer;transition:background 0.2s}tr:hover{background:#f8f9fa}tr.row-selected{outline:5px solid #0055FF !important;outline-offset:-5px;background:#e8f4fc !important}" +
            "tr.dragging{opacity:0.5;background:#e3f2fd !important}tr.drag-over{border-top:3px solid #2196F3 !important}td.name-drop-target{background:#e3f2fd !important;outline:3px dashed #2196F3 !important}" +
            ".c-barcode{background:#FFD700;color:#000;font-weight:bold}.c-name{background:#FFFFFF;color:#000;text-align:center;font-weight:500}.c-inv{background:#32CD32;color:#000;font-weight:bold}.c-sale{background:#FFB6C1;color:#000;font-weight:bold}.c-dmd-neg{background:#800080;color:#FFFFFF;font-weight:bold}.c-dmd-pos{background:#FFFFFF;color:#000;font-weight:bold}.c-days-alert{background:#FF0000;color:#FFFFFF;font-weight:bold}.c-days-ok{background:#FFFFFF;color:#000}.c-default{background:#FFFFFF;color:#000}" +
            ".c-vendor-row{background:#006400 !important;color:white !important;cursor:default !important}.c-vendor-row.delete-hover{background:#8B0000 !important}.c-vendor-name{background:#006400 !important;color:white !important;text-align:center !important;font-weight:bold;font-size:16px;padding:12px;outline:none;user-select:none}.c-vendor-name.editable{cursor:text;user-select:text;background:#004d00 !important;border-bottom:2px dashed #90EE90}.c-vendor-name.delete-hover{background:#8B0000 !important}" +
            ".status-bar{padding:10px;text-align:center;font-size:12px;color:#7f8c8d;background:#fff;border-top:1px solid #eee;position:fixed;bottom:0;width:100%;display:flex;justify-content:center;align-items:center;gap:10px;z-index:101;box-shadow:0 -2px 10px rgba(0,0,0,0.1)}.live-dot{width:8px;height:8px;background:#27ae60;border-radius:50%;animation:pulse 2s infinite}@keyframes pulse{0%{opacity:1}50%{opacity:0.3}100%{opacity:1}}" +
            ".num-flash{animation:numFlash 0.6s ease}@keyframes numFlash{0%{background:transparent}30%{background:#fff176}100%{background:transparent}}" +
            "@media(max-width:768px){.palette-panel{width:250px}.navbar h2{font-size:14px}.nav-center input{max-width:200px;padding:8px 10px;font-size:14px}}" +
            "</style>" +
            "</head><body>" +
            "<div id='alertPopup' class='alert-popup'><h3>\u26A0\uFE0F Price Change Alert!</h3><div id='alertContent'></div><button class='close-x' onclick='hideAlert()'>\u00D7</button></div>" +
            "<div class='navbar'>" +
            "<div class='nav-left'><a href='/' class='back-btn'>\u2190</a><h2>Stock</h2></div>" +
            "<div class='nav-center'><input type='text' id='search' placeholder='Search (Press Enter to jump)...' onkeydown='handleSearchKey(event)'></div>" +
            "<div class='nav-right'>" +
            "<button id='btnAddVendor' class='btn-vendor' onclick='toggleVendorMode()'>+ Vendor</button>" +
            "<button id='btnDelVendor' class='btn-del-vendor' onclick='toggleDeleteVendorMode()'>- Vendor</button>" +
            "<button id='btnDragMode' class='btn-icon' onclick='toggleDragMode()'>Drag Mode: OFF</button>" +
            "<button id='wakeBtn' class='btn-icon' onclick='toggleWake()'>\u2600 Awake</button></div></div>" +
            "<div class='main-container'>" +
            "<div class='table-section' id='tableSection'><div class='table-wrapper'>" +
            "<table><thead><tr><th>Barcode</th><th>Name</th><th>Inventory</th><th>30 Days Sale</th><th>Demand</th><th>Days Stock</th><th>Sale Rate</th><th>TP</th></tr></thead>" +
            "<tbody id='body'></tbody></table></div></div>" +
            "<div class='palette-panel' id='palettePanel'>" +
            "<div class='palette-header'>\uD83D\uDCCB Search Results (Drag to Add)</div>" +
            "<div class='palette-search'><input type='text' id='paletteSearch' placeholder='Search items...' oninput='updatePaletteList()'></div>" +
            "<div class='palette-list' id='paletteList'><div class='palette-empty'>Type to search...</div></div></div>" +
            "</div>" +
            "<div class='status-bar'><span id='status'>Connecting...</span><div class='live-dot'></div></div>" +
            "<script>" +
            "let allData=[];" +
            "let filteredData=[];" +
            "let previousPrices={};" +
            "let selectedRowId=null;" +
            "let isVendorInsertMode=false;" +
            "let isDeleteVendorMode=false;" +
            "let dragMode=false;" +
            "let draggedBarcode=null;" +
            "let draggedFromPalette=false;" +
            "let dragSourceIndex=-1;" +
            "const tbody=document.getElementById('body');" +
            "const searchInput=document.getElementById('search');" +
            "const statusDiv=document.getElementById('status');" +
            "const wakeBtn=document.getElementById('wakeBtn');" +
            "const palettePanel=document.getElementById('palettePanel');" +
            "const paletteList=document.getElementById('paletteList');" +
            "const paletteSearch=document.getElementById('paletteSearch');" +
            "let wakeLock=null;" +

            /* ===== SEARCH LOGIC (SAME AS QADRISTORE) ===== */
            "function handleSearchKey(event){if(event.key==='Enter'){event.preventDefault();performSearchJump()}else if(event.key==='Escape'){searchInput.value='';selectedRowId=null;doSearch();statusDiv.innerText='Search cleared'}}" +
            "function performSearchJump(){let query=searchInput.value.trim().toLowerCase();if(!query)return;let startIdx=0;if(selectedRowId){let cur=filteredData.findIndex(i=>i.bc===selectedRowId);if(cur!==-1)startIdx=cur+1}for(let i=startIdx;i<filteredData.length;i++){let it=filteredData[i];if(!it.bc.startsWith('VENDOR_')&&(it.nm.toLowerCase().includes(query)||it.bc.includes(query))){selectAndScrollToTop(i);return}}for(let i=0;i<startIdx;i++){let it=filteredData[i];if(!it.bc.startsWith('VENDOR_')&&(it.nm.toLowerCase().includes(query)||it.bc.includes(query))){selectAndScrollToTop(i);return}}statusDiv.innerText='No match found for: '+query}" +
            "function selectAndScrollToTop(viewIdx){let item=filteredData[viewIdx];selectedRowId=item.bc;doSearch();setTimeout(()=>{let row=tbody.querySelector(`tr[data-row-id='${item.bc}']`);if(row){let sc=document.getElementById('tableSection');let th=document.querySelector('thead');let thH=th?th.offsetHeight:40;let rR=row.getBoundingClientRect();let cR=sc.getBoundingClientRect();sc.scrollTo({top:(rR.top-cR.top)+sc.scrollTop-thH-5,behavior:'smooth'})}},50);statusDiv.innerText='Match found at row '+(viewIdx+1)}" +

            /* ===== ORDER MANAGEMENT ===== */
            "async function saveOrderToServer(){" +
            "const order=allData.map(item=>item.bc);" +
            "localStorage.setItem('stockOrder',JSON.stringify(order));" +
            "try{await fetch('/api/save-order',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(order)})}catch(e){}}" +
            "function saveCurrentOrder(){saveOrderToServer()}" +

            /* ===== MODES SAVE/RESTORE ===== */
            "function saveModes(){localStorage.setItem('vendorInsertMode',isVendorInsertMode);localStorage.setItem('deleteVendorMode',isDeleteVendorMode);localStorage.setItem('dragMode',dragMode)}" +
            "function restoreModes(){isVendorInsertMode=localStorage.getItem('vendorInsertMode')==='true';isDeleteVendorMode=localStorage.getItem('deleteVendorMode')==='true';dragMode=localStorage.getItem('dragMode')==='true';if(isVendorInsertMode){document.getElementById('btnAddVendor').classList.add('active');document.getElementById('btnAddVendor').innerText='Cancel';statusDiv.innerText='Mode: Edit Names / Click row to ADD'}else if(isDeleteVendorMode){document.getElementById('btnDelVendor').classList.add('active');document.getElementById('btnDelVendor').innerText='Cancel';statusDiv.innerText='Mode: Click on Vendor row to DELETE'}if(dragMode){activateDragModeUI()}}" +

            /* ===== UTILITY FUNCTIONS ===== */
            "function playBeep(){try{const ctx=new(window.AudioContext||window.webkitAudioContext)();const osc=ctx.createOscillator();const gain=ctx.createGain();osc.connect(gain);gain.connect(ctx.destination);osc.frequency.value=800;osc.type='square';gain.gain.value=0.5;osc.start();osc.stop(ctx.currentTime+0.3)}catch(e){}}" +
            "function showAlert(changes){const popup=document.getElementById('alertPopup');const content=document.getElementById('alertContent');let html='';changes.forEach(c=>{html+='<p>'+c.nm+': Rs '+c.old+' \u2192 Rs '+c.new+'</p>'});content.innerHTML=html;popup.style.display='block';setTimeout(()=>popup.style.display='none',10000)}" +
            "function hideAlert(){document.getElementById('alertPopup').style.display='none'}" +
            "async function toggleWake(){try{if(!wakeLock){wakeLock=await navigator.wakeLock.request('screen');wakeBtn.classList.add('awake-on');wakeBtn.innerText='\u2600 ON';wakeLock.addEventListener('release',()=>{wakeLock=null;wakeBtn.innerText='\u2600 Awake';wakeBtn.classList.remove('awake-on')})}else{wakeLock.release()}}catch(e){}}" +
            "function verifyPassword(){const p=prompt('Enter Password:');if(p==='123')return true;alert('Incorrect Password!');return false}" +

            /* ===== DRAG MODE TOGGLE ===== */
            "function toggleDragMode(){if(!verifyPassword())return;dragMode=!dragMode;if(dragMode){activateDragModeUI()}else{deactivateDragModeUI()}saveModes()}" +
            "function activateDragModeUI(){dragMode=true;const btn=document.getElementById('btnDragMode');btn.classList.add('drag-on');btn.innerText='Drag Mode: ON';palettePanel.classList.add('visible');updatePaletteList();statusDiv.innerText='Drag Mode ON.'}" +
            "function deactivateDragModeUI(){dragMode=false;const btn=document.getElementById('btnDragMode');btn.classList.remove('drag-on');btn.innerText='Drag Mode: OFF';palettePanel.classList.remove('visible');statusDiv.innerText='Drag Mode OFF.';doSearch()}" +

            /* ===== PALETTE LIST UPDATE ===== */
            "function updatePaletteList(){const q=paletteSearch.value.toLowerCase();let html='';let count=0;for(let i=0;i<allData.length;i++){let it=allData[i];if(it.bc.startsWith('VENDOR_'))continue;if(it.nm.toLowerCase().includes(q)||it.bc.includes(q)){html+=`<div class='palette-item' draggable='true' data-bc='${it.bc}' data-index='${i}' ondragstart='onPaletteDragStart(event)' ondragend='onPaletteDragEnd(event)'><span class='item-name'>${it.nm}</span><span class='item-barcode'>${it.bc}</span></div>`;count++;if(count>=100)break}}paletteList.innerHTML=html||'<div class=\"palette-empty\">No items found</div>'}" +

            /* ===== PALETTE DRAG HANDLERS ===== */
            "function onPaletteDragStart(event){draggedBarcode=event.target.getAttribute('data-bc');draggedFromPalette=true;dragSourceIndex=-1;event.dataTransfer.setData('text/plain',draggedBarcode);event.dataTransfer.effectAllowed='copy';event.target.style.opacity='0.5'}" +
            "function onPaletteDragEnd(event){event.target.style.opacity='1';draggedBarcode=null;draggedFromPalette=false;removeDropIndicators()}" +

            /* ===== TABLE ROW DRAG HANDLERS ===== */
            "function onRowDragStart(event,filteredIdx){if(!dragMode){event.preventDefault();return}const item=filteredData[filteredIdx];if(!item||item.bc.startsWith('VENDOR_')){event.preventDefault();return}draggedBarcode=item.bc;draggedFromPalette=false;dragSourceIndex=allData.findIndex(i=>i.bc===item.bc);event.dataTransfer.setData('text/plain','move');event.dataTransfer.effectAllowed='move';event.currentTarget.classList.add('dragging')}" +
            "function onRowDragEnd(event){event.currentTarget.classList.remove('dragging');draggedBarcode=null;draggedFromPalette=false;dragSourceIndex=-1;removeDropIndicators();document.querySelectorAll('tr').forEach(tr=>tr.classList.remove('drag-over'))}" +
            "function onRowDragOver(event){if(!dragMode||!draggedBarcode)return;event.preventDefault();event.dataTransfer.dropEffect=draggedFromPalette?'copy':'move';const tr=event.currentTarget;if(!tr.classList.contains('dragging'))tr.classList.add('drag-over')}" +
            "function onRowDragLeave(event){event.currentTarget.classList.remove('drag-over')}" +

            /* ===== DROP HANDLER ===== */
            "function onRowDrop(event,filteredIdx){event.preventDefault();const targetTr=event.currentTarget;targetTr.classList.remove('drag-over');removeDropIndicators();if(!dragMode||!draggedBarcode)return;const targetItem=filteredData[filteredIdx];if(!targetItem)return;const targetGlobalIdx=allData.findIndex(i=>i.bc===targetItem.bc);if(targetGlobalIdx===-1)return;if(!draggedFromPalette&&event.dataTransfer.getData('text/plain')==='move'){performInternalMove(targetGlobalIdx)}else{performImport(targetGlobalIdx)}}" +

            /* ===== PERFORM INTERNAL MOVE ===== */
            "function performInternalMove(targetGlobalIdx){const sourceGlobalIdx=dragSourceIndex;if(sourceGlobalIdx===-1||sourceGlobalIdx===targetGlobalIdx)return;const[movedItem]=allData.splice(sourceGlobalIdx,1);let insertAt=targetGlobalIdx;if(sourceGlobalIdx<targetGlobalIdx)insertAt--;allData.splice(insertAt,0,movedItem);filteredData=[...allData];doSearch();saveCurrentOrder();statusDiv.innerText='Moved: '+movedItem.nm;setTimeout(()=>{if(statusDiv.innerText.startsWith('Moved'))statusDiv.innerText='Updated: '+new Date().toLocaleTimeString()},2000)}" +

            /* ===== PERFORM IMPORT FROM PALETTE ===== */
            "function performImport(targetGlobalIdx){const barcode=draggedBarcode;if(!barcode)return;let existingIndex=allData.findIndex(i=>i.bc===barcode);let existingData=null;if(existingIndex!==-1){existingData=allData[existingIndex];allData.splice(existingIndex,1);if(existingIndex<targetGlobalIdx)targetGlobalIdx--}let rowData;if(existingData){rowData=existingData}else{const original=allData.find(i=>i.bc===barcode)||{bc:barcode,nm:'Unknown Item',inv:0,sale:0,dmd:0,days:999,rate:0,tp:0};rowData={...original}}allData.splice(targetGlobalIdx,0,rowData);filteredData=[...allData];doSearch();saveCurrentOrder();statusDiv.innerText=(existingData?'Moved':'Added')+': '+rowData.nm;setTimeout(()=>{statusDiv.innerText='Updated: '+new Date().toLocaleTimeString()},2000)}" +

            /* ===== NAME CELL DROP HANDLERS ===== */
            "function onNameDragOver(event){if(!dragMode)return;event.preventDefault();event.dataTransfer.dropEffect=draggedFromPalette?'copy':'move';event.currentTarget.classList.add('name-drop-target')}" +
            "function onNameDragLeave(event){event.currentTarget.classList.remove('name-drop-target')}" +
            "function onNameDrop(event,filteredIdx){event.preventDefault();event.currentTarget.classList.remove('name-drop-target');if(!dragMode)return;onRowDrop(event,filteredIdx)}" +
            "function removeDropIndicators(){document.querySelectorAll('.name-drop-target').forEach(el=>el.classList.remove('name-drop-target'));document.querySelectorAll('.drag-over').forEach(el=>el.classList.remove('drag-over'))}" +

            /* ===== FULL LOAD ===== */
            "async function fullLoad(){" +
            "statusDiv.innerText='Connecting...';" +
            "let serverItems=[];" +
            "try{" +
            "const controller=new AbortController();" +
            "const timeoutId=setTimeout(()=>controller.abort(),5000);" +
            "let res=await fetch('/data',{signal:controller.signal});" +
            "clearTimeout(timeoutId);" +
            "if(!res.ok)throw new Error('HTTP '+res.status);" +
            "serverItems=await res.json();" +
            "checkPriceChanges(serverItems);" +
            "}catch(e){" +
            "if(e.name==='AbortError')statusDiv.innerText='Error: Data Server Timeout!';" +
            "else statusDiv.innerText='Error: '+e.message;" +
            "serverItems=[];" +
            "}" +
            "let vendorList=[];" +
            "try{" +
            "let vRes=await fetch('/api/vendors');" +
            "vendorList=await vRes.json();" +
            "}catch(e){console.log('Vendor fetch failed')}" +
            "let itemMap=new Map();" +
            "for(let item of serverItems){itemMap.set(item.bc,item)}" +
            "let savedOrder=[];" +
            "try{" +
            "let oRes=await fetch('/api/get-order');" +
            "if(oRes.ok){savedOrder=await oRes.json()}" +
            "}catch(e){}" +
            "if(savedOrder.length===0){const saved=localStorage.getItem('stockOrder');if(saved){savedOrder=JSON.parse(saved)}}" +
            "allData=[];" +
            "let placedBcs=new Set();" +
            "for(let bc of savedOrder){" +
            "if(bc.startsWith('VENDOR_')){" +
            "let vendor=vendorList.find(v=>v.bc===bc);" +
            "if(vendor){" +
            "allData.push({bc:vendor.bc,nm:vendor.nm,inv:0,sale:0,dmd:0,days:999,rate:0,tp:0});" +
            "placedBcs.add(bc);" +
            "}" +
            "}else{" +
            "if(itemMap.has(bc)){" +
            "allData.push(itemMap.get(bc));" +
            "placedBcs.add(bc);" +
            "}" +
            "}" +
            "}" +
            "for(let item of serverItems){if(!placedBcs.has(item.bc)){allData.push(item)}}" +
            "for(let vendor of vendorList){if(!placedBcs.has(vendor.bc)){allData.push({bc:vendor.bc,nm:vendor.nm,inv:0,sale:0,dmd:0,days:999,rate:0,tp:0})}}" +
            "filteredData=[...allData];" +
            "restoreModes();" +
            "doSearch();" +
            "if(palettePanel.classList.contains('visible')){updatePaletteList();}" +
            "if(!statusDiv.innerText.startsWith('Error')&&!statusDiv.innerText.startsWith('Mode'))statusDiv.innerText='Updated: '+new Date().toLocaleTimeString();" +
            "}" +

            /* ===== LIGHTWEIGHT UPDATE ===== */
            "async function updateNumericOnly(){" +
            "try{" +
            "const controller=new AbortController();" +
            "const timeoutId=setTimeout(()=>controller.abort(),5000);" +
            "let res=await fetch('/data',{signal:controller.signal});" +
            "clearTimeout(timeoutId);" +
            "if(!res.ok)throw new Error('HTTP '+res.status);" +
            "let serverItems=await res.json();" +
            "checkPriceChanges(serverItems);" +
            "let itemMap=new Map();" +
            "for(let item of serverItems){itemMap.set(item.bc,item)}" +
            "for(let i=0;i<allData.length;i++){" +
            "let item=allData[i];" +
            "if(item.bc.startsWith('VENDOR_'))continue;" +
            "if(itemMap.has(item.bc)){" +
            "let s=itemMap.get(item.bc);" +
            "item.inv=s.inv;item.sale=s.sale;item.dmd=s.dmd;item.days=s.days;item.rate=s.rate;item.tp=s.tp;" +
            "}" +
            "}" +
            "for(let i=0;i<filteredData.length;i++){" +
            "let item=filteredData[i];" +
            "if(item.bc.startsWith('VENDOR_'))continue;" +
            "if(itemMap.has(item.bc)){" +
            "let s=itemMap.get(item.bc);" +
            "item.inv=s.inv;item.sale=s.sale;item.dmd=s.dmd;item.days=s.days;item.rate=s.rate;item.tp=s.tp;" +
            "}" +
            "}" +
            "patchDOMCells(itemMap);" +
            "if(!statusDiv.innerText.startsWith('Error')&&!statusDiv.innerText.startsWith('Mode'))statusDiv.innerText='Updated: '+new Date().toLocaleTimeString();" +
            "}catch(e){" +
            "if(e.name==='AbortError')statusDiv.innerText='Error: Data Server Timeout!';" +
            "else statusDiv.innerText='Error: '+e.message;" +
            "}" +
            "}" +

            /* ===== PATCH DOM CELLS ===== */
            "function patchDOMCells(itemMap){" +
            "let rows=tbody.querySelectorAll('tr[data-row-id]');" +
            "for(let r=0;r<rows.length;r++){" +
            "let row=rows[r];" +
            "let bc=row.getAttribute('data-row-id');" +
            "if(bc.startsWith('VENDOR_'))continue;" +
            "if(!itemMap.has(bc))continue;" +
            "let s=itemMap.get(bc);" +
            "let cells=row.querySelectorAll('td');" +
            "if(cells.length<8)continue;" +
            "if(cells[2].textContent!==String(s.inv)){cells[2].textContent=s.inv;flashCell(cells[2])}" +
            "if(cells[3].textContent!==String(s.sale)){cells[3].textContent=s.sale;flashCell(cells[3])}" +
            "let dmdCls=parseFloat(s.dmd)<0?'c-dmd-neg':'c-dmd-pos';" +
            "let dmdChg=cells[4].textContent!==String(s.dmd)||cells[4].className!==dmdCls;" +
            "if(dmdChg){cells[4].textContent=s.dmd;cells[4].className=dmdCls;flashCell(cells[4])}" +
            "let dayCls=(parseFloat(s.days)<=15&&parseFloat(s.days)!=999)?'c-days-alert':'c-days-ok';" +
            "let dayChg=cells[5].textContent!==String(s.days)||cells[5].className!==dayCls;" +
            "if(dayChg){cells[5].textContent=s.days;cells[5].className=dayCls;flashCell(cells[5])}" +
            "if(cells[6].textContent!==String(s.rate)){cells[6].textContent=s.rate;flashCell(cells[6])}" +
            "if(cells[7].textContent!==String(s.tp)){cells[7].textContent=s.tp;flashCell(cells[7])}" +
            "}" +
            "}" +

            /* ===== FLASH ANIMATION ===== */
            "function flashCell(cell){cell.classList.remove('num-flash');void cell.offsetWidth;cell.classList.add('num-flash')}" +

            "function checkPriceChanges(data){let changes=[];for(let i=0;i<data.length;i++){let item=data[i];if(item.bc.startsWith('VENDOR_'))continue;let newRate=parseFloat(item.rate);if(previousPrices[item.bc]!==undefined&&previousPrices[item.bc]!==newRate)changes.push({nm:item.nm,old:previousPrices[item.bc],new:newRate});previousPrices[item.bc]=newRate}if(changes.length>0){playBeep();showAlert(changes);if(navigator.vibrate)navigator.vibrate([200,100,200])}}" +

            /* ===== MODE RESET ===== */
            "function resetModes(){isVendorInsertMode=false;isDeleteVendorMode=false;document.getElementById('btnAddVendor').classList.remove('active');document.getElementById('btnAddVendor').innerText='+ Vendor';document.getElementById('btnDelVendor').classList.remove('active');document.getElementById('btnDelVendor').innerText='- Vendor';saveModes();doSearch()}" +

            /* ===== VENDOR MODE TOGGLES (WITH PASSWORD) ===== */
            "function toggleVendorMode(){if(isVendorInsertMode){resetModes();statusDiv.innerText='Mode: Normal';return}if(!verifyPassword())return;isDeleteVendorMode=false;document.getElementById('btnDelVendor').classList.remove('active');document.getElementById('btnDelVendor').innerText='- Vendor';isVendorInsertMode=true;const btn=document.getElementById('btnAddVendor');btn.classList.add('active');btn.innerText='Cancel';statusDiv.innerText='Mode: Edit Names / Click row to ADD';saveModes();doSearch()}" +
            "function toggleDeleteVendorMode(){if(isDeleteVendorMode){resetModes();statusDiv.innerText='Mode: Normal';return}if(!verifyPassword())return;isVendorInsertMode=false;document.getElementById('btnAddVendor').classList.remove('active');document.getElementById('btnAddVendor').innerText='+ Vendor';isDeleteVendorMode=true;const btn=document.getElementById('btnDelVendor');btn.classList.add('active');btn.innerText='Cancel';statusDiv.innerText='Mode: Click on Vendor row to DELETE';saveModes();doSearch()}" +
            "function updateVendorName(vendorId,newName){let item=allData.find(i=>i.bc===vendorId);if(item)item.nm=newName;fetch('/api/update-vendor-name',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({bc:vendorId,nm:newName})})}" +

            /* ===== DELETE VENDOR ===== */
            "function deleteVendor(vendorId){let idx=allData.findIndex(i=>i.bc===vendorId);if(idx!==-1)allData.splice(idx,1);filteredData=[...allData];fetch('/api/delete-vendor',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({bc:vendorId})});resetModes();statusDiv.innerText='Vendor Deleted!';if(navigator.vibrate)navigator.vibrate(100);doSearch();saveCurrentOrder()}" +

            /* ===== SEARCH / DISPLAY FUNCTION (NO FILTERING, EXACT QADRISTORE STYLE) ===== */
            "function doSearch(){filteredData=[...allData];displayRows()}" +

            /* ===== ROW CLICK HANDLER ===== */
            "function handleRowClick(e){" +
            "if(e.target.getAttribute('contenteditable')==='true')return;" +
            "let clickedId=e.currentTarget.getAttribute('data-row-id');" +
            "if(isVendorInsertMode){" +
            "if(clickedId.startsWith('VENDOR_'))return;" +
            "let index=allData.findIndex(item=>item.bc===clickedId);" +
            "if(index!==-1){" +
            "let vendorId='VENDOR_'+Date.now();" +
            "let newVendor={bc:vendorId,nm:'New Vendor',inv:0,sale:0,dmd:0,days:999,rate:0,tp:0};" +
            "allData.splice(index,0,newVendor);" +
            "filteredData=[...allData];" +
            "fetch('/api/add-vendor',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({bc:vendorId,nm:'New Vendor'})});" +
            "resetModes();statusDiv.innerText='Vendor Added!';doSearch();saveCurrentOrder();" +
            "}" +
            "return;" +
            "}" +
            "if(isDeleteVendorMode){if(!clickedId.startsWith('VENDOR_'))return;deleteVendor(clickedId);return}" +
            "if(clickedId.startsWith('VENDOR_'))return;" +
            "tbody.querySelectorAll('tr').forEach(r=>r.classList.remove('row-selected'));" +
            "if(selectedRowId===clickedId)selectedRowId=null;" +
            "else{e.currentTarget.classList.add('row-selected');selectedRowId=clickedId}" +
            "}" +

            "function vendorRowMouseOver(e){if(!isDeleteVendorMode)return;let tr=e.currentTarget;tr.classList.add('delete-hover');let td=tr.querySelector('.c-vendor-name');if(td)td.classList.add('delete-hover')}" +
            "function vendorRowMouseOut(e){let tr=e.currentTarget;tr.classList.remove('delete-hover');let td=tr.querySelector('.c-vendor-name');if(td)td.classList.remove('delete-hover')}" +

            // Show total weight for a vendor (in kg) on double-click
            "function parseWeightGramsFromName(name){try{let s=name.toLowerCase();let m;let reKg=/([0-9]+(?:\\.[0-9]+)?)\\s*(kg|kgs)\\b/;let reG=/([0-9]+(?:\\.[0-9]+)?)\\s*(g|gm|gr|grams?)\\b/;let reUnpack=/unpack\\s*[\\-\\s]*(\\d+(?:\\.\\d+)?)/;if((m=s.match(reKg))){return parseFloat(m[1])*1000;} if((m=s.match(reG))){return parseFloat(m[1]);} if((m=s.match(reUnpack))){return parseFloat(m[1]);} let reAny=/([0-9]+(?:\\.[0-9]+)?)(kg|kgs|g|gm|gr)\\b/; if((m=s.match(reAny))){if(m[2].startsWith('kg')) return parseFloat(m[1])*1000; return parseFloat(m[1]);} return 0;}catch(e){return 0}}" +

            "function showVendorWeight(vendorId){try{let vendor=allData.find(i=>i.bc===vendorId);if(!vendor){alert('Vendor not found');return;}let vname=vendor.nm.toLowerCase().trim();let totalGrams=0;for(let it of allData){if(it.bc&&it.bc.startsWith('VENDOR_'))continue; if(!it.nm)continue; let nm=it.nm.toLowerCase().trim(); if(nm.startsWith(vname)){let perUnit=parseWeightGramsFromName(it.nm); let qty=parseFloat(it.inv)||0; totalGrams+=perUnit*qty;} } let kg=Math.round((totalGrams/1000)*1000)/1000; alert('Total weight for '+vendor.nm+': '+kg+' kg');}catch(e){alert('Error calculating weight: '+e.message);}}" +

            /* ===== DISPLAY ROWS ===== */
            "function displayRows(){" +
            "let h='';let canEdit=isVendorInsertMode;" +
            "for(let i=0;i<filteredData.length;i++){" +
            "let r=filteredData[i];" +
            "if(r.bc.startsWith('VENDOR_')){" +
            "let editAttr=canEdit?\"contenteditable='true'\":'';" +
            "let blurAttr=canEdit?`onblur=\"updateVendorName('${r.bc}',this.innerText)\"`:'';" +
            "let editClass=canEdit?' editable':'';" +
            "h+=`<tr class='c-vendor-row' data-row-id='${r.bc}' onclick='handleRowClick(event)' ondblclick=\"showVendorWeight('${r.bc}')\" onmouseover='vendorRowMouseOver(event)' onmouseout='vendorRowMouseOut(event)'><td></td><td class='c-vendor-name${editClass}' ${editAttr} ${blurAttr}>${r.nm}</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>`;" +
            "continue;" +
            "}" +
            "let dC=parseFloat(r.dmd)<0?'c-dmd-neg':'c-dmd-pos';" +
            "let dyC=(parseFloat(r.days)<=15&&parseFloat(r.days)!=999)?'c-days-alert':'c-days-ok';" +
            "let s=(selectedRowId===r.bc)?' row-selected':'';" +
            "let rowDragAttrs=dragMode?`draggable='true' ondragstart='onRowDragStart(event,${i})' ondragend='onRowDragEnd(event)' ondragover='onRowDragOver(event)' ondragleave='onRowDragLeave(event)' ondrop='onRowDrop(event,${i})'`:'';" +
            "let nameDropAttrs=dragMode?`ondragover='onNameDragOver(event)' ondragleave='onNameDragLeave(event)' ondrop='onNameDrop(event,${i})'`:'';" +
            "h+=`<tr class='${s}' data-row-id='${r.bc}' onclick='handleRowClick(event)' ${rowDragAttrs}>`+" +
            "`<td class='c-barcode'>${r.bc}</td>`+" +
            "`<td class='c-name' ${nameDropAttrs}>${r.nm}</td>`+" +
            "`<td class='c-inv'>${r.inv}</td>`+" +
            "`<td class='c-sale'>${r.sale}</td>`+" +
            "`<td class='${dC}'>${r.dmd}</td>`+" +
            "`<td class='${dyC}'>${r.days}</td>`+" +
            "`<td class='c-default'>${r.rate}</td>`+" +
            "`<td class='c-default'>${r.tp}</td>`+" +
            "`</tr>`;" +
            "}" +
            "tbody.innerHTML=h;" +
            "}" +

            /* ===== INITIALIZATION ===== */
            "fullLoad();" +
            "setInterval(updateNumericOnly,10000);" +
            "</script></body></html>";
           
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            byte[] bytes = html.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        }
    }
}