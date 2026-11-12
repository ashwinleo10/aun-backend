
package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.UpdateOrderStatusRequest;
import com.example.demo.service.AdminOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(
            AdminOrderService adminOrderService) {

        this.adminOrderService = adminOrderService;
    }

    // Admin: Get all customer orders
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        List<OrderResponse> response =
                adminOrderService.getAllOrders();

        return ResponseEntity.ok(response);
    }

    // Admin: Get a specific order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Integer orderId) {

        OrderResponse response =
                adminOrderService.getOrderById(orderId);

        return ResponseEntity.ok(response);
    }

    // Admin: Update order status
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Integer orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderResponse response =
                adminOrderService.updateOrderStatus(
                        orderId, request.getOrderStatus());

        return ResponseEntity.ok(response);
    }

    // Admin: Confirm COD payment
    @PutMapping("/{orderId}/confirm-payment")
    public ResponseEntity<OrderResponse> confirmCodPayment(
            @PathVariable Integer orderId) {

        OrderResponse response =
                adminOrderService.confirmCodPayment(orderId);

        return ResponseEntity.ok(response);
    }
}
