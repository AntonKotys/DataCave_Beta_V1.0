package com.example.datacave_beta.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.example.datacave_beta.model.AnnotationSchema;
import com.example.datacave_beta.model.SchemaField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * AI pre-labeling (human-in-the-loop) + quality/relevance verification.
 *
 * A single Claude call returns both the structured labels AND a relevance_score
 * (0-100) assessing whether the photo actually matches the quest type. Low-scoring
 * submissions are rejected before any reward is credited.
 *
 * If ANTHROPIC_API_KEY is not set a mock labeler runs, using file size as a
 * crude proxy for a real photo so the quality gate is still demonstrable.
 */
@Service
@Slf4j
public class LabelingService {

    private final ObjectMapper mapper = new ObjectMapper();
    private AnthropicClient client;

    @Value("${datacave.labeling.model:claude-opus-4-8}")
    private String model;

    public LabelingService() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            try {
                this.client = AnthropicOkHttpClient.fromEnv();
                log.info("LabelingService: Claude vision enabled.");
            } catch (Exception e) {
                log.warn("LabelingService: could not init Anthropic client ({}); using mock labeler.", e.getMessage());
            }
        } else {
            log.info("LabelingService: no ANTHROPIC_API_KEY — using mock labeler.");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelResult {
        private Map<String, Object> labels;
        private String source;
        private String model;
        private int relevanceScore;   // 0-100: how relevant is the image to the quest?
        private String relevanceNote; // e.g. "Clear outfit photo" or "Not a food image"
    }

    /** Analyse an image: return structured labels + a relevance/quality score. */
    public LabelResult analyze(byte[] image, String contentType, String category, AnnotationSchema schema) {
        if (client == null) {
            return mockResult(image, category, schema);
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(image);
            String prompt = buildPrompt(category, schema);

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(1024L)
                    .addUserMessageOfBlockParams(List.of(
                            ContentBlockParam.ofImage(ImageBlockParam.builder()
                                    .source(Base64ImageSource.builder()
                                            .data(base64)
                                            .mediaType(mediaType(contentType))
                                            .build())
                                    .build()),
                            ContentBlockParam.ofText(TextBlockParam.builder().text(prompt).build())
                    ))
                    .build();

            Message response = client.messages().create(params);
            String text = response.content().stream()
                    .flatMap(b -> b.text().stream())
                    .map(t -> t.text())
                    .collect(Collectors.joining());

            Map<String, Object> full = parseJson(text);

            // Extract relevance fields, then remove them from the labels map.
            int relevance = toInt(full.remove("relevance_score"), 80);
            String note = full.getOrDefault("relevance_note", "").toString();
            full.remove("relevance_note");

            return LabelResult.builder()
                    .labels(full).source("claude").model(model)
                    .relevanceScore(relevance).relevanceNote(note)
                    .build();
        } catch (Exception e) {
            log.warn("Claude labeling failed ({}); falling back to mock.", e.getMessage());
            return mockResult(image, category, schema);
        }
    }

    // ---- prompt -------------------------------------------------------------

    private String buildPrompt(String category, AnnotationSchema schema) {
        StringBuilder sb = new StringBuilder();

        if (schema != null && schema.getFields() != null && !schema.getFields().isEmpty()) {
            sb.append("Analyze this image for the \"").append(schema.getName())
              .append("\" annotation schema.\n\n")
              .append("Return ONLY a JSON object (no prose, no markdown) with exactly these keys:\n");
            for (SchemaField f : schema.getFields()) {
                sb.append("- \"").append(f.getName()).append("\" (").append(f.getType()).append("): ")
                  .append(f.getDescription());
                if (f.getOptions() != null && !f.getOptions().isEmpty()) {
                    sb.append(" — valid values: ").append(String.join(", ", f.getOptions()));
                }
                if ("multi-choice".equals(f.getType())) {
                    sb.append(" — return as a JSON array, multiple values allowed");
                }
                if (f.isRequired()) sb.append(" [REQUIRED]");
                sb.append("\n");
            }
        } else {
            sb.append("Analyze this ").append(category != null ? category.toLowerCase() : "image")
              .append(" and return ONLY a JSON object with: ")
              .append("garment_types (array), primary_color (string), secondary_colors (array), ")
              .append("pattern (string), style_aesthetic (string), occasion (array of applicable values), ")
              .append("season (array of applicable seasons), trend_score (integer 1-10).\n");
        }

        // Always append relevance check.
        sb.append("\nAlso include these two quality fields in the same JSON object:\n")
          .append("- \"relevance_score\" (integer 0-100): how well does this image match a ")
          .append(category != null ? category : "image").append(" data collection quest? ")
          .append("100 = perfect match. 0 = completely irrelevant or not a real photo.\n")
          .append("- \"relevance_note\" (string): one short sentence explaining the score.\n\n")
          .append("Be accurate. Return only the JSON object.");
        return sb.toString();
    }

    private Base64ImageSource.MediaType mediaType(String contentType) {
        if (contentType == null) return Base64ImageSource.MediaType.IMAGE_JPEG;
        String c = contentType.toLowerCase();
        if (c.contains("png")) return Base64ImageSource.MediaType.IMAGE_PNG;
        if (c.contains("webp")) return Base64ImageSource.MediaType.IMAGE_WEBP;
        if (c.contains("gif")) return Base64ImageSource.MediaType.IMAGE_GIF;
        return Base64ImageSource.MediaType.IMAGE_JPEG;
    }

    private Map<String, Object> parseJson(String text) {
        try {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return mapper.readValue(text.substring(start, end + 1),
                        new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Could not parse model JSON: {}", e.getMessage());
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("raw", text);
        return fallback;
    }

    // ---- mock ---------------------------------------------------------------

    private LabelResult mockResult(byte[] image, String category, AnnotationSchema schema) {
        // Use file size as a crude proxy: a real photo is typically > 1 KB.
        // Very small bytes are almost certainly test data / invalid submissions.
        boolean looksReal = image != null && image.length > 1024;
        int relevance = looksReal
                ? ThreadLocalRandom.current().nextInt(72, 96)
                : ThreadLocalRandom.current().nextInt(5, 25);
        String note = looksReal
                ? "Image appears to be a real photo relevant to the quest."
                : "Image is too small or does not appear to be a real photo.";

        return LabelResult.builder()
                .labels(mockLabels(category, schema))
                .source("mock").model("heuristic")
                .relevanceScore(relevance).relevanceNote(note)
                .build();
    }

    private Map<String, Object> mockLabels(String category, AnnotationSchema schema) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (schema != null && schema.getFields() != null) {
            for (SchemaField f : schema.getFields()) {
                if ("multi-choice".equals(f.getType()) && f.getOptions() != null && f.getOptions().size() >= 2) {
                    // Return 1-2 random options for multi-choice fields.
                    List<String> opts = f.getOptions();
                    int pick = ThreadLocalRandom.current().nextInt(opts.size());
                    int pick2 = (pick + 1 + ThreadLocalRandom.current().nextInt(opts.size() - 1)) % opts.size();
                    m.put(f.getName(), List.of(opts.get(pick), opts.get(pick2)));
                } else if ("single-choice".equals(f.getType()) && f.getOptions() != null && !f.getOptions().isEmpty()) {
                    m.put(f.getName(), f.getOptions().get(ThreadLocalRandom.current().nextInt(f.getOptions().size())));
                } else if ("number".equals(f.getType())) {
                    m.put(f.getName(), ThreadLocalRandom.current().nextInt(1, 11));
                } else if ("boolean".equals(f.getType())) {
                    m.put(f.getName(), ThreadLocalRandom.current().nextBoolean());
                } else {
                    m.put(f.getName(), "");  // empty string, not "auto", so the user must fill it
                }
            }
            return m;
        }
        // Default fashion mock.
        m.put("garment_types", List.of("Dress"));
        m.put("primary_color", "");
        m.put("secondary_colors", List.of());
        m.put("pattern", "Solid");
        m.put("style_aesthetic", "Smart casual");
        m.put("occasion", List.of("Work", "Everyday"));
        m.put("season", List.of("Spring", "Autumn"));
        m.put("trend_score", ThreadLocalRandom.current().nextInt(6, 10));
        return m;
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        try { return ((Number) v).intValue(); } catch (ClassCastException e) {
            try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ex) { return def; }
        }
    }
}
