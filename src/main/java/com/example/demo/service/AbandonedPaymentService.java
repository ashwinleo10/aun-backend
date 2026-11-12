
package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.repository.OrderRepository;
import com.razorpay.Payment;
import com.razorpay.RazorpayException;

@Service
public class AbandonedPaymentService {

    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final PaymentOrderCancellationService cancellationService;

    @Value("${app.payment.pending-expiry-minutes:30}")
    private long expiryMinutes;

    public AbandonedPaymentService(
            OrderRepository orderRepository,
            RazorpayService razorpayService,
            PaymentOrderCancellationService cancellationService) {

        this.orderRepository = orderRepository;
        this.razorpayService = razorpayService;
        this.cancellationService = cancellationService;
    }

    // Find and process expired pending online payments
    public int cancelExpiredPendingPayments() {

        if (expiryMinutes <= 0) {
            throw new IllegalStateException(
                    "Payment expiry must be greater than zero");
        }

        LocalDateTime expiryTime =
                LocalDateTime.now().minusMinutes(expiryMinutes);

        List<Order> pendingOrders =
                orderRepository
                .findByPaymentMethodAndOrderStatusAndPaymentStatusAndCreatedAtBefore(
                        PaymentMethod.ONLINE,
                        OrderStatus.PENDING_PAYMENT,
                        PaymentStatus.PENDING,
                        expiryTime);

        int cancelledCount = 0;

        for (Order order : pendingOrders) {

            try {

                String razorpayOrderId =
                        order.getRazorpayOrderId();

                if (razorpayOrderId == null
                        || razorpayOrderId.isBlank()) {
                    continue;
                }

                // Check the latest Razorpay order status.
                com.razorpay.Order razorpayOrder =
                        razorpayService.fetchRazorpayOrder(
                                razorpayOrderId);

                String razorpayStatus =
                        razorpayOrder.get("status");

                // Never cancel an order reported as paid.
                if (!"created".equalsIgnoreCase(razorpayStatus)) {
                    continue;
                }

                // Check all payment attempts.
                List<Payment> payments =
                        razorpayService.fetchPaymentsForOrder(
                                razorpayOrderId);

                // Any payment attempt requires reconciliation.
                if (payments == null || !payments.isEmpty()) {
                    continue;
                }

                // Recheck and cancel inside a separate short transaction.
                boolean cancelled =
                        cancellationService.cancelIfStillPending(
                                order.getOrderId(),
                                razorpayOrderId);

                if (cancelled) {
                    cancelledCount++;
                }

            } catch (RazorpayException exception) {

                // Keep the order pending if Razorpay is unavailable.
                System.err.println(
                        "Unable to reconcile Razorpay order "
                        + order.getRazorpayOrderId()
                        + ": " + exception.getMessage());
            }
        }

        return cancelledCount;
    }
}
