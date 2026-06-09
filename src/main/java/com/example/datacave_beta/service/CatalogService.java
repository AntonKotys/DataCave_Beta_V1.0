package com.example.datacave_beta.service;

import com.example.datacave_beta.model.AnnotationSchema;
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
        String title = orDefault(input.getTitle(), "Untitled Quest");

        // --- resolve labeling schema ----------------------------------------
        Long schemaId = input.getSchemaId();

        // If custom labels are defined, auto-create a schema for this quest.
        if (schemaId == null
                && input.getCustomLabels() != null
                && !input.getCustomLabels().isEmpty()) {
            String schemaName = orDefault(input.getSchemaName(), title + " Labels");
            long sid = store.nextSchemaId();
            AnnotationSchema schema = AnnotationSchema.builder()
                    .id(sid)
                    .name(schemaName)
                    .vertical(orDefault(input.getCategory(), "Custom"))
                    .description("Custom labeling schema for quest: " + title)
                    .version("1.0")
                    .rating(0)
                    .usageCount(1)
                    .fields(input.getCustomLabels())
                    .build();
            store.putSchema(schema);
            schemaId = sid;
        } else if (schemaId != null) {
            // Increment usage counter on an existing schema.
            AnnotationSchema existing = store.schema(schemaId);
            if (existing != null) {
                existing.setUsageCount(existing.getUsageCount() + 1);
                store.touch();
            }
        }
        // --------------------------------------------------------------------

        Quest q = Quest.builder()
                .id(id)
                .title(title)
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
                .schemaId(schemaId)
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
