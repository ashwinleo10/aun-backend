
package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.PlaceOrderRequest;
import com.example.demo.dto.RazorpayOrderResponse;
import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Place a COD order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {

        OrderResponse response = orderService.placeOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Create a Razorpay order for online payment
    @PostMapping("/online-payment")
    public ResponseEntity<RazorpayOrderResponse> createOnlinePayment(
            @Valid @RequestBody PlaceOrderRequest request) {

        RazorpayOrderResponse response =
                orderService.createOnlinePayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Verify Razorpay payment
    @PostMapping("/verify-payment")
    public ResponseEntity<OrderResponse> verifyOnlinePayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        OrderResponse response =
                orderService.verifyOnlinePayment(request);

        return ResponseEntity.ok(response);
    }

    // Get all orders of logged-in customer
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders() {

        List<OrderResponse> response = orderService.getMyOrders();

        return ResponseEntity.ok(response);
    }

    // Get a specific order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            @PathVariable Integer orderId) {

        OrderResponse response = orderService.getMyOrderById(orderId);

        return ResponseEntity.ok(response);
    }

    // Cancel an order
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Integer orderId) {

        OrderResponse response = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(response);
    }
}
