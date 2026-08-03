import type { KeyValueStorage } from "./browserStorage";

export class JsonStorage<T> {
  constructor(
    private readonly storage: KeyValueStorage,
    private readonly key: string,
    private readonly fallback: () => T,
  ) {}

  read(): T {
    const serialized = this.storage.read(this.key);
    if (!serialized) return this.fallback();
    try {
      return JSON.parse(serialized) as T;
    } catch {
      return this.fallback();
    }
  }

  write(value: T): void {
    this.storage.write(this.key, JSON.stringify(value));
  }

  remove(): void {
    this.storage.remove(this.key);
  }

  subscribe(listener: () => void): () => void {
    return this.storage.subscribe(this.key, listener);
  }
}
