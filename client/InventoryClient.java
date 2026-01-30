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

        /* ================= STATIC IMAGES ================= */
        server.createContext("/images", ex -> {
            File file = new File("." + ex.getRequestURI().getPath());
            if (!file.exists()) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            String type = Files.probeContentType(file.toPath());
            ex.getResponseHeaders().set("Content-Type",
                    type == null ? "image/jpeg" : type);
            byte[] data = Files.readAllBytes(file.toPath());
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
        });

        /* ================= STORE PAGE ================= */
        server.createContext("/", ex -> {

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            List<Product> products = port.getAllProducts();

            synchronized (stockMap) {
                if (stockMap.isEmpty()) {
                    for (Product p : products)
                        stockMap.put(p.getId(), p.getStock());
                }
            }

            StringBuilder html = new StringBuilder();
            html.append(header("Inventory Store"));

            html.append("<div class='top'>")
                .append("<h1>Inventory Store</h1>")
                .append("<a class='btn' href='/my'>My Inventory</a>")
                .append("</div>");

            html.append("<div class='grid'>");

            for (Product p : products) {
                int stock = stockMap.getOrDefault(p.getId(), 0);

                html.append("<div class='card'>")

                    .append("<div class='product-img'>")
                    .append("<img src='").append(productImage(p.getName())).append("'>")
                    .append("</div>")

                    .append("<h3>").append(escape(p.getName())).append("</h3>")
                    .append("<p class='meta'>ID ").append(p.getId()).append("</p>")
                    .append("<p>Price: ").append(p.getPrice()).append("</p>")
                    .append("<p>Available: ").append(stock).append("</p>");

                if (stock > 0) {
                    html.append("<form action='/order'>")
                        .append("<input type='hidden' name='pid' value='").append(p.getId()).append("'>")
                        .append("<input type='number' name='qty' min='1' value='1'>")
                        .append("<button class='add'>Add</button>")
                        .append("</form>");
                } else {
                    html.append("<p class='err'>Out of stock</p>");
                }

                html.append("</div>");
            }

            html.append("</div>");
            html.append(footer());
            send(ex, html.toString());
        });

        /* ================= ORDER ================= */
        server.createContext("/order", ex -> {

            Map<String, String> q = parse(ex.getRequestURI().getQuery());
            int pid = parseInt(q.get("pid"));
            int qty = Math.max(1, parseInt(q.get("qty")));

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            Product p = port.getProductById(pid);

            if (p == null) {
                send(ex, message("Product not found", "Unknown product", true));
                return;
            }

            synchronized (stockMap) {
                int available = stockMap.getOrDefault(pid, 0);
                if (available < qty) {
                    send(ex, message("Insufficient stock", p.getName(), true));
                    return;
                }
                stockMap.put(pid, available - qty);
                myInventory.put(pid,
                        myInventory.getOrDefault(pid, 0) + qty);
            }

            send(ex, message("Added to inventory (Qty " + qty + ")", p.getName(), false));
        });

        /* ================= REMOVE ================= */
        server.createContext("/remove", ex -> {

            Map<String, String> q = parse(ex.getRequestURI().getQuery());
            int pid = parseInt(q.get("pid"));

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            Product p = port.getProductById(pid);

            synchronized (myInventory) {
                int have = myInventory.getOrDefault(pid, 0);
                if (have <= 0) {
                    send(ex, message("Item not in inventory",
                            p != null ? p.getName() : "Unknown", true));
                    return;
                }
                myInventory.remove(pid);
                stockMap.put(pid,
                        stockMap.getOrDefault(pid, 0) + have);
            }

            send(ex, message("Removed from inventory",
                    p != null ? p.getName() : "Unknown", false));
        });

        /* ================= MY INVENTORY ================= */
        server.createContext("/my", ex -> {

            InventoryService port =
                    new InventoryServiceService().getInventoryServicePort();
            StringBuilder html = new StringBuilder();
            html.append(header("My Inventory"));

            html.append("<div class='top'>")
                .append("<h1>My Inventory</h1>")
                .append("<a class='btn' href='/'>Back</a>")
                .append("</div>");

            html.append("<div class='grid'>");

            if (myInventory.isEmpty()) {
                html.append("<p class='empty'>No products added.</p>");
            } else {
                for (Map.Entry<Integer, Integer> e : myInventory.entrySet()) {
                    Product p = port.getProductById(e.getKey());
                    if (p == null) continue;

                    html.append("<div class='card'>")

                        .append("<div class='product-img'>")
                        .append("<img src='").append(productImage(p.getName())).append("'>")
                        .append("</div>")

                        .append("<h3>").append(escape(p.getName())).append("</h3>")
                        .append("<p>Quantity: ").append(e.getValue()).append("</p>")
                        .append("<form action='/remove'>")
                        .append("<input type='hidden' name='pid' value='").append(p.getId()).append("'>")
                        .append("<button class='remove'>Remove</button>")
                        .append("</form>")
                        .append("</div>");
                }
            }

            html.append("</div>");
            html.append(footer());
            send(ex, html.toString());
        });

        server.start();
        System.out.println("Server running at http://localhost:9000/");
    }

    /* ================= HELPERS ================= */

    static String productImage(String name) {
        if (name == null) return "/images/default.jpg";
        name = name.toLowerCase();
        if (name.contains("laptop")) return "/images/laptop.jpg";
        if (name.contains("mobile")) return "/images/mobile.jpg";
        if (name.contains("headphone")) return "/images/headphone.jpg";
        return "/images/default.jpg";
    }

    static String message(String text, String product, boolean error) {
        return header("Message") +
            "<div class='msg'>" +
            "<h2 class='" + (error ? "err" : "ok") + "'>" + escape(text) + "</h2>" +
            "<p>" + escape(product) + "</p>" +
            "<a class='btn' href='/'>Home</a> <a class='btn' href='/my'>My Inventory</a>" +
            "</div>" +
            footer();
    }

    static void send(HttpExchange ex, String html) throws IOException {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    static Map<String, String> parse(String q) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (q == null) return map;
        for (String p : q.split("&")) {
            String[] kv = p.split("=");
            map.put(kv[0], kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "");
        }
        return map;
    }

    static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    static String escape(String s) {
        return s == null ? "" :
                s.replace("&","&amp;")
                 .replace("<","&lt;")
                 .replace(">","&gt;");
    }

    static String header(String title) {
        return "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>"
            + "body{margin:0;font-family:Segoe UI,Arial;background:#f4f6f8}"
            + ".top{display:flex;justify-content:space-between;align-items:center;padding:16px;background:#111;color:#fff}"
            + ".btn{background:#0b69ff;color:#fff;padding:8px 14px;border-radius:6px;text-decoration:none}"
            + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px;padding:16px}"
            + ".card{background:#fff;border-radius:10px;padding:12px;box-shadow:0 3px 8px rgba(0,0,0,.08)}"
            + ".product-img{width:100%;height:180px;background:#f0f0f0;display:flex;align-items:center;justify-content:center;border-radius:8px;overflow:hidden}"
            + ".product-img img{width:100%;height:100%;object-fit:contain}"
            + "input{padding:6px;width:100%}"
            + "button{padding:8px;border:none;border-radius:6px;cursor:pointer}"
            + ".add{background:#28a745;color:#fff}"
            + ".remove{background:#dc3545;color:#fff}"
            + ".err{color:#dc3545}.ok{color:#28a745}"
            + ".msg{text-align:center;padding:30px}"
            + ".empty{text-align:center;padding:20px;color:#555}"
            + "</style></head><body>";
    }

    static String footer() {
        return "</body></html>";
    }
}
