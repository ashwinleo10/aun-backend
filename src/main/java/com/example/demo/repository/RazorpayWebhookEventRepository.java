
package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RazorpayWebhookEvent;

@Repository
public interface RazorpayWebhookEventRepository
        extends JpaRepository<RazorpayWebhookEvent, Integer> {

    // Find a previously received webhook event
    Optional<RazorpayWebhookEvent> findByEventId(String eventId);

    // Check whether an event has already been processed
    boolean existsByEventId(String eventId);
}
