package com.shopping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "orders")  // plural recommended
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Orders {  // ← Keep name as "Orders" to avoid conflict

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private double total;

    @Column(nullable = false)
    private String status = "PENDING"; // Default

    @Column(nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "transaction_id")
    private String transactionId;
    // === RELATIONSHIPS ===

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusLog> statusLogs = new ArrayList<>();

  
}