import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, ScrollView, ActivityIndicator,
  RefreshControl, TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, diffColor } from '../src/theme';
import { Pill, ProgressBar, StatCard } from '../src/components';
import { api } from '../src/api';

export default function DashboardScreen({ navigation }) {
  const [d, setD] = useState(null);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setD(await api.getDashboard(1));
    } catch (e) {
      setError(e.message);
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (error) {
    return (
      <SafeAreaView style={styles.center}>
        <Text style={styles.errEmoji}>🔌</Text>
        <Text style={styles.errTitle}>Can't reach the backend</Text>
        <TouchableOpacity style={styles.retry} onPress={load}>
          <Text style={styles.retryText}>Retry</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }
  if (!d) {
    return <SafeAreaView style={styles.center}><ActivityIndicator color={colors.teal} size="large" /></SafeAreaView>;
  }

  const tierPct = Math.round((d.tierProgress / d.tierTarget) * 100);

  return (
    <SafeAreaView style={styles.screen} edges={['top']}>
      <ScrollView
        contentContainerStyle={{ padding: 16, paddingBottom: 32 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={colors.teal} />}
      >
        {/* Profile header */}
        <View style={styles.profile}>
          <View style={styles.avatar}><Text style={styles.avatarText}>AM</Text></View>
          <View style={{ flex: 1 }}>
            <Text style={styles.name}>{d.name}</Text>
            <View style={styles.tierBadge}><Text style={styles.tierText}>🥈 {d.tier}</Text></View>
            <Text style={styles.rank}>🌍 Rank #{d.globalRank.toLocaleString()}   🔥 {d.streak}-day streak</Text>
          </View>
        </View>

        {/* Tier progress */}
        <View style={styles.tierCard}>
          <View style={styles.tierLabels}>
            <Text style={styles.tierLabel}>{d.tier}</Text>
            <Text style={styles.tierLabel}>Gold Researcher</Text>
          </View>
          <ProgressBar pct={tierPct} />
          <Text style={styles.tierHint}>{d.tierProgress} / {d.tierTarget} XP to Gold Researcher</Text>
        </View>

        {/* Stats */}
        <View style={styles.statRow}>
          <StatCard label="Total Earned" value={`$${d.totalEarnings.toFixed(2)}`} sub={`+$${d.weeklyEarnings.toFixed(2)} this week`} valueColor={colors.teal} />
          <StatCard label="Quests Done" value={d.completedQuests} sub="All time" valueColor={colors.indigo} />
        </View>
        <View style={styles.statRow}>
          <StatCard label="Reputation" value={`${d.reputationScore}/100`} sub="out of 100" valueColor={colors.amber} />
          <StatCard label="This Month" value={`$${d.monthlyEarnings.toFixed(2)}`} sub="Earnings" valueColor={colors.green} />
        </View>

        {/* Active quests */}
        <Text style={styles.section}>Active Quests</Text>
        <View style={styles.panel}>
          {d.activeQuests.map((q, i) => (
            <TouchableOpacity
              key={q.id}
              style={[styles.questRow, i < d.activeQuests.length - 1 && styles.divider]}
              onPress={() => navigation.navigate('Quests', { screen: 'QuestDetail', params: { id: q.id, title: q.title } })}
            >
              <Text style={styles.qIcon}>{q.categoryIcon}</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.qTitle}>{q.title}</Text>
                <View style={styles.qMeta}>
                  <Pill label={q.difficulty} color={diffColor(q.difficulty)} />
                  <Text style={styles.qDue}>Due {q.deadline}</Text>
                </View>
              </View>
              <Text style={styles.qReward}>${q.reward.toFixed(2)}</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Recent earnings */}
        <Text style={styles.section}>Recent Earnings</Text>
        <View style={styles.panel}>
          {d.recentEarnings.map((e, i) => (
            <View key={i} style={[styles.earnRow, i < d.recentEarnings.length - 1 && styles.divider]}>
              <View style={{ flex: 1 }}>
                <Text style={styles.earnTitle} numberOfLines={1}>{e.questTitle}</Text>
                <Text style={styles.earnDate}>{e.date}</Text>
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text style={styles.earnAmt}>+${e.amount.toFixed(2)}</Text>
                <Text style={styles.earnStatus}>{e.status}</Text>
              </View>
            </View>
          ))}
        </View>

        {/* Badges */}
        <Text style={styles.section}>Achievements</Text>
        <View style={styles.badgeGrid}>
          {d.badges.map((b) => (
            <View key={b.name} style={styles.badge}>
              <Text style={styles.badgeIcon}>{b.icon}</Text>
              <Text style={styles.badgeName}>{b.name}</Text>
              <Text style={styles.badgeDate}>{b.earnedDate}</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  center: { flex: 1, backgroundColor: colors.bg, alignItems: 'center', justifyContent: 'center', padding: 32 },
  profile: { flexDirection: 'row', alignItems: 'center', gap: 14, marginBottom: 16 },
  avatar: { width: 56, height: 56, borderRadius: 28, backgroundColor: colors.indigo, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: '#fff', fontWeight: '800', fontSize: 18 },
  name: { color: colors.text, fontSize: 20, fontWeight: '800' },
  tierBadge: { alignSelf: 'flex-start', backgroundColor: '#475569', borderRadius: 999, paddingHorizontal: 12, paddingVertical: 4, marginVertical: 5 },
  tierText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  rank: { color: colors.textMuted, fontSize: 12 },
  tierCard: { backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 14, padding: 16, marginBottom: 16 },
  tierLabels: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  tierLabel: { color: colors.textMuted, fontSize: 12 },
  tierHint: { color: colors.textFaint, fontSize: 11, marginTop: 8 },
  statRow: { flexDirection: 'row', gap: 12, marginBottom: 12 },
  section: { color: colors.text, fontSize: 16, fontWeight: '700', marginTop: 12, marginBottom: 10 },
  panel: { backgroundColor: colors.card, borderColor: colors.cardBorder, borderWidth: 1, borderRadius: 14, paddingHorizontal: 16 },
  questRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 14 },
  divider: { borderBottomWidth: 1, borderBottomColor: colors.cardBorder },
  qIcon: { fontSize: 20 },
  qTitle: { color: colors.text, fontSize: 14, fontWeight: '600' },
  qMeta: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 5 },
  qDue: { color: colors.textFaint, fontSize: 11 },
  qReward: { color: colors.teal, fontSize: 15, fontWeight: '700' },
  earnRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12 },
  earnTitle: { color: colors.text, fontSize: 13, fontWeight: '500' },
  earnDate: { color: colors.textFaint, fontSize: 11, marginTop: 2 },
  earnAmt: { color: colors.teal, fontSize: 14, fontWeight: '700' },
  earnStatus: { color: colors.green, fontSize: 10, marginTop: 2 },
  badgeGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  badge: { width: '31%', flexGrow: 1, backgroundColor: colors.pill, borderColor: colors.pillBorder, borderWidth: 1, borderRadius: 12, padding: 14, alignItems: 'center' },
  badgeIcon: { fontSize: 26, marginBottom: 6 },
  badgeName: { color: colors.text, fontSize: 11, fontWeight: '600', textAlign: 'center' },
  badgeDate: { color: colors.textFaint, fontSize: 10, marginTop: 2 },
  errEmoji: { fontSize: 40, marginBottom: 12 },
  errTitle: { color: colors.text, fontSize: 16, fontWeight: '600' },
  retry: { marginTop: 16, backgroundColor: colors.tealDeep, borderRadius: 10, paddingHorizontal: 24, paddingVertical: 10 },
  retryText: { color: '#fff', fontWeight: '600' },
});
