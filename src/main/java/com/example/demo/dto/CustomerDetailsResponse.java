package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.User;

public class CustomerDetailsResponse {

    private Integer userId;
    private String username;
    private String email;
    private String role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer totalOrders;
    private BigDecimal totalSpent;
    private List<CustomerOrderResponse> orders;

    public CustomerDetailsResponse() {
    }

    public static CustomerDetailsResponse fromEntity(
            User user,
            Integer totalOrders,
            BigDecimal totalSpent,
            List<CustomerOrderResponse> orders) {

        CustomerDetailsResponse response =
                new CustomerDetailsResponse();

        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        response.setTotalOrders(totalOrders);
        response.setTotalSpent(totalSpent);
        response.setOrders(orders);

        return response;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(
            Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(
            BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public List<CustomerOrderResponse> getOrders() {
        return orders;
    }

    public void setOrders(
            List<CustomerOrderResponse> orders) {
        this.orders = orders;
    }
}