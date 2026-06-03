# DataCave — iOS Mobile App (Expo / React Native)

The contributor-facing mobile app for DataCave. Talks to the same Spring Boot
backend as the web app (`/api/quests`, `/api/contributor`, `/api/datasets`).

## Screens

- **Quests** — browse live quests, filter by category, tap for full detail + "Accept Quest"
- **Dashboard** — earnings, tier progress, active quests, recent payouts, badges
- **Profile** — reputation tiers, consent/ethics info, backend connection status

## Running it

### 1. Start the backend first

From the project root (`DataCave_Beta`):

```bash
./gradlew bootRun
```

The API will be on port `8080`.

### 2. Start the Expo app

```bash
cd mobile
npm start
```

Then either:
- **On your iPhone:** install **Expo Go** from the App Store, scan the QR code in the terminal. Your phone and Mac must be on the same Wi-Fi.
- **iOS Simulator:** press `i` in the terminal (requires Xcode installed).

### Backend connection

The app auto-detects your Mac's IP from the Expo dev server, so it should "just work."
If quests don't load on a physical device, open `src/api.js` and set:

```js
const MANUAL_HOST = '10.181.149.44'; // your Mac's LAN IP (run: ipconfig getifaddr en0)
```

## Data collection (implemented)

Tapping **Start Quest** opens a collector that adapts to the quest type:

- **Image quests** → take a photo or pick from library (`expo-image-picker`), uploaded to the backend
- **Audio quests** → record audio in-app (`expo-audio`), uploaded as a file
- **Location quests** → capture current GPS coordinates (`expo-location`)
- **Health/survey quests** → free-text response + confidence rating
- **Labeling quests** → pick a classification label

Each submission POSTs to `/api/submissions`; the backend auto-reviews it, credits
your reward, and the Dashboard tab reflects the new earnings on refresh.

iOS permission strings for camera/mic/location are configured in `app.json`.
All three modules run inside Expo Go — no custom dev build needed.
```
