
package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.dto.UpdateCartItemRequest;
import com.example.demo.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Add product to cart
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request) {

        CartResponse response =
                cartService.addToCartResponse(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // View customer's cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {

        CartResponse response =
                cartService.getCartResponse();

        return ResponseEntity.ok(response);
    }

    // Update cart item quantity
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Integer itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        CartResponse response =
                cartService.updateCartItem(itemId, request);

        return ResponseEntity.ok(response);
    }

    // Remove item from cart
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable Integer itemId) {

        CartResponse response =
                cartService.removeCartItem(itemId);

        return ResponseEntity.ok(response);
    }

    // Clear all items from cart
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart() {

        CartResponse response = cartService.clearCart();

        return ResponseEntity.ok(response);
    }
}
