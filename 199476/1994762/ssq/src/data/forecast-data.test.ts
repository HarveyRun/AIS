import { describe, expect, it } from 'vitest';
import data from './forecast-data.json';

describe('forecast data contract', () => {
  it('keeps the verified issue and number ranges', () => {
    expect(data.meta.forecastIssue).toBe('2026088');
    expect(data.meta.drawCount).toBe(2045);
    expect(data.probabilities.red).toHaveLength(33);
    expect(data.probabilities.blue).toHaveLength(16);
  });

  it('keeps all original prediction schemes', () => {
    expect(data.prediction.schemes.map((scheme) => scheme.code)).toEqual(['61', '62', '71', '72', '81', '82']);
  });
});
