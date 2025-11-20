package com.shopping.service;

import com.shopping.dto.CartItemDTO;
import com.shopping.dto.CartResponseDTO;
import com.shopping.entity.*;
import com.shopping.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ========================= ADD TO CART =========================
    @Transactional
    public void addToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(user));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // CHECK STOCK BEFORE ANYTHING
        if (product.getStock() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStock() + " " + product.getName() + " left. Cannot add " + quantity + ".");
        }

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            if (product.getStock() < newQty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only " + product.getStock() + " " + product.getName() + " available. Cannot add more.");
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // REDUCE STOCK IMMEDIATELY
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        cartRepository.save(cart);
        notificationService.sendCartUpdate(userId);
        notificationService.sendStockAlert(product);
    }

    private Cart createNewCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    // ========================= GET CART DTO =========================
    @Transactional(readOnly = true)
    public CartResponseDTO getCartDTO(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        CartResponseDTO dto = new CartResponseDTO();
        dto.setItems(new ArrayList<>());
        dto.setTotalPrice(0.0);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUserEmail(user.getEmail());
        }

        if (cart == null) return dto;

        dto.setId(cart.getId());
        List<CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(this::toCartItemDTO)
                .toList();

        dto.setItems(itemDTOs);
        dto.setTotalPrice(calculateTotal(itemDTOs));
        return dto;
    }

    private CartItemDTO toCartItemDTO(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setPrice(item.getProduct().getPrice());
        dto.setQuantity(item.getQuantity());

        String base64Image = null;
        List<ProductImage> images = item.getProduct().getImages();

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

    private double calculateTotal(List<CartItemDTO> items) {
        return items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
    }

    // ========================= UPDATE QUANTITY =========================
    @Transactional
    public void updateQuantity(Long userId, Long productId, int newQuantity) {
        if (newQuantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        Product product = item.getProduct();

        // RESTORE OLD STOCK
        int oldQty = item.getQuantity();
        product.setStock(product.getStock() + oldQty);
        productRepository.save(product);

        if (newQuantity == 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            if (product.getStock() < newQuantity) {
                // REVERT STOCK CHANGE
                product.setStock(product.getStock() - oldQty);
                productRepository.save(product);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only " + product.getStock() + " " + product.getName() + " available.");
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);

            // DEDUCT NEW STOCK
            product.setStock(product.getStock() - newQuantity);
            productRepository.save(product);
        }

        cartRepository.save(cart);
        notificationService.sendCartUpdate(userId);
    }

    // ========================= REMOVE ITEM =========================
    @Transactional
    public void removeItem(Long userId, Long productId) {
        updateQuantity(userId, productId, 0);
    }

    // ========================= CLEAR CART =========================
   @Transactional
public void clearCart(Long userId) {
    Cart cart = cartRepository.findByUserId(userId).orElse(null);
    if (cart != null) {
        // DELETE ALL CART ITEMS FROM DB
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
    }
    notificationService.sendCartUpdate(userId);
}
    // ========================= INTERNAL: GET RAW CART =========================
    @Transactional(readOnly = true)
    public Cart getCart(Long userId) {
        return cartRepository.findByUserId(userId).orElse(null);
    }


    // Add this method at the end of CartService.java
@Transactional
public void updateCartCount(Long userId) {
    Cart cart = cartRepository.findByUserId(userId).orElse(null);
    int itemCount = 0;
    if (cart != null && cart.getItems() != null) {
        itemCount = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    // You can store in Redis, DB, or send via WebSocket
    // For now, just log or notify
    notificationService.sendCartCountUpdate(userId, itemCount);
}
}