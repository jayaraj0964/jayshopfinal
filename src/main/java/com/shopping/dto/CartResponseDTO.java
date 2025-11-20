package com.shopping.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartResponseDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    
    private List<CartItemDTO> items = new ArrayList<>();
    private double totalPrice = 0.0;
}