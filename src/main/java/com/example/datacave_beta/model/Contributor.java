package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Persistent contributor profile. Live totals (earnings, completed quests,
 * active quests, recent earnings) are computed in DashboardService from
 * the contributor's submissions + acceptances on top of these seed values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contributor {
    private Long id;
    private String name;
    private String tier;
    private int tierProgress;
    private int tierTarget;
    private int reputationScore;
    private int globalRank;
    private int streak;
    // Seed offsets so the demo profile looks established; new submissions add on top.
    private double baseEarnings;
    private double baseWeekly;
    private double baseMonthly;
    private int baseCompleted;
    private List<Badge> badges;
    private List<EarningsEntry> seededEarnings;

    // --- expert tier (B) ----------------------------------------------------
    private boolean expertVerified;        // passed credential verification
    private List<String> expertDomains;    // verified domains, e.g. ["fashion", "surgical"]
    private String expertStatus;           // null, "PENDING", "VERIFIED", "REJECTED"
}
