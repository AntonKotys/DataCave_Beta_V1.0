package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.Submission;
import com.example.datacave_beta.service.SubmissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubmissionController {

    private final SubmissionService service;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a submission. Accepts multipart/form-data so a photo or audio file
     * can be attached, plus an optional JSON "payload" for survey/location/label data.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(
            @RequestParam(defaultValue = "1") Long contributorId,
            @RequestParam Long questId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String payload,
            @RequestParam(required = false) String labels,
            @RequestParam(required = false) String aiLabels,
            @RequestParam(required = false, defaultValue = "false") boolean humanValidated,
            @RequestParam(required = false) String labelSource,
            @RequestParam(required = false, defaultValue = "false") boolean simulateReject) {
        try {
            Map<String, Object> data = parse(payload);
            Map<String, Object> finalLabels = parse(labels);
            Map<String, Object> ai = parse(aiLabels);
            Submission s = service.submit(contributorId, questId, type, file, data,
                    finalLabels, ai, humanValidated, labelSource, simulateReject);
            return ResponseEntity.status(HttpStatus.CREATED).body(s);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> parse(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        Submission s = service.submission(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        Path p = service.fileFor(s);
        if (p == null || !p.toFile().exists()) return ResponseEntity.notFound().build();
        MediaType mt = s.getContentType() != null
                ? MediaType.parseMediaType(s.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mt).body(new FileSystemResource(p));
    }
}
