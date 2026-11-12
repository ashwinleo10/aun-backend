package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.repository.OrderRepository;

@Service
public class PaymentOrderCancellationService {

    private final OrderRepository orderRepository;

    public PaymentOrderCancellationService(
            OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public boolean cancelIfStillPending(
            Integer orderId,
            String expectedRazorpayOrderId) {

        // Lock and reload the order inside a short transaction.
        Order order = orderRepository
                .findWithLockByOrderId(orderId)
                .orElse(null);

        if (order == null) {
            return false;
        }

        // Ensure this is the same Razorpay order we checked earlier.
        if (expectedRazorpayOrderId == null
                || !expectedRazorpayOrderId.equals(
                        order.getRazorpayOrderId())) {
            return false;
        }

        // Recheck all application-side payment conditions.
        if (order.getPaymentMethod() != PaymentMethod.ONLINE
                || order.getOrderStatus() != OrderStatus.PENDING_PAYMENT
                || order.getPaymentStatus() != PaymentStatus.PENDING) {
            return false;
        }

        // Cancel without changing stock or cart.
        order.setOrderStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return true;
    }
}
