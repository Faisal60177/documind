// Server-side client for the Spring Boot documents API.
import type { BackendResult, ProblemDetail } from "@/lib/auth/types";
import type { DocumentItem } from "./types";

async function request<T>(
  path: string,
  accessToken: string,
  init: { method?: string; body?: BodyInit } = {},
): Promise<BackendResult<T>> {
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
      ...init,
      headers: { Authorization: `Bearer ${accessToken}` },
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

export function listDocuments(accessToken: string) {
  return request<DocumentItem[]>("/documents", accessToken);
}

export function uploadDocument(accessToken: string, file: File) {
  const body = new FormData();
  body.append("file", file, file.name);
  return request<DocumentItem>("/documents", accessToken, {
    method: "POST",
    body,
  });
}

export function deleteDocument(accessToken: string, id: string) {
  return request<void>(`/documents/${encodeURIComponent(id)}`, accessToken, {
    method: "DELETE",
  });
}
