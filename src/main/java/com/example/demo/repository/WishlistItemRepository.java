
package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.WishlistItem;

@Repository
public interface WishlistItemRepository
        extends JpaRepository<WishlistItem, Integer> {

    // Get all items in a wishlist
    List<WishlistItem> findByWishlist_WishlistId(Integer wishlistId);

    // Find a specific product in a wishlist
    Optional<WishlistItem> findByWishlist_WishlistIdAndProduct_ProductId(
            Integer wishlistId, Integer productId);

    // Find an item belonging to a specific wishlist
    Optional<WishlistItem> findByWishlist_WishlistIdAndWishlistItemId(
            Integer wishlistId, Integer wishlistItemId);

    // Delete all items from a wishlist
    void deleteByWishlist_WishlistId(Integer wishlistId);
}
