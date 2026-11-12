
package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.Wishlist;

public class WishlistResponse {

    private Integer wishlistId;
    private Integer userId;
    private List<WishlistItemResponse> items;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WishlistResponse fromEntity(
            Wishlist wishlist,
            List<WishlistItemResponse> items) {

        WishlistResponse response = new WishlistResponse();

        response.setWishlistId(wishlist.getWishlistId());
        response.setUserId(wishlist.getUser().getUserId());
        response.setItems(items);
        response.setTotalItems(items.size());
        response.setCreatedAt(wishlist.getCreatedAt());
        response.setUpdatedAt(wishlist.getUpdatedAt());

        return response;
    }

    public Integer getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(Integer wishlistId) {
        this.wishlistId = wishlistId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<WishlistItemResponse> getItems() {
        return items;
    }

    public void setItems(List<WishlistItemResponse> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
