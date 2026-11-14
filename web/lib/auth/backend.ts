// Server-side client for the Spring Boot auth API.
// Imported only from the proxy, server actions and server components.
import type {
  AuthTokens,
  AuthUser,
  BackendResult,
  ProblemDetail,
} from "./types";

async function post<T>(path: string, body: unknown): Promise<BackendResult<T>> {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) {
    return {
      ok: false,
      status: 500,
      problem: { detail: "The API address is not configured." },
    };
  }

  try {
    const res = await fetch(`${baseUrl}/api/v1${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      cache: "no-store",
    });

    if (res.status === 204) return { ok: true, data: undefined as T };

    const json: unknown = await res.json().catch(() => ({}));
    if (res.ok) return { ok: true, data: json as T };
    return { ok: false, status: res.status, problem: json as ProblemDetail };
  } catch {
    return {
      ok: false,
      status: 503,
      problem: { detail: "The service is unavailable. Please try again." },
    };
  }
}

export function registerUser(email: string, password: string) {
  return post<AuthTokens>("/auth/register", { email, password });
}

export function loginUser(email: string, password: string) {
  return post<AuthTokens>("/auth/login", { email, password });
}

export function refreshSession(refreshToken: string) {
  return post<AuthTokens>("/auth/refresh", { refreshToken });
}

export function logoutUser(refreshToken: string) {
  return post<void>("/auth/logout", { refreshToken });
}

export async function fetchCurrentUser(
  accessToken: string,
): Promise<AuthUser | null> {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) return null;

  try {
    const res = await fetch(`${baseUrl}/api/v1/users/me`, {
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: "no-store",
    });
    return res.ok ? ((await res.json()) as AuthUser) : null;
  } catch {
    return null;
  }
}
