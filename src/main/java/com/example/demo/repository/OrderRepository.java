
package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    Optional<Order> findByOrderIdAndUser_UserId(
            Integer orderId, Integer userId);

    // Find and lock an order using Razorpay order ID and customer ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByRazorpayOrderIdAndUser_UserId(
            String razorpayOrderId, Integer userId);

    // Find and lock an order directly using Razorpay order ID
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Find expired pending online payments
    List<Order> findByPaymentMethodAndOrderStatusAndPaymentStatusAndCreatedAtBefore(
            PaymentMethod paymentMethod,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            LocalDateTime expiryTime);

    // Lock one order before changing its status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockByOrderId(Integer orderId);
}
