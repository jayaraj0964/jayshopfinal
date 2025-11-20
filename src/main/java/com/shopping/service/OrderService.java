// src/main/java/com/shopping/service/OrderService.java
package com.shopping.service;

import com.shopping.dto.*;
import com.shopping.entity.*;
import com.shopping.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartRepository cartRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final CartItemRepository cartItemRepo;

    // ============================= 1. CREATE PENDING ORDER (CheckoutRequest) =============================
    @Transactional
    public Orders createPendingOrder(Long userId, CheckoutRequest request) {
        Cart cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

     Orders order = new Orders();
    order.setUser(cart.getUser());
    double totalRupees = calculateTotal(cart);  // Calculate in rupees
    order.setTotal(totalRupees * 100);  // ← STORE IN PAISE (2.00 * 100 = 200.0)
    order.setStatus("PENDING");
    order.setOrderDate(LocalDateTime.now());
    order.setShippingAddress(request.getShippingAddress());
    order.setItems(new ArrayList<>());
        Orders savedOrder = orderRepo.save(order);

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPriceAtPurchase(ci.getProduct().getPrice());
            orderItemRepo.save(oi);
            savedOrder.getItems().add(oi);
        }

        notificationService.sendOrderUpdate(savedOrder);
        return savedOrder;
    }

    // ============================= 1.1 OVERLOADED: CREATE PENDING ORDER (UpiPaymentRequest) =============================
    @Transactional
    public Orders createPendingOrder(Long userId, UpiPaymentRequest request) {
        // Reuse CheckoutRequest logic
        CheckoutRequest checkoutReq = new CheckoutRequest();
        checkoutReq.setShippingAddress(request.getShippingAddress());
        return createPendingOrder(userId, checkoutReq);
    }

    // ============================= 2. CONFIRM ORDER AFTER PAYMENT =============================
    @Transactional
    public Orders confirmOrder(Long orderId, String paymentIntentId) {
        Orders order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Order already processed");
        }

        order.setStatus("PAID");
        order.setTransactionId(paymentIntentId);
        Orders updatedOrder = orderRepo.save(order);

        // CLEAR CART
        Cart cart = cartRepo.findByUserId(order.getUser().getId()).orElse(null);
        if (cart != null) {
            cartItemRepo.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepo.saveAndFlush(cart);
        }

        notificationService.sendOrderUpdate(updatedOrder);
        return updatedOrder;
    }

    // ============================= 3. CANCEL ORDER (USER) =============================
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Orders order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        if (!List.of("PENDING", "PAID").contains(order.getStatus())) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");
        orderRepo.save(order);

        // RESTORE STOCK
        for (OrderItem oi : order.getItems()) {
            Product p = oi.getProduct();
            p.setStock(p.getStock() + oi.getQuantity());
            productRepo.save(p);
        }

        notificationService.sendOrderUpdate(order);
    }

    // ============================= 4. UPDATE STATUS (ADMIN) =============================
    @Transactional
    public Orders updateOrderStatus(Long orderId, String newStatus) {
        Orders order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!List.of("PENDING", "PAID", "SHIPPED", "DELIVERED", "CANCELLED").contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }

        order.setStatus(newStatus);
        Orders updated = orderRepo.save(order);
        notificationService.sendOrderUpdate(updated);
        return updated;
    }

    // ============================= 5. GET USER ORDERS (WITH IMAGES) =============================
    @Transactional
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        List<Orders> orders = orderRepo.findByUserIdOrderByOrderDateDesc(userId);
        return orders.stream().map(this::toDTO).toList();
    }

    // ============================= 6. GET SINGLE ORDER (USER) =============================
    @Transactional
    public OrderResponseDTO getOrderById(Long orderId, Long userId) {
        Orders order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toDTO(order);
    }

    // ============================= 7. GET ALL ORDERS (ADMIN) =============================
    @Transactional
    public List<OrderResponseDTO> getAllOrders() {
        List<Orders> orders = orderRepo.findAllByOrderByOrderDateDesc();
        return orders.stream().map(this::toDTO).toList();
    }

    // ============================= 8. CONVERT TO DTO WITH IMAGE =============================
    private OrderResponseDTO toDTO(Orders order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setUserId(order.getUser().getId());

        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(this::toOrderItemDTO)
                .toList();

        dto.setItems(itemDTOs);
        return dto;
    }

    // ============================= 9. CONVERT ITEM TO DTO WITH IMAGE =============================
    private OrderItemDTO toOrderItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        Product product = item.getProduct();

        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setQuantity(item.getQuantity());
        dto.setPriceAtPurchase(item.getPriceAtPurchase());

        // === FETCH IMAGE FROM PRODUCT ===
        String base64Image = null;
        List<ProductImage> images = product.getImages();

        if (images != null && !images.isEmpty()) {
            ProductImage mainImage = images.stream()
                    .filter(ProductImage::isMain)
                    .findFirst()
                    .orElse(images.get(0));

            if (mainImage.getImageData() != null && mainImage.getImageData().length > 0) {
                String mimeType = getMimeType(mainImage.getImageName());
                base64Image = "data:" + mimeType + ";base64," +
                        Base64.getEncoder().encodeToString(mainImage.getImageData());
            }
        }

        if (base64Image == null) {
            base64Image = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjUwIiB5PSI1MCIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=";
        }

        dto.setProductImageBase64(base64Image);
        return dto;
    }

    private String getMimeType(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "image/jpeg";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "image/jpeg";
        };
    }

    // ============================= 10. CALCULATE TOTAL =============================
    private double calculateTotal(Cart cart) {
        if (cart == null || cart.getItems() == null) return 0.0;
        return cart.getItems().stream()
                .filter(item -> item.getProduct() != null)
                .mapToDouble(item -> {
                    Double price = item.getProduct().getPrice();
                    return (price != null ? price : 0.0) * item.getQuantity();
                })
                .sum();
    }

    // ============================= 11. FIND USER BY EMAIL =============================
    public User findUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ============================= 12. NOTIFY UPDATE (Optional) =============================
    public void notifyOrderUpdate(Orders order) {
        notificationService.sendOrderUpdate(order);
    }
}