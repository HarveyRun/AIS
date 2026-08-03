import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['ssq/src/**/*.test.ts'],
  },
});
