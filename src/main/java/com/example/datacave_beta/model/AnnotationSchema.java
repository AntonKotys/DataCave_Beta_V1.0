package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A reusable, versioned annotation schema for a vertical (N). Buyers start from
 * a proven schema instead of inventing one — reducing inter-annotator drift (D).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationSchema {
    private Long id;
    private String name;
    private String vertical;     // Fashion, Surgical, Marketing, Retail, NLP ...
    private String description;
    private String version;      // e.g. "1.0"
    private int rating;          // community rating 0-100
    private int usageCount;      // how many quests use it
    private List<SchemaField> fields;
}
