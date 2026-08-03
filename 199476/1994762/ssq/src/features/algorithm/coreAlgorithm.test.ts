import { describe, expect, it } from 'vitest';
import data from '../../data/forecast-data.json';
import { scoreCombination } from './coreAlgorithm';

describe('combination scoring', () => {
  it('reproduces the score stored in the current prediction', () => {
    const selected = data.prediction.selected;
    expect(scoreCombination(selected)).toBeCloseTo(selected.score, 5);
  });
});
