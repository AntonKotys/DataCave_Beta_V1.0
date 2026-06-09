package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One delivered batch from a standing quest (E). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    private int batchNumber;
    private long deliveredAt;   // epoch millis
    private int sampleCount;
    private int freshnessScore; // 100 at delivery, decays over time
    private String note;
}
