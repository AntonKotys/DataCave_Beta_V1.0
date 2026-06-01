package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributorStats {
    private Long id;
    private String name;
    private String tier;
    private int tierProgress;
    private int tierTarget;
    private double totalEarnings;
    private double weeklyEarnings;
    private double monthlyEarnings;
    private int completedQuests;
    private int reputationScore;
    private int globalRank;
    private int streak;
    private List<Quest> activeQuests;
    private List<Badge> badges;
    private List<EarningsEntry> recentEarnings;
}
