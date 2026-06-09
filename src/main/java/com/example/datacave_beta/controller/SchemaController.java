package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.AnnotationSchema;
import com.example.datacave_beta.service.SchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Annotation schema library (N). */
@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SchemaController {

    private final SchemaService schemas;

    @GetMapping
    public List<AnnotationSchema> all(@RequestParam(required = false) String vertical) {
        return (vertical != null && !vertical.isBlank()) ? schemas.byVertical(vertical) : schemas.all();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnotationSchema> get(@PathVariable Long id) {
        return schemas.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
