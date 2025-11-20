// src/main/java/com/shopping/dto/ProductResponse.java
package com.shopping.dto;

import com.shopping.entity.Product;
import com.shopping.entity.ProductImage;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public record ProductResponse(
    Long id,
    String name,
    String description,
    Double price,
    Integer stock,
    String category,
    String size,
    String color,
    List<ProductImageResponse> images  // ← Full list with base64
) {
    public ProductResponse(Product product) {
        this(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCategory(),
            product.getSize(),
            product.getColor(),
            product.getImages() != null
                ? product.getImages().stream()
                    .map(ProductImageResponse::fromEntity)
                    .collect(Collectors.toList())
                : List.of()
        );
    }

    // Helper to get first image base64 (for cart-like preview)
    public String getFirstImageBase64() {
        return images != null && !images.isEmpty() && images.get(0).base64Image() != null
            ? images.get(0).base64Image()
            : null;
    }
}