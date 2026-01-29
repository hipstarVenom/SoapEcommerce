package com.ecommerce.service;

import javax.xml.ws.Endpoint;

public class InventoryServer {

    public static void main(String[] args) {
        Endpoint.publish(
                "http://localhost:8080/inventory",
                new InventoryService()
        );
        System.out.println("SOAP E-Commerce Inventory Service running...");
    }
}
