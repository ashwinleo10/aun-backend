
package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;

@Service
public class RazorpayWebhookService {

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    // Verify the authenticity of a Razorpay webhook
    public void verifyWebhookSignature(
            String payload,
            String signature) {

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook payload is required");
        }

        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook signature is required");
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay webhook secret is not configured");
        }

        try {
            boolean valid = Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret);

            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid Razorpay webhook signature");
            }

        } catch (RazorpayException exception) {
            throw new IllegalArgumentException(
                    "Unable to verify Razorpay webhook signature",
                    exception);
        }
    }
}
