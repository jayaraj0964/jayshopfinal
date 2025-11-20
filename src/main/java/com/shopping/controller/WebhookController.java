package com.shopping.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.entity.Orders;
import com.shopping.repository.OrderRepository;
import com.shopping.service.CashfreeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final CashfreeService cashfreeService;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/jay/webhook/cashfree")
    public ResponseEntity<String> handleCashfreeWebhook(
            @RequestBody String body,
            @RequestHeader("x-webhook-timestamp") String timestamp,
            @RequestHeader("x-webhook-signature") String signature,
            HttpServletRequest request) {

        log.info("=== CASHFREE WEBHOOK HIT ===");
        log.info("IP: {} | Timestamp: {}", request.getRemoteAddr(), timestamp);
        log.info("Payload: {}", body);

        // Verify signature
        if (!cashfreeService.verifyWebhookSignature(body, signature, timestamp)) {
            log.warn("Invalid webhook signature. Computed signature did not match.");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            JsonNode json = objectMapper.readTree(body);
            String eventType = json.path("type").asText();
            String orderId = json.path("data").path("order").path("order_id").asText("");
            String paymentId = json.path("data").path("payment").path("payment_id").asText("");

            log.info("Webhook Event: {} | Cashfree OrderId: {} | PaymentId: {}", eventType, orderId, paymentId);

            if (orderId.isEmpty()) {
                log.error("No order_id in webhook payload");
                return ResponseEntity.ok("Ignored");
            }

            Long dbId;
            try {
                // Strip "ORD_" and any non-numeric characters
                dbId = Long.parseLong(orderId.replace("ORD_", "").replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                log.error("Failed to parse dbOrderId from {}", orderId, e);
                return ResponseEntity.ok("Ignored");
            }

            Orders order = orderRepository.findById(dbId).orElse(null);
            if (order == null) {
                log.error("Order not found in DB: {}", dbId);
                return ResponseEntity.ok("Ignored");
            }

            log.info("DB Order {} current status: {}", dbId, order.getStatus());

            // Handle multiple possible event types
            if ("PAYMENT_SUCCESS".equalsIgnoreCase(eventType) || "order.paid".equalsIgnoreCase(eventType)) {
                order.setStatus("PAID");
                order.setTransactionId(paymentId);
                orderRepository.saveAndFlush(order);
                log.info("Order {} marked as PAID", dbId);
            } else if ("PAYMENT_FAILED".equalsIgnoreCase(eventType) || "payment.failed".equalsIgnoreCase(eventType)) {
                order.setStatus("FAILED");
                order.setTransactionId(paymentId);
                orderRepository.saveAndFlush(order);
                log.info("Order {} marked as FAILED", dbId);
            } else {
                log.info("Unhandled event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
        }

        return ResponseEntity.ok("OK");
    }
}
