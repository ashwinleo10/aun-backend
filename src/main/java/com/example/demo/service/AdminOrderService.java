
package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;

@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // Get all customer orders for admin
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        List<Order> orders =
                orderRepository.findAllByOrderByCreatedAtDesc();

        return orders.stream()
                .map(order -> {

                    List<OrderItemResponse> items =
                            orderItemRepository
                                    .findByOrder_OrderId(order.getOrderId())
                                    .stream()
                                    .map(OrderItemResponse::fromEntity)
                                    .toList();

                    return OrderResponse.fromEntity(order, items);
                })
                .toList();
    }

    // Get a specific order by ID for admin
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer orderId) {

        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrder_OrderId(order.getOrderId())
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList();

        return OrderResponse.fromEntity(order, items);
    }

    // Update order status by admin
    @Transactional
    public OrderResponse updateOrderStatus(
            Integer orderId, OrderStatus newStatus) {

        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("Order status is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getOrderStatus();

        boolean validTransition =
                (currentStatus == OrderStatus.PLACED
                        && newStatus == OrderStatus.CONFIRMED)
                ||
                (currentStatus == OrderStatus.CONFIRMED
                        && newStatus == OrderStatus.SHIPPED)
                ||
                (currentStatus == OrderStatus.SHIPPED
                        && newStatus == OrderStatus.DELIVERED);

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid order status transition from "
                    + currentStatus + " to " + newStatus);
        }

        order.setOrderStatus(newStatus);

        Order updatedOrder = orderRepository.save(order);

        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrder_OrderId(updatedOrder.getOrderId())
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList();

        return OrderResponse.fromEntity(updatedOrder, items);
    }

    // Confirm COD payment collected by the admin
    @Transactional
    public OrderResponse confirmCodPayment(Integer orderId) {

        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("Invalid order ID");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new IllegalArgumentException(
                    "This endpoint is only for COD payments");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "COD payment can only be confirmed after delivery");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Payment cannot be confirmed because its status is "
                    + order.getPaymentStatus());
        }

        order.setPaymentStatus(PaymentStatus.PAID);

        Order updatedOrder = orderRepository.save(order);

        List<OrderItemResponse> items =
                orderItemRepository
                        .findByOrder_OrderId(updatedOrder.getOrderId())
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList();

        return OrderResponse.fromEntity(updatedOrder, items);
    }
}
