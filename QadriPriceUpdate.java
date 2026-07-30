import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.sql.*;
import java.util.concurrent.Executors;

public class QadriPriceUpdate {

    // --- CONFIGURATION ---
    String serverName = "DESKTOP-18V9L1H"; 
    String dbName = "QadriStore-BE";
    String user = "wasif";
    String pass = "123";
    String url = "jdbc:sqlserver://" + serverName + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true;";

    // Ye method main server call karega
    public static void registerContexts(HttpServer server) {
        QadriPriceUpdate instance = new QadriPriceUpdate();
        server.createContext("/price", instance.new PriceUpdateHandler());
        server.createContext("/price/api/update-price", instance.new UpdatePriceHandler());
        System.out.println("Price Module Loaded (Contexts: /price, /price/api/update-price...)");
    }
    
    String escape(String s) { 
        if(s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ").replace("\t", " ");
    }

    // ================================================================
    // PRICE UPDATE UI
    // ================================================================
    class PriceUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = 
            "<!DOCTYPE html><html><head>" +
            "   <title>Price Update</title><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "   <style>" +
            "       * { box-sizing: border-box; } body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; padding-bottom: 20px; }" +
            "       .navbar { background: #e74c3c; padding: 15px; display: flex; align-items: center; color: white; box-shadow: 0 2px 5px rgba(0,0,0,0.2); position: sticky; top: 0; z-index: 100; }" +
            "       .back-btn { background: rgba(255,255,255,0.2); border-radius: 50%; width: 35px; height: 35px; display: flex; align-items: center; justify-content: center; text-decoration: none; color: white; font-weight: bold; margin-right: 15px; }" +
            "       .navbar h2 { font-size: 18px; margin: 0; flex-grow: 1; }" +
            "       .barcode-section { padding: 15px; background: white; border-bottom: 1px solid #eee; }" +
            "       .barcode-section label { display: block; font-weight: bold; color: #2c3e50; margin-bottom: 10px; font-size: 14px; }" +
            "       .barcode-input { width: 100%; padding: 15px; border: 2px solid #e74c3c; border-radius: 10px; font-size: 18px; text-align: center; letter-spacing: 2px; font-weight: bold; }" +
            "       .barcode-input:focus { outline: none; border-color: #c0392b; box-shadow: 0 0 10px rgba(231,76,60,0.3); }" +
            "       .search-btn { width: 100%; padding: 12px; background: #9b59b6; color: white; border: none; border-radius: 10px; font-size: 16px; font-weight: bold; cursor: pointer; margin-top: 10px; display: flex; align-items: center; justify-content: center; gap: 8px; }" +
            "       .product-card { margin: 15px; background: white; border-radius: 15px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden; display: none; }" +
            "       .product-card.show { display: block; animation: slideUp 0.3s ease; }" +
            "       @keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }" +
            "       .product-header { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; padding: 20px; }" +
            "       .product-name { font-size: 18px; font-weight: bold; margin-bottom: 5px; word-wrap: break-word; } .product-barcode { font-size: 12px; opacity: 0.8; }" +
            "       .product-body { padding: 20px; }" +
            "       .old-price-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; padding: 12px 15px; background: #fff3cd; border-radius: 10px; border-left: 4px solid #f39c12; }" +
            "       .old-price-label { font-size: 13px; color: #856404; font-weight: bold; } .old-price-value { font-size: 16px; color: #856404; font-weight: bold; text-decoration: line-through; }" +
            "       .price-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 15px; padding: 15px; background: #f8f9fa; border-radius: 10px; }" +
            "       .price-label { font-weight: bold; color: #2c3e50; font-size: 14px; }" +
            "       .price-value-box { background: white; border: 2px solid #27ae60; border-radius: 8px; padding: 10px 15px; min-width: 120px; text-align: right; cursor: pointer; }" +
            "       .price-input { border: none; outline: none; font-size: 20px; font-weight: bold; color: #27ae60; text-align: right; width: 100%; background: transparent; -moz-appearance: textfield; }" +
            "       .price-input::-webkit-inner-spin-button, .price-input::-webkit-outer-spin-button { -webkit-appearance: none; }" +
            "       .price-hint { font-size: 11px; color: #95a5a6; text-align: center; margin-top: 5px; }" +
            "       .update-btn { width: 100%; padding: 15px; background: linear-gradient(135deg, #27ae60, #2ecc71); color: white; border: none; border-radius: 10px; font-size: 18px; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 10px; }" +
            "       .update-btn:active { transform: scale(0.98); } .update-btn:disabled { background: #95a5a6; cursor: not-allowed; }" +
            "       .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); z-index: 1000; align-items: center; justify-content: center; }" +
            "       .modal-content { background: white; width: 95%; max-width: 500px; max-height: 85%; border-radius: 15px; overflow: hidden; display: flex; flex-direction: column; }" +
            "       .modal-header { background: #9b59b6; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; }" +
            "       .close-btn { background: none; border: none; color: white; font-size: 24px; cursor: pointer; }" +
            "       .modal-search { padding: 10px; background: white; border-bottom: 1px solid #eee; }" +
            "       .modal-search input { width: 100%; padding: 12px; border: 2px solid #9b59b6; border-radius: 8px; font-size: 16px; } .modal-search input:focus { outline: none; border-color: #8e44ad; }" +
            "       .product-list { overflow-y: auto; flex-grow: 1; }" +
            "       .list-item { padding: 15px; border-bottom: 1px solid #eee; cursor: pointer; display: flex; justify-content: space-between; align-items: center; } .list-item:active { background: #f8f9fa; }" +
            "       .list-item-name { font-weight: bold; color: #2c3e50; font-size: 14px; flex-grow: 1; padding-right: 10px; }" +
            "       .list-item-price { background: #e8f8f0; color: #27ae60; padding: 8px 12px; border-radius: 20px; font-weight: bold; font-size: 13px; white-space: nowrap; }" +
            "       .no-result { padding: 40px; text-align: center; color: #95a5a6; } .no-result span { font-size: 50px; display: block; margin-bottom: 10px; }" +
            "       .loading-spinner { display: inline-block; width: 20px; height: 20px; border: 3px solid rgba(255,255,255,0.3); border-radius: 50%; border-top-color: white; animation: spin 1s ease-in-out infinite; } @keyframes spin { to { transform: rotate(360deg); } }" +
            "       #toast { visibility: hidden; min-width: 250px; background-color: #333; color: #fff; text-align: center; border-radius: 25px; padding: 16px; position: fixed; z-index: 2000; left: 50%; bottom: 30px; transform: translateX(-50%); font-size: 14px; opacity: 0; transition: opacity 0.3s, bottom 0.3s; }" +
            "       #toast.show { visibility: visible; opacity: 1; bottom: 50px; } #toast.success { background-color: #27ae60; } #toast.error { background-color: #e74c3c; }" +
            "       .history-section { margin: 15px; } .history-title { font-size: 14px; color: #7f8c8d; margin-bottom: 10px; font-weight: bold; }" +
            "       .history-card { background: white; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); overflow: hidden; }" +
            "       .history-item { padding: 12px 15px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; } .history-item:last-child { border-bottom: none; }" +
            "       .history-name { font-size: 13px; color: #2c3e50; font-weight: 500; flex-grow: 1; } .history-prices { display: flex; gap: 10px; align-items: center; }" +
            "       .history-old { font-size: 12px; color: #e74c3c; text-decoration: line-through; } .history-new { font-size: 13px; color: #27ae60; font-weight: bold; }" +
            "       .history-time { font-size: 11px; color: #95a5a6; } .status-msg { text-align: center; padding: 10px; font-size: 12px; color: #7f8c8d; background: white; border-bottom: 1px solid #eee; }" +
            "   </style>" +
            "</head><body>" +
            "   <div class='navbar'><a href='/' class='back-btn'>←</a><h2>💰 Price Update</h2></div>" +
            "   <div class='status-msg' id='statusMsg'>Loading products...</div>" +
            "   <div class='barcode-section'>" +
            "       <label>📊 Scan or Type Barcode</label>" +
            "       <input type='text' class='barcode-input' id='barcodeInput' placeholder='Scan Barcode Here...' onkeypress='handleBarcodePress(event)' autofocus>" +
            "       <button class='search-btn' onclick='openSearchModal()'>🔍 Search Product by Name</button>" +
            "   </div>" +
            "   <div class='product-card' id='productCard'>" +
            "       <div class='product-header'><div class='product-name' id='productName'>-</div><div class='product-barcode' id='productBarcode'>-</div></div>" +
            "       <div class='product-body'>" +
            "           <div class='old-price-row'><span class='old-price-label'>Old Sale Rate</span><span class='old-price-value' id='oldPrice'>Rs 0</span></div>" +
            "           <div class='price-row'><span class='price-label'>New Sale Rate</span><div class='price-value-box' onclick='selectPrice()'><input type='number' class='price-input' id='newPriceInput' placeholder='0' onkeypress='handlePriceEnter(event)'></div></div>" +
            "           <div class='price-hint'>💡 Click on price box or type new price & press Enter</div>" +
            "           <button class='update-btn' id='updateBtn' onclick='updatePrice()' disabled>✅ Update Price</button>" +
            "       </div>" +
            "   </div>" +
            "   <div class='history-section' id='historySection' style='display:none;'><div class='history-title'>📝 Recent Updates</div><div class='history-card' id='historyList'></div></div>" +
            "   <div class='modal' id='searchModal'><div class='modal-content'>" +
            "       <div class='modal-header'><h3>🔍 Search Products</h3><button class='close-btn' onclick='closeSearchModal()'>&times;</button></div>" +
            "       <div class='modal-search'><input type='text' id='modalSearchInput' placeholder='Type product name...' oninput='searchProducts()'></div>" +
            "       <div class='product-list' id='productList'><div class='no-result'><span>🔍</span><p>Type to search products...</p></div></div>" +
            "   </div></div>" +
            "   <div id='toast'></div>" +
            "   <script>" +
            "       let currentProduct = null; let allProducts = []; let updateHistory = []; let searchResults = []; let searchTimeout = null;" +
            "       function loadHistory() { try { let d = localStorage.getItem('price_update_history'); updateHistory = d ? JSON.parse(d) : []; } catch(e) { updateHistory = []; } renderHistory(); }" +
            "       function saveHistory() { try { localStorage.setItem('price_update_history', JSON.stringify(updateHistory)); } catch(e) {} }" +
            "       function addToHistory(n, o, p) { updateHistory.unshift({name:n, oldPrice:o, newPrice:p, time:new Date().toISOString()}); if(updateHistory.length > 50) updateHistory = updateHistory.slice(0, 50); saveHistory(); renderHistory(); }" +
            "       function renderHistory() { let s=document.getElementById('historySection'), l=document.getElementById('historyList'); if(!updateHistory.length){s.style.display='none';return;} s.style.display='block'; let h=''; for(let i=0;i<Math.min(updateHistory.length,10);i++){let x=updateHistory[i]; h+=`<div class='history-item'><span class='history-name'>${x.name}</span><div class='history-prices'><span class='history-old'>Rs ${x.oldPrice}</span><span class='history-new'>Rs ${x.newPrice}</span></div><span class='history-time'>${new Date(x.time).toLocaleString()}</span></div>`;} l.innerHTML=h; }" +
            "       function handleBarcodePress(e) { if(e.key==='Enter'){let b=document.getElementById('barcodeInput').value.trim(); if(!b)return; findProductByBarcode(b);} }" +
            "       function findProductByBarcode(b) { let sc=b.replace(/^0+/, ''); if(!sc)sc='0'; let f=null; for(let i=0;i<allProducts.length;i++){let p=allProducts[i]; if(p.bc && p.bc.replace(/^0+/, '')===sc){f=p;break;}} if(f)showProduct(f); else showToast('Product not found!','error'); }" +
            "       function showProduct(p) { currentProduct=p; document.getElementById('productName').innerText=p.nm; document.getElementById('productBarcode').innerText='Barcode: '+p.bc; document.getElementById('oldPrice').innerText='Rs '+p.rate; document.getElementById('newPriceInput').value=''; document.getElementById('updateBtn').disabled=true; document.getElementById('productCard').classList.add('show'); document.getElementById('barcodeInput').value=''; setTimeout(()=>document.getElementById('newPriceInput').focus(),300); }" +
            "       function selectPrice() { let i=document.getElementById('newPriceInput'); i.focus(); i.select(); }" +
            "       document.getElementById('newPriceInput').addEventListener('input', function(){ document.getElementById('updateBtn').disabled = (!this.value || parseFloat(this.value)<=0); });" +
            "       function handlePriceEnter(e) { if(e.key==='Enter' && document.getElementById('newPriceInput').value>0) updatePrice(); }" +
            "       async function updatePrice() { if(!currentProduct)return; let np=parseFloat(document.getElementById('newPriceInput').value); if(!np||np<=0){showToast('Invalid price!','error');return;} let op=parseFloat(currentProduct.rate); if(np===op){showToast('Price is same!','error');return;} let btn=document.getElementById('updateBtn'); btn.disabled=true; btn.innerHTML='<span class=\"loading-spinner\"></span> Updating...'; try{ let r=await fetch('/price/api/update-price',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name:currentProduct.nm,oldPrice:op,newPrice:np})}); let res=await r.json(); if(res.success){showToast('Price updated!','success'); addToHistory(currentProduct.nm,op,np); currentProduct.rate=np; document.getElementById('oldPrice').innerText='Rs '+np; document.getElementById('newPriceInput').value=''; btn.disabled=true; document.getElementById('barcodeInput').focus(); if(navigator.vibrate)navigator.vibrate([100]);}else showToast(res.message||'Failed','error');}catch(err){showToast('Network error!','error');}finally{btn.innerHTML='✅ Update Price';} }" +
            "       function showToast(m,t){let x=document.getElementById('toast');x.innerText=m;x.className='show '+(t||'');setTimeout(()=>x.className='',3000);}" +
            "       function openSearchModal(){document.getElementById('searchModal').style.display='flex';document.getElementById('modalSearchInput').value='';document.getElementById('productList').innerHTML='<div class=\"no-result\"><span>🔍</span><p>Type to search...</p></div>';setTimeout(()=>document.getElementById('modalSearchInput').focus(),100);}" +
            "       function closeSearchModal(){document.getElementById('searchModal').style.display='none';document.getElementById('barcodeInput').focus();}" +
            "       function searchProducts(){if(searchTimeout)clearTimeout(searchTimeout);searchTimeout=setTimeout(()=>{let q=document.getElementById('modalSearchInput').value.toLowerCase().trim();if(q.length<2){document.getElementById('productList').innerHTML='<div class=\"no-result\"><span>🔍</span><p>Type 2 chars...</p></div>';return;}searchResults=[];for(let i=0;i<allProducts.length;i++){if(allProducts[i].nm.toLowerCase().includes(q))searchResults.push(allProducts[i]);}renderProductList(searchResults);},200);}" +
            "       function renderProductList(p){let l=document.getElementById('productList');if(!p.length){l.innerHTML='<div class=\"no-result\"><span>😕</span><p>No products found</p></div>';return;}let h='';for(let i=0;i<Math.min(p.length,50);i++){h+=`<div class='list-item' onclick='selectProduct(${i})'><span class='list-item-name'>${p[i].nm}</span><span class='list-item-price'>Rs ${p[i].rate}</span></div>`;}l.innerHTML=h;}" +
            "       function selectProduct(i){if(searchResults[i]){showProduct(searchResults[i]);closeSearchModal();}}" +
            "       async function loadAllProducts() { try { let r = await fetch('/data'); if(!r.ok) throw new Error(r.status); allProducts = await r.json(); document.getElementById('statusMsg').innerText = '✅ ' + allProducts.length + ' products loaded'; document.getElementById('statusMsg').style.color = '#27ae60'; setTimeout(() => document.getElementById('statusMsg').style.display = 'none', 3000); } catch(err) { document.getElementById('statusMsg').innerText = '❌ Error: ' + err.message; document.getElementById('statusMsg').style.color = '#e74c3c'; } }" +
            "       window.onload = function() { loadAllProducts(); loadHistory(); document.getElementById('barcodeInput').focus(); };" +
            "   </script></body></html>";
            sendResponse(exchange, html);
        }
    }

    // ================================================================
    // UPDATE PRICE API
    // ================================================================
    class UpdatePriceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.sendResponseHeaders(405, 0); exchange.close(); return; }

            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
            StringBuilder jsonBuilder = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) jsonBuilder.append(line);
            br.close(); String jsonData = jsonBuilder.toString();

            boolean success = false; String message = "";
            try {
                String name = ""; double newPrice = 0;
                int nmIdx = jsonData.indexOf("\"name\":\""); if (nmIdx != -1) name = jsonData.substring(nmIdx + 8, jsonData.indexOf("\"", nmIdx + 8));
                int priceIdx = jsonData.indexOf("\"newPrice\":"); if (priceIdx != -1) newPrice = Double.parseDouble(jsonData.substring(priceIdx + 11).split("[,\\}]")[0].trim());
                
                if (name.isEmpty() || newPrice <= 0) throw new Exception("Invalid input");

                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    try (Statement stmt = conn.createStatement()) {
                        int rows = stmt.executeUpdate("UPDATE ProductItem SET SaleRate = " + newPrice + " WHERE LongName LIKE '%" + name.replace("'", "''") + "%'");
                        if (rows > 0) { success = true; message = "Price updated for " + rows + " product(s)"; }
                        else { message = "No product found"; }
                    }
                }
            } catch (Exception e) { message = "Error: " + e.getMessage(); }

            String response = String.format("{\"success\":%b,\"message\":\"%s\"}", success, escape(message));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody(); os.write(response.getBytes("UTF-8")); os.close();
        }
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        byte[] bytes = response.getBytes("UTF-8"); exchange.sendResponseHeaders(200, bytes.length); OutputStream os = exchange.getResponseBody(); os.write(bytes); os.close();
    }
}