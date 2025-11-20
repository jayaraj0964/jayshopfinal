// src/main/java/com/shopping/service/PaymentService.java
package com.shopping.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

  public Map<String, String> generateUpiLinks(double amountInRupees, String orderId) {
    long amountInPaise = Math.round(amountInRupees * 100);  // 2.00 → 200
    String desc = URLEncoder.encode("Order #" + orderId, StandardCharsets.UTF_8);
    String merchantName = URLEncoder.encode("YourShop", StandardCharsets.UTF_8);
    String upiId = "7989951894@ybl";  

    Map<String, String> links = new HashMap<>();

    links.put("generic", String.format(
        "upi://pay?pa=%s&pn=%s&am=%d&cu=INR&tn=%s&tr=%s",
        upiId, merchantName, amountInPaise, desc, orderId
    ));

    links.put("phonepe", String.format(
        "phonepe://pay?pa=%s&pn=%s&am=%d&cu=INR&tn=%s&tr=%s",
        upiId, merchantName, amountInPaise, desc, orderId
    ));

    links.put("gpay", String.format(
        "tez://upi/pay?pa=%s&pn=%s&am=%d&cu=INR&tn=%s&tr=%s",
        upiId, merchantName, amountInPaise, desc, orderId
    ));

    return links;
}

    // GENERATE QR FROM UPI LINK (Fallback - External Server)
   public String generateQrCodeString(String upiUrl) {
    return "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=" + 
           URLEncoder.encode(upiUrl, StandardCharsets.UTF_8);
}
}