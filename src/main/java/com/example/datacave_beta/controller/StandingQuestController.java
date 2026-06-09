package com.example.datacave_beta.controller;

import com.example.datacave_beta.model.StandingQuest;
import com.example.datacave_beta.service.StandingQuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Standing quests / data subscriptions (E). */
@RestController
@RequestMapping("/api/standing-quests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StandingQuestController {

    private final StandingQuestService service;

    @GetMapping
    public List<StandingQuest> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandingQuest> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StandingQuest create(@RequestBody StandingQuest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<StandingQuest> pause(@PathVariable Long id) {
        return service.setStatus(id, "PAUSED").map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<StandingQuest> resume(@PathVariable Long id) {
        return service.setStatus(id, "ACTIVE").map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<StandingQuest> deliver(@PathVariable Long id) {
        return service.deliverNow(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
