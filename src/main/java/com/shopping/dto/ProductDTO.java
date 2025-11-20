package com.shopping.dto;

public record ProductDTO(
    String name,
    String description,
    double price,
    int stock,
    String category,
    String size,
    String color
) {}