package com.ecommerce.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class InventoryService {

    @WebMethod
    public Product getProductById(int productId) {

        switch (productId) {
            case 1:
                return createProduct(1, "Mechanical Keyboard (Hot-Swap)", 8500, 12);
            case 2:
                return createProduct(2, "Noise-Canceling Earbuds", 6500, 20);
            case 3:
                return createProduct(3, "Smart Desk Lamp (AI Sensor)", 4200, 15);
            case 4:
                return createProduct(4, "Portable SSD (1TB)", 7200, 18);
            case 5:
                return createProduct(5, "Ergonomic Office Chair", 14500, 6);
            case 6:
                return createProduct(6, "Smart Water Bottle (Hydration Tracker)", 3800, 25);
            default:
                return null;
        }
    }

    @WebMethod
    public Product[] getAllProducts() {

        Product p1 = createProduct(1, "Mechanical Keyboard (Hot-Swap)", 8500, 12);
        Product p2 = createProduct(2, "Noise-Canceling Earbuds", 6500, 20);
        Product p3 = createProduct(3, "Smart Desk Lamp (AI Sensor)", 4200, 15);
        Product p4 = createProduct(4, "Portable SSD (1TB)", 7200, 18);
        Product p5 = createProduct(5, "Ergonomic Office Chair", 14500, 6);
        Product p6 = createProduct(6, "Smart Water Bottle (Hydration Tracker)", 3800, 25);

        return new Product[]{p1, p2, p3, p4, p5, p6};
    }

    private Product createProduct(int id, String name, int price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setStock(stock);
        return p;
    }
}
