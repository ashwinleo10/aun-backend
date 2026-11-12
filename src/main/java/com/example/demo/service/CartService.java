
package com.example.demo.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.dto.CartItemResponse;
import com.example.demo.dto.CartResponse;
import com.example.demo.dto.UpdateCartItemRequest;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // Get the currently authenticated customer
    private User getAuthenticatedCustomer() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String username)) {

            throw new IllegalArgumentException("User is not authenticated");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can access the cart");
        }

        return user;
    }

    // Get existing cart or create a new cart
    @Transactional
    public Cart getOrCreateCart() {

        User user = getAuthenticatedCustomer();

        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    // Get the existing customer's cart
    @Transactional(readOnly = true)
    public Cart getCart() {

        User user = getAuthenticatedCustomer();

        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart not found"));
    }

    // Find the actual available size or color, ignoring letter case
    private String findAvailableOption(
            Set<String> availableOptions,
            String requestedOption,
            String optionName) {

        if (requestedOption == null || requestedOption.isBlank()) {
            throw new IllegalArgumentException(
                    optionName + " is required");
        }

        return availableOptions.stream()
                .filter(option -> option.equalsIgnoreCase(
                        requestedOption.trim()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected " + optionName
                                + " is not available for this product"));
    }

    // Add product to cart with selected size and color
    @Transactional
    public CartItem addToCart(AddToCartRequest request) {

        if (request == null || request.getProductId() == null
                || request.getQuantity() == null
                || request.getProductId() < 1
                || request.getQuantity() < 1) {

            throw new IllegalArgumentException(
                    "Product ID and quantity must be valid");
        }

        Cart cart = getOrCreateCart();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException(
                    "This product is currently unavailable");
        }

        String selectedSize = findAvailableOption(
                product.getSizes(),
                request.getSelectedSize(),
                "size");

        String selectedColor = findAvailableOption(
                product.getColors(),
                request.getSelectedColor(),
                "color");

        if (request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity exceeds available stock");
        }

        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndProduct_ProductIdAndSelectedSizeAndSelectedColor(
                        cart.getCartId(),
                        product.getProductId(),
                        selectedSize,
                        selectedColor)
                .orElse(null);

        if (cartItem != null) {

            int updatedQuantity =
                    cartItem.getQuantity() + request.getQuantity();

            if (updatedQuantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Requested quantity exceeds available stock");
            }

            cartItem.setQuantity(updatedQuantity);

        } else {

            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setSelectedSize(selectedSize);
            cartItem.setSelectedColor(selectedColor);
        }

        return cartItemRepository.save(cartItem);
    }

    // Get complete cart response
    @Transactional(readOnly = true)
    public CartResponse getCartResponse() {

        Cart cart = getCart();

        List<CartItemResponse> items = cartItemRepository
                .findByCart_CartId(cart.getCartId())
                .stream()
                .map(CartItemResponse::fromEntity)
                .toList();

        return CartResponse.fromEntity(cart, items);
    }

    // Add product and return updated cart
    @Transactional
    public CartResponse addToCartResponse(AddToCartRequest request) {

        addToCart(request);

        return getCartResponse();
    }

    // Update cart item quantity
    @Transactional
    public CartResponse updateCartItem(
            Integer cartItemId,
            UpdateCartItemRequest request) {

        if (request == null || request.getQuantity() == null
                || request.getQuantity() < 1) {

            throw new IllegalArgumentException(
                    "Quantity must be at least 1");
        }

        Cart cart = getCart();

        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndCartItemId(
                        cart.getCartId(), cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found"));

        Product product = cartItem.getProduct();

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException(
                    "This product is currently unavailable");
        }

        if (request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity exceeds available stock");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return getCartResponse();
    }

    // Remove item from cart
    @Transactional
    public CartResponse removeCartItem(Integer cartItemId) {

        Cart cart = getCart();

        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndCartItemId(
                        cart.getCartId(), cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found"));

        cartItemRepository.delete(cartItem);

        return getCartResponse();
    }

    // Clear all items from cart
    @Transactional
    public CartResponse clearCart() {

        Cart cart = getCart();

        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        return getCartResponse();
    }
}
