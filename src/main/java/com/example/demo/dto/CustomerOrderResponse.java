package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.Order;

public class CustomerOrderResponse {

    private Integer orderId;
    private BigDecimal totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;

    public CustomerOrderResponse() {
    }

    public static CustomerOrderResponse fromEntity(
            Order order) {

        CustomerOrderResponse response =
                new CustomerOrderResponse();

        response.setOrderId(order.getOrderId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(
                order.getOrderStatus().name());
        response.setPaymentStatus(
                order.getPaymentStatus().name());
        response.setPaymentMethod(
                order.getPaymentMethod().name());
        response.setCreatedAt(order.getCreatedAt());

        return response;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(
            BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(
            String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(
            String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(
            String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}