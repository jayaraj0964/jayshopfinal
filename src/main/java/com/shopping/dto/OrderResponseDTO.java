
package com.shopping.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private Long id;
    private double total;
    private String status;
    private LocalDateTime orderDate;
    private String shippingAddress;
    private List<OrderItemDTO> items;
    private Long userId;
}