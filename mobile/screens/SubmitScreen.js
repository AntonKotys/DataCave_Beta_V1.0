import React, { useState, useEffect } from 'react';
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  ActivityIndicator, Image, TextInput, Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import * as Location from 'expo-location';
import {
  useAudioRecorder, AudioModule, RecordingPresets, setAudioModeAsync,
} from 'expo-audio';
import { colors } from '../src/theme';
import { api } from '../src/api';

// Decide what kind of collector this quest needs.
function modeFor(category) {
  switch (category) {
    case 'Image': return 'PHOTO';
    case 'Audio': return 'AUDIO';
    case 'Motion & Location': return 'LOCATION';
    case 'Health Data': return 'SURVEY';
    default: return 'LABEL';
  }
}

const LABEL_OPTIONS = ['Positive', 'Neutral', 'Negative', 'Mixed'];

export default function SubmitScreen({ route, navigation }) {
  const { quest } = route.params;
  const mode = modeFor(quest.category);

  const [busy, setBusy] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // PHOTO
  const [photo, setPhoto] = useState(null);
  // AUDIO
  const audioRecorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const [recording, setRecording] = useState(false);
  const [audioUri, setAudioUri] = useState(null);
  // LOCATION
  const [coords, setCoords] = useState(null);
  // SURVEY
  const [response, setResponse] = useState('');
  const [rating, setRating] = useState(0);
  // LABEL
  const [label, setLabel] = useState(null);

  useEffect(() => {
    navigation.setOptions({ title: 'Complete Quest' });
  }, [navigation]);

  // ---- PHOTO ----
  const takePhoto = async (fromLibrary) => {
    try {
      const perm = fromLibrary
        ? await ImagePicker.requestMediaLibraryPermissionsAsync()
        : await ImagePicker.requestCameraPermissionsAsync();
      if (!perm.granted) {
        Alert.alert('Permission needed', 'Please allow access to continue.');
        return;
      }
      const res = fromLibrary
        ? await ImagePicker.launchImageLibraryAsync({ quality: 0.7, mediaTypes: ['images'] })
        : await ImagePicker.launchCameraAsync({ quality: 0.7 });
      if (!res.canceled && res.assets?.length) {
        const a = res.assets[0];
        setPhoto({ uri: a.uri, name: a.fileName || `photo-${Date.now()}.jpg`, mimeType: a.mimeType || 'image/jpeg' });
      }
    } catch (e) {
      Alert.alert('Camera error', e.message);
    }
  };

  // ---- AUDIO ----
  const toggleRecord = async () => {
    try {
      if (recording) {
        await audioRecorder.stop();
        setRecording(false);
        setAudioUri(audioRecorder.uri);
        return;
      }
      const perm = await AudioModule.requestRecordingPermissionsAsync();
      if (!perm.granted) {
        Alert.alert('Permission needed', 'Microphone access is required to record audio.');
        return;
      }
      await setAudioModeAsync({ allowsRecording: true, playsInSilentMode: true });
      await audioRecorder.prepareToRecordAsync();
      audioRecorder.record();
      setAudioUri(null);
      setRecording(true);
    } catch (e) {
      Alert.alert('Recording error', e.message);
    }
  };

  // ---- LOCATION ----
  const captureLocation = async () => {
    try {
      setBusy(true);
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission needed', 'Location access is required for this quest.');
        return;
      }
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      setCoords({
        lat: loc.coords.latitude,
        lng: loc.coords.longitude,
        accuracy: Math.round(loc.coords.accuracy),
      });
    } catch (e) {
      Alert.alert('Location error', e.message);
    } finally {
      setBusy(false);
    }
  };

  // ---- SUBMIT ----
  const canSubmit = () => {
    if (mode === 'PHOTO') return !!photo;
    if (mode === 'AUDIO') return !!audioUri;
    if (mode === 'LOCATION') return !!coords;
    if (mode === 'SURVEY') return response.trim().length > 0;
    if (mode === 'LABEL') return !!label;
    return false;
  };

  const doSubmit = async () => {
    try {
      setSubmitting(true);
      let opts = { questId: quest.id, type: mode };
      if (mode === 'PHOTO') opts.file = photo;
      else if (mode === 'AUDIO') opts.file = { uri: audioUri, name: `recording-${Date.now()}.m4a`, mimeType: 'audio/m4a' };
      else if (mode === 'LOCATION') opts.payload = coords;
      else if (mode === 'SURVEY') opts.payload = { response: response.trim(), rating };
      else if (mode === 'LABEL') opts.payload = { label };

      const result = await api.submit(opts);
      Alert.alert(
        'Submitted! 🎉',
        `Your ${mode.toLowerCase()} was accepted (quality ${result.qualityScore}/100). You earned $${result.reward.toFixed(2)}.`,
        [{ text: 'Done', onPress: () => navigation.navigate('Dashboard') }]
      );
    } catch (e) {
      Alert.alert('Submission failed', e.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.screen} edges={['bottom']}>
      <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: 40 }}>
        <View style={styles.head}>
          <Text style={styles.icon}>{quest.categoryIcon}</Text>
          <View style={{ flex: 1 }}>
            <Text style={styles.title}>{quest.title}</Text>
            <Text style={styles.reward}>Earn ${quest.reward.toFixed(2)} on acceptance</Text>
          </View>
        </View>
        <Text style={styles.instructions}>{quest.description}</Text>

        {/* Consent note */}
        <View style={styles.consent}>
          <Text style={styles.consentText}>
            🔒 By submitting, you consent to share this data with {quest.company} for the stated purpose. You can withdraw anytime.
          </Text>
        </View>

        {/* Collector by mode */}
        {mode === 'PHOTO' && (
          <View style={styles.block}>
            {photo ? (
              <Image source={{ uri: photo.uri }} style={styles.preview} />
            ) : (
              <View style={styles.placeholder}><Text style={styles.placeholderText}>No photo yet</Text></View>
            )}
            <View style={styles.row}>
              <TouchableOpacity style={styles.secondary} onPress={() => takePhoto(false)}>
                <Text style={styles.secondaryText}>📷 Take Photo</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.secondary} onPress={() => takePhoto(true)}>
                <Text style={styles.secondaryText}>🖼️ Choose</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {mode === 'AUDIO' && (
          <View style={styles.block}>
            <View style={[styles.placeholder, recording && { borderColor: colors.red }]}>
              <Text style={styles.bigEmoji}>{recording ? '🔴' : audioUri ? '✅' : '🎙️'}</Text>
              <Text style={styles.placeholderText}>
                {recording ? 'Recording… tap to stop' : audioUri ? 'Recording captured' : 'Tap to start recording'}
              </Text>
            </View>
            <TouchableOpacity style={[styles.secondary, { alignSelf: 'stretch' }]} onPress={toggleRecord}>
              <Text style={styles.secondaryText}>{recording ? '⏹ Stop' : '⏺ Record'}</Text>
            </TouchableOpacity>
          </View>
        )}

        {mode === 'LOCATION' && (
          <View style={styles.block}>
            <View style={styles.placeholder}>
              {coords ? (
                <>
                  <Text style={styles.bigEmoji}>📍</Text>
                  <Text style={styles.coordText}>{coords.lat.toFixed(5)}, {coords.lng.toFixed(5)}</Text>
                  <Text style={styles.placeholderText}>±{coords.accuracy}m accuracy</Text>
                </>
              ) : (
                <Text style={styles.placeholderText}>No location captured</Text>
              )}
            </View>
            <TouchableOpacity style={[styles.secondary, { alignSelf: 'stretch' }]} onPress={captureLocation} disabled={busy}>
              <Text style={styles.secondaryText}>{busy ? 'Locating…' : '📍 Capture Current Location'}</Text>
            </TouchableOpacity>
          </View>
        )}

        {mode === 'SURVEY' && (
          <View style={styles.block}>
            <Text style={styles.fieldLabel}>Your response</Text>
            <TextInput
              style={styles.input}
              placeholder="Describe your answer…"
              placeholderTextColor={colors.textFainter}
              multiline
              value={response}
              onChangeText={setResponse}
            />
            <Text style={styles.fieldLabel}>How confident are you? (1–5)</Text>
            <View style={styles.row}>
              {[1, 2, 3, 4, 5].map((n) => (
                <TouchableOpacity key={n} style={[styles.chip, rating === n && styles.chipActive]} onPress={() => setRating(n)}>
                  <Text style={[styles.chipText, rating === n && styles.chipTextActive]}>{n}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        {mode === 'LABEL' && (
          <View style={styles.block}>
            <Text style={styles.fieldLabel}>Choose the best label</Text>
            <View style={styles.wrap}>
              {LABEL_OPTIONS.map((opt) => (
                <TouchableOpacity key={opt} style={[styles.chip, label === opt && styles.chipActive]} onPress={() => setLabel(opt)}>
                  <Text style={[styles.chipText, label === opt && styles.chipTextActive]}>{opt}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        <TouchableOpacity
          style={[styles.cta, !canSubmit() && styles.ctaDisabled]}
          onPress={doSubmit}
          disabled={!canSubmit() || submitting}
        >
          {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.ctaText}>Submit & Earn ${quest.reward.toFixed(2)}</Text>}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  head: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 12 },
  icon: { fontSize: 34 },
  title: { color: colors.text, fontSize: 17, fontWeight: '800' },
  reward: { color: colors.teal, fontSize: 13, marginTop: 2, fontWeight: '600' },
  instructions: { color: colors.textMuted, fontSize: 14, lineHeight: 20, marginBottom: 16 },
  consent: { backgroundColor: 'rgba(99,102,241,0.08)', borderColor: 'rgba(99,102,241,0.3)', borderWidth: 1, borderRadius: 12, padding: 12, marginBottom: 20 },
  consentText: { color: colors.textMuted, fontSize: 12, lineHeight: 17 },
  block: { marginBottom: 24, gap: 12 },
  preview: { width: '100%', height: 240, borderRadius: 14, backgroundColor: colors.card },
  placeholder: { borderWidth: 1, borderColor: colors.cardBorder, borderStyle: 'dashed', borderRadius: 14, paddingVertical: 36, alignItems: 'center', backgroundColor: colors.card, gap: 8 },
  placeholderText: { color: colors.textFaint, fontSize: 14 },
  bigEmoji: { fontSize: 40 },
  coordText: { color: colors.text, fontSize: 16, fontWeight: '700' },
  row: { flexDirection: 'row', gap: 10 },
  wrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  secondary: { flex: 1, backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 10, paddingVertical: 14, alignItems: 'center' },
  secondaryText: { color: colors.text, fontWeight: '600', fontSize: 14 },
  fieldLabel: { color: colors.textMuted, fontSize: 13, fontWeight: '500', marginBottom: 4 },
  input: { backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 12, padding: 14, color: colors.text, fontSize: 14, minHeight: 90, textAlignVertical: 'top' },
  chip: { backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 10, paddingVertical: 12, paddingHorizontal: 18, alignItems: 'center' },
  chipActive: { borderColor: colors.tealDeep, backgroundColor: 'rgba(20,184,166,0.12)' },
  chipText: { color: colors.textMuted, fontWeight: '600' },
  chipTextActive: { color: colors.teal },
  cta: { backgroundColor: colors.tealDeep, borderRadius: 12, paddingVertical: 16, alignItems: 'center', marginTop: 8 },
  ctaDisabled: { opacity: 0.4 },
  ctaText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
