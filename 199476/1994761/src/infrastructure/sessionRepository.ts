import { SESSION_STORAGE_KEY } from "../domain/constants";
import { persistentStorage } from "./storage/browserStorage";

let snapshot = persistentStorage.read(SESSION_STORAGE_KEY) || "";
const listeners = new Set<() => void>();

function emit(): void {
  listeners.forEach((listener) => listener());
}

export const sessionRepository = {
  getSnapshot: () => snapshot,
  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  set(email: string): void {
    snapshot = email;
    persistentStorage.write(SESSION_STORAGE_KEY, email);
    emit();
  },
  clear(): void {
    snapshot = "";
    persistentStorage.remove(SESSION_STORAGE_KEY);
    emit();
  },
  validate(users: Record<string, unknown>): void {
    if (snapshot && !users[snapshot]) this.clear();
  },
};

persistentStorage.subscribe(SESSION_STORAGE_KEY, () => {
  snapshot = persistentStorage.read(SESSION_STORAGE_KEY) || "";
  emit();
});
