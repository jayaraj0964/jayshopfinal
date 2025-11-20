package com.shopping.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.shopping.config.JwtAuthenticationFilter;
import com.shopping.dto.CartResponseDTO;
import com.shopping.dto.CheckoutRequest;
import com.shopping.dto.OrderRequestDTO;
import com.shopping.dto.OrderResponseDTO;
import com.shopping.dto.ProductResponse;
import com.shopping.dto.QuantityRequest;
import com.shopping.entity.Cart;
import com.shopping.entity.Orders;
import com.shopping.entity.Product;
import com.shopping.entity.User;
import com.shopping.repository.OrderRepository;
import com.shopping.repository.UserRepository;
import com.shopping.service.CartService;
import com.shopping.service.OrderService;
import com.shopping.service.ProductService;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepo;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);


   @GetMapping("/products")
public ResponseEntity<List<ProductResponse>> getAllProducts() {
    return ResponseEntity.ok(productService.getAllProducts());
}
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));

    }

    
    @PostMapping("/cart/{productId}")
public ResponseEntity<String> addToCart(
        @PathVariable Long productId,
        @RequestBody Map<String, Integer> request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    log.info("=== ADD TO CART ===");
    log.info("Product ID: {}", productId);
    log.info("UserDetails: {}", userDetails);

    if (userDetails == null) {
        log.error("UserDetails is NULL → 401");
        return ResponseEntity.status(401).body("Unauthorized");
    }

    String email = userDetails.getUsername();
    log.info("Authenticated user email: {}", email);

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("User not found in DB: {}", email);
                return new RuntimeException("User not found");
            });

    log.info("User found in DB: ID = {}", user.getId());

    Integer quantity = request.getOrDefault("quantity", 1);
    cartService.addToCart(user.getId(), productId, quantity);

    log.info("Item added to cart successfully");
    return ResponseEntity.ok("Added to cart!");
}

@GetMapping("/cart")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<CartResponseDTO> getCart(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
        return ResponseEntity.status(401).build();
    }

    String email = userDetails.getUsername();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    CartResponseDTO cartDTO = cartService.getCartDTO(user.getId());  // ← Use DTO method
    return ResponseEntity.ok(cartDTO);
}
@PutMapping("/cart/{productId}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<String> updateCartItem(
        @PathVariable Long productId,
        @RequestBody QuantityRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {

    String email = userDetails.getUsername();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    cartService.updateQuantity(user.getId(), productId, request.getQuantity());
    return ResponseEntity.ok("Quantity updated!");
}

// REMOVE ITEM
@DeleteMapping("/cart/{productId}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<String> removeFromCart(
        @PathVariable Long productId,
        @AuthenticationPrincipal UserDetails userDetails) {

    String email = userDetails.getUsername();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    cartService.removeItem(user.getId(), productId);
    return ResponseEntity.ok("Item removed!");
}
  @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<OrderResponseDTO> orders = orderService.getUserOrders(user.getId());
        return ResponseEntity.ok(orders);
    }

 @PostMapping("/checkout")
public ResponseEntity<Map<String, Object>> checkout(
        @RequestBody OrderRequestDTO request,
        @AuthenticationPrincipal UserDetails userDetails) {

    String email = userDetails.getUsername();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    // ✅ Get cart total from CartService
    CartResponseDTO cartDTO = cartService.getCartDTO(user.getId());
    double cartTotal = cartDTO.getTotalPrice();  

    // Create pending order
    Orders order = new Orders();
    order.setUser(user);
    order.setShippingAddress(request.getShippingAddress());
    order.setStatus("PENDING");
    order.setTotal(cartTotal);  // ✅ Use actual cart total
    order.setOrderDate(java.time.LocalDateTime.now());
    order = orderRepo.save(order);

    Map<String, Object> res = new HashMap<>();
    res.put("orderId", order.getId());
    res.put("amount", order.getTotal());  // No need to divide by 100

    return ResponseEntity.ok(res);
}


}