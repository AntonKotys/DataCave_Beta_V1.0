package com.example.datacave_beta.service;

import com.example.datacave_beta.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DataService {

    private final List<Quest> quests = List.of(
        Quest.builder().id(1L).title("Urban Street Audio Collection").description("Record 30 seconds of ambient street noise in your city — traffic, voices, environment. No music.").category("Audio").categoryIcon("🎙️").reward(2.50).difficulty("Easy").deadline("Jun 15, 2026").requiredCount(500).completedCount(312).company("SoundAI Labs").tags(List.of("audio", "urban", "ambient")).status("OPEN").estimatedMinutes(5).build(),
        Quest.builder().id(2L).title("Grocery Shelf Photo Mapping").description("Photograph grocery store shelves in your local supermarket. Capture product labels clearly.").category("Image").categoryIcon("📸").reward(3.75).difficulty("Easy").deadline("Jun 20, 2026").requiredCount(1000).completedCount(734).company("RetailVision AI").tags(List.of("image", "retail", "computer-vision")).status("OPEN").estimatedMinutes(10).build(),
        Quest.builder().id(3L).title("Emotion Labeling – Short Video Clips").description("Watch 10-second video clips and rate the emotional content displayed. Multiple choice labeling.").category("Text & Label").categoryIcon("🏷️").reward(1.80).difficulty("Easy").deadline("Jun 25, 2026").requiredCount(2000).completedCount(1450).company("EmotiSense").tags(List.of("labeling", "emotion", "video")).status("OPEN").estimatedMinutes(8).build(),
        Quest.builder().id(4L).title("Dialect Speech Recording – Georgian").description("Read 20 short sentences aloud in your native Georgian dialect. Clear pronunciation required.").category("Audio").categoryIcon("🎙️").reward(5.00).difficulty("Medium").deadline("Jul 1, 2026").requiredCount(300).completedCount(89).company("LinguaCore").tags(List.of("audio", "speech", "language")).status("OPEN").estimatedMinutes(15).build(),
        Quest.builder().id(5L).title("Pedestrian Path Tracking").description("Walk your usual commute route while the app records anonymized movement patterns via GPS.").category("Motion & Location").categoryIcon("📍").reward(4.20).difficulty("Medium").deadline("Jul 5, 2026").requiredCount(800).completedCount(421).company("UrbanFlow Analytics").tags(List.of("location", "motion", "mobility")).status("OPEN").estimatedMinutes(20).build(),
        Quest.builder().id(6L).title("Medical Symptom Survey – Sleep Patterns").description("Complete a structured 15-minute survey about your sleep quality and habits. Consent required.").category("Health Data").categoryIcon("🩺").reward(8.00).difficulty("Medium").deadline("Jul 10, 2026").requiredCount(1500).completedCount(203).company("HealthAI Research").tags(List.of("health", "survey", "sleep")).status("OPEN").estimatedMinutes(15).build(),
        Quest.builder().id(7L).title("Handwriting Digitization Samples").description("Write 10 short paragraphs by hand and photograph them. Helps train handwriting recognition models.").category("Image").categoryIcon("📸").reward(3.00).difficulty("Easy").deadline("Jul 15, 2026").requiredCount(600).completedCount(580).company("DocuSense AI").tags(List.of("image", "handwriting", "ocr")).status("OPEN").estimatedMinutes(12).build(),
        Quest.builder().id(8L).title("Real-time Traffic Scene Classification").description("Review dashcam-style images and classify road conditions, hazards, and vehicle types.").category("Text & Label").categoryIcon("🏷️").reward(2.20).difficulty("Medium").deadline("Jun 30, 2026").requiredCount(5000).completedCount(3800).company("AutoDrive Labs").tags(List.of("labeling", "traffic", "automotive")).status("OPEN").estimatedMinutes(6).build(),
        Quest.builder().id(9L).title("Wearable Heart Rate Data Contribution").description("Share 7 days of anonymized heart rate data from your smartwatch or fitness tracker.").category("Health Data").categoryIcon("🩺").reward(12.00).difficulty("Hard").deadline("Jul 20, 2026").requiredCount(200).completedCount(34).company("CardioAI").tags(List.of("health", "wearable", "biometric")).status("OPEN").estimatedMinutes(0).build(),
        Quest.builder().id(10L).title("Product Review Sentiment Tagging").description("Read e-commerce product reviews and tag them: positive, negative, neutral, mixed. 50 reviews per session.").category("Text & Label").categoryIcon("🏷️").reward(2.00).difficulty("Easy").deadline("Jun 28, 2026").requiredCount(10000).completedCount(6200).company("ShopMind AI").tags(List.of("text", "nlp", "sentiment")).status("OPEN").estimatedMinutes(20).build()
    );

    private final List<Dataset> datasets = List.of(
        Dataset.builder().id(1L).name("Urban Audio Dataset v2").description("500+ hours of consented urban ambient audio across 12 cities. Fully labeled and ethics-verified.").category("Audio").size("48 GB").format("WAV / JSON metadata").price(4500).license("Commercial Research License").qualityScore(94).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(312).lastUpdated("May 2026").tags(List.of("audio", "urban", "ambient")).build(),
        Dataset.builder().id(2L).name("Retail Shelf Image Corpus").description("220,000 annotated grocery shelf images from 8 countries. Bounding boxes on 1,200+ product categories.").category("Image").size("130 GB").format("JPEG / COCO JSON").price(8900).license("Enterprise License").qualityScore(91).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(734).lastUpdated("May 2026").tags(List.of("image", "retail", "cv")).build(),
        Dataset.builder().id(3L).name("Multilingual Dialect Speech Pack").description("Recorded speech in 14 under-resourced language dialects. Native speakers, verified pronunciation.").category("Audio").size("22 GB").format("MP3 / TextGrid").price(6200).license("Research License").qualityScore(97).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(89).lastUpdated("Apr 2026").tags(List.of("audio", "speech", "language")).build(),
        Dataset.builder().id(4L).name("Sleep Quality Survey Dataset").description("Structured sleep pattern surveys from 203 verified contributors with demographic metadata.").category("Health").size("2.1 GB").format("CSV / Parquet").price(3100).license("Medical Research License").qualityScore(89).ethicsVerified(true).consentDocumented(true).gdprCompliant(true).contributorCount(203).lastUpdated("May 2026").tags(List.of("health", "survey", "wellness")).build()
    );

    private final ContributorStats demo = ContributorStats.builder()
        .id(1L).name("Alex M.").tier("Silver Mapper").tierProgress(340).tierTarget(500)
        .totalEarnings(847.30).weeklyEarnings(42.50).monthlyEarnings(163.20)
        .completedQuests(73).reputationScore(88).globalRank(1204).streak(12)
        .activeQuests(quests.subList(0, 3))
        .badges(List.of(
            Badge.builder().name("First Quest").icon("🚀").description("Completed your first quest").earnedDate("Jan 2026").build(),
            Badge.builder().name("Audio Expert").icon("🎙️").description("Completed 20+ audio quests").earnedDate("Feb 2026").build(),
            Badge.builder().name("Week Streak").icon("🔥").description("Active 7 days in a row").earnedDate("Mar 2026").build(),
            Badge.builder().name("Quality Star").icon("⭐").description("95%+ acceptance rate for 30 quests").earnedDate("Apr 2026").build(),
            Badge.builder().name("City Scout").icon("🗺️").description("Completed 5 location quests").earnedDate("Apr 2026").build(),
            Badge.builder().name("Top 5%").icon("🏆").description("Ranked in top 5% globally").earnedDate("May 2026").build()
        ))
        .recentEarnings(List.of(
            EarningsEntry.builder().questTitle("Urban Street Audio Collection").amount(2.50).date("Jun 1, 2026").status("PAID").build(),
            EarningsEntry.builder().questTitle("Grocery Shelf Photo Mapping").amount(3.75).date("May 31, 2026").status("PAID").build(),
            EarningsEntry.builder().questTitle("Emotion Labeling – Short Video Clips").amount(1.80).date("May 30, 2026").status("PAID").build(),
            EarningsEntry.builder().questTitle("Product Review Sentiment Tagging").amount(2.00).date("May 29, 2026").status("PAID").build(),
            EarningsEntry.builder().questTitle("Real-time Traffic Scene Classification").amount(2.20).date("May 28, 2026").status("PAID").build(),
            EarningsEntry.builder().questTitle("Dialect Speech Recording – Georgian").amount(5.00).date("May 27, 2026").status("PAID").build()
        ))
        .build();

    public List<Quest> getAllQuests() { return quests; }

    public Optional<Quest> getQuestById(Long id) {
        return quests.stream().filter(q -> q.getId().equals(id)).findFirst();
    }

    public List<Quest> getQuestsByCategory(String category) {
        return quests.stream().filter(q -> q.getCategory().equalsIgnoreCase(category)).toList();
    }

    public ContributorStats getContributorStats(Long id) { return demo; }

    public List<Dataset> getAllDatasets() { return datasets; }

    public Optional<Dataset> getDatasetById(Long id) {
        return datasets.stream().filter(d -> d.getId().equals(id)).findFirst();
    }
}
