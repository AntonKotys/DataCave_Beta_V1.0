package com.example.datacave_beta.service;

import com.example.datacave_beta.model.Contributor;
import com.example.datacave_beta.model.Quest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Expert contributor tier (B). Verifies domain credentials and gates
 * specialist / tier-restricted quests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpertService {

    private final Store store;

    private static final Map<String, Integer> TIER_RANK = Map.of(
            "Bronze Contributor", 1,
            "Silver Mapper", 2,
            "Gold Researcher", 3,
            "Verified Expert", 4
    );

    /**
     * Prototype credential check: instantly verifies the contributor in the
     * requested domains. In production this would route to manual review.
     */
    public Contributor verify(Long contributorId, List<String> domains) {
        Contributor c = store.contributor(contributorId);
        if (c == null) throw new IllegalArgumentException("Unknown contributor " + contributorId);
        c.setExpertVerified(true);
        c.setExpertStatus("VERIFIED");
        c.setExpertDomains(domains != null ? domains : List.of());
        store.putContributor(c);
        log.info("Contributor {} verified as expert in {}", contributorId, domains);
        return c;
    }

    /**
     * @return null if the contributor may take the quest, otherwise a human-readable reason.
     */
    public String lockedReason(Quest quest, Contributor c) {
        if (quest == null) return "Unknown quest";
        if (c == null) return "Unknown contributor";

        if (quest.isExpertOnly()) {
            boolean ok = c.isExpertVerified()
                    && c.getExpertDomains() != null
                    && quest.getDomain() != null
                    && c.getExpertDomains().contains(quest.getDomain());
            if (!ok) {
                return "Requires Verified Expert status in the \"" + quest.getDomain() + "\" domain";
            }
        }

        if (quest.getRequiredTier() != null && !quest.getRequiredTier().isBlank()) {
            int need = TIER_RANK.getOrDefault(quest.getRequiredTier(), 0);
            int have = TIER_RANK.getOrDefault(c.getTier(), 1);
            if (c.isExpertVerified()) have = Math.max(have, TIER_RANK.get("Verified Expert"));
            if (have < need) {
                return "Requires " + quest.getRequiredTier() + " tier or higher";
            }
        }
        return null;
    }
}
