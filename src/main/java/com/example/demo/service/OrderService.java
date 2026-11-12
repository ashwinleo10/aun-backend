package com.example.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Utils;

import com.example.demo.dto.OrderItemResponse;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.PlaceOrderRequest;
import com.example.demo.dto.RazorpayOrderResponse;
import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.CustomerAddress;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.PaymentMethod;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.Product;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.CustomerAddressRepository;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerAddressRepository addressRepository;
    private final RazorpayService razorpayService;
    private final OnlinePaymentFinalizationService finalizationService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerAddressRepository addressRepository,
            RazorpayService razorpayService,
            OnlinePaymentFinalizationService finalizationService) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.razorpayService = razorpayService;
        this.finalizationService = finalizationService;
    }

    // Get the currently authenticated customer
    private User getAuthenticatedCustomer() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String username)) {

            throw new IllegalArgumentException("User is not authenticated");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Only customers can access orders");
        }

        return user;
    }

    // Validate selected size and color
    private void validateProductVariant(
            CartItem cartItem,
            Product product) {

        Set<String> sizes = product.getSizes();
        Set<String> colors = product.getColors();

        boolean validSize = sizes != null && sizes.stream()
                .anyMatch(size ->
                        size.equalsIgnoreCase(
                                cartItem.getSelectedSize()));

        boolean validColor = colors != null && colors.stream()
                .anyMatch(color ->
                        color.equalsIgnoreCase(
                                cartItem.getSelectedColor()));

        if (!validSize) {
            throw new IllegalArgumentException(
                    "Selected size is no longer available for "
                    + product.getProductName());
        }

        if (!validColor) {
            throw new IllegalArgumentException(
                    "Selected color is no longer available for "
                    + product.getProductName());
        }
    }

    // Build permanent shipping address snapshot
    private String createShippingAddressSnapshot(
            CustomerAddress address) {

        StringBuilder snapshot = new StringBuilder();

        snapshot.append("Name: ")
                .append(address.getFullName());

        snapshot.append("\nPhone: ")
                .append(address.getPhoneNumber());

        snapshot.append("\nAddress: ")
                .append(address.getAddressLine1());

        if (address.getAddressLine2() != null
                && !address.getAddressLine2().isBlank()) {

            snapshot.append(", ")
                    .append(address.getAddressLine2());
        }

        if (address.getLandmark() != null
                && !address.getLandmark().isBlank()) {

            snapshot.append("\nLandmark: ")
                    .append(address.getLandmark());
        }

        snapshot.append("\nCity: ")
                .append(address.getCity());

        snapshot.append("\nState: ")
                .append(address.getState());

        snapshot.append("\nPincode: ")
                .append(address.getPincode());

        snapshot.append("\nCountry: ")
                .append(address.getCountry());

        snapshot.append("\nAddress Type: ")
                .append(address.getAddressType());

        return snapshot.toString();
    }

    // Get the customer's cart
    private Cart getCustomerCart(Integer userId) {

        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart not found"));

        List<CartItem> cartItems =
                cartItemRepository.findByCart_CartId(
                        cart.getCartId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot place an order with an empty cart");
        }

        return cart;
    }

    // Prepare products and combined quantities from the cart
    private Map<Integer, Product> getProductsAndValidateStock(
            List<CartItem> cartItems,
            Map<Integer, Integer> totalQuantityByProduct) {

        Map<Integer, Product> products = new LinkedHashMap<>();

        for (CartItem cartItem : cartItems) {

            if (cartItem.getQuantity() == null
                    || cartItem.getQuantity() < 1) {

                throw new IllegalArgumentException(
                        "Invalid cart item quantity");
            }

            Integer productId =
                    cartItem.getProduct().getProductId();

            Product product = products.get(productId);

            if (product == null) {

                product = productRepository.findById(productId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Product not found"));

                products.put(productId, product);
            }

            totalQuantityByProduct.merge(
                    productId,
                    cartItem.getQuantity(),
                    Integer::sum);
        }

        for (Map.Entry<Integer, Product> entry :
                products.entrySet()) {

            Product product = entry.getValue();

            if (!Boolean.TRUE.equals(product.getActive())) {

                throw new IllegalArgumentException(
                        product.getProductName()
                        + " is currently unavailable");
            }

            Integer totalQuantity =
                    totalQuantityByProduct.get(entry.getKey());

            if (product.getStockQuantity() == null
                    || totalQuantity > product.getStockQuantity()) {

                throw new IllegalArgumentException(
                        "Insufficient stock for "
                        + product.getProductName()
                        + ". Available stock: "
                        + product.getStockQuantity()
                        + ", requested quantity: "
                        + totalQuantity);
            }
        }

        return products;
    }

    // Create order item snapshots and calculate total amount
    private List<OrderItem> createOrderItems(
            Order order,
            List<CartItem> cartItems,
            Map<Integer, Product> products,
            BigDecimal[] totalAmountHolder) {

        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            Integer productId =
                    cartItem.getProduct().getProductId();

            Product product = products.get(productId);

            validateProductVariant(cartItem, product);

            BigDecimal unitPrice =
                    product.getDiscountPrice() != null
                    ? product.getDiscountPrice()
                    : product.getPrice();

            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(
                    product.getProductName());
            orderItem.setSelectedSize(
                    cartItem.getSelectedSize());
            orderItem.setSelectedColor(
                    cartItem.getSelectedColor());
            orderItem.setQuantity(
                    cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        totalAmountHolder[0] = totalAmount;

        return orderItems;
    }

    // Reduce stock after successful COD placement
    // or verified online payment
    private void reduceProductStock(
            Map<Integer, Product> products,
            Map<Integer, Integer> totalQuantityByProduct) {

        for (Map.Entry<Integer, Product> entry :
                products.entrySet()) {

            Product product = entry.getValue();

            Integer quantity =
                    totalQuantityByProduct.get(
                            entry.getKey());

            product.setStockQuantity(
                    product.getStockQuantity() - quantity);

            productRepository.save(product);
        }
    }

    // Place a COD order
    @Transactional
    public OrderResponse placeOrder(
            PlaceOrderRequest request) {

        validatePlaceOrderRequest(request);

        if (request.getPaymentMethod() != PaymentMethod.COD) {

            throw new IllegalArgumentException(
                    "Use the online payment endpoint for ONLINE payments");
        }

        User customer = getAuthenticatedCustomer();

        CustomerAddress address =
                getCustomerAddress(
                        request.getAddressId(),
                        customer);

        Cart cart =
                getCustomerCart(
                        customer.getUserId());

        List<CartItem> cartItems =
                cartItemRepository.findByCart_CartId(
                        cart.getCartId());

        Map<Integer, Integer> totalQuantityByProduct =
                new LinkedHashMap<>();

        Map<Integer, Product> products =
                getProductsAndValidateStock(
                        cartItems,
                        totalQuantityByProduct);

        Order order = new Order();

        order.setUser(customer);
        order.setOrderStatus(
                OrderStatus.PLACED);
        order.setPaymentStatus(
                PaymentStatus.PENDING);
        order.setPaymentMethod(
                PaymentMethod.COD);
        order.setShippingAddress(
                createShippingAddressSnapshot(
                        address));

        BigDecimal[] totalAmountHolder =
                new BigDecimal[1];

        List<OrderItem> orderItems =
                createOrderItems(
                        order,
                        cartItems,
                        products,
                        totalAmountHolder);

        order.setTotalAmount(
                totalAmountHolder[0]);

        Order savedOrder =
                orderRepository.save(order);

        orderItemRepository.saveAll(
                orderItems);

        reduceProductStock(
                products,
                totalQuantityByProduct);

        cartItemRepository.deleteByCart_CartId(
                cart.getCartId());

        return buildOrderResponse(
                savedOrder);
    }

    // Create a pending order and Razorpay order
    // for ONLINE payment
    @Transactional
    public RazorpayOrderResponse createOnlinePayment(
            PlaceOrderRequest request) {

        validatePlaceOrderRequest(request);

        if (request.getPaymentMethod()
                != PaymentMethod.ONLINE) {

            throw new IllegalArgumentException(
                    "Payment method must be ONLINE");
        }

        User customer =
                getAuthenticatedCustomer();

        CustomerAddress address =
                getCustomerAddress(
                        request.getAddressId(),
                        customer);

        Cart cart =
                getCustomerCart(
                        customer.getUserId());

        List<CartItem> cartItems =
                cartItemRepository.findByCart_CartId(
                        cart.getCartId());

        Map<Integer, Integer> totalQuantityByProduct =
                new LinkedHashMap<>();

        Map<Integer, Product> products =
                getProductsAndValidateStock(
                        cartItems,
                        totalQuantityByProduct);

        Order order = new Order();

        order.setUser(customer);
        order.setOrderStatus(
                OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(
                PaymentStatus.PENDING);
        order.setPaymentMethod(
                PaymentMethod.ONLINE);
        order.setShippingAddress(
                createShippingAddressSnapshot(
                        address));

        BigDecimal[] totalAmountHolder =
                new BigDecimal[1];

        List<OrderItem> orderItems =
                createOrderItems(
                        order,
                        cartItems,
                        products,
                        totalAmountHolder);

        BigDecimal totalAmount =
                totalAmountHolder[0];

        order.setTotalAmount(
                totalAmount);

        Order savedOrder =
                orderRepository.save(order);

        orderItemRepository.saveAll(
                orderItems);

        try {

            long amountInPaise =
                    totalAmount
                            .movePointRight(2)
                            .longValueExact();

            com.razorpay.Order razorpayOrder =
                    razorpayService.createRazorpayOrder(
                            amountInPaise,
                            "AUN_ORDER_"
                            + savedOrder.getOrderId());

            String razorpayOrderId =
                    razorpayOrder.get("id");

            savedOrder.setRazorpayOrderId(
                    razorpayOrderId);

            orderRepository.save(
                    savedOrder);

            return new RazorpayOrderResponse(
                    savedOrder.getOrderId(),
                    razorpayOrderId,
                    razorpayKeyId,
                    totalAmount,
                    "INR");

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to create Razorpay order: "
                    + exception.getMessage(),
                    exception);
        }
    }

    // Verify Razorpay payment signature
    // and use the shared finalization service
    @Transactional
    public OrderResponse verifyOnlinePayment(
            VerifyPaymentRequest request) {

        if (request == null
                || request.getOrderId() == null
                || request.getOrderId() <= 0
                || request.getRazorpayOrderId() == null
                || request.getRazorpayOrderId().isBlank()
                || request.getRazorpayPaymentId() == null
                || request.getRazorpayPaymentId().isBlank()
                || request.getRazorpaySignature() == null
                || request.getRazorpaySignature().isBlank()) {

            throw new IllegalArgumentException(
                    "All payment verification details are required");
        }

        User customer =
                getAuthenticatedCustomer();

        Order order =
                orderRepository
                        .findByRazorpayOrderIdAndUser_UserId(
                                request.getRazorpayOrderId(),
                                customer.getUserId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment order not found"));

        if (!order.getOrderId()
                .equals(request.getOrderId())) {

            throw new IllegalArgumentException(
                    "Application order ID does not match");
        }

        if (order.getPaymentMethod()
                != PaymentMethod.ONLINE) {

            throw new IllegalArgumentException(
                    "This is not an online payment order");
        }

        // Verify checkout signature
        try {

            JSONObject paymentDetails =
                    new JSONObject();

            paymentDetails.put(
                    "razorpay_order_id",
                    request.getRazorpayOrderId());

            paymentDetails.put(
                    "razorpay_payment_id",
                    request.getRazorpayPaymentId());

            paymentDetails.put(
                    "razorpay_signature",
                    request.getRazorpaySignature());

            boolean signatureValid =
                    Utils.verifyPaymentSignature(
                            paymentDetails,
                            razorpayKeySecret);

            if (!signatureValid) {

                throw new IllegalArgumentException(
                        "Invalid Razorpay payment signature");
            }

        } catch (com.razorpay.RazorpayException exception) {

            throw new IllegalArgumentException(
                    "Unable to verify Razorpay payment signature",
                    exception);
        }

        if (order.getTotalAmount() == null) {

            throw new IllegalStateException(
                    "Order total amount is missing");
        }

        final long amountInPaise;

        try {

            amountInPaise =
                    order.getTotalAmount()
                            .movePointRight(2)
                            .longValueExact();

        } catch (ArithmeticException exception) {

            throw new IllegalStateException(
                    "Order amount cannot be converted to paise exactly",
                    exception);
        }

        return finalizationService
                .finalizeOnlinePayment(
                        request.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        amountInPaise,
                        "INR",
                        request.getRazorpaySignature());
    }

    // Validate checkout request
    private void validatePlaceOrderRequest(
            PlaceOrderRequest request) {

        if (request == null
                || request.getAddressId() == null
                || request.getAddressId() <= 0
                || request.getPaymentMethod() == null) {

            throw new IllegalArgumentException(
                    "Valid address ID and payment method are required");
        }
    }

    // Verify that the address belongs to
    // the authenticated customer
    private CustomerAddress getCustomerAddress(
            Integer addressId,
            User customer) {

        return addressRepository
                .findByAddressIdAndUser_UserId(
                        addressId,
                        customer.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Delivery address not found"));
    }

    // Build order response
    private OrderResponse buildOrderResponse(
            Order order) {

        List<OrderItemResponse> itemResponses =
                orderItemRepository
                        .findByOrder_OrderId(
                                order.getOrderId())
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList();

        return OrderResponse.fromEntity(
                order,
                itemResponses);
    }

    // Cancel an order belonging to
    // the authenticated customer
    @Transactional
    public OrderResponse cancelOrder(
            Integer orderId) {

        if (orderId == null
                || orderId < 1) {

            throw new IllegalArgumentException(
                    "Invalid order ID");
        }

        User customer =
                getAuthenticatedCustomer();

        Order order =
                orderRepository
                        .findByOrderIdAndUser_UserId(
                                orderId,
                                customer.getUserId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found"));

        // Paid online orders cannot be cancelled
        // because refund handling is not implemented.
        if (order.getPaymentMethod()
                == PaymentMethod.ONLINE
                && order.getPaymentStatus()
                == PaymentStatus.PAID) {

            throw new IllegalArgumentException(
                    "Paid online orders cannot be cancelled");
        }

        if (order.getOrderStatus()
                != OrderStatus.PLACED
                && order.getOrderStatus()
                != OrderStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "This order cannot be cancelled because its status is "
                    + order.getOrderStatus());
        }

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrder_OrderId(
                                orderId);

        Map<Integer, Product> products =
                new LinkedHashMap<>();

        Map<Integer, Integer>
                totalQuantityByProduct =
                        new LinkedHashMap<>();

        for (OrderItem orderItem :
                orderItems) {

            Integer productId =
                    orderItem.getProduct()
                            .getProductId();

            Product product =
                    products.get(productId);

            if (product == null) {

                product =
                        productRepository
                                .findById(productId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException(
                                                "Product not found"));

                products.put(
                        productId,
                        product);
            }

            totalQuantityByProduct.merge(
                    productId,
                    orderItem.getQuantity(),
                    Integer::sum);
        }

        for (Map.Entry<Integer, Product> entry :
                products.entrySet()) {

            Product product =
                    entry.getValue();

            Integer restoreQuantity =
                    totalQuantityByProduct
                            .get(entry.getKey());

            product.setStockQuantity(
                    product.getStockQuantity()
                    + restoreQuantity);

            productRepository.save(
                    product);
        }

        order.setOrderStatus(
                OrderStatus.CANCELLED);

        Order savedOrder =
                orderRepository.save(
                        order);

        return buildOrderResponse(
                savedOrder);
    }

    // Get all orders belonging to
    // the authenticated customer
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {

        User customer =
                getAuthenticatedCustomer();

        List<Order> orders =
                orderRepository
                        .findByUser_UserIdOrderByCreatedAtDesc(
                                customer.getUserId());

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    // Get one order belonging to
    // the authenticated customer
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(
            Integer orderId) {

        if (orderId == null
                || orderId < 1) {

            throw new IllegalArgumentException(
                    "Invalid order ID");
        }

        User customer =
                getAuthenticatedCustomer();

        Order order =
                orderRepository
                        .findByOrderIdAndUser_UserId(
                                orderId,
                                customer.getUserId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found"));

        return buildOrderResponse(
                order);
    }

    // Get all orders for admin
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin() {

        List<Order> orders =
                orderRepository
                        .findAllByOrderByCreatedAtDesc();

        return orders.stream()
                .map(this::buildOrderResponse)
                .toList();
    }
}