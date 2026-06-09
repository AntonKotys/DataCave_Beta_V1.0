package com.example.datacave_beta.service;

import com.example.datacave_beta.model.Dataset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Freshness scoring + expiry alerts (G). Every dataset gets a freshness score
 * (0-100) based on how old it is relative to its domain's decay window.
 */
@Service
@RequiredArgsConstructor
public class FreshnessService {

    private final Store store;

    @Value("${datacave.freshness.alert-threshold:50}")
    private int alertThreshold;

    /** Datasets enriched with live freshness info. */
    public List<Map<String, Object>> datasetsWithFreshness() {
        long now = System.currentTimeMillis();
        return store.datasets().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(d -> enrich(d, now))
                .toList();
    }

    private Map<String, Object> enrich(Dataset d, long now) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dataset", d);
        int ageDays = d.getCollectedAt() > 0 ? (int) ((now - d.getCollectedAt()) / (24L * 3600 * 1000)) : -1;
        int decay = d.getDomainDecayDays() > 0 ? d.getDomainDecayDays() : 365;
        int score = freshnessScore(ageDays, decay);
        m.put("ageDays", ageDays);
        m.put("decayDays", decay);
        m.put("freshnessScore", score);
        m.put("stale", score < alertThreshold);
        m.put("needsRefresh", score < alertThreshold);
        m.put("status", score >= 80 ? "Fresh" : score >= alertThreshold ? "Aging" : "Stale");
        return m;
    }

    /** Linear decay: 100 at collection, 0 once age reaches the decay window. */
    public int freshnessScore(int ageDays, int decayDays) {
        if (ageDays < 0) return 100;
        if (decayDays <= 0) return 100;
        int score = (int) Math.round(100.0 * (1.0 - (double) ageDays / decayDays));
        return Math.max(0, Math.min(100, score));
    }
}
