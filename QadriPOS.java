import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class QadriPOS {

    // Lock for thread-safe file access
    private static final Object FILE_LOCK = new Object();

    // Ye method main server call karega
    public static void registerContexts(HttpServer server) {
        QadriPOS instance = new QadriPOS();
        server.createContext("/pos", instance.new POSBillingHandler());
        server.createContext("/pos/records", instance.new GetRecordUIHandler());
        server.createContext("/pos/save", instance.new SaveSaleHandler());
        server.createContext("/pos/api/records", instance.new GetRecordAPIHandler());
        System.out.println("POS Module Loaded (Contexts: /pos, /pos/save...)");
    }

    String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }

    // ================================================================
    // HANDLER 1: POS BILLING UI
    // ================================================================
    class POSBillingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "   <title>POS - New Sale</title>" +
            "   <meta charset='UTF-8'>" +
            "   <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "   <style>" +
            "       * { box-sizing: border-box; } body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; padding-bottom: 140px; }" +
            "       .navbar { background: #3498db; padding: 15px; display: flex; align-items: center; justify-content: space-between; color: white; box-shadow: 0 2px 5px rgba(0,0,0,0.2); position: sticky; top: 0; z-index: 100; }" +
            "       .nav-left { display: flex; align-items: center; }" +
            "       .back-btn { background: rgba(255,255,255,0.2); border-radius: 50%; width: 35px; height: 35px; display: flex; align-items: center; justify-content: center; text-decoration: none; color: white; font-weight: bold; margin-right: 15px; }" +
            "       .navbar h2 { font-size: 18px; margin: 0; }" +
            "       .nav-btns { display: flex; gap: 6px; }" +
            "       .btn-clear-all { background: #e74c3c; color: white; border: none; padding: 8px 12px; border-radius: 20px; font-weight: bold; cursor: pointer; font-size: 11px; }" +
            "       .btn-hold { background: #f39c12; color: white; border: none; padding: 8px 12px; border-radius: 20px; font-weight: bold; cursor: pointer; font-size: 11px; position: relative; }" +
            "       .hold-badge { position: absolute; top: -6px; right: -6px; background: #e74c3c; color: white; border-radius: 50%; width: 18px; height: 18px; font-size: 10px; display: flex; align-items: center; justify-content: center; font-weight: bold; }" +
            "       .input-area { padding: 10px; background: white; border-bottom: 1px solid #eee; }" +
            "       input { width: 100%; padding: 12px; border: 2px solid #bdc3c7; border-radius: 8px; font-size: 16px; margin-bottom: 10px; }" +
            "       input:focus { border-color: #3498db; outline: none; }" +
            "       .btn-search-open { width: 100%; padding: 12px; background: #9b59b6; color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: bold; cursor: pointer; }" +
            "       .cart-container { background: white; margin: 10px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); overflow: hidden; }" +
            "       .cart-header { background: #2c3e50; color: white; padding: 10px; font-weight: bold; text-align: center; }" +
            "       .cart-item { display: flex; align-items: center; padding: 10px; border-bottom: 1px solid #eee; }" +
            "       .cart-info { flex-grow: 1; margin-right: 10px; }" +
            "       .cart-name { font-weight: bold; color: #2c3e50; font-size: 14px; }" +
            "       .cart-price { color: #7f8c8d; font-size: 12px; margin-top: 2px; }" +
            "       .qty-controls { display: flex; align-items: center; margin-right: 15px; }" +
            "       .qty-btn { background: #ecf0f1; border: 1px solid #bdc3c7; width: 30px; height: 30px; border-radius: 5px; font-weight: bold; cursor: pointer; font-size: 16px; }" +
            "       .qty-num { background: #fff; border: 1px solid #bdc3c7; width: 45px; height: 30px; text-align: center; margin: 0 2px; font-weight: bold; font-size: 14px; padding: 0; -moz-appearance: textfield; }" +
            "       .qty-num::-webkit-outer-spin-button, .qty-num::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }" +
            "       .btn-delete { background: #e74c3c; color: white; border: none; padding: 8px 12px; border-radius: 5px; font-weight: bold; cursor: pointer; font-size: 12px; }" +
            "       .summary-bar { position: fixed; bottom: 0; width: 100%; background: #2c3e50; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; z-index: 99; box-shadow: 0 -2px 10px rgba(0,0,0,0.2); }" +
            "       .sum-left { flex: 2; display: flex; justify-content: space-around; }" +
            "       .sum-item { text-align: center; }" +
            "       .sum-label { font-size: 10px; color: #bdc3c7; display: block; }" +
            "       .sum-val { font-size: 18px; font-weight: bold; }" +
            "       .sum-total { color: #2ecc71; font-size: 22px; }" +
            "       .btn-save { flex: 1; margin-left: 15px; background: #27ae60; color: white; border: none; padding: 15px; border-radius: 10px; font-size: 16px; font-weight: bold; cursor: pointer; }" +
            "       .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; align-items: center; justify-content: center; }" +
            "       .modal-content { background: white; width: 90%; max-height: 90%; border-radius: 15px; overflow: hidden; display: flex; flex-direction: column; }" +
            "       .modal-header { background: #9b59b6; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; }" +
            "       .modal-header-save { background: #27ae60; }" +
            "       .modal-header-hold { background: #f39c12; }" +
            "       .close-btn { background: none; border: none; color: white; font-size: 24px; cursor: pointer; font-weight: bold; }" +
            "       .product-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 10px; overflow-y: auto; }" +
            "       .item { background: white; padding: 15px; border-radius: 10px; text-align: center; box-shadow: 0 2px 5px rgba(0,0,0,0.05); border: 1px solid #eee; }" +
            "       .item b { display: block; margin-bottom: 5px; font-size: 14px; color: #2c3e50; height: 35px; overflow: hidden; }" +
            "       .item small { color: #95a5a6; font-size: 10px; display: block; margin-bottom: 10px; }" +
            "       .btn-add { background: #27ae60; color: white; border: none; padding: 8px 20px; border-radius: 20px; font-weight: bold; cursor: pointer; width: 100%; font-size: 14px; }" +
            "       .save-options { padding: 20px; display: flex; flex-direction: column; gap: 15px; }" +
            "       .action-btn { width: 100%; padding: 15px; border: none; border-radius: 10px; font-size: 18px; font-weight: bold; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 10px; }" +
            "       .btn-save-final { background: #27ae60; color: white; }" +
            "       #loading-msg { text-align: center; padding: 20px; color: #7f8c8d; font-weight: bold; display: block; }" +
            "       #toast { visibility: hidden; min-width: 250px; background-color: #333; color: #fff; text-align: center; border-radius: 25px; padding: 16px; position: fixed; z-index: 2000; left: 50%; bottom: 80px; transform: translateX(-50%); font-size: 16px; opacity: 0; transition: opacity 0.3s, bottom 0.3s; }" +
            "       #toast.show { visibility: visible; opacity: 1; bottom: 100px; }" +
            "       .hold-list { padding: 10px; overflow-y: auto; max-height: 70vh; }" +
            "       .hold-card { background: #fff8e1; border: 2px solid #f39c12; border-radius: 10px; padding: 15px; margin-bottom: 10px; cursor: pointer; transition: all 0.2s; }" +
            "       .hold-card:active { transform: scale(0.98); background: #ffecb3; }" +
            "       .hold-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }" +
            "       .hold-time { font-size: 12px; color: #7f8c8d; }" +
            "       .hold-total { font-size: 18px; font-weight: bold; color: #f39c12; }" +
            "       .hold-items-preview { font-size: 12px; color: #555; max-height: 40px; overflow: hidden; }" +
            "       .hold-card-actions { display: flex; gap: 8px; margin-top: 10px; }" +
            "       .hold-btn-load { flex: 1; background: #27ae60; color: white; border: none; padding: 8px; border-radius: 5px; font-weight: bold; cursor: pointer; font-size: 12px; }" +
            "       .hold-btn-delete { background: #e74c3c; color: white; border: none; padding: 8px 12px; border-radius: 5px; font-weight: bold; cursor: pointer; font-size: 12px; }" +
            "       .hold-empty { text-align: center; padding: 40px; color: #95a5a6; }" +
            "       .hold-empty span { font-size: 50px; display: block; margin-bottom: 10px; }" +
            "       .hold-clear-all-btn { display: block; width: 100%; padding: 12px; background: #e74c3c; color: white; border: none; border-radius: 8px; font-weight: bold; cursor: pointer; font-size: 14px; margin-top: 10px; }" +
            "   </style>" +
            "</head>" +
            "<body>" +
            "   <div class='navbar'>" +
            "       <div class='nav-left'>" +
            "           <a href='/' class='back-btn'>←</a>" +
            "           <h2>New Sale</h2>" +
            "       </div>" +
            "       <div class='nav-btns'>" +
            "           <button class='btn-hold' id='holdBtn' onclick='openHoldModal()'>" +
            "               ⏸ Hold" +
            "               <span class='hold-badge' id='holdBadge' style='display:none;'>0</span>" +
            "           </button>" +
            "           <button class='btn-clear-all' onclick='clearCart()'>🗑 Clear</button>" +
            "       </div>" +
            "   </div>" +
            "   <div id='loading-msg'>Loading Data...</div>" +
            "   <div class='input-area'>" +
            "       <input type='text' id='barcode-input' placeholder='Scan / Type Code & Press Enter' onkeypress='handleBarcode(event)'>" +
            "       <button class='btn-search-open' onclick='openSearchModal()'>🔍 Search Product</button>" +
            "   </div>" +
            "   <div class='cart-container' id='cart-box' style='display:none;'>" +
            "       <div class='cart-header'>Current Bill Items</div>" +
            "       <div id='cart-list'></div>" +
            "   </div>" +
            "   <div class='modal' id='searchModal'>" +
            "       <div class='modal-content'>" +
            "           <div class='modal-header'>" +
            "               <h3>Search Products</h3>" +
            "               <button class='close-btn' onclick='closeSearchModal()'>&times;</button>" +
            "           </div>" +
            "           <div style='padding:10px; background:white; border-bottom:1px solid #eee;'>" +
            "               <input type='text' id='modal-search' placeholder='Type name to search...' oninput='doModalSearch()'>" +
            "           </div>" +
            "           <div id='modal-list' class='product-grid'></div>" +
            "       </div>" +
            "   </div>" +
            "   <div class='modal' id='saveModal'>" +
            "       <div class='modal-content' style='height:auto; max-width:350px;'>" +
            "           <div class='modal-header modal-header-save'>" +
            "               <h3>Save Bill</h3>" +
            "               <button class='close-btn' onclick='closeSaveModal()'>&times;</button>" +
            "           </div>" +
            "           <div class='save-options'>" +
            "               <button class='action-btn btn-save-final' onclick='confirmSave()'>✅ Confirm & Save</button>" +
            "           </div>" +
            "       </div>" +
            "   </div>" +
            "   <div class='modal' id='holdModal'>" +
            "       <div class='modal-content'>" +
            "           <div class='modal-header modal-header-hold'>" +
            "               <h3>⏸ Held Bills</h3>" +
            "               <button class='close-btn' onclick='closeHoldModal()'>&times;</button>" +
            "           </div>" +
            "           <div class='hold-list' id='holdList'>" +
            "               <div class='hold-empty'>" +
            "                   <span>📭</span>" +
            "                   <p>No held bills</p>" +
            "               </div>" +
            "           </div>" +
            "       </div>" +
            "   </div>" +
            "   <div class='summary-bar'>" +
            "       <div class='sum-left'>" +
            "           <div class='sum-item'><span class='sum-label'>ITEMS</span><span class='sum-val' id='s-items'>0</span></div>" +
            "           <div class='sum-item'><span class='sum-label'>QTY</span><span class='sum-val' id='s-qty'>0</span></div>" +
            "           <div class='sum-item'><span class='sum-label'>TOTAL</span><span class='sum-val sum-total' id='s-total'>Rs 0</span></div>" +
            "       </div>" +
            "       <button class='btn-save' onclick='openSaveModal()'>SAVE</button>" +
            "   </div>" +
            "   <div id='toast'>Product Added!</div>" +
            "   <script>" +
            "       let allData = []; let cart = []; let currentModalItems = [];" +
            "       let holdList = []; const HOLD_KEY = 'qadri_hold_bills';" +
            "       let lastHoldHash = '';" +
            "       function getCartHash() { return JSON.stringify(cart); }" +
            "       function getHoldList() { try { let data = localStorage.getItem(HOLD_KEY); if(data) { holdList = JSON.parse(data); } else { holdList = []; } } catch(e) { holdList = []; } updateHoldBadge(); }" +
            "       function saveHoldList() { try { localStorage.setItem(HOLD_KEY, JSON.stringify(holdList)); } catch(e) { console.error('Hold save error', e); } updateHoldBadge(); }" +
            "       function updateHoldBadge() { let badge = document.getElementById('holdBadge'); if(holdList.length > 0) { badge.style.display = 'flex'; badge.innerText = holdList.length; } else { badge.style.display = 'none'; } }" +
            "       function addToHold() { if(cart.length === 0) { return false; } let currentHash = getCartHash(); if(lastHoldHash === currentHash) { return false; } let total = 0; for(let i=0; i<cart.length; i++) { total += cart[i].rate * cart[i].qty; } let holdItem = { id: Date.now(), date: new Date().toISOString(), items: JSON.parse(JSON.stringify(cart)), total: total }; holdList.unshift(holdItem); saveHoldList(); lastHoldHash = currentHash; showToast('Bill held successfully!'); return true; }" +
            "       function loadFromHold(index) { if(cart.length > 0) { if(!confirm('Current cart has items. Replace with held bill?')) return; } let holdItem = holdList[index]; if(holdItem) { cart = JSON.parse(JSON.stringify(holdItem.items)); lastHoldHash = getCartHash(); holdList.splice(index, 1); saveHoldList(); updateCartUI(); closeHoldModal(); showToast('Held bill loaded!'); document.getElementById('barcode-input').focus(); } }" +
            "       function deleteHold(index) { if(confirm('Delete this held bill?')) { holdList.splice(index, 1); saveHoldList(); renderHoldList(); showToast('Held bill deleted!'); } }" +
            "       function clearAllHolds() { if(confirm('Delete ALL held bills? This cannot be undone!')) { holdList = []; saveHoldList(); renderHoldList(); showToast('All held bills cleared!'); } }" +
            "       function openHoldModal() { getHoldList(); renderHoldList(); document.getElementById('holdModal').style.display = 'flex'; }" +
            "       function closeHoldModal() { document.getElementById('holdModal').style.display = 'none'; }" +
            "       function renderHoldList() { let container = document.getElementById('holdList'); if(holdList.length === 0) { container.innerHTML = '<div class=\"hold-empty\"><span>📭</span><p>No held bills</p></div>'; return; } let html = ''; for(let i=0; i<holdList.length; i++) { let h = holdList[i]; let dateStr = new Date(h.date).toLocaleString(); let itemNames = []; for(let j=0; j<h.items.length; j++) { itemNames.push(h.items[j].nm + ' x' + h.items[j].qty); } let preview = itemNames.join(', '); html += '<div class=\"hold-card\">' + '<div class=\"hold-card-header\">' + '<span class=\"hold-time\">' + dateStr + '</span>' + '<span class=\"hold-total\">Rs ' + h.total + '</span>' + '</div>' + '<div class=\"hold-items-preview\">' + preview + '</div>' + '<div class=\"hold-card-actions\">' + '<button class=\"hold-btn-load\" onclick=\"loadFromHold(' + i + ')\">📥 Load Bill</button>' + '<button class=\"hold-btn-delete\" onclick=\"deleteHold(' + i + ')\">🗑 Delete</button>' + '</div>' + '</div>'; } html += '<button class=\"hold-clear-all-btn\" onclick=\"clearAllHolds()\">🗑 Clear All Held Bills</button>'; container.innerHTML = html; }" +
            "       function handleBarcode(e) { if (e.key === 'Enter') { if(allData.length === 0) { alert('Data loading...'); return; } let rawCode = document.getElementById('barcode-input').value.trim(); if(!rawCode) return; let searchCode = rawCode.replace(/^0+/, ''); if(searchCode == '') searchCode = '0'; let found = null; for(let i=0; i<allData.length; i++) { let p = allData[i]; if(p.bc) { let dbCode = p.bc.replace(/^0+/, ''); if(dbCode == searchCode) { found = p; break; } } } if(found) { addItemToCart(found); document.getElementById('barcode-input').value = ''; } else { alert('Not found: ' + rawCode); } } }" +
            "       function showToast(message) { var x = document.getElementById('toast'); x.innerText = message; x.className = 'show'; setTimeout(function(){ x.className = x.className.replace('show', ''); }, 3000); }" +
            "       function addItemToCart(p) { let exists = false; for(let i=0; i<cart.length; i++) { if(cart[i].bc == p.bc) { cart[i].qty++; exists = true; break; } } if(!exists) { cart.unshift({bc: p.bc, nm: p.nm, rate: p.rate, qty: 1}); showToast('Product Added!'); } else { showToast('Quantity Increased!'); } lastHoldHash = ''; updateCartUI(); }" +
            "       function handleQtyEnter(e, index) { if (e.key === 'Enter') { e.target.blur(); let val = parseInt(e.target.value); if (val && val > 0) { cart[index].qty = val; lastHoldHash = ''; updateCartUI(); showToast('Quantity Updated!'); document.getElementById('barcode-input').focus(); } else { alert('Please enter a valid quantity (min 1)'); e.target.value = cart[index].qty; } } }" +
            "       function incQty(index) { cart[index].qty++; lastHoldHash = ''; updateCartUI(); showToast('Quantity Increased!'); }" +
            "       function decQty(index) { if(cart[index].qty > 1) { cart[index].qty--; lastHoldHash = ''; updateCartUI(); showToast('Quantity Decreased!'); } else { removeItem(index); } }" +
            "       function removeItem(index) { if(confirm('Delete Item?')) { cart.splice(index, 1); lastHoldHash = ''; updateCartUI(); showToast('Item Removed!'); } }" +
            "       function clearCart() { if(cart.length > 0) { let total = 0; for(let i=0; i<cart.length; i++) { total += cart[i].rate * cart[i].qty; } let msg = 'Total: Rs ' + total + '\\n\\nCart will be saved to HOLD before clearing.\\nContinue?'; if(confirm(msg)) { addToHold(); cart = []; lastHoldHash = ''; updateCartUI(); showToast('Cart cleared & held!'); } } }" +
            "       function updateCartUI() { let totalItems = cart.length; let totalQty = 0; let grandTotal = 0; let cartBox = document.getElementById('cart-box'); let cartList = document.getElementById('cart-list'); if(cart.length > 0) { cartBox.style.display = 'block'; let html = ''; for(let i=0; i<cart.length; i++) { let c = cart[i]; let lineTotal = c.rate * c.qty; totalQty += c.qty; grandTotal += lineTotal; html += '<div class=\"cart-item\"><div class=\"cart-info\"><div class=\"cart-name\">' + c.nm + '</div><div class=\"cart-price\">Rate: Rs. ' + c.rate + '</div></div><div class=\"qty-controls\"><button class=\"qty-btn\" onclick=\"decQty(' + i + ')\">-</button><input type=\"number\" class=\"qty-num\" value=\"' + c.qty + '\" onfocus=\"this.select()\" onkeypress=\"handleQtyEnter(event, ' + i + ')\"><button class=\"qty-btn\" onclick=\"incQty(' + i + ')\">+</button></div><button class=\"btn-delete\" onclick=\"removeItem(' + i + ')\">DEL</button></div>'; } cartList.innerHTML = html; } else { cartBox.style.display = 'none'; } document.getElementById('s-items').innerText = totalItems; document.getElementById('s-qty').innerText = totalQty; document.getElementById('s-total').innerText = 'Rs ' + grandTotal; }" +
            "       function openSearchModal() { if(allData.length === 0) { alert('Loading...'); return; } document.getElementById('searchModal').style.display = 'flex'; document.getElementById('modal-search').value = ''; currentModalItems = allData; displayModalItems(allData); setTimeout(function() { document.getElementById('modal-search').focus(); }, 100); }" +
            "       function closeSearchModal() { document.getElementById('searchModal').style.display = 'none'; }" +
            "       function openSaveModal() { if(cart.length===0){alert('Empty Cart'); return;} document.getElementById('saveModal').style.display = 'flex'; }" +
            "       function closeSaveModal() { document.getElementById('saveModal').style.display = 'none'; }" +
            "       function doModalSearch() { const q = document.getElementById('modal-search').value.toLowerCase(); if(!q) { currentModalItems = allData; displayModalItems(allData); return; } let res = []; for(let i=0; i<allData.length; i++) { if(allData[i].nm.toLowerCase().includes(q)) res.push(allData[i]); } currentModalItems = res; displayModalItems(res); }" +
            "       function displayModalItems(data) { let h = ''; let lim = Math.min(data.length, 50); for(let i=0; i<lim; i++) { let p = data[i]; h += '<div class=\"item\"><b>' + p.nm + '</b><small>Rs: ' + p.rate + '</small><button class=\"btn-add\" onclick=\"addFromModal(' + i + ')\">Add</button></div>'; } document.getElementById('modal-list').innerHTML = h; }" +
            "       function addFromModal(index) { let p = currentModalItems[index]; addItemToCart(p); closeSearchModal(); document.getElementById('barcode-input').focus(); }" +
            "       async function saveSaleToDatabase() { try { const response = await fetch('/pos/save', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(cart) }); if (!response.ok) throw new Error('Save failed'); return true; } catch (error) { console.error('Error saving:', error); alert('Error saving invoice!'); return false; } }" +
            "       async function confirmSave() { if(cart.length===0){alert('Empty Cart'); return;} let success = await saveSaleToDatabase(); if(!success) return; closeSaveModal(); cart = []; lastHoldHash = ''; updateCartUI(); showToast('Bill Saved Successfully!'); }" +
            "       async function doAppShare() { if(cart.length===0){ if(window.AndroidApp) window.AndroidApp.showToast('Cart is empty!'); return; } let success = await saveSaleToDatabase(); if(!success) return; let text = 'Qadri Store Bill\\n----------------\\n'; let gt = 0; for(let i=0; i<cart.length; i++) { let c = cart[i]; let t = c.rate * c.qty; gt += t; text += c.nm + ' x' + c.qty + ' = ' + t + '\\n'; } text += '----------------\\nTotal: Rs ' + gt; if(window.AndroidApp) { window.AndroidApp.shareText(text); } else { if(navigator.share) { navigator.share({ title: 'Bill', text: text }); } else { alert('Copied to log'); console.log(text); } } cart = []; lastHoldHash = ''; updateCartUI(); }" +
            "       async function doAppPdf() { if(cart.length===0){ if(window.AndroidApp) window.AndroidApp.showToast('Cart is empty!'); return; } let success = await saveSaleToDatabase(); if(!success) return; let html = '<html><head><title>Bill</title><style>body{font-family:Arial;text-align:center;} table{width:100%;border-collapse:collapse;margin-top:20px;} th,td{border:1px solid #000;padding:8px;}</style></head><body>'; html += '<h2>Qadri Store</h2><p>Receipt</p><hr><p>Date: ' + new Date().toLocaleString() + '</p><table><tr><th>Item</th><th>Rate</th><th>Qty</th><th>Total</th></tr>'; let gt = 0; for(let i=0; i<cart.length; i++) { let c = cart[i]; let t = c.rate * c.qty; gt += t; html += '<tr><td>'+c.nm+'</td><td>'+c.rate+'</td><td>'+c.qty+'</td><td>'+t+'</td></tr>'; } html += '<tr><td colspan=3><b>Grand Total</b></td><td><b>Rs '+gt+'</b></td></tr></table><hr><p>Thank you!</p></body></html>'; if(window.AndroidApp) { window.AndroidApp.printHtml(html); } else { let win = window.open('', '', 'height=600,width=800'); win.document.write(html); win.document.close(); win.print(); } cart = []; lastHoldHash = ''; updateCartUI(); }" +
            "       window.addEventListener('beforeunload', function(e) { if(cart.length > 0) { addToHold(); e.preventDefault(); e.returnValue = ''; } });" +
            "       window.onload = function() { if(document.getElementById('barcode-input')) document.getElementById('barcode-input').focus(); getHoldList(); fetch('/data').then(r => r.json()).then(data => { allData = data; if(document.getElementById('loading-msg')) document.getElementById('loading-msg').style.display = 'none'; if(holdList.length > 0) { showToast('You have ' + holdList.length + ' held bill(s)! Click HOLD to view.'); } }).catch(e => { console.error(e); alert('Error loading products: ' + e); }); };" +
            "   </script>" +
            "</body>" +
            "</html>";
            sendResponse(exchange, html);
        }
    }

    // ================================================================
    // HANDLER 2: INVOICE RECORDS UI
    // ================================================================
    class GetRecordUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html =
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "   <title>Saved Invoices</title>" +
            "   <meta charset='UTF-8'>" +
            "   <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
            "   <style>" +
            "       * { box-sizing: border-box; } body { font-family: Arial, sans-serif; margin: 0; background: #f4f6f9; padding-bottom: 20px; }" +
            "       .navbar { background: #9b59b6; padding: 15px; display: flex; align-items: center; color: white; box-shadow: 0 2px 5px rgba(0,0,0,0.2); }" +
            "       .back-btn { background: rgba(255,255,255,0.2); border-radius: 50%; width: 35px; height: 35px; display: flex; align-items: center; justify-content: center; text-decoration: none; color: white; font-weight: bold; margin-right: 15px; }" +
            "       .navbar h2 { font-size: 18px; margin: 0; flex-grow: 1; }" +
            "       .container { padding: 15px; max-width: 800px; margin: 0 auto; }" +
            "       .invoice-card { background: white; border-radius: 10px; padding: 15px; margin-bottom: 15px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); border-left: 5px solid #9b59b6; }" +
            "       .inv-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }" +
            "       .inv-date { font-size: 14px; color: #7f8c8d; }" +
            "       .inv-total { font-size: 18px; font-weight: bold; color: #27ae60; }" +
            "       .btn-view { background: #3498db; color: white; border: none; padding: 8px 15px; border-radius: 5px; cursor: pointer; font-size: 12px; }" +
            "       .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.6); z-index: 1000; align-items: center; justify-content: center; }" +
            "       .modal-content { background: white; width: 95%; max-width: 600px; max-height: 90%; border-radius: 15px; overflow: hidden; display: flex; flex-direction: column; }" +
            "       .modal-header { background: #9b59b6; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; }" +
            "       .close-btn { background: none; border: none; color: white; font-size: 24px; cursor: pointer; }" +
            "       .modal-body { padding: 20px; overflow-y: auto; }" +
            "       table { width: 100%; border-collapse: collapse; margin-top: 10px; }" +
            "       th { background: #f8f9fa; text-align: left; padding: 10px; font-size: 14px; color: #2c3e50; }" +
            "       td { border-bottom: 1px solid #eee; padding: 10px; font-size: 14px; }" +
            "       td.text-right { text-align: right; font-weight: bold; }" +
            "       .loading { text-align: center; padding: 20px; color: #7f8c8d; }" +
            "   </style>" +
            "</head>" +
            "<body>" +
            "   <div class='navbar'>" +
            "       <a href='/pos' class='back-btn'>←</a>" +
            "       <h2>Invoice History</h2>" +
            "   </div>" +
            "   <div class='container' id='list'>" +
            "       <div class='loading'>Loading records...</div>" +
            "   </div>" +
            "   <div class='modal' id='detailModal'>" +
            "       <div class='modal-content'>" +
            "           <div class='modal-header'>" +
            "               <h3 id='modalTitle'>Invoice Details</h3>" +
            "               <button class='close-btn' onclick='closeModal()'>&times;</button>" +
            "           </div>" +
            "           <div class='modal-body' id='modalBody'></div>" +
            "       </div>" +
            "   </div>" +
            "   <script>" +
            "       let records = [];" +
            "       function loadRecords() {" +
            "           fetch('/pos/api/records', { cache: 'no-store' })" +
            "           .then(res => { if(!res.ok) throw new Error('Server error: ' + res.status); return res.json(); })" +
            "           .then(data => { records = data.reverse(); renderList(); })" +
            "           .catch(err => { console.error(err); alert('Error loading records: ' + err.message); document.getElementById('list').innerHTML = '<p class=\"loading\">Error loading data.</p>'; });" +
            "       }" +
            "       function renderList() { const container = document.getElementById('list'); if(records.length === 0) { container.innerHTML = '<p class=\"loading\">No invoices saved yet.</p>'; return; } let html = ''; records.forEach(function(rec, idx) { let dateStr = new Date(rec.date).toLocaleString(); html += '<div class=\"invoice-card\">' + '<div class=\"inv-header\">' + '<span class=\"inv-date\">' + dateStr + '</span>' + '<span class=\"inv-total\">Rs ' + rec.total + '</span>' + '</div>' + '<button class=\"btn-view\" onclick=\"viewDetail(' + idx + ')\">View Items</button>' + '</div>'; }); container.innerHTML = html; }" +
            "       function viewDetail(idx) { let rec = records[idx]; document.getElementById('modalTitle').innerText = 'Invoice - ' + new Date(rec.date).toLocaleDateString(); let html = '<table><thead><tr><th>Item</th><th>Qty</th><th>Rate</th><th class=\"text-right\">Total</th></tr></thead><tbody>'; rec.items.forEach(function(item) { html += '<tr><td>' + item.nm + '</td><td>' + item.qty + '</td><td>' + item.rate + '</td><td class=\"text-right\">' + (item.qty * item.rate) + '</td></tr>'; }); html += '</tbody></table><h3 style=\"text-align:right; margin-top:20px;\">Grand Total: Rs ' + rec.total + '</h3>'; document.getElementById('modalBody').innerHTML = html; document.getElementById('detailModal').style.display = 'flex'; }" +
            "       function closeModal() { document.getElementById('detailModal').style.display = 'none'; }" +
            "       loadRecords();" +
            "   </script>" +
            "</body>" +
            "</html>";
            sendResponse(exchange, html);
        }
    }

    // ================================================================
    // HANDLER 3: GET RECORD API
    // ================================================================
    class GetRecordAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
            exchange.getResponseHeaders().add("Pragma", "no-cache");

            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("[");

            boolean first = true;
            File file = new File("invoices_history.txt");

            synchronized (FILE_LOCK) {
                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            if (!first) { jsonResponse.append(","); }
                            jsonResponse.append(line);
                            first = false;
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }

            jsonResponse.append("]");
            String responseString = jsonResponse.toString();

            exchange.sendResponseHeaders(200, responseString.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseString.getBytes("UTF-8"));
            os.close();
        }
    }

    // ================================================================
    // HANDLER 4: SAVE SALE
    // ================================================================
    class SaveSaleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) { jsonBuilder.append(line); }
            br.close();
            String jsonData = jsonBuilder.toString();

            boolean success = false;
            synchronized (FILE_LOCK) {
                try {
                    File aggFile = new File("sales_data.txt");
                    Map<String, Integer> currentSales = new HashMap<>();
                    if (aggFile.exists()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(aggFile))) {
                            String fileLine;
                            while ((fileLine = reader.readLine()) != null) {
                                String[] parts = fileLine.split("\\|");
                                if (parts.length == 2) { currentSales.put(parts[0].trim(), Integer.parseInt(parts[1].trim())); }
                            }
                        }
                    }

                    String cleanJson = jsonData.replace("[", "").replace("]", "").trim();
                    double grandTotal = 0.0;

                    if (cleanJson.length() > 0) {
                        String[] items = cleanJson.split("\\},\\{");
                        for (String itemStr : items) {
                            try {
                                itemStr = itemStr.replace("{", "").replace("}", "").trim();
                                String[] parts = itemStr.split(",");
                                String name = ""; int qty = 0; double rate = 0;
                                for (String part : parts) {
                                    String[] kv = part.split(":", 2);
                                    if (kv.length == 2) {
                                        String key = kv[0].replace("\"", "").trim();
                                        String val = kv[1].replace("\"", "").trim();
                                        if ("nm".equals(key)) name = val;
                                        else if ("qty".equals(key)) qty = Integer.parseInt(val);
                                        else if ("rate".equals(key)) rate = Double.parseDouble(val);
                                    }
                                }
                                if (!name.isEmpty() && qty > 0) {
                                    currentSales.put(name, currentSales.getOrDefault(name, 0) + qty);
                                    grandTotal += (qty * rate);
                                }
                            } catch (Exception e) { System.err.println("Error parsing item: " + itemStr); }
                        }
                    }

                    try (PrintWriter writer = new PrintWriter(new FileWriter(aggFile))) {
                        for (Map.Entry<String, Integer> entry : currentSales.entrySet()) { writer.println(entry.getKey() + "|" + entry.getValue()); }
                    }

                    File historyFile = new File("invoices_history.txt");
                    try (FileWriter fw = new FileWriter(historyFile, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {
                        String historyEntry = String.format("{\"date\":\"%s\", \"total\":\"%.2f\", \"items\":%s}", java.time.Instant.now().toString(), grandTotal, jsonData);
                        out.println(historyEntry);
                    }
                    success = true;
                } catch (Exception e) { e.printStackTrace(); success = false; }
            }

            String response = success ? "{\"status\":\"success\"}" : "{\"status\":\"error\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        }
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================
    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");

        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}