package com.example.datacave_beta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** One field in an annotation schema (N). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaField {
    private String name;
    private String type;          // text, single-choice, multi-choice, number, boolean
    private String description;
    private List<String> options; // for choice types
    private boolean required;
}
