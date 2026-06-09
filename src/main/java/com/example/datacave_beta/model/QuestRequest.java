package com.example.datacave_beta.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Inbound payload for the "Post a Quest" form. Uses wrapper types so Jackson
 * deserializes via the no-args constructor + setters and missing fields stay null
 * (avoids the primitive-creator strictness in Jackson 3).
 */
@Data
@NoArgsConstructor
public class QuestRequest {
    private String title;
    private String description;
    private String category;
    private Double reward;
    private String difficulty;
    private String deadline;
    private Integer requiredCount;
    private String company;
    private Integer estimatedMinutes;
    private List<String> tags;

    // --- labeling requirements -------------------------------------------
    /** Attach an existing schema from the library by ID. */
    private Long schemaId;

    /**
     * Define custom label fields inline. If provided (and schemaId is null),
     * a new AnnotationSchema is auto-created and attached to the quest.
     */
    private List<SchemaField> customLabels;

    /** Name for the auto-created schema. Defaults to "{title} Labels". */
    private String schemaName;
}
