export type StorageScope = "local" | "session";

export interface KeyValueStorage {
  read(key: string): string | null;
  write(key: string, value: string): void;
  remove(key: string): void;
  subscribe(key: string, listener: () => void): () => void;
}

export class BrowserStorage implements KeyValueStorage {
  constructor(
    private readonly storage: Storage,
    private readonly scope: StorageScope,
  ) {}

  read(key: string): string | null {
    return this.storage.getItem(key);
  }

  write(key: string, value: string): void {
    this.storage.setItem(key, value);
  }

  remove(key: string): void {
    this.storage.removeItem(key);
  }

  subscribe(key: string, listener: () => void): () => void {
    if (this.scope !== "local") return () => undefined;
    const handleStorage = (event: StorageEvent) => {
      if (event.storageArea === this.storage && event.key === key) listener();
    };
    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }
}

export const persistentStorage = new BrowserStorage(
  window.localStorage,
  "local",
);
export const transientStorage = new BrowserStorage(
  window.sessionStorage,
  "session",
);
