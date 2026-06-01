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
public class Dataset {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String size;
    private String format;
    private double price;
    private String license;
    private int qualityScore;
    private boolean ethicsVerified;
    private boolean consentDocumented;
    private boolean gdprCompliant;
    private int contributorCount;
    private String lastUpdated;
    private List<String> tags;
}
