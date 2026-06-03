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
}
