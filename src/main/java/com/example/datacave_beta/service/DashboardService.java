package com.example.datacave_beta.service;

import com.example.datacave_beta.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the live contributor dashboard from stored profile + the contributor's
 * acceptances and submissions (earnings, active quests and history are dynamic).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final Store store;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    public ContributorStats dashboard(Long contributorId) {
        Contributor c = store.contributor(contributorId);
        if (c == null) return null;

        long now = System.currentTimeMillis();
        long weekAgo = now - 7L * 24 * 3600 * 1000;
        long monthAgo = now - 30L * 24 * 3600 * 1000;

        List<Submission> accepted = store.submissions().stream()
                .filter(s -> s.getContributorId().equals(contributorId))
                .filter(s -> "ACCEPTED".equals(s.getStatus()))
                .sorted(Comparator.comparingLong(Submission::getSubmittedAt).reversed())
                .toList();

        double newTotal = accepted.stream().mapToDouble(Submission::getReward).sum();
        double newWeek = accepted.stream().filter(s -> s.getSubmittedAt() >= weekAgo).mapToDouble(Submission::getReward).sum();
        double newMonth = accepted.stream().filter(s -> s.getSubmittedAt() >= monthAgo).mapToDouble(Submission::getReward).sum();

        // Active quests = accepted-and-active acceptances resolved to quests.
        List<Quest> activeQuests = store.acceptances().stream()
                .filter(a -> a.getContributorId().equals(contributorId))
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .sorted(Comparator.comparingLong(Acceptance::getAcceptedAt))
                .map(a -> store.quest(a.getQuestId()))
                .filter(q -> q != null)
                .toList();

        // Recent earnings: live submissions first, then seeded history.
        List<EarningsEntry> recent = new ArrayList<>();
        for (Submission s : accepted) {
            recent.add(EarningsEntry.builder()
                    .questTitle(s.getQuestTitle())
                    .amount(s.getReward())
                    .date(DATE_FMT.format(Instant.ofEpochMilli(s.getSubmittedAt())))
                    .status("PAID")
                    .build());
        }
        if (c.getSeededEarnings() != null) recent.addAll(c.getSeededEarnings());
        List<EarningsEntry> recentTop = recent.stream().limit(8).toList();

        int completed = c.getBaseCompleted() + accepted.size();
        int tierProgress = Math.min(c.getTierTarget(), c.getTierProgress() + accepted.size() * 10);

        return ContributorStats.builder()
                .id(c.getId())
                .name(c.getName())
                .tier(c.getTier())
                .tierProgress(tierProgress)
                .tierTarget(c.getTierTarget())
                .totalEarnings(round(c.getBaseEarnings() + newTotal))
                .weeklyEarnings(round(c.getBaseWeekly() + newWeek))
                .monthlyEarnings(round(c.getBaseMonthly() + newMonth))
                .completedQuests(completed)
                .reputationScore(c.getReputationScore())
                .globalRank(c.getGlobalRank())
                .streak(c.getStreak())
                .activeQuests(activeQuests)
                .badges(c.getBadges())
                .recentEarnings(recentTop)
                .build();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
