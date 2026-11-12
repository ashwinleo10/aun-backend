
package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.Product;
import com.example.demo.entity.WishlistItem;

public class WishlistItemResponse {

    private Integer wishlistItemId;
    private Integer productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String categoryName;
    private Integer stockQuantity;
    private Boolean active;
    private LocalDateTime addedAt;

    public static WishlistItemResponse fromEntity(
            WishlistItem wishlistItem) {

        Product product = wishlistItem.getProduct();

        WishlistItemResponse response = new WishlistItemResponse();

        response.setWishlistItemId(
                wishlistItem.getWishlistItemId());

        response.setProductId(product.getProductId());
        response.setProductName(product.getProductName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setDiscountPrice(product.getDiscountPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setActive(product.getActive());
        response.setAddedAt(wishlistItem.getAddedAt());

        if (product.getCategory() != null) {
            response.setCategoryName(
                    product.getCategory().getCategoryName());
        }

        return response;
    }

    public Integer getWishlistItemId() {
        return wishlistItemId;
    }

    public void setWishlistItemId(Integer wishlistItemId) {
        this.wishlistItemId = wishlistItemId;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
