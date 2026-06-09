import Constants from 'expo-constants';
import { Platform } from 'react-native';

// ----------------------------------------------------------------------------
// Where is the Spring Boot backend?
//
// - iOS simulator can reach your Mac via "localhost".
// - A physical phone running Expo Go must use your Mac's LAN IP (e.g. 192.168.x.x).
//
// We try to auto-detect the Mac's IP from the Expo dev server host. If that
// fails, edit MANUAL_HOST below to your Mac's IP address (run `ipconfig getifaddr en0`).
// ----------------------------------------------------------------------------
const MANUAL_HOST = null; // e.g. '192.168.1.42'
const PORT = 8080;

function resolveHost() {
  if (MANUAL_HOST) return MANUAL_HOST;

  // Expo exposes the dev machine host here (works for Expo Go on a real device).
  const hostUri =
    Constants.expoConfig?.hostUri ||
    Constants.expoGoConfig?.debuggerHost ||
    Constants.manifest2?.extra?.expoGo?.debuggerHost;

  if (hostUri) {
    const host = hostUri.split(':')[0];
    if (host && host !== 'localhost' && host !== '127.0.0.1') return host;
  }

  // Fallbacks for simulators/emulators.
  if (Platform.OS === 'android') return '10.0.2.2'; // Android emulator -> host loopback
  return 'localhost';
}

export const API_BASE = `http://${resolveHost()}:${PORT}`;

// DEMO TOGGLE: when true, the backend auto-rejects every submission so you can
// test the rejection / quality-gate flow without a real API key. Set to false
// to restore normal accept/reject behaviour.
const FORCE_REJECT = false;

async function get(path) {
  const res = await fetch(`${API_BASE}${path}`);
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

async function post(path) {
  const res = await fetch(`${API_BASE}${path}`, { method: 'POST' });
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

const CONTRIBUTOR_ID = 1;

export const api = {
  baseUrl: API_BASE,
  contributorId: CONTRIBUTOR_ID,
  getQuests: (category) =>
    get(`/api/quests${category && category !== 'all' ? `?category=${encodeURIComponent(category)}` : ''}`),
  getQuest: (id) => get(`/api/quests/${id}`),
  getDashboard: (id = CONTRIBUTOR_ID) => get(`/api/contributor/${id}/dashboard`),
  getDatasets: () => get('/api/datasets'),

  acceptQuest: (questId, contributorId = CONTRIBUTOR_ID) =>
    post(`/api/quests/${questId}/accept?contributorId=${contributorId}`),

  getProfile: (id = CONTRIBUTOR_ID) => get(`/api/contributor/${id}`),

  /** AI pre-labeling: send a photo, get back structured labels for human validation. */
  analyzeLabels: async ({ questId, file }) => {
    const form = new FormData();
    form.append('questId', String(questId));
    form.append('file', {
      uri: file.uri,
      name: file.name || `photo-${Date.now()}.jpg`,
      type: file.mimeType || 'image/jpeg',
    });
    const res = await fetch(`${API_BASE}/api/labeling/analyze`, {
      method: 'POST', body: form, headers: { Accept: 'application/json' },
    });
    if (!res.ok) throw new Error(`Analyze failed: ${res.status}`);
    return res.json();
  },

  /**
   * Submit data toward a quest.
   * @param {object} opts
   * @param {number} opts.questId
   * @param {string} opts.type        PHOTO | AUDIO | SURVEY | LOCATION | LABEL
   * @param {object} [opts.file]      { uri, name, mimeType } for photo/audio
   * @param {object} [opts.payload]   survey answers / coordinates / labels
   */
  submit: async ({ questId, type, file, payload, labels, aiLabels, humanValidated, labelSource, contributorId = CONTRIBUTOR_ID }) => {
    const form = new FormData();
    form.append('contributorId', String(contributorId));
    form.append('questId', String(questId));
    if (type) form.append('type', type);
    if (payload) form.append('payload', JSON.stringify(payload));
    if (labels) { form.append('labels', JSON.stringify(labels)); form.append('humanValidated', String(humanValidated ?? true)); }
    if (aiLabels) form.append('aiLabels', JSON.stringify(aiLabels));
    if (labelSource) form.append('labelSource', labelSource);
    if (FORCE_REJECT) form.append('simulateReject', 'true');
    if (file?.uri) {
      form.append('file', {
        uri: file.uri,
        name: file.name || `upload-${Date.now()}`,
        type: file.mimeType || 'application/octet-stream',
      });
    }
    const res = await fetch(`${API_BASE}/api/submissions`, {
      method: 'POST',
      body: form,
      headers: { Accept: 'application/json' },
    });
    if (!res.ok) {
      let msg = `Submit failed: ${res.status}`;
      try { const j = await res.json(); if (j.error) msg = j.error; } catch (_) {}
      throw new Error(msg);
    }
    return res.json();
  },
};
