
package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.CartItem;

public class CartItemResponse {

    private Integer cartItemId;
    private Integer productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String categoryName;
    private String selectedSize;
    private String selectedColor;
    private LocalDateTime addedAt;

    public CartItemResponse() {
    }

    public static CartItemResponse fromEntity(CartItem cartItem) {

        CartItemResponse response = new CartItemResponse();

        response.setCartItemId(cartItem.getCartItemId());

        response.setProductId(
                cartItem.getProduct().getProductId());

        response.setProductName(
                cartItem.getProduct().getProductName());

        response.setDescription(
                cartItem.getProduct().getDescription());

        response.setPrice(
                cartItem.getProduct().getPrice());

        response.setDiscountPrice(
                cartItem.getProduct().getDiscountPrice());

        response.setQuantity(cartItem.getQuantity());

        response.setSelectedSize(cartItem.getSelectedSize());

        response.setSelectedColor(cartItem.getSelectedColor());

        BigDecimal effectivePrice =
                cartItem.getProduct().getDiscountPrice() != null
                ? cartItem.getProduct().getDiscountPrice()
                : cartItem.getProduct().getPrice();

        response.setSubtotal(
                effectivePrice.multiply(
                        BigDecimal.valueOf(cartItem.getQuantity())));

        response.setCategoryName(
                cartItem.getProduct().getCategory().getCategoryName());

        response.setAddedAt(cartItem.getAddedAt());

        return response;
    }

    public Integer getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Integer cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(String selectedSize) {
        this.selectedSize = selectedSize;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
