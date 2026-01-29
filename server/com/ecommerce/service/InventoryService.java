package com.ecommerce.service;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class InventoryService {

    @WebMethod
    public Product getProductById(int productId) {

        switch (productId) {
            case 1:
                return createProduct(1, "Laptop", 60000, 5);
            case 2:
                return createProduct(2, "Mobile", 30000, 10);
            case 3:
                return createProduct(3, "Headphones", 2000, 25);
            default:
                return null;
        }
    }

    @WebMethod
    public Product[] getAllProducts() {

        Product p1 = createProduct(1, "Laptop", 60000, 5);
        Product p2 = createProduct(2, "Mobile", 30000, 10);
        Product p3 = createProduct(3, "Headphones", 2000, 25);

        return new Product[]{p1, p2, p3};
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
