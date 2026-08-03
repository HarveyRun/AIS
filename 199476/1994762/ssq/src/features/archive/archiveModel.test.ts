import { describe, expect, it } from 'vitest';
import {
  createForecastSnapshot,
  mergeForecastArchive,
  parseForecastArchive,
  reviewSnapshot,
  type ArchiveSourceData,
} from './archiveModel';

const source = {
  meta: {
    version: '4.0',
    generatedAt: '2026-07-31 15:16 +08:00',
    dataEndIssue: '2026087',
    drawCount: 2045,
    forecastIssue: '2026088',
    forecastDate: '2026-08-02',
  },
  prediction: {
    selected: { reds: [2, 9, 10, 13, 22, 25], blue: 11 },
    selectedDetails: {
      2: { number: 2, modelProb: 0.18, rank: 5, recent20: 2, gap: 1, positive: [{ name: '近期次数' }], negative: [] },
    },
    blueDetail: { number: 11, modelProb: 0.07, rank: 1, recent20: 1, gap: 0 },
    modelWeights: {
      red: { logistic: 0.1, gradientBoosting: 0.8, extraTrees: 0.1 },
      blue: { logistic: 0.8, gradientBoosting: 0.2, extraTrees: 0 },
    },
  },
  stats: { draws: [] },
} satisfies ArchiveSourceData;

describe('forecast archive model', () => {
  it('creates an immutable current-issue snapshot with version metadata', () => {
    const snapshot = createForecastSnapshot(source);
    expect(snapshot.issue).toBe('2026088');
    expect(snapshot.algorithm.fingerprint).toContain('2026087');
    expect(snapshot.reasons.map((reason) => reason.number)).toEqual([2, 11]);
  });

  it('reviews a snapshot when its actual draw becomes available', () => {
    const snapshot = createForecastSnapshot(source);
    const reviewed = reviewSnapshot(snapshot, [{ issue: '2026088', date: '2026-08-02', reds: [2, 4, 9, 18, 25, 31], blue: 11 }]);
    expect(reviewed.review?.redHits).toEqual([2, 9, 25]);
    expect(reviewed.review?.blueHit).toBe(true);
  });

  it('keeps the first saved snapshot for the same issue and ignores invalid storage', () => {
    const current = createForecastSnapshot(source);
    const saved = { ...current, reds: [1, 2, 3, 4, 5, 6] };
    expect(mergeForecastArchive([saved], current, [])[0].reds).toEqual(saved.reds);
    expect(parseForecastArchive('{bad json')).toEqual([]);
  });
});
