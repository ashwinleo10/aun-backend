package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CustomerDetailsResponse;
import com.example.demo.dto.CustomerOrderResponse;
import com.example.demo.dto.CustomerResponse;
import com.example.demo.entity.Order;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;

@Service
public class AdminCustomerService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AdminCustomerService(
            UserRepository userRepository,
            OrderRepository orderRepository) {

        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {

        List<User> customers =
                userRepository.findByRoleOrderByCreatedAtDesc(
                        Role.CUSTOMER);

        return customers.stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerDetailsResponse getCustomerById(
            Integer userId) {

        if (userId == null || userId < 1) {
            throw new IllegalArgumentException(
                    "Invalid customer ID");
        }

        User customer =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Customer not found"));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "User is not a customer");
        }

        List<Order> orders =
                orderRepository
                        .findByUser_UserIdOrderByCreatedAtDesc(
                                userId);

        List<CustomerOrderResponse>
                orderResponses =
                        orders.stream()
                                .map(CustomerOrderResponse::fromEntity)
                                .toList();

        BigDecimal totalSpent =
                orders.stream()
                        .filter(order ->
                                order.getPaymentStatus() != null &&
                                order.getPaymentStatus().name()
                                        .equals("PAID"))
                        .map(Order::getTotalAmount)
                        .filter(amount -> amount != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add);

        return CustomerDetailsResponse.fromEntity(
                customer,
                orders.size(),
                totalSpent,
                orderResponses);
    }

    @Transactional
    public CustomerResponse deactivateCustomer(
            Integer userId) {

        User customer =
                getCustomerForStatusUpdate(userId);

        if (!customer.isActive()) {
            throw new IllegalArgumentException(
                    "Customer account is already inactive");
        }

        customer.setActive(false);

        return CustomerResponse.fromEntity(
                userRepository.save(customer));
    }

    @Transactional
    public CustomerResponse activateCustomer(
            Integer userId) {

        User customer =
                getCustomerForStatusUpdate(userId);

        if (customer.isActive()) {
            throw new IllegalArgumentException(
                    "Customer account is already active");
        }

        customer.setActive(true);

        return CustomerResponse.fromEntity(
                userRepository.save(customer));
    }

    private User getCustomerForStatusUpdate(
            Integer userId) {

        if (userId == null || userId < 1) {
            throw new IllegalArgumentException(
                    "Invalid customer ID");
        }

        User customer =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Customer not found"));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "User is not a customer");
        }

        return customer;
    }
}