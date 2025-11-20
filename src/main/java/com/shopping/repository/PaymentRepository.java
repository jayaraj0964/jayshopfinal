package com.shopping.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopping.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {}