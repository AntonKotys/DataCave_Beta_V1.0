package com.example.datacave_beta.service;

import com.example.datacave_beta.model.AuditReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dataset quality audit (M). Buyers upload an existing dataset (CSV/JSON) and
 * get a quick health report: row/column counts, duplicates, missing values,
 * class balance, and an overall quality score.
 */
@Service
@Slf4j
public class AuditService {

    private final ObjectMapper mapper = new ObjectMapper();

    public AuditReport audit(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        boolean json = name.toLowerCase().endsWith(".json") || content.trim().startsWith("[") || content.trim().startsWith("{");
        return json ? auditJson(name, content) : auditCsv(name, content);
    }

    // ---- CSV ----------------------------------------------------------------

    private AuditReport auditCsv(String name, String content) {
        String[] lines = content.replace("\r", "").split("\n");
        List<String> nonEmpty = new ArrayList<>();
        for (String l : lines) if (!l.isBlank()) nonEmpty.add(l);
        if (nonEmpty.isEmpty()) return empty(name, "CSV");

        String[] header = nonEmpty.get(0).split(",", -1);
        List<String> columns = Arrays.stream(header).map(String::trim).toList();
        int cols = columns.size();

        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < nonEmpty.size(); i++) rows.add(nonEmpty.get(i).split(",", -1));

        int missing = 0;
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        Map<String, Integer> colDistinct = new HashMap<>();
        List<Set<String>> distinctValues = new ArrayList<>();
        for (int c = 0; c < cols; c++) distinctValues.add(new HashSet<>());

        for (String[] row : rows) {
            String key = String.join("", row);
            if (!seen.add(key)) duplicates++;
            for (int c = 0; c < cols; c++) {
                String val = c < row.length ? row[c].trim() : "";
                if (val.isEmpty()) missing++;
                else distinctValues.get(c).add(val);
            }
        }

        // class balance: most categorical column — fewest distinct values in [2,12],
        // skipping near-unique columns (likely IDs).
        int bestCol = -1, bestDistinct = Integer.MAX_VALUE;
        for (int c = 0; c < cols; c++) {
            int d = distinctValues.get(c).size();
            colDistinct.put(columns.get(c), d);
            if (d >= 2 && d <= 12 && d < rows.size() && d < bestDistinct) {
                bestDistinct = d;
                bestCol = c;
            }
        }
        Map<String, Object> balance = new LinkedHashMap<>();
        if (bestCol >= 0) {
            final int bc = bestCol;
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String[] row : rows) {
                String v = bc < row.length ? row[bc].trim() : "(empty)";
                counts.merge(v.isEmpty() ? "(empty)" : v, 1L, Long::sum);
            }
            balance.put("column", columns.get(bestCol));
            balance.put("counts", counts);
        }

        return score(name, "CSV", rows.size(), cols, columns, duplicates, missing, balance);
    }

    // ---- JSON ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private AuditReport auditJson(String name, String content) throws Exception {
        Object parsed = mapper.readValue(content, Object.class);
        List<Map<String, Object>> records = new ArrayList<>();
        if (parsed instanceof List<?> list) {
            for (Object o : list) if (o instanceof Map) records.add((Map<String, Object>) o);
        } else if (parsed instanceof Map) {
            records.add((Map<String, Object>) parsed);
        }
        if (records.isEmpty()) return empty(name, "JSON");

        Set<String> cols = new LinkedHashSet<>();
        records.forEach(r -> cols.addAll(r.keySet()));
        List<String> columns = new ArrayList<>(cols);

        int missing = 0;
        Set<String> seen = new HashSet<>();
        int duplicates = 0;
        for (Map<String, Object> r : records) {
            if (!seen.add(r.toString())) duplicates++;
            for (String c : columns) {
                Object v = r.get(c);
                if (v == null || String.valueOf(v).isBlank()) missing++;
            }
        }

        // class balance on first column with few distinct values
        Map<String, Object> balance = new LinkedHashMap<>();
        for (String c : columns) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Map<String, Object> r : records) {
                Object v = r.get(c);
                counts.merge(v == null ? "(null)" : String.valueOf(v), 1L, Long::sum);
            }
            if (counts.size() >= 2 && counts.size() <= 12) {
                balance.put("column", c);
                balance.put("counts", counts);
                break;
            }
        }

        return score(name, "JSON", records.size(), columns.size(), columns, duplicates, missing, balance);
    }

    // ---- scoring ------------------------------------------------------------

    private AuditReport score(String name, String format, int rows, int cols, List<String> columns,
                              int duplicates, int missing, Map<String, Object> balance) {
        int cells = Math.max(1, rows * cols);
        double missingPct = Math.round((100.0 * missing / cells) * 10) / 10.0;
        double dupPct = rows == 0 ? 0 : 100.0 * duplicates / rows;

        int quality = 100;
        List<String> issues = new ArrayList<>();
        List<String> strengths = new ArrayList<>();

        if (missingPct > 0) {
            quality -= (int) Math.min(40, missingPct * 1.5);
            issues.add(String.format("%.1f%% of cells are missing/empty (%d cells)", missingPct, missing));
        } else strengths.add("No missing values detected");

        if (duplicates > 0) {
            quality -= (int) Math.min(30, dupPct);
            issues.add(String.format("%d duplicate rows (%.1f%%)", duplicates, dupPct));
        } else strengths.add("No duplicate rows");

        if (rows < 100) { quality -= 15; issues.add("Low row count (<100) — may be too small to train on"); }
        else strengths.add(rows + " rows is a reasonable sample size");

        if (balance.containsKey("counts")) {
            @SuppressWarnings("unchecked")
            Map<String, Long> counts = (Map<String, Long>) balance.get("counts");
            long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
            long min = counts.values().stream().mapToLong(Long::longValue).min().orElse(1);
            if (max > min * 5) {
                quality -= 10;
                issues.add("Class imbalance in '" + balance.get("column") + "' (ratio " + (max / Math.max(1, min)) + ":1)");
            } else {
                strengths.add("Reasonable class balance in '" + balance.get("column") + "'");
            }
        }

        quality = Math.max(0, Math.min(100, quality));
        if (issues.isEmpty()) issues.add("No major issues found");

        return AuditReport.builder()
                .fileName(name).format(format)
                .rowCount(rows).columnCount(cols).columns(columns)
                .duplicateRows(duplicates).missingCells(missing).missingPct(missingPct)
                .qualityScore(quality).classBalance(balance)
                .issues(issues).strengths(strengths)
                .build();
    }

    private AuditReport empty(String name, String format) {
        return AuditReport.builder()
                .fileName(name).format(format).rowCount(0).columnCount(0)
                .columns(List.of()).duplicateRows(0).missingCells(0).missingPct(0)
                .qualityScore(0).classBalance(Map.of())
                .issues(List.of("File appears empty or could not be parsed"))
                .strengths(List.of())
                .build();
    }
}
