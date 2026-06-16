# DataCave

DataCave is an ethical, consent-first data marketplace that connects everyday **contributors** — who collect and label real-world data (photos, audio, surveys, locations) — with **enterprises and AI/ML teams** that need high-quality, legally clean training data. Contributors complete "quests" and earn rewards, while every submission goes through a quality gate and optional AI pre-labeling (a vision LLM suggests structured labels that the contributor then confirms or corrects). The platform ships as a Spring Boot backend with a web portal for both sides, plus a companion React Native (Expo) mobile app for contributors.

## Tech Stack

- **Backend / Web:** Spring Boot (Java 17), Gradle, static HTML/Tailwind portal
- **Mobile:** React Native + Expo (SDK 54)
- **AI labeling (optional):** multimodal vision LLM via the Anthropic API

---

## 1. Web version (backend + browser portal)

The backend serves the REST API **and** the web portal on port `8080`.

**Requirements:** Java 17+ (the Gradle wrapper handles the rest).

```bash
# from the project root
./gradlew bootRun
```

Then open in your browser:

| Page | URL |
|------|-----|
| Landing | http://localhost:8080/ |
| Browse & complete quests | http://localhost:8080/quests.html |
| Contributor dashboard | http://localhost:8080/dashboard.html |
| Company portal | http://localhost:8080/company.html |

Prototype data and uploads are stored locally under the `data/` folder.

### Optional: enable real AI auto-labeling
By default the app runs with a built-in mock labeler (no key required). To use real vision-based labeling, set an API key **before** starting the server:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew bootRun
```

---

## 2. Mobile version (Expo)

**Requirements:** Node.js 18+, the **Expo Go** app on your iOS/Android device (or a simulator).

```bash
cd mobile
npm install
npm start
```

This starts the Expo dev server and shows a QR code:

- **Physical phone:** open Expo Go and scan the QR code (phone and computer must be on the same Wi-Fi).
- **iOS simulator:** press `i` &nbsp;•&nbsp; **Android emulator:** press `a`.

The mobile app auto-detects your computer's LAN IP to reach the backend on port `8080`, so **make sure the backend (step 1) is running first**. If auto-detection fails, set your machine's IP manually in `mobile/src/api.js` (`MANUAL_HOST`).

---

## Project structure

```
.
├── src/main/java/...          # Spring Boot backend (controllers, services, models)
├── src/main/resources/static  # Web portal (index, quests, dashboard, company)
├── mobile/                     # Expo React Native app
└── data/                       # Local prototype state + uploaded files (gitignored)
```
