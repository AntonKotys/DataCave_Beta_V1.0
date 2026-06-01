package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsEntry {
    private String questTitle;
    private double amount;
    private String date;
    private String status;
}
