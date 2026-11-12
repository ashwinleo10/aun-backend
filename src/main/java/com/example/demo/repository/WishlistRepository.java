
package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Wishlist;

@Repository
public interface WishlistRepository
        extends JpaRepository<Wishlist, Integer> {

    // Find wishlist belonging to a customer
    Optional<Wishlist> findByUser_UserId(Integer userId);
}
