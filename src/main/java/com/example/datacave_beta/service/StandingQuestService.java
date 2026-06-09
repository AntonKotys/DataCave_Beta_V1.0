package com.example.datacave_beta.service;

import com.example.datacave_beta.model.Delivery;
import com.example.datacave_beta.model.StandingQuest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Standing quests / data subscriptions (E). Delivers a fresh batch on an
 * interval until paused. A scheduled task advances any due subscriptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StandingQuestService {

    private final Store store;

    public List<StandingQuest> all() {
        return store.standingQuests().stream()
                .sorted(Comparator.comparing(StandingQuest::getId)).toList();
    }

    public Optional<StandingQuest> get(Long id) {
        return Optional.ofNullable(store.standingQuest(id));
    }

    public StandingQuest create(StandingQuest req) {
        long now = System.currentTimeMillis();
        long id = store.nextStandingId();
        StandingQuest sq = StandingQuest.builder()
                .id(id)
                .title(req.getTitle() != null ? req.getTitle() : "Untitled subscription")
                .company(req.getCompany() != null ? req.getCompany() : "Your Company")
                .category(req.getCategory() != null ? req.getCategory() : "Image")
                .description(req.getDescription() != null ? req.getDescription() : "")
                .rewardPerSubmission(req.getRewardPerSubmission() > 0 ? req.getRewardPerSubmission() : 2.0)
                .batchSize(req.getBatchSize() > 0 ? req.getBatchSize() : 100)
                .frequency(req.getFrequency() != null ? req.getFrequency() : "WEEKLY")
                .schemaId(req.getSchemaId())
                .status("ACTIVE")
                .createdAt(now)
                .nextDeliveryAt(now + intervalMillis(req.getFrequency()))
                .deliveriesCompleted(0)
                .build();
        store.putStandingQuest(sq);
        return sq;
    }

    public Optional<StandingQuest> setStatus(Long id, String status) {
        StandingQuest sq = store.standingQuest(id);
        if (sq == null) return Optional.empty();
        sq.setStatus(status);
        store.touch();
        return Optional.of(sq);
    }

    /** Manually deliver the next batch (also used to demo without waiting for the interval). */
    public Optional<StandingQuest> deliverNow(Long id) {
        StandingQuest sq = store.standingQuest(id);
        if (sq == null) return Optional.empty();
        deliver(sq);
        store.touch();
        return Optional.of(sq);
    }

    private void deliver(StandingQuest sq) {
        long now = System.currentTimeMillis();
        int batch = sq.getDeliveriesCompleted() + 1;
        sq.getDeliveries().add(Delivery.builder()
                .batchNumber(batch).deliveredAt(now).sampleCount(sq.getBatchSize())
                .freshnessScore(100).note("Auto-delivered batch")
                .build());
        sq.setDeliveriesCompleted(batch);
        sq.setNextDeliveryAt(now + intervalMillis(sq.getFrequency()));
    }

    private long intervalMillis(String frequency) {
        long day = 24L * 3600 * 1000;
        if (frequency == null) return 7 * day;
        return switch (frequency.toUpperCase()) {
            case "BIWEEKLY" -> 14 * day;
            case "MONTHLY" -> 30 * day;
            default -> 7 * day; // WEEKLY
        };
    }

    /** Advance any due active subscriptions. Checks every 5 minutes. */
    @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L)
    void runDueDeliveries() {
        long now = System.currentTimeMillis();
        for (StandingQuest sq : store.standingQuests()) {
            if ("ACTIVE".equals(sq.getStatus()) && sq.getNextDeliveryAt() <= now) {
                deliver(sq);
                log.info("Standing quest {} delivered batch {}", sq.getId(), sq.getDeliveriesCompleted());
            }
        }
        store.touch();
    }
}
