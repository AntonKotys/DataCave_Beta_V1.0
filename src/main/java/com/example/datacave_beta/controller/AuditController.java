package com.example.datacave_beta.controller;

import com.example.datacave_beta.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Dataset quality audit (M). */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> audit(@RequestParam MultipartFile file) {
        try {
            return ResponseEntity.ok(auditService.audit(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not audit file: " + e.getMessage()));
        }
    }
}
