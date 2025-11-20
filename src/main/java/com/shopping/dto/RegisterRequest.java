package com.shopping.dto;

public record RegisterRequest(
    String name,
    String email,
    String password,
    String phone // optional
) {}
