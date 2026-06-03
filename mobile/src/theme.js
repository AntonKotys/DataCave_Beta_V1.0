// DataCave shared theme — mirrors the web app's dark "cave" palette.
export const colors = {
  bg: '#0a0e1a',
  bgElevated: '#111827',
  card: '#111827',
  cardBorder: '#1e293b',
  pill: '#1a2236',
  pillBorder: '#2d3748',
  teal: '#2dd4bf',
  tealDeep: '#14b8a6',
  indigo: '#6366f1',
  amber: '#fbbf24',
  green: '#4ade80',
  red: '#f87171',
  text: '#f1f5f9',
  textMuted: '#94a3b8',
  textFaint: '#64748b',
  textFainter: '#475569',
};

export const diffColor = (d) => {
  if (d === 'Easy') return colors.green;
  if (d === 'Medium') return colors.amber;
  return colors.red;
};
