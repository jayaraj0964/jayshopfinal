package com.shopping.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopping.entity.OrderStatusLog;

@Repository
public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {}
