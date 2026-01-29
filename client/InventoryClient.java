import com.ecommerce.service.InventoryService;
import com.ecommerce.service.InventoryServiceService;
import com.ecommerce.service.Product;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

public class InventoryClient {

    // Stock maintained on web-server side
    static Map<Integer, Integer> stockMap = new HashMap<>();

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(9000), 0);

        // HOME PAGE – FETCH FROM SOAP & DISPLAY
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
            html.append("<html><body style='font-family:Arial;text-align:center'>");
            html.append("<h2>Product Menu</h2>");

            for (Product p : products) {
                int stock = stockMap.get(p.getId());

                html.append("<div style='margin:15px'>")
                    .append("<b>").append(p.getName()).append("</b>")
                    .append(" | ").append(p.getPrice())
                    .append(" | Stock: ").append(stock);

                if (stock > 0) {
                    html.append(" <a href='/order?pid=")
                        .append(p.getId())
                        .append("'>Order</a>");
                } else {
                    html.append(" <span style='color:red'>Out of Stock</span>");
                }

                html.append("</div>");
            }

            html.append("</body></html>");

            send(exchange, html.toString());
        });

        // ORDER HANDLER
        server.createContext("/order", exchange -> {

            Map<String, String> params =
                    parseQuery(exchange.getRequestURI().getQuery());

            int pid = Integer.parseInt(params.get("pid"));
            String response;

            if (!stockMap.containsKey(pid) || stockMap.get(pid) <= 0) {
                response =
                    "<html><body style='font-family:Arial;text-align:center'>" +
                    "<h2 style='color:red'>Out of Stock</h2>" +
                    "<a href='/'>Back</a>" +
                    "</body></html>";
            } else {

                // Reduce stock
                stockMap.put(pid, stockMap.get(pid) - 1);

                InventoryServiceService service =
                        new InventoryServiceService();
                InventoryService port =
                        service.getInventoryServicePort();

                Product p = port.getProductById(pid);

                response =
                    "<html><body style='font-family:Arial;text-align:center'>" +
                    "<h2>Order Successful</h2>" +
                    "<p>Product: " + p.getName() + "</p>" +
                    "<p>Price: " + p.getPrice() + "</p>" +
                    "<p>Remaining Stock: " + stockMap.get(pid) + "</p>" +
                    "<br><a href='/'>Back to Menu</a>" +
                    "</body></html>";
            }

            send(exchange, response);
        });

        server.start();
        System.out.println("Web server running at http://localhost:9000/");
    }

    // Utility to send HTML
    private static void send(HttpExchange ex, String html) throws IOException {
        ex.sendResponseHeaders(200, html.length());
        ex.getResponseBody().write(html.getBytes());
        ex.close();
    }

    // Parse URL params
    private static Map<String, String> parseQuery(String query)
            throws UnsupportedEncodingException {

        Map<String, String> map = new HashMap<>();
        if (query == null) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            map.put(parts[0],
                    URLDecoder.decode(parts[1], "UTF-8"));
        }
        return map;
    }
}
