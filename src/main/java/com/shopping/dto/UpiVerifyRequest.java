// src/main/java/com/shopping/dto/UpiVerifyRequest.java
package com.shopping.dto;

import lombok.Data;

@Data
public class UpiVerifyRequest {
    private Long orderId;
    private String transactionId;
    private String status; // SUCCESS, FAILED, PENDING
}