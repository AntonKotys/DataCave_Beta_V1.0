package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Records that a contributor has started (accepted) a quest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acceptance {
    private Long id;
    private Long contributorId;
    private Long questId;
    private String status;   // ACTIVE, COMPLETED, ABANDONED
    private long acceptedAt; // epoch millis
}
