import { describe, expect, it, vi } from "vitest";
import type { KeyValueStorage } from "./browserStorage";
import { JsonStorage } from "./jsonStorage";

class MemoryStorage implements KeyValueStorage {
  private readonly values = new Map<string, string>();
  read(key: string) {
    return this.values.get(key) ?? null;
  }
  write(key: string, value: string) {
    this.values.set(key, value);
  }
  remove(key: string) {
    this.values.delete(key);
  }
  subscribe() {
    return () => undefined;
  }
}

describe("JsonStorage", () => {
  it("统一完成结构化数据的读写和删除", () => {
    const storage = new JsonStorage(new MemoryStorage(), "profile", () => ({
      name: "",
    }));
    expect(storage.read()).toEqual({ name: "" });
    storage.write({ name: "点成" });
    expect(storage.read()).toEqual({ name: "点成" });
    storage.remove();
    expect(storage.read()).toEqual({ name: "" });
  });

  it("损坏的旧数据回退到默认结构", () => {
    const backend = new MemoryStorage();
    backend.write("profile", "{invalid");
    const fallback = vi.fn(() => ({ name: "default" }));
    expect(new JsonStorage(backend, "profile", fallback).read()).toEqual({
      name: "default",
    });
    expect(fallback).toHaveBeenCalledOnce();
  });
});
