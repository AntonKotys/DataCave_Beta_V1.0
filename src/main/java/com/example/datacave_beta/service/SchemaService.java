package com.example.datacave_beta.service;

import com.example.datacave_beta.model.AnnotationSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Annotation schema library (N). */
@Service
@RequiredArgsConstructor
public class SchemaService {

    private final Store store;

    public List<AnnotationSchema> all() {
        return store.schemas().stream().sorted(Comparator.comparing(AnnotationSchema::getId)).toList();
    }

    public Optional<AnnotationSchema> get(Long id) {
        return Optional.ofNullable(store.schema(id));
    }

    public List<AnnotationSchema> byVertical(String vertical) {
        return store.schemas().stream()
                .filter(s -> s.getVertical().equalsIgnoreCase(vertical))
                .toList();
    }
}
