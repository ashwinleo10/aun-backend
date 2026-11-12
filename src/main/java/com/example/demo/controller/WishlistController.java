
package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddToWishlistRequest;
import com.example.demo.dto.WishlistResponse;
import com.example.demo.service.WishlistService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // Add product to wishlist
    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request) {

        WishlistResponse response =
                wishlistService.addToWishlist(request.getProductId());

        return ResponseEntity.ok(response);
    }

    // Get customer's wishlist
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist() {

        WishlistResponse response = wishlistService.getWishlist();

        return ResponseEntity.ok(response);
    }

    // Remove one product from wishlist
    @DeleteMapping("/items/{wishlistItemId}")
    public ResponseEntity<WishlistResponse> removeFromWishlist(
            @PathVariable Integer wishlistItemId) {

        WishlistResponse response =
                wishlistService.removeFromWishlist(wishlistItemId);

        return ResponseEntity.ok(response);
    }

    // Clear entire wishlist
    @DeleteMapping
    public ResponseEntity<WishlistResponse> clearWishlist() {

        WishlistResponse response = wishlistService.clearWishlist();

        return ResponseEntity.ok(response);
    }
}
