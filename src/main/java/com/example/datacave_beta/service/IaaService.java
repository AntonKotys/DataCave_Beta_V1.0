package com.example.datacave_beta.service;

import com.example.datacave_beta.model.IaaReport;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.model.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Inter-annotator agreement (D). Computes percent agreement across the label
 * submissions for a quest, per field, so a buyer can see consistency before
 * trusting the dataset.
 */
@Service
@RequiredArgsConstructor
public class IaaService {

    private final Store store;

    @Value("${datacave.iaa.threshold:75}")
    private int threshold;

    public IaaReport forQuest(Long questId) {
        Quest quest = store.quest(questId);
        String title = quest != null ? quest.getTitle() : ("Quest " + questId);

        // Gather label-bearing submissions for this quest.
        List<Map<String, Object>> labelSets = new ArrayList<>();
        Set<Long> annotators = new HashSet<>();
        for (Submission s : store.submissions()) {
            if (!questId.equals(s.getQuestId())) continue;
            Map<String, Object> labels = s.getLabels() != null ? s.getLabels() : s.getPayload();
            if (labels == null || labels.isEmpty()) continue;
            labelSets.add(labels);
            annotators.add(s.getContributorId());
        }

        if (labelSets.size() < 2) {
            return IaaReport.builder()
                    .questId(questId).questTitle(title)
                    .annotatorCount(annotators.size()).labeledSubmissions(labelSets.size())
                    .overlappingItems(0).agreementScore(0)
                    .perFieldAgreement(Map.of())
                    .interpretation("Not enough data")
                    .meetsThreshold(false).flaggedFields(List.of())
                    .build();
        }

        // Union of fields across all label sets.
        Set<String> fields = new LinkedHashSet<>();
        labelSets.forEach(l -> fields.addAll(l.keySet()));

        Map<String, Double> perField = new LinkedHashMap<>();
        List<String> flagged = new ArrayList<>();
        double sum = 0;
        int counted = 0;
        for (String field : fields) {
            List<String> values = labelSets.stream()
                    .filter(l -> l.get(field) != null)
                    .map(l -> String.valueOf(l.get(field)))
                    .toList();
            if (values.size() < 2) continue;
            // percent that match the modal value
            Map<String, Long> counts = new HashMap<>();
            values.forEach(v -> counts.merge(v, 1L, Long::sum));
            long modal = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
            double agreement = Math.round((100.0 * modal / values.size()) * 10) / 10.0;
            perField.put(field, agreement);
            if (agreement < threshold) flagged.add(field);
            sum += agreement;
            counted++;
        }

        double overall = counted == 0 ? 0 : Math.round((sum / counted) * 10) / 10.0;
        String interpretation = overall >= 90 ? "Excellent" : overall >= threshold ? "Acceptable" : "Needs review";

        return IaaReport.builder()
                .questId(questId).questTitle(title)
                .annotatorCount(annotators.size()).labeledSubmissions(labelSets.size())
                .overlappingItems(labelSets.size()).agreementScore(overall)
                .perFieldAgreement(perField)
                .interpretation(interpretation)
                .meetsThreshold(overall >= threshold)
                .flaggedFields(flagged)
                .build();
    }
}
