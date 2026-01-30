import com.ecommerce.service.InventoryService;
import com.ecommerce.service.InventoryServiceService;
import com.ecommerce.service.Product;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class InventoryClient {

    static final Map<Integer, Integer> stockMap = new HashMap<>();
    static final Map<Integer, Integer> myInventory = new HashMap<>();

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);

        /* ===== STATIC IMAGES ===== */
        server.createContext("/images", ex -> {
            File file = new File("." + ex.getRequestURI().getPath());
            if (!file.exists()) { ex.sendResponseHeaders(404, -1); return; }
            ex.getResponseHeaders().set("Content-Type", "image/jpeg");
            byte[] data = Files.readAllBytes(file.toPath());
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
        });

        server.createContext("/", InventoryClient::renderStore);
        server.createContext("/my", InventoryClient::renderInventory);

        /* ===== ADD PRODUCT ===== */
        server.createContext("/order", ex -> {
            Map<String,String> q = parse(ex.getRequestURI().getQuery());
            int pid = parseInt(q.get("pid"));
            int qty = Math.max(1, parseInt(q.get("qty")));

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            Product p = port.getProductById(pid);

            if (p == null) {
                send(ex, "Invalid product");
                return;
            }

            synchronized (stockMap) {
                int available = stockMap.getOrDefault(pid, 0);
                if (available <= 0) {
                    send(ex, "Out of stock");
                    return;
                }
                if (available < qty) {
                    send(ex, "Only " + available + " left");
                    return;
                }
                stockMap.put(pid, available - qty);
                myInventory.put(pid,
                        myInventory.getOrDefault(pid, 0) + qty);
            }

            send(ex, "Added " + qty + " × " + p.getName());
        });

        /* ===== REMOVE PRODUCT ===== */
        server.createContext("/remove", ex -> {
            Map<String,String> q = parse(ex.getRequestURI().getQuery());
            int pid = parseInt(q.get("pid"));
            int qty = Math.max(1, parseInt(q.get("qty")));

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            Product p = port.getProductById(pid);

            synchronized (myInventory) {
                int have = myInventory.getOrDefault(pid, 0);
                if (have <= 0) {
                    send(ex, "Item not in inventory");
                    return;
                }

                int removeQty = Math.min(have, qty);
                int left = have - removeQty;

                if (left == 0) myInventory.remove(pid);
                else myInventory.put(pid, left);

                stockMap.put(pid,
                        stockMap.getOrDefault(pid, 0) + removeQty);
            }

            send(ex, "Removed " + qty + " × " + p.getName());
        });

        server.start();
        System.out.println("Server running at http://localhost:9000/");
    }

    /* ================= STORE ================= */

    static void renderStore(HttpExchange ex) throws IOException {

        InventoryService port =
                new InventoryServiceService().getInventoryServicePort();
        List<Product> products = port.getAllProducts();

        if (stockMap.isEmpty())
            for (Product p : products)
                stockMap.put(p.getId(), p.getStock());

        StringBuilder h = new StringBuilder();
        h.append(header("Store"));

        h.append("<div class='top'>")
         .append("<h1>Inventory Store</h1>")
         .append("<a class='btn-small' href='/my'>My Inventory</a>")
         .append("</div>");

        h.append("<div class='grid'>");

        for (Product p : products) {
            int stock = stockMap.getOrDefault(p.getId(), 0);
            boolean disabled = stock <= 0;

            h.append("<div class='card'>")
             .append("<div class='img'><img src='").append(img(p.getName())).append("'></div>")
             .append("<h3>").append(escape(p.getName())).append("</h3>")
             .append("<p>Price: ").append(p.getPrice()).append("</p>")
             .append("<p>Stock: ").append(stock).append("</p>")
             .append("<input type='number' min='1' value='1' id='add").append(p.getId()).append("' ")
             .append(disabled ? "disabled" : "").append(">")
             .append("<button class='add ").append(disabled ? "disabled" : "").append("' ")
             .append(disabled ? "disabled" : "")
             .append(" onclick=\"act('/order',").append(p.getId()).append(",'add')\">Add</button>")
             .append("</div>");
        }

        h.append("</div>");
        h.append(footer());
        send(ex, h.toString());
    }

    /* ================= INVENTORY ================= */

    static void renderInventory(HttpExchange ex) throws IOException {

        InventoryService port =
                new InventoryServiceService().getInventoryServicePort();

        StringBuilder h = new StringBuilder();
        h.append(header("My Inventory"));

        h.append("<div class='top'>")
         .append("<h1>My Inventory</h1>")
         .append("<a class='btn-small' href='/'>Back</a>")
         .append("</div>");

        h.append("<div class='grid'>");

        if (myInventory.isEmpty()) {
            h.append("<p class='empty'>No items added</p>");
        } else {
            for (Map.Entry<Integer,Integer> e : myInventory.entrySet()) {
                Product p = port.getProductById(e.getKey());

                h.append("<div class='card'>")
                 .append("<div class='img'><img src='").append(img(p.getName())).append("'></div>")
                 .append("<h3>").append(escape(p.getName())).append("</h3>")
                 .append("<p>Owned: ").append(e.getValue()).append("</p>")
                 .append("<input type='number' min='1' max='").append(e.getValue())
                 .append("' value='1' id='rem").append(p.getId()).append("'>")
                 .append("<button class='remove' onclick=\"act('/remove',")
                 .append(p.getId()).append(",'rem')\">Remove</button>")
                 .append("</div>");
            }
        }

        h.append("</div>");
        h.append(footer());
        send(ex, h.toString());
    }

    /* ================= UI ================= */

    static String header(String title) {
        return "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
        + "<style>"
        + "body{margin:0;font-family:Segoe UI;background:#f4f6f8}"
        + ".top{background:#111;color:#fff;padding:14px 18px;display:flex;justify-content:space-between;align-items:center}"
        + ".btn-small{background:#0b69ff;color:#fff;padding:6px 12px;border-radius:6px;font-size:14px;text-decoration:none;height:fit-content}"
        + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:16px;padding:16px}"
        + ".card{background:#fff;border-radius:10px;padding:12px;box-shadow:0 2px 6px rgba(0,0,0,.1)}"
        + ".img{height:160px;background:#eee;border-radius:8px;display:flex;align-items:center;justify-content:center}"
        + ".img img{width:100%;height:100%;object-fit:contain}"
        + "input{width:100%;padding:6px;margin-top:6px}"
        + "button{width:100%;padding:8px;margin-top:6px;border:none;border-radius:6px}"
        + ".add{background:#28a745;color:#fff}"
        + ".remove{background:#dc3545;color:#fff}"
        + ".disabled{background:#aaa !important;cursor:not-allowed}"
        + ".empty{text-align:center;color:#555}"
        + "#toast-root{position:fixed;bottom:20px;left:50%;transform:translateX(-50%);z-index:9999}"
        + ".toast{padding:12px 18px;border-radius:8px;color:#fff;margin-top:8px}"
        + ".ok{background:#28a745}.err{background:#dc3545}"
        + "</style>"

        + "<script>"
        + "function act(url,id,type){"
        + "let q=document.getElementById((type==='add'?'add':'rem')+id);"
        + "if(q.disabled) return;"
        + "fetch(url+'?pid='+id+'&qty='+q.value)"
        + ".then(r=>r.text()).then(t=>{showToast(t); setTimeout(refresh,1000);});}"
        + "function refresh(){fetch(location.pathname).then(r=>r.text()).then(t=>document.body.innerHTML=t);}"
        + "function showToast(msg){"
        + "let r=document.getElementById('toast-root');"
        + "let d=document.createElement('div');"
        + "d.className='toast '+(msg.includes('Added')||msg.includes('Removed')?'ok':'err');"
        + "d.innerText=msg;"
        + "r.appendChild(d);"
        + "setTimeout(()=>d.remove(),2500);}"
        + "</script>"

        + "</head><body><div id='toast-root'></div>";
    }

    static String footer() { return "</body></html>"; }

    /* ================= UTIL ================= */

    static String img(String n){
        n=n.toLowerCase();
        if(n.contains("head")) return "/images/headphones.jpg";
        if(n.contains("laptop")) return "/images/laptop.jpg";
        if(n.contains("mobile")||n.contains("phone")) return "/images/mobile.jpg";
        return "/images/default.jpg";
    }

    static void send(HttpExchange ex,String h)throws IOException{
        ex.sendResponseHeaders(200,h.getBytes().length);
        ex.getResponseBody().write(h.getBytes());
        ex.close();
    }

    static Map<String,String> parse(String q)throws UnsupportedEncodingException{
        Map<String,String> m=new HashMap<>();
        if(q==null) return m;
        for(String p:q.split("&")){
            String[] kv=p.split("=");
            m.put(kv[0],kv.length>1?URLDecoder.decode(kv[1],"UTF-8"):"");
        }
        return m;
    }

    static int parseInt(String s){ try{return Integer.parseInt(s);}catch(Exception e){return 0;} }
    static String escape(String s){ return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
