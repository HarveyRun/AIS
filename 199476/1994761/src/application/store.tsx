import {
  createContext,
  useContext,
  useMemo,
  useSyncExternalStore,
  type PropsWithChildren,
} from "react";
import type { AppData, UserAccount } from "../domain/types";
import { appApi } from "../api/client";
import { sessionRepository } from "../infrastructure/sessionRepository";

interface StoreValue {
  data: AppData;
  sessionEmail: string;
  user: UserAccount | null;
  isAdmin: boolean;
}

const StoreContext = createContext<StoreValue | null>(null);

export function StoreProvider({ children }: PropsWithChildren) {
  const data = useSyncExternalStore(appApi.subscribe, appApi.getSnapshot);
  const sessionEmail = useSyncExternalStore(
    sessionRepository.subscribe,
    sessionRepository.getSnapshot,
  );
  const value = useMemo<StoreValue>(
    () => ({
      data,
      sessionEmail,
      user: sessionEmail ? data.users[sessionEmail] || null : null,
      isAdmin: sessionEmail === "timeline.1994.1976@gmail.com",
    }),
    [data, sessionEmail],
  );
  return (
    <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
  );
}

export function useAppStore(): StoreValue {
  const value = useContext(StoreContext);
  if (!value) throw new Error("useAppStore must be used inside StoreProvider");
  return value;
}
