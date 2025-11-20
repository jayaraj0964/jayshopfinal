// src/main/java/com/shopping/dto/UpiPaymentRequest.java
package com.shopping.dto;

import lombok.Data;

@Data
public class UpiPaymentRequest extends OrderRequestDTO {
    private Long orderId;
    // private String vpa;  // UPI ID like "1234567890@ybl"
}