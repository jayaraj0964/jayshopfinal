package com.shopping.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpiPaymentResponse {
private Long orderId;
    private Double amount;
    private String qrCodeUrl;
}