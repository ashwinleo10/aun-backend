
package com.example.demo.service;

import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    public RazorpayService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    // Create a Razorpay order
    public Order createRazorpayOrder(
            long amountInPaise,
            String receipt) throws RazorpayException {

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);

        return razorpayClient.orders.create(orderRequest);
    }

    // Fetch an existing Razorpay order
    public Order fetchRazorpayOrder(
            String razorpayOrderId) throws RazorpayException {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID is required");
        }

        return razorpayClient.orders.fetch(razorpayOrderId);
    }

    // Fetch all payment attempts associated with an order
    public List<Payment> fetchPaymentsForOrder(
            String razorpayOrderId) throws RazorpayException {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay order ID is required");
        }

        return razorpayClient.orders.fetchPayments(razorpayOrderId);
    }
}
