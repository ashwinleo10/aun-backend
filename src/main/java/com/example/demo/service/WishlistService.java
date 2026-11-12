
package com.example.demo.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.WishlistItemResponse;
import com.example.demo.dto.WishlistResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.Wishlist;
import com.example.demo.entity.WishlistItem;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WishlistItemRepository;
import com.example.demo.repository.WishlistRepository;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {

        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // Get the currently authenticated customer
    private User getAuthenticatedCustomer() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String username)) {

            throw new IllegalArgumentException("User is not authenticated");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can access wishlist");
        }

        return user;
    }

    // Get an existing wishlist or create a new one
    private Wishlist getOrCreateWishlist(User customer) {

        return wishlistRepository
                .findByUser_UserId(customer.getUserId())
                .orElseGet(() -> {

                    Wishlist wishlist = new Wishlist();
                    wishlist.setUser(customer);

                    return wishlistRepository.save(wishlist);
                });
    }

    // Convert wishlist entity into response DTO
    private WishlistResponse buildWishlistResponse(Wishlist wishlist) {

        List<WishlistItemResponse> items =
                wishlistItemRepository
                        .findByWishlist_WishlistId(
                                wishlist.getWishlistId())
                        .stream()
                        .map(WishlistItemResponse::fromEntity)
                        .toList();

        return WishlistResponse.fromEntity(wishlist, items);
    }

    // Add product to wishlist
    @Transactional
    public WishlistResponse addToWishlist(Integer productId) {

        if (productId == null || productId < 1) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        User customer = getAuthenticatedCustomer();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException(
                    "This product is currently unavailable");
        }

        Wishlist wishlist = getOrCreateWishlist(customer);

        // Prevent duplicate product in the same wishlist
        boolean alreadyExists = wishlistItemRepository
                .findByWishlist_WishlistIdAndProduct_ProductId(
                        wishlist.getWishlistId(), productId)
                .isPresent();

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Product is already in your wishlist");
        }

        WishlistItem wishlistItem = new WishlistItem();

        wishlistItem.setWishlist(wishlist);
        wishlistItem.setProduct(product);

        wishlistItemRepository.save(wishlistItem);

        return buildWishlistResponse(wishlist);
    }

    // Get customer's wishlist
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist() {

        User customer = getAuthenticatedCustomer();

        Wishlist wishlist = wishlistRepository
                .findByUser_UserId(customer.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wishlist not found"));

        return buildWishlistResponse(wishlist);
    }

    // Remove one product from wishlist
    @Transactional
    public WishlistResponse removeFromWishlist(Integer wishlistItemId) {

        if (wishlistItemId == null || wishlistItemId < 1) {
            throw new IllegalArgumentException(
                    "Invalid wishlist item ID");
        }

        User customer = getAuthenticatedCustomer();

        Wishlist wishlist = wishlistRepository
                .findByUser_UserId(customer.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wishlist not found"));

        WishlistItem wishlistItem = wishlistItemRepository
                .findByWishlist_WishlistIdAndWishlistItemId(
                        wishlist.getWishlistId(), wishlistItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wishlist item not found"));

        wishlistItemRepository.delete(wishlistItem);

        return buildWishlistResponse(wishlist);
    }

    // Clear entire wishlist
    @Transactional
    public WishlistResponse clearWishlist() {

        User customer = getAuthenticatedCustomer();

        Wishlist wishlist = wishlistRepository
                .findByUser_UserId(customer.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wishlist not found"));

        wishlistItemRepository.deleteByWishlist_WishlistId(
                wishlist.getWishlistId());

        return buildWishlistResponse(wishlist);
    }
}
