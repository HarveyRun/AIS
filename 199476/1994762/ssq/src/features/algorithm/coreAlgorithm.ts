export interface CombinationScoreInput {
  marginal: number;
  structureScore: number;
  pairPmi: number;
}

export const COMBINATION_WEIGHTS = {
  structure: 0.55,
  pairPmi: 0.35,
} as const;

export function scoreCombination(input: CombinationScoreInput): number {
  return input.marginal
    + COMBINATION_WEIGHTS.structure * input.structureScore
    + COMBINATION_WEIGHTS.pairPmi * input.pairPmi;
}
