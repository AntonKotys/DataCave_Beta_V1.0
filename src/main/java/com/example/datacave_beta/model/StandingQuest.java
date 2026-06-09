package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A recurring data subscription (E): the platform delivers a fresh batch of
 * submissions every interval until paused — instead of a one-off quest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingQuest {
    private Long id;
    private String title;
    private String company;
    private String category;
    private String description;
    private double rewardPerSubmission;
    private int batchSize;            // submissions per delivery
    private String frequency;         // WEEKLY, BIWEEKLY, MONTHLY
    private Long schemaId;            // optional annotation schema
    private String status;           // ACTIVE, PAUSED
    private long createdAt;           // epoch millis
    private long nextDeliveryAt;      // epoch millis
    private int deliveriesCompleted;
    @Builder.Default
    private List<Delivery> deliveries = new ArrayList<>();
}
