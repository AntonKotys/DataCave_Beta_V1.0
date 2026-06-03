import React, { useEffect, useState } from 'react';
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  ActivityIndicator, Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors } from '../src/theme';
import { Pill, ProgressBar } from '../src/components';
import { api } from '../src/api';

export default function QuestDetailScreen({ route, navigation }) {
  const { id } = route.params;
  const [q, setQ] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.getQuest(id).then(setQ).catch((e) => setError(e.message));
  }, [id]);

  if (error) {
    return (
      <SafeAreaView style={styles.center}>
        <Text style={{ color: colors.textMuted }}>Couldn't load quest.</Text>
      </SafeAreaView>
    );
  }
  if (!q) {
    return (
      <SafeAreaView style={styles.center}>
        <ActivityIndicator color={colors.teal} size="large" />
      </SafeAreaView>
    );
  }

  const pct = Math.round((q.completedCount / q.requiredCount) * 100);
  const spotsLeft = q.requiredCount - q.completedCount;

  const start = () => navigation.navigate('Submit', { quest: q });

  return (
    <SafeAreaView style={styles.screen} edges={['bottom']}>
      <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: 40 }}>
        <View style={styles.headRow}>
          <Text style={styles.icon}>{q.categoryIcon}</Text>
          <View style={{ flex: 1 }}>
            <Text style={styles.title}>{q.title}</Text>
            <Text style={styles.company}>{q.company}</Text>
          </View>
          <Text style={styles.reward}>${q.reward.toFixed(2)}</Text>
        </View>

        <Text style={styles.desc}>{q.description}</Text>

        <View style={styles.grid}>
          <Info label="Difficulty" value={q.difficulty} />
          <Info label="Time" value={q.estimatedMinutes > 0 ? `~${q.estimatedMinutes} min` : 'Passive'} />
          <Info label="Deadline" value={q.deadline} />
          <Info label="Spots left" value={spotsLeft.toLocaleString()} />
        </View>

        <ProgressBar pct={pct} />
        <Text style={styles.progressText}>
          {q.completedCount.toLocaleString()} / {q.requiredCount.toLocaleString()} submissions ({pct}% full)
        </Text>

        <View style={styles.tags}>
          {q.tags.map((t) => <Pill key={t} label={`#${t}`} />)}
        </View>

        <TouchableOpacity style={styles.cta} onPress={start} activeOpacity={0.85}>
          <Text style={styles.ctaText}>Start Quest →</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.back} onPress={() => navigation.goBack()}>
          <Text style={styles.backText}>Back to quests</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

function Info({ label, value }) {
  return (
    <View style={styles.infoCell}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  center: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center' },
  headRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 16 },
  icon: { fontSize: 38 },
  title: { color: colors.text, fontSize: 18, fontWeight: '800' },
  company: { color: colors.textMuted, fontSize: 13, marginTop: 2 },
  reward: { color: colors.teal, fontSize: 24, fontWeight: '800' },
  desc: { color: colors.text, fontSize: 14, lineHeight: 21, marginBottom: 20, opacity: 0.85 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginBottom: 20 },
  infoCell: {
    width: '47%', flexGrow: 1, backgroundColor: colors.bgElevated,
    borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.cardBorder,
  },
  infoLabel: { color: colors.textFaint, fontSize: 11, marginBottom: 4 },
  infoValue: { color: colors.text, fontSize: 15, fontWeight: '600' },
  progressText: { color: colors.textFaint, fontSize: 12, marginTop: 8, marginBottom: 20 },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 28 },
  cta: { backgroundColor: colors.tealDeep, borderRadius: 12, paddingVertical: 16, alignItems: 'center' },
  ctaText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  back: { alignItems: 'center', marginTop: 16 },
  backText: { color: colors.textMuted, fontSize: 13 },
});
