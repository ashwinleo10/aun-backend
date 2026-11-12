
package com.example.demo.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.Product;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class OnlinePaymentFinalizationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public OnlinePaymentFinalizationService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /*
     * Finalizes an online payment after the caller has verified
     * the payment using the appropriate Razorpay verification method.
     *
     * Frontend verification:
     * Verify the Razorpay payment signature first.
     *
     * Webhook:
     * Verify the webhook signature and confirm that the payment
     * is captured before calling this method.
     *
     * This method handles stock deduction and cart clearing
     * inside one database transaction.
     */
    @Transactional
    public OrderResponse finalizeOnlinePayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            long amountInPaise,
            String currency,
            String paymentSignature) {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {

            throw new IllegalArgumentException(
                    "Valid Razorpay order ID and payment ID are required");
        }

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        if (currency == null || !"INR".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException(
                    "Only INR payments are supported");
        }

        // Lock the order to prevent simultaneous finalization.
        Order order = orderRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found for this Razorpay payment"));

        // Verify the payment amount against the application order.
        BigDecimal expectedAmount = order.getTotalAmount();

        long expectedAmountInPaise;

        try {
            expectedAmountInPaise = expectedAmount
                    .movePointRight(2)
                    .longValueExact();

        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Invalid order amount configuration",
                    exception);
        }

        if (amountInPaise != expectedAmountInPaise) {
            throw new IllegalArgumentException(
                    "Payment amount does not match the order amount");
        }

        if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new IllegalArgumentException(
                    "This order is not an online payment order");
        }

        /*
         * Idempotency:
         * If the same payment has already finalized this order,
         * return the existing order without deducting stock again.
         */
        if (order.getPaymentStatus() == PaymentStatus.PAID
                && order.getOrderStatus() == OrderStatus.PLACED) {

            if (razorpayPaymentId.equals(order.getRazorpayPaymentId())) {
                return buildOrderResponse(order);
            }

            throw new IllegalArgumentException(
                    "This order has already been paid using another payment");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT
                || order.getPaymentStatus() != PaymentStatus.PENDING) {

            throw new IllegalArgumentException(
                    "This order is not awaiting payment finalization");
        }

        // Load the order items.
        List<OrderItem> orderItems =
                orderItemRepository.findByOrder_OrderId(
                        order.getOrderId());

        if (orderItems.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot finalize an order without order items");
        }

        // Combine quantities when the same product appears more than once.
        Map<Integer, Product> products = new LinkedHashMap<>();

        Map<Integer, Integer> totalQuantityByProduct =
                new LinkedHashMap<>();

        for (OrderItem orderItem : orderItems) {

            Integer productId =
                    orderItem.getProduct().getProductId();

            Product product = products.get(productId);

            if (product == null) {

                product = productRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product not found: " + productId));

                products.put(productId, product);
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is no longer available: "
                                + product.getProductName());
            }

            if (orderItem.getQuantity() == null
                    || orderItem.getQuantity() < 1) {

                throw new IllegalStateException(
                        "Invalid order item quantity");
            }

            totalQuantityByProduct.merge(
                    productId,
                    orderItem.getQuantity(),
                    Integer::sum);
        }

        // Check current stock before changing anything.
        for (Map.Entry<Integer, Product> entry : products.entrySet()) {

            Product product = entry.getValue();

            Integer requiredQuantity =
                    totalQuantityByProduct.get(entry.getKey());

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < requiredQuantity) {

                throw new IllegalArgumentException(
                        "Insufficient stock to complete paid order for "
                                + product.getProductName()
                                + ". A refund may be required.");
            }
        }

        // Save the successful payment details.
        order.setRazorpayPaymentId(razorpayPaymentId);

        // Webhook payments do not provide the frontend signature.
        if (paymentSignature != null && !paymentSignature.isBlank()) {
            order.setRazorpaySignature(paymentSignature);
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.PLACED);

        // Deduct stock exactly once within this transaction.
        for (Map.Entry<Integer, Product> entry : products.entrySet()) {

            Product product = entry.getValue();

            Integer quantity =
                    totalQuantityByProduct.get(entry.getKey());

            product.setStockQuantity(
                    product.getStockQuantity() - quantity);

            productRepository.save(product);
        }

        // Clear the customer's cart after successful payment.
        Integer customerId = order.getUser().getUserId();

        Cart cart = cartRepository
                .findByUser_UserId(customerId)
                .orElse(null);

        if (cart != null) {
            cartItemRepository.deleteByCart_CartId(
                    cart.getCartId());
        }

        Order savedOrder = orderRepository.save(order);

        return buildOrderResponse(savedOrder);
    }

    private OrderResponse buildOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses =
                orderItemRepository
                        .findByOrder_OrderId(order.getOrderId())
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList();

        return OrderResponse.fromEntity(order, itemResponses);
    }
}
