import type { ApiError } from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_KEY = "ghost_kitchen_token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export class HttpError extends Error {
  status: number;
  apiError: ApiError | null;

  constructor(status: number, apiError: ApiError | null, fallbackMessage: string) {
    super(apiError?.message ?? fallbackMessage);
    this.status = status;
    this.apiError = apiError;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  isForm?: boolean;
  query?: Record<string, string | undefined>;
}

function buildUrl(path: string, query?: Record<string, string | undefined>): string {
  const url = new URL(API_BASE_URL + path);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) url.searchParams.set(key, value);
    }
  }
  return url.toString();
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let body: BodyInit | undefined;
  if (options.body !== undefined) {
    if (options.isForm) {
      body = options.body as FormData;
    } else {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(options.body);
    }
  }

  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? "GET",
    headers,
    body,
  });

  if (!response.ok) {
    let apiError: ApiError | null = null;
    try {
      apiError = await response.json();
    } catch {
      // body wasn't JSON (e.g. an upstream proxy error page) - fall back to status text below
    }
    throw new HttpError(response.status, apiError, response.statusText);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  get: <T>(path: string, query?: Record<string, string | undefined>) => request<T>(path, { query }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: "POST", body }),
  postForm: <T>(path: string, form: FormData, query?: Record<string, string | undefined>) =>
    request<T>(path, { method: "POST", body: form, isForm: true, query }),
};
