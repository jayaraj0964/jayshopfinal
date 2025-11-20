
package com.shopping.dto;

import java.util.Base64;

import com.shopping.entity.ProductImage;

public record ProductImageResponse(
    Long id,
    String imageName,
    boolean isMain,
    String base64Image  // ← CAN BE null
) {
   public static ProductImageResponse fromEntity(ProductImage entity) {
        String base64 = entity.getImageData() != null
            ? "data:image/png;base64," + Base64.getEncoder().encodeToString(entity.getImageData())
            : null;

        return new ProductImageResponse(
            entity.getId(),
            entity.getImageName(),
            entity.isMain(),
            base64
        );
    }
}