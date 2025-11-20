package com.shopping.dto;

import java.util.List;

public record ProductUpdateDTO(
    String name,
    String description,
    Double price,
    Integer stock,
    List<Long> imageIdsToDelete // Optional: delete specific images
) {}