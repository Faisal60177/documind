import "server-only";

import { cache } from "react";
import { cookies } from "next/headers";
import { fetchCurrentUser } from "./backend";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  accessCookieMaxAge,
  sessionCookieOptions,
} from "./config";
import type { AuthTokens } from "./types";

export async function createSession(tokens: AuthTokens) {
  const store = await cookies();
  store.set(
    ACCESS_COOKIE,
    tokens.accessToken,
    sessionCookieOptions(accessCookieMaxAge(tokens.expiresIn)),
  );
  store.set(
    REFRESH_COOKIE,
    tokens.refreshToken,
    sessionCookieOptions(tokens.refreshExpiresIn),
  );
}

export async function deleteSession() {
  const store = await cookies();
  store.delete(ACCESS_COOKIE);
  store.delete(REFRESH_COOKIE);
}

export async function getAccessToken(): Promise<string | undefined> {
  const store = await cookies();
  return store.get(ACCESS_COOKIE)?.value;
}

export async function getRefreshToken(): Promise<string | undefined> {
  const store = await cookies();
  return store.get(REFRESH_COOKIE)?.value;
}

export const getCurrentUser = cache(async () => {
  const accessToken = await getAccessToken();
  if (!accessToken) return null;
  return fetchCurrentUser(accessToken);
});
