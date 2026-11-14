export const ACCESS_COOKIE = "documind_access";
export const REFRESH_COOKIE = "documind_refresh";

// The access cookie expires slightly before the JWT so the session is refreshed in time.
export function accessCookieMaxAge(expiresInSeconds: number): number {
  return Math.max(expiresInSeconds - 30, 1);
}

export function sessionCookieOptions(maxAgeSeconds: number) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
    maxAge: maxAgeSeconds,
  };
}
