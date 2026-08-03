export interface ArchiveModelWeights {
  logistic: number;
  gradientBoosting: number;
  extraTrees: number;
}

export interface ArchiveDraw {
  issue: string;
  date: string;
  reds: number[];
  blue: number;
}

export interface ArchiveReason {
  number: number;
  color: 'red' | 'blue';
  rank: number;
  probability: number;
  recent20: number;
  gap: number;
  positive: string[];
  negative: string[];
}

export interface ForecastSnapshot {
  issue: string;
  forecastDate: string;
  createdAt: string;
  dataEndIssue: string;
  drawCount: number;
  reds: number[];
  blue: number;
  reasons: ArchiveReason[];
  algorithm: {
    version: string;
    fingerprint: string;
    redWeights: ArchiveModelWeights;
    blueWeights: ArchiveModelWeights;
    structureWeight: number;
    pairWeight: number;
  };
  actual?: ArchiveDraw;
  review?: {
    redHits: number[];
    blueHit: boolean;
  };
}

interface DetailSource {
  number: number;
  modelProb: number;
  rank: number;
  recent20: number;
  gap: number;
  positive?: Array<{ name: string }>;
  negative?: Array<{ name: string }>;
}

export interface ArchiveSourceData {
  meta: {
    version: string;
    generatedAt: string;
    dataEndIssue: string;
    drawCount: number;
    forecastIssue: string;
    forecastDate: string;
  };
  prediction: {
    selected: {
      reds: number[];
      blue: number;
    };
    selectedDetails: Record<string, DetailSource>;
    blueDetail: DetailSource;
    modelWeights: {
      red: ArchiveModelWeights;
      blue: ArchiveModelWeights;
    };
  };
  stats: {
    draws: ArchiveDraw[];
  };
}

function toReason(detail: DetailSource, color: 'red' | 'blue'): ArchiveReason {
  return {
    number: detail.number,
    color,
    rank: detail.rank,
    probability: detail.modelProb,
    recent20: detail.recent20,
    gap: detail.gap,
    positive: (detail.positive ?? []).slice(0, 3).map((item) => item.name),
    negative: (detail.negative ?? []).slice(0, 3).map((item) => item.name),
  };
}

export function createForecastSnapshot(data: ArchiveSourceData): ForecastSnapshot {
  const redReasons = data.prediction.selected.reds
    .map((number) => data.prediction.selectedDetails[String(number)])
    .filter((detail): detail is DetailSource => Boolean(detail))
    .map((detail) => toReason(detail, 'red'));

  return {
    issue: data.meta.forecastIssue,
    forecastDate: data.meta.forecastDate,
    createdAt: data.meta.generatedAt,
    dataEndIssue: data.meta.dataEndIssue,
    drawCount: data.meta.drawCount,
    reds: [...data.prediction.selected.reds],
    blue: data.prediction.selected.blue,
    reasons: [...redReasons, toReason(data.prediction.blueDetail, 'blue')],
    algorithm: {
      version: data.meta.version,
      fingerprint: `SSQ-${data.meta.version}-${data.meta.dataEndIssue}-${data.meta.forecastIssue}`,
      redWeights: { ...data.prediction.modelWeights.red },
      blueWeights: { ...data.prediction.modelWeights.blue },
      structureWeight: 0.55,
      pairWeight: 0.35,
    },
  };
}

export function reviewSnapshot(snapshot: ForecastSnapshot, draws: ArchiveDraw[]): ForecastSnapshot {
  const actual = draws.find((draw) => draw.issue === snapshot.issue);
  if (!actual) return snapshot;

  const redHits = snapshot.reds.filter((number) => actual.reds.includes(number));
  return {
    ...snapshot,
    actual,
    review: {
      redHits,
      blueHit: snapshot.blue === actual.blue,
    },
  };
}

export function mergeForecastArchive(
  saved: ForecastSnapshot[],
  current: ForecastSnapshot,
  draws: ArchiveDraw[],
): ForecastSnapshot[] {
  const snapshots = new Map(saved.map((snapshot) => [snapshot.issue, snapshot]));
  if (!snapshots.has(current.issue)) snapshots.set(current.issue, current);

  return [...snapshots.values()]
    .map((snapshot) => reviewSnapshot(snapshot, draws))
    .sort((left, right) => right.issue.localeCompare(left.issue));
}

export function parseForecastArchive(raw: string | null): ForecastSnapshot[] {
  if (!raw) return [];
  try {
    const value: unknown = JSON.parse(raw);
    if (!Array.isArray(value)) return [];
    return value.filter((item): item is ForecastSnapshot => (
      typeof item === 'object'
      && item !== null
      && typeof Reflect.get(item, 'issue') === 'string'
      && Array.isArray(Reflect.get(item, 'reds'))
    ));
  } catch {
    return [];
  }
}
