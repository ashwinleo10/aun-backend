
package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.RazorpayWebhookProcessingService;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private final RazorpayWebhookProcessingService processingService;

    public RazorpayWebhookController(
            RazorpayWebhookProcessingService processingService) {

        this.processingService = processingService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false) String signature,
            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false) String eventId) {

        String result = processingService.processWebhook(
                payload,
                signature,
                eventId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result);
    }
}
