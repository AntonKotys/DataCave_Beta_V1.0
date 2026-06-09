package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Inter-annotator agreement report for a quest (D). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaaReport {
    private Long questId;
    private String questTitle;
    private int annotatorCount;          // distinct contributors
    private int labeledSubmissions;      // total label submissions analysed
    private int overlappingItems;        // items with >= 2 independent labels
    private double agreementScore;       // 0-100 percent agreement
    private Map<String, Double> perFieldAgreement; // field -> agreement %
    private String interpretation;       // "Excellent", "Acceptable", "Needs review"
    private boolean meetsThreshold;      // >= configured threshold
    private List<String> flaggedFields;  // fields below threshold
}
