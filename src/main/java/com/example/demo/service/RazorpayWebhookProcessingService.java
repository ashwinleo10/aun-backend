package com.example.demo.service;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.entity.RazorpayWebhookEvent;
import com.example.demo.repository.RazorpayWebhookEventRepository;

@Service
public class RazorpayWebhookProcessingService {

    private final RazorpayWebhookService webhookService;
    private final OnlinePaymentFinalizationService finalizationService;
    private final RazorpayWebhookEventRepository eventRepository;
    private final TransactionTemplate transactionTemplate;

    public RazorpayWebhookProcessingService(
            RazorpayWebhookService webhookService,
            OnlinePaymentFinalizationService finalizationService,
            RazorpayWebhookEventRepository eventRepository,
            PlatformTransactionManager transactionManager) {

        this.webhookService = webhookService;
        this.finalizationService = finalizationService;
        this.eventRepository = eventRepository;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // Process webhook events with duplicate protection
    public synchronized String processWebhook(
            String payload,
            String signature,
            String eventId) {

        return transactionTemplate.execute(status ->
                processWebhookTransaction(payload, signature, eventId));
    }

    private String processWebhookTransaction(
            String payload,
            String signature,
            String eventId) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay webhook event ID is required");
        }

        // Verify Razorpay webhook signature first
        webhookService.verifyWebhookSignature(payload, signature);

        // Ignore already processed events
        if (eventRepository.existsByEventId(eventId)) {
            return "Duplicate webhook event ignored";
        }

        try {

            JSONObject webhookJson = new JSONObject(payload);

            String eventType = webhookJson.optString("event");

            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException(
                        "Webhook event type is missing");
            }

            switch (eventType) {

                case "payment.captured":
                    processCapturedPayment(webhookJson);
                    break;

                case "payment.failed":
                    // A failed payment can still be retried by the customer.
                    // Do not cancel the application order here.
                    break;

                default:
                    // Ignore unsupported event types.
                    break;
            }

            // Store the event only after successful processing.
            RazorpayWebhookEvent event =
                    new RazorpayWebhookEvent();

            event.setEventId(eventId);
            event.setEventType(eventType);

            eventRepository.save(event);

            return "Webhook processed successfully";

        } catch (JSONException exception) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook payload",
                    exception);
        }
    }

    // Process successful captured payments
    private void processCapturedPayment(JSONObject webhookJson) {

        JSONObject payload =
                webhookJson.getJSONObject("payload");

        JSONObject payment =
                payload.getJSONObject("payment")
                       .getJSONObject("entity");

        String paymentId = payment.optString("id");
        String razorpayOrderId = payment.optString("order_id");
        String paymentStatus = payment.optString("status");
        String currency = payment.optString("currency");

        long amount = payment.optLong("amount", 0);

        if (paymentId == null || paymentId.isBlank()
                || razorpayOrderId == null
                || razorpayOrderId.isBlank()) {

            throw new IllegalArgumentException(
                    "Payment ID or Razorpay order ID is missing");
        }

        if (!"captured".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalArgumentException(
                    "Payment is not in captured status");
        }

        // Use the same finalization logic as frontend verification.
        finalizationService.finalizeOnlinePayment(
                razorpayOrderId,
                paymentId,
                amount,
                currency,
                null);
    }
}