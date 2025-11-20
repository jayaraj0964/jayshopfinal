package com.shopping.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageBase64;
    private double price;
    private int quantity;
}