import { createContext, type ReactNode, useCallback, useContext, useMemo, useState } from 'react';

export type PageId = 'forecast' | 'dimensions' | 'history' | 'archive' | 'professional' | 'algorithm';
export type WorkspacePageId = Exclude<PageId, 'forecast'>;

interface NavigationState {
  activePage: PageId;
  lastWorkspacePage: WorkspacePageId;
  activeHistorySection: string;
  activeProfessionalSection: string;
  setActivePage: (page: PageId) => void;
  setActiveHistorySection: (section: string) => void;
  setActiveProfessionalSection: (section: string) => void;
}

const NavigationContext = createContext<NavigationState | null>(null);

export function NavigationProvider({ children }: { children: ReactNode }) {
  const [activePage, setActivePageState] = useState<PageId>('forecast');
  const [lastWorkspacePage, setLastWorkspacePage] = useState<WorkspacePageId>('dimensions');
  const [activeHistorySection, setActiveHistorySection] = useState('overview');
  const [activeProfessionalSection, setActiveProfessionalSection] = useState('model');

  const setActivePage = useCallback((page: PageId) => {
    setActivePageState(page);
    if (page !== 'forecast') setLastWorkspacePage(page);
  }, []);

  const value = useMemo<NavigationState>(() => ({
    activePage,
    lastWorkspacePage,
    activeHistorySection,
    activeProfessionalSection,
    setActivePage,
    setActiveHistorySection,
    setActiveProfessionalSection,
  }), [activeHistorySection, activePage, activeProfessionalSection, lastWorkspacePage, setActivePage]);

  return <NavigationContext.Provider value={value}>{children}</NavigationContext.Provider>;
}

export function useNavigation(): NavigationState {
  const value = useContext(NavigationContext);
  if (!value) throw new Error('NavigationContext is missing');
  return value;
}
