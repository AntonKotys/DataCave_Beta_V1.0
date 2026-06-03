import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../src/theme';
import { api } from '../src/api';

const TIERS = [
  { icon: '🥉', name: 'Bronze Contributor', desc: 'Entry-level quests. Build your acceptance rate and streak.', pay: '$1–5 / submission' },
  { icon: '🥈', name: 'Silver Mapper', desc: 'Unlocks location quests, surveys, and specialist roles.', pay: '$3–12 / submission' },
  { icon: '🥇', name: 'Gold Researcher', desc: 'High-value health data, wearable contributions, royalties.', pay: '$8–50 / submission + royalties' },
];

export default function ProfileScreen() {
  return (
    <SafeAreaView style={styles.screen} edges={['top']}>
      <ScrollView contentContainerStyle={{ padding: 16, paddingBottom: 32 }}>
        <Text style={styles.h1}>🪨 <Text style={{ color: colors.teal }}>DataCave</Text></Text>
        <Text style={styles.tagline}>The ethical data marketplace — fair pay, full consent.</Text>

        <Text style={styles.section}>Reputation Tiers</Text>
        {TIERS.map((t) => (
          <View key={t.name} style={styles.tierCard}>
            <Text style={styles.tierIcon}>{t.icon}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.tierName}>{t.name}</Text>
              <Text style={styles.tierDesc}>{t.desc}</Text>
              <Text style={styles.tierPay}>{t.pay}</Text>
            </View>
          </View>
        ))}

        <Text style={styles.section}>How DataCave protects you</Text>
        <View style={styles.panel}>
          <Bullet text="Every quest shows a full consent form before you start." />
          <Bullet text="You control exactly what data you share, and with whom." />
          <Bullet text="Transparent, fair pay — the same rate everywhere in the world." />
          <Bullet text="GDPR-compliant and ethically sourced by design." />
        </View>

        <Text style={styles.section}>Connection</Text>
        <View style={styles.panel}>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Backend</Text>
            <Text style={styles.infoValue}>{api.baseUrl}</Text>
          </View>
        </View>

        <Text style={styles.footer}>© 2026 DataCave · Built by Anton, Artem & Konrad</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

function Bullet({ text }) {
  return (
    <View style={styles.bullet}>
      <Text style={styles.check}>✓</Text>
      <Text style={styles.bulletText}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  h1: { color: colors.text, fontSize: 26, fontWeight: '800' },
  tagline: { color: colors.textMuted, fontSize: 13, marginTop: 4, marginBottom: 8 },
  section: { color: colors.text, fontSize: 16, fontWeight: '700', marginTop: 20, marginBottom: 10 },
  tierCard: { flexDirection: 'row', gap: 12, backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 14, padding: 16, marginBottom: 10 },
  tierIcon: { fontSize: 30 },
  tierName: { color: colors.text, fontSize: 14, fontWeight: '700' },
  tierDesc: { color: colors.textMuted, fontSize: 12, marginTop: 3, lineHeight: 17 },
  tierPay: { color: colors.teal, fontSize: 12, fontWeight: '600', marginTop: 5 },
  panel: { backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 14, padding: 16 },
  bullet: { flexDirection: 'row', gap: 10, marginBottom: 10 },
  check: { color: colors.green, fontWeight: '700' },
  bulletText: { color: colors.text, fontSize: 13, flex: 1, opacity: 0.85, lineHeight: 18 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  infoLabel: { color: colors.textMuted, fontSize: 13 },
  infoValue: { color: colors.teal, fontSize: 12, fontWeight: '600' },
  footer: { color: colors.textFainter, fontSize: 11, textAlign: 'center', marginTop: 28 },
});
