package com.shopping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.config.CashfreeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashfreeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CashfreeConfig cashfreeConfig;

    public static class CreateOrderResult {
        public String orderId;
        public Double amount;
        public String qrCodeUrl;
        public String paymentSessionId;
        public String paymentLink;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", cashfreeConfig.getAppId());
        headers.set("x-client-secret", cashfreeConfig.getSecretKey());
        headers.set("x-api-version", "2025-01-01");  // Latest working version
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

   // ONLY THIS BLOCK REPLACE CHEY – BAKI SAME
public CreateOrderResult createOrder(Long dbOrderId, double amount, String email, String name, String phone) {
    log.info("CASHFREE ORDER CREATION – PRODUCTION MODE");
    log.info("DB ID: {} | Amount: ₹{} | User: {} ({}) | Phone: {}", dbOrderId, amount, name, email, phone);

    String url = "https://api.cashfree.com/pg/orders";
    String orderId = "ORD_" + dbOrderId;

    Map<String, Object> body = new HashMap<>();
    body.put("order_id", orderId);
    body.put("order_amount", amount);
    body.put("order_currency", "INR");

    Map<String, Object> customer = new HashMap<>();
    customer.put("customer_id", "cust_" + dbOrderId);
    customer.put("customer_name", name);
    customer.put("customer_email", email);
    customer.put("customer_phone", phone);
    body.put("customer_details", customer);

    Map<String, Object> meta = new HashMap<>();
    meta.put("return_url", "https://jayshopy-ma48.vercel.app/order-success?order_id=" + orderId);
    meta.put("notify_url", "https://jayshopfinal.onrender.com/jay/webhook/cashfree");
    body.put("order_meta", meta);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, getHeaders());

    try {
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        log.info("CASHFREE RESPONSE: {}", response.getBody());

        JsonNode root = objectMapper.readTree(response.getBody());

        CreateOrderResult result = new CreateOrderResult();
        result.orderId = root.path("order_id").asText();
        result.amount = amount;
        result.paymentSessionId = root.path("payment_session_id").asText();
        result.qrCodeUrl = generateUpiQr(orderId, amount, cashfreeConfig.getMerchantUpiId()); // Your fallback QR

        if (result.paymentSessionId == null || result.paymentSessionId.isEmpty()) {
            throw new RuntimeException("No payment_session_id – check keys");
        }

        log.info("PRODUCTION SESSION ID READY: {}", result.paymentSessionId);
        return result;

    } catch (Exception e) {
        log.error("CASHFREE ERROR", e);
        throw new RuntimeException("Payment setup failed: " + e.getMessage());
    }
}

// Helper for UPI QR (keep your existing)
private String generateUpiQr(String orderId, double amount, String vpa) {
    if (vpa == null) return null;
    String upi = String.format("upi://pay?pa=%s&pn=JayShoppy&am=%.2f&cu=INR&tr=%s", vpa, amount, orderId);
    return "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + URLEncoder.encode(upi, StandardCharsets.UTF_8);
}

    // WEBHOOK VERIFICATION
   public boolean verifyWebhookSignature(String payload, String receivedSignature, String timestamp) {
    try {
        // Cashfree docs: signature = Base64(HMAC_SHA256(timestamp + "." + payload, secretKey))
        String data = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                cashfreeConfig.getSecretKey().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(key);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String computedSignature = Base64.getEncoder().encodeToString(hash);

        // Debug logs
        log.info("Webhook Signature Verification -> Timestamp: {}", timestamp);
        log.info("Payload: {}", payload);
        log.info("Received Signature: {}", receivedSignature);
        log.info("Computed Signature: {}", computedSignature);

        boolean match = computedSignature.equals(receivedSignature);
        if (!match) {
            log.warn("Signature mismatch! Webhook rejected.");
        }
        return match;

    } catch (Exception e) {
        log.error("Webhook signature verification failed", e);
        return false;
    }
}

}