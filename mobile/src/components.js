import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors } from './theme';

export function Pill({ label, color }) {
  return (
    <View style={styles.pill}>
      <Text style={[styles.pillText, color && { color }]}>{label}</Text>
    </View>
  );
}

export function ProgressBar({ pct }) {
  return (
    <View style={styles.track}>
      <View style={[styles.fill, { width: `${Math.min(100, Math.max(0, pct))}%` }]} />
    </View>
  );
}

export function StatCard({ label, value, sub, valueColor }) {
  return (
    <View style={styles.statCard}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={[styles.statValue, valueColor && { color: valueColor }]}>{value}</Text>
      {sub ? <Text style={styles.statSub}>{sub}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  pill: {
    backgroundColor: colors.pill,
    borderColor: colors.pillBorder,
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 3,
    alignSelf: 'flex-start',
  },
  pillText: { color: colors.textMuted, fontSize: 11, fontWeight: '600' },
  track: { backgroundColor: colors.cardBorder, borderRadius: 999, height: 6, overflow: 'hidden' },
  fill: { height: '100%', borderRadius: 999, backgroundColor: colors.teal },
  statCard: {
    flex: 1,
    backgroundColor: colors.card,
    borderColor: colors.cardBorder,
    borderWidth: 1,
    borderRadius: 14,
    padding: 16,
  },
  statLabel: { color: colors.textFaint, fontSize: 11, marginBottom: 4 },
  statValue: { color: colors.text, fontSize: 22, fontWeight: '800' },
  statSub: { color: colors.textFaint, fontSize: 11, marginTop: 4 },
});
