package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Result of auditing an uploaded dataset (M). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditReport {
    private String fileName;
    private String format;          // CSV or JSON
    private int rowCount;
    private int columnCount;
    private List<String> columns;
    private int duplicateRows;
    private int missingCells;
    private double missingPct;
    private int qualityScore;       // 0-100 overall
    private Map<String, Object> classBalance; // value -> count for the first categorical column
    private List<String> issues;    // human-readable findings
    private List<String> strengths;
}
