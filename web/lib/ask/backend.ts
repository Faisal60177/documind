// Server-side client for the Spring Boot ask API.
import type { BackendResult, ProblemDetail } from "@/lib/auth/types";
import type { AskResponse } from "./types";

export async function askQuestion(
  accessToken: string,
  question: string,
): Promise<BackendResult<AskResponse>> {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) {
    return {
      ok: false,
      status: 500,
      problem: { detail: "The API address is not configured." },
    };
  }

  try {
    const res = await fetch(`${baseUrl}/api/v1/ask`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ question }),
      cache: "no-store",
      signal: AbortSignal.timeout(90_000),
    });

    const json: unknown = await res.json().catch(() => ({}));
    if (res.ok) return { ok: true, data: json as AskResponse };
    return { ok: false, status: res.status, problem: json as ProblemDetail };
  } catch {
    return {
      ok: false,
      status: 503,
      problem: {
        detail: "The service took too long to respond. Please try again.",
      },
    };
  }
}
