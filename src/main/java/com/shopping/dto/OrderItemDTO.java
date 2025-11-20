// src/main/java/com/shopping/dto/OrderItemDTO.java
package com.shopping.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private int quantity;
    private String productImageBase64;  
    private double priceAtPurchase;
}