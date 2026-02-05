import com.ecommerce.service.InventoryService;
import com.ecommerce.service.InventoryServiceService;
import com.ecommerce.service.Product;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryClient {

    // Stock maintained on web-server side
    static Map<Integer, Integer> stockMap = new HashMap<>();

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(9000), 0);

        // ================= HOME PAGE =================
        server.createContext("/", exchange -> {

            InventoryServiceService service =
                    new InventoryServiceService();
            InventoryService port =
                    service.getInventoryServicePort();

            List<Product> products = port.getAllProducts();

            // Initialize stock once
            if (stockMap.isEmpty()) {
                for (Product p : products) {
                    stockMap.put(p.getId(), p.getStock());
                }
            }

            StringBuilder html = new StringBuilder();

            html.append("<html>")
                .append("<head>")
                .append("<title>Inventory Store</title>")
                .append("<style>")
                .append("body{font-family:Arial;background:#f4f6f8;margin:0;padding:0;}")
                .append("h2{text-align:center;margin:20px 0;}")
                .append(".container{width:90%;max-width:1100px;margin:auto;}")
                .append(".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:20px;}")
                .append(".card{background:#fff;padding:20px;border-radius:10px;box-shadow:0 4px 10px rgba(0,0,0,0.1);}")
                .append(".name{font-size:18px;font-weight:bold;}")
                .append(".price{color:#2c7be5;margin:8px 0;}")
                .append(".stock{margin-bottom:12px;font-size:14px;}")
                .append(".in{color:green;font-weight:bold;}")
                .append(".out{color:red;font-weight:bold;}")
                .append(".btn{display:inline-block;padding:10px 15px;background:#2c7be5;color:#fff;text-decoration:none;border-radius:6px;}")
                .append(".btn:hover{background:#1a5fc4;}")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<h2>Inventory Store</h2>")
                .append("<div class='container'><div class='grid'>");

            for (Product p : products) {
                int stock = stockMap.get(p.getId());

                html.append("<div class='card'>")
                    .append("<div class='name'>").append(p.getName()).append("</div>")
                    .append("<div class='price'>").append(p.getPrice()).append("</div>")
                    .append("<div class='stock'>Stock: ");

                if (stock > 0) {
                    html.append("<span class='in'>")
                        .append(stock).append(" Available</span></div>")
                        .append("<a class='btn' href='/order?pid=")
                        .append(p.getId()).append("'>Buy Now</a>");
                } else {
                    html.append("<span class='out'>Out of Stock</span></div>");
                }

                html.append("</div>");
            }

            html.append("</div></div></body></html>");

            send(exchange, html.toString());
        });

        // ================= ORDER PAGE =================
        server.createContext("/order", exchange -> {

            Map<String, String> params =
                    parseQuery(exchange.getRequestURI().getQuery());

            int pid = Integer.parseInt(params.get("pid"));
            String response;

            // ---------- OUT OF STOCK PAGE ----------
            if (!stockMap.containsKey(pid) || stockMap.get(pid) <= 0) {

                response =
                    "<html>" +
                    "<head>" +
                    "<title>Out of Stock</title>" +
                    "<style>" +
                    "body{font-family:Arial;background:#f4f6f8;margin:0;padding:0;}" +
                    ".container{width:90%;max-width:600px;margin:60px auto;}" +
                    ".card{background:#fff;padding:30px;border-radius:10px;" +
                    "box-shadow:0 4px 10px rgba(0,0,0,0.1);text-align:center;}" +
                    "h2{color:red;margin-bottom:15px;}" +
                    ".btn{display:inline-block;margin-top:20px;padding:10px 20px;" +
                    "background:#2c7be5;color:#fff;text-decoration:none;border-radius:6px;}" +
                    ".btn:hover{background:#1a5fc4;}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='card'>" +
                    "<h2>Out of Stock</h2>" +
                    "<p>This product is currently unavailable.</p>" +
                    "<a class='btn' href='/'>Back to Store</a>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            } else {

                // Reduce stock
                stockMap.put(pid, stockMap.get(pid) - 1);

                InventoryServiceService service =
                        new InventoryServiceService();
                InventoryService port =
                        service.getInventoryServicePort();

                Product p = port.getProductById(pid);

                // ---------- ORDER SUCCESS PAGE ----------
                response =
                    "<html>" +
                    "<head>" +
                    "<title>Order Confirmation</title>" +
                    "<style>" +
                    "body{font-family:Arial;background:#f4f6f8;margin:0;padding:0;}" +
                    ".container{width:90%;max-width:600px;margin:60px auto;}" +
                    ".card{background:#fff;padding:30px;border-radius:10px;" +
                    "box-shadow:0 4px 10px rgba(0,0,0,0.1);text-align:center;}" +
                    "h2{color:#2c7be5;margin-bottom:20px;}" +
                    ".row{margin:10px 0;font-size:16px;}" +
                    ".label{font-weight:bold;}" +
                    ".btn{display:inline-block;margin-top:20px;padding:10px 20px;" +
                    "background:#2c7be5;color:#fff;text-decoration:none;border-radius:6px;}" +
                    ".btn:hover{background:#1a5fc4;}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='card'>" +
                    "<h2>Order Successful</h2>" +
                    "<div class='row'><span class='label'>Product:</span> " + p.getName() + "</div>" +
                    "<div class='row'><span class='label'>Price:</span> " + p.getPrice() + "</div>" +
                    "<div class='row'><span class='label'>Remaining Stock:</span> " + stockMap.get(pid) + "</div>" +
                    "<a class='btn' href='/'>Back to Store</a>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            }

            send(exchange, response);
        });

        server.start();
        System.out.println("Web server running at http://localhost:9000/");
    }

    // ================= UTILITIES =================
    private static void send(HttpExchange ex, String html) throws IOException {
        ex.sendResponseHeaders(200, html.getBytes().length);
        ex.getResponseBody().write(html.getBytes());
        ex.close();
    }

    private static Map<String, String> parseQuery(String query) throws IOException {

        Map<String, String> map = new HashMap<>();
        if (query == null) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            map.put(parts[0], URLDecoder.decode(parts[1], "UTF-8"));
        }
        return map;
    }
}
