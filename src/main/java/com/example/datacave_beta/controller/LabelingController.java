package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.AnnotationSchema;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.service.CatalogService;
import com.example.datacave_beta.service.LabelingService;
import com.example.datacave_beta.service.SchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI pre-labeling endpoint. The contributor uploads a photo; we return
 * structured labels (Claude vision, or a mock when no API key) for them to
 * confirm/correct before submitting.
 */
@RestController
@RequestMapping("/api/labeling")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LabelingController {

    private final LabelingService labelingService;
    private final CatalogService catalog;
    private final SchemaService schemas;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Long questId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long schemaId) {
        try {
            String cat = category;
            AnnotationSchema schema = null;

            if (questId != null) {
                Quest q = catalog.quest(questId).orElse(null);
                if (q != null) {
                    cat = q.getCategory();
                    if (q.getSchemaId() != null) schema = schemas.get(q.getSchemaId()).orElse(null);
                }
            }
            if (schema == null && schemaId != null) schema = schemas.get(schemaId).orElse(null);

            LabelingService.LabelResult result = labelingService.analyze(
                    file.getBytes(), file.getContentType(), cat, schema);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("labels", result.getLabels());
            body.put("source", result.getSource());
            body.put("model", result.getModel());
            body.put("schema", schema != null ? schema.getName() : null);
            // Include full field definitions so the frontend can render proper controls.
            body.put("schemaFields", schema != null ? schema.getFields() : null);
            body.put("relevanceScore", result.getRelevanceScore());
            body.put("relevanceNote", result.getRelevanceNote());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
