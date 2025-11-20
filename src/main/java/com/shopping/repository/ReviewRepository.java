package com.shopping.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shopping.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {}