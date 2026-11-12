
package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.example.demo.entity.PaymentMethod;

public class PlaceOrderRequest {

    @NotNull(message = "Address ID is required")
    @Positive(message = "Address ID must be positive")
    private Integer addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    public PlaceOrderRequest() {
    }

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
