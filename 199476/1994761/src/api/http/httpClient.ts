import { API_TOKEN_STORAGE_KEY } from "../../domain/constants";
import { sessionRepository } from "../../infrastructure/sessionRepository";

const API_BASE = import.meta.env.VITE_API_URL || "/api";

export function getApiToken(): string {
  return window.localStorage.getItem(API_TOKEN_STORAGE_KEY) || "";
}

export function setApiToken(token: string): void {
  if (token) window.localStorage.setItem(API_TOKEN_STORAGE_KEY, token);
  else window.localStorage.removeItem(API_TOKEN_STORAGE_KEY);
}

export function apiUrl(path: string): string {
  return `${API_BASE}${path}`;
}

export async function httpRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getApiToken();
  const headers = new Headers(options.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(apiUrl(path), { ...options, headers });
  const payload = (await response.json().catch(() => null)) as
    | (T & { error?: string })
    | null;
  if (!response.ok) {
    if (response.status === 401) {
      setApiToken("");
      sessionRepository.clear();
    }
    throw new Error(payload?.error || `接口请求失败（${response.status}）`);
  }
  if (!payload) throw new Error("接口返回了无效数据。");
  return payload;
}

export function jsonBody(value: unknown): RequestInit {
  return { body: JSON.stringify(value) };
}
