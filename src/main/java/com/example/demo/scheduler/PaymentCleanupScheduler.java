
package com.example.demo.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.service.AbandonedPaymentService;

@Component
public class PaymentCleanupScheduler {

    private final AbandonedPaymentService abandonedPaymentService;

    public PaymentCleanupScheduler(
            AbandonedPaymentService abandonedPaymentService) {
        this.abandonedPaymentService = abandonedPaymentService;
    }

    // Run payment cleanup every 5 minutes
    @Scheduled(fixedDelay = 300000)
    public void cleanupAbandonedPayments() {

        int cancelledCount =
                abandonedPaymentService.cancelExpiredPendingPayments();

        if (cancelledCount > 0) {
            System.out.println(
                    "Abandoned payments cancelled: " + cancelledCount);
        }
    }
}
