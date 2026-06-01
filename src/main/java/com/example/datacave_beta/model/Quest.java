package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quest {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String categoryIcon;
    private double reward;
    private String difficulty;
    private String deadline;
    private int requiredCount;
    private int completedCount;
    private String company;
    private List<String> tags;
    private String status;
    private int estimatedMinutes;
}
