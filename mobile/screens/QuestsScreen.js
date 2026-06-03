import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity,
  ActivityIndicator, RefreshControl, ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, diffColor } from '../src/theme';
import { Pill, ProgressBar } from '../src/components';
import { api } from '../src/api';

const CATEGORIES = [
  { key: 'all', label: 'All' },
  { key: 'Audio', label: '🎙️ Audio' },
  { key: 'Image', label: '📸 Image' },
  { key: 'Text & Label', label: '🏷️ Labeling' },
  { key: 'Motion & Location', label: '📍 Location' },
  { key: 'Health Data', label: '🩺 Health' },
];

export default function QuestsScreen({ navigation }) {
  const [quests, setQuests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [cat, setCat] = useState('all');

  const load = useCallback(async (category) => {
    try {
      setError(null);
      const data = await api.getQuests(category);
      setQuests(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { setLoading(true); load(cat); }, [cat, load]);

  const onRefresh = () => { setRefreshing(true); load(cat); };

  const renderItem = ({ item: q }) => {
    const pct = Math.round((q.completedCount / q.requiredCount) * 100);
    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.8}
        onPress={() => navigation.navigate('QuestDetail', { id: q.id, title: q.title })}
      >
        <View style={styles.cardTop}>
          <View style={styles.row}>
            <Text style={styles.icon}>{q.categoryIcon}</Text>
            <Pill label={q.category} />
          </View>
          <Text style={styles.reward}>${q.reward.toFixed(2)}</Text>
        </View>
        <Text style={styles.title}>{q.title}</Text>
        <Text style={styles.desc} numberOfLines={2}>{q.description}</Text>
        <View style={styles.metaRow}>
          <Pill label={q.difficulty} color={diffColor(q.difficulty)} />
          {q.estimatedMinutes > 0 && <Text style={styles.meta}>~{q.estimatedMinutes} min</Text>}
          <Text style={[styles.meta, { marginLeft: 'auto' }]}>Due {q.deadline}</Text>
        </View>
        <ProgressBar pct={pct} />
        <View style={styles.progressLabels}>
          <Text style={styles.faint}>{q.completedCount.toLocaleString()} submitted</Text>
          <Text style={styles.faint}>{pct}% full</Text>
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.screen} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.h1}>Browse <Text style={{ color: colors.teal }}>Quests</Text></Text>
        <Text style={styles.sub}>Complete data tasks, earn real money.</Text>
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={styles.filters}
        contentContainerStyle={{ paddingHorizontal: 16, gap: 8 }}
      >
        {CATEGORIES.map((c) => (
          <TouchableOpacity
            key={c.key}
            style={[styles.filterBtn, cat === c.key && styles.filterBtnActive]}
            onPress={() => setCat(c.key)}
          >
            <Text style={[styles.filterText, cat === c.key && styles.filterTextActive]}>{c.label}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading ? (
        <View style={styles.center}><ActivityIndicator color={colors.teal} size="large" /></View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.errEmoji}>🔌</Text>
          <Text style={styles.errTitle}>Can't reach the backend</Text>
          <Text style={styles.errMsg}>Make sure the Spring Boot server is running.{'\n'}{api.baseUrl}</Text>
          <TouchableOpacity style={styles.retry} onPress={() => { setLoading(true); load(cat); }}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={quests}
          keyExtractor={(q) => String(q.id)}
          renderItem={renderItem}
          contentContainerStyle={{ padding: 16, paddingBottom: 32 }}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.teal} />}
          ListEmptyComponent={<Text style={styles.empty}>No quests in this category.</Text>}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  header: { paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },
  h1: { color: colors.text, fontSize: 26, fontWeight: '800' },
  sub: { color: colors.textMuted, fontSize: 13, marginTop: 2 },
  filters: { flexGrow: 0, marginBottom: 8 },
  filterBtn: {
    borderWidth: 1, borderColor: colors.cardBorder, backgroundColor: colors.card,
    borderRadius: 8, paddingHorizontal: 14, paddingVertical: 7,
  },
  filterBtnActive: { borderColor: colors.tealDeep, backgroundColor: 'rgba(20,184,166,0.1)' },
  filterText: { color: colors.textMuted, fontSize: 13 },
  filterTextActive: { color: colors.teal, fontWeight: '600' },
  card: {
    backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1,
    borderRadius: 16, padding: 16, marginBottom: 14,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  icon: { fontSize: 22 },
  reward: { color: colors.teal, fontSize: 18, fontWeight: '800' },
  title: { color: colors.text, fontSize: 15, fontWeight: '700', marginBottom: 4 },
  desc: { color: colors.textFaint, fontSize: 12, lineHeight: 17, marginBottom: 12 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 12 },
  meta: { color: colors.textFaint, fontSize: 11 },
  progressLabels: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 6 },
  faint: { color: colors.textFainter, fontSize: 11 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  errEmoji: { fontSize: 40, marginBottom: 12 },
  errTitle: { color: colors.text, fontSize: 16, fontWeight: '600' },
  errMsg: { color: colors.textMuted, fontSize: 13, textAlign: 'center', marginTop: 6 },
  retry: { marginTop: 16, backgroundColor: colors.tealDeep, borderRadius: 10, paddingHorizontal: 24, paddingVertical: 10 },
  retryText: { color: '#fff', fontWeight: '600' },
  empty: { color: colors.textFaint, textAlign: 'center', marginTop: 40 },
});
