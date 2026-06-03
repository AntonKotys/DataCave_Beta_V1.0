package com.example.datacave_beta.service;

import com.example.datacave_beta.model.Dataset;
import com.example.datacave_beta.model.Quest;
import com.example.datacave_beta.model.QuestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final Store store;

    public List<Quest> allQuests() {
        return store.quests().stream()
                .sorted(Comparator.comparing(Quest::getId))
                .toList();
    }

    public List<Quest> questsByCategory(String category) {
        return store.quests().stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(Quest::getId))
                .toList();
    }

    public Optional<Quest> quest(Long id) {
        return Optional.ofNullable(store.quest(id));
    }

    /** Company-side: create a new quest from the "Post a Quest" form. */
    public Quest createQuest(QuestRequest input) {
        long id = store.nextQuestId();
        double reward = input.getReward() != null && input.getReward() > 0 ? input.getReward() : 1.0;
        int required = input.getRequiredCount() != null && input.getRequiredCount() > 0 ? input.getRequiredCount() : 100;
        int minutes = input.getEstimatedMinutes() != null ? input.getEstimatedMinutes() : 0;
        Quest q = Quest.builder()
                .id(id)
                .title(orDefault(input.getTitle(), "Untitled Quest"))
                .description(orDefault(input.getDescription(), ""))
                .category(orDefault(input.getCategory(), "Text & Label"))
                .categoryIcon(iconFor(input.getCategory()))
                .reward(reward)
                .difficulty(orDefault(input.getDifficulty(), "Easy"))
                .deadline(orDefault(input.getDeadline(), "Open"))
                .requiredCount(required)
                .completedCount(0)
                .company(orDefault(input.getCompany(), "Your Company"))
                .tags(input.getTags() != null ? input.getTags() : List.of())
                .status("OPEN")
                .estimatedMinutes(minutes)
                .build();
        store.putQuest(q);
        return q;
    }

    public List<Dataset> allDatasets() {
        return store.datasets().stream()
                .sorted(Comparator.comparing(Dataset::getId))
                .toList();
    }

    public Optional<Dataset> dataset(Long id) {
        return Optional.ofNullable(store.dataset(id));
    }

    private static String orDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    public static String iconFor(String category) {
        if (category == null) return "🏷️";
        return switch (category) {
            case "Audio" -> "🎙️";
            case "Image" -> "📸";
            case "Motion & Location" -> "📍";
            case "Health Data" -> "🩺";
            case "Video" -> "🎬";
            case "Survey" -> "📋";
            default -> "🏷️";
        };
    }
}
