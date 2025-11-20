package com.shopping.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.entity.Orders;
import com.shopping.entity.User;
import com.shopping.repository.OrderRepository;
import com.shopping.repository.UserRepository;
import com.shopping.service.CashfreeService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "https://jayshopy-ma48.vercel.app"})
public class PaymentController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CashfreeService cashfreeService;
    private final ObjectMapper objectMapper;  // JSON convert kosam

    // CREATE CARD + UPI PAYMENT (BOTH WORK!)
 @PostMapping({"/create-card-payment", "/create-upi-payment"})
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> req, Authentication auth) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get shipping & total
            Map<String, Object> shipping = (Map<String, Object>) req.get("shippingAddress");
            double total = ((Number) req.get("total")).doubleValue();

            if (shipping == null || total <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid data"));
            }

            // Create DB Order FIRST
            Orders order = new Orders();
            order.setUser(user);
            order.setShippingAddress(objectMapper.writeValueAsString(shipping));
            order.setTotal(total);
            order.setStatus("PENDING");
            order = orderRepository.save(order);

            // Create Cashfree Order
            CashfreeService.CreateOrderResult result = cashfreeService.createOrder(
                order.getId(),
                total,
                user.getEmail(),
                user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Customer",
                user.getPhone()
            );

            // FINAL RESPONSE – DB ORDER ID MANDATORY!
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("orderId", result.orderId);           // Cashfree ID: ORD_26
            res.put("dbOrderId", order.getId());          // DB ID: 26  ← POLLING KI IDI KAVALI
            res.put("amount", total);
            res.put("paymentSessionId", result.paymentSessionId); // For SDK
            res.put("qrCodeUrl", result.qrCodeUrl);
            // res.put("paymentLink", result.paymentLink);

            log.info("Payment created → DB ID: {} | Cashfree ID: {}", order.getId(), result.orderId);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            log.error("Payment creation failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }



  @GetMapping("/order-status/{id}")
public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String id, Authentication auth) {
    try {
        Long orderId;
        // Support both: 26 or ORD_26
        if (id.startsWith("ORD_")) {
            orderId = Long.parseLong(id.substring(4));
        } else {
            orderId = Long.parseLong(id);
        }

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Map<String, Object> res = new HashMap<>();
        res.put("status", order.getStatus());
        res.put("transactionId", order.getTransactionId() != null ? order.getTransactionId() : "");
        return ResponseEntity.ok(res);

    } catch (Exception e) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "PENDING");
        return ResponseEntity.ok(res);
    }
}
}
