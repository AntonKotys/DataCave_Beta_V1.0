package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * A single piece of data a contributor submits toward a quest:
 * a photo, an audio recording, a survey response, a captured location, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private Long id;
    private Long questId;
    private String questTitle;
    private Long contributorId;
    private String type;          // PHOTO, AUDIO, SURVEY, LOCATION, LABEL
    private String fileName;      // stored file name on disk (null for data-only submissions)
    private String originalName;  // original upload name
    private String contentType;   // MIME type
    private Long fileSize;        // bytes
    private Map<String, Object> payload; // survey answers, gps coords, labels, etc.
    private String status;        // PENDING_REVIEW, ACCEPTED, REJECTED
    private double reward;         // amount credited if accepted
    private int qualityScore;     // 0-100 auto quality estimate
    private long submittedAt;     // epoch millis
    private long reviewedAt;      // epoch millis (0 if not reviewed)

    // --- AI pre-labeling (human-in-the-loop) --------------------------------
    private Map<String, Object> aiLabels; // labels the AI generated automatically
    private Map<String, Object> labels;   // final labels (human-confirmed/corrected)
    private boolean humanValidated;        // true once a contributor confirmed/edited the AI labels
    private String labelSource;            // "claude", "mock", or "manual"

    // --- quality verification -----------------------------------------------
    private int relevanceScore;   // 0-100 AI-assessed relevance to the quest
    private String relevanceNote; // short explanation (e.g. "Not an outfit photo")
}
