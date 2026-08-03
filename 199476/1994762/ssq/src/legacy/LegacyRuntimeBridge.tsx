import { useEffect, useLayoutEffect } from 'react';
import { useNavigation } from '../navigation/NavigationContext';
import { bootLegacyRuntime } from './bootLegacy';

function scheduleChartRedraw(delay = 70) {
  window.setTimeout(() => window.__drawVisibleCharts?.(), delay);
}

export function LegacyRuntimeBridge({ data }: { data: unknown }) {
  const { activePage, activeHistorySection, activeProfessionalSection } = useNavigation();

  useLayoutEffect(() => {
    bootLegacyRuntime(data);
  }, [data]);

  useEffect(() => {
    scheduleChartRedraw(80);
  }, [activePage, activeHistorySection, activeProfessionalSection]);

  return null;
}
