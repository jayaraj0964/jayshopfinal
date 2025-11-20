package com.shopping.controller;

import com.shopping.dto.OrderResponseDTO;
import com.shopping.dto.ProductDTO;
import com.shopping.dto.ProductResponse;
import com.shopping.dto.ProductUpdateDTO;
import com.shopping.entity.Product;
import com.shopping.entity.User;
import com.shopping.repository.UserRepository;
import com.shopping.service.OrderService;
import com.shopping.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final OrderService orderService;
    // REMOVE THIS LINE → DTO IS NOT A BEAN!
    // private final ProductUpdateDTO productUpdateDTO;

    // === PRODUCT APIs ===

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> uploadProduct(
        @RequestPart("data") ProductDTO dto,
        @RequestPart("mainFile") MultipartFile mainFile,
        @RequestPart(value = "extraFiles", required = false) List<MultipartFile> extraFiles
    ) throws IOException {
        ProductResponse response = productService.uploadProduct(dto, mainFile, extraFiles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // === UPDATE PRODUCT ===
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateDTO dto) {
        Product updated = productService.updateProduct(id, dto);
        return ResponseEntity.ok(updated);
    }

    // === DELETE PRODUCT ===
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

//==get all users===
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    // === USER ROLE MANAGEMENT ===
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<String> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newRole = body.get("role");
        if (newRole == null || !Set.of("USER", "ADMIN").contains(newRole.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body("Invalid role. Must be 'USER' or 'ADMIN'");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setRole(newRole.toUpperCase());
        userRepository.save(user);

        return ResponseEntity.ok("User role updated to " + newRole.toUpperCase());
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<OrderResponseDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // ADMIN: Get User Orders by User ID
    @GetMapping("/admin/users/{userId}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrdersAdmin(@PathVariable Long userId) {
        List<OrderResponseDTO> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }
}