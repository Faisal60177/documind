import { NextResponse, type NextRequest } from "next/server";
import { refreshSession } from "@/lib/auth/backend";
import {
  ACCESS_COOKIE,
  REFRESH_COOKIE,
  accessCookieMaxAge,
  sessionCookieOptions,
} from "@/lib/auth/config";

function redirectToLogin(request: NextRequest) {
  const url = request.nextUrl.clone();
  url.pathname = "/login";
  url.search = "";
  return NextResponse.redirect(url);
}

export async function proxy(request: NextRequest) {
  const accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;

  // Signed in: the backend verifies the token on every API call.
  if (accessToken) return NextResponse.next();

  if (!refreshToken) return redirectToLogin(request);

  // The access cookie expired: use the refresh token to start a new session.
  const result = await refreshSession(refreshToken);
  if (!result.ok) {
    const response = redirectToLogin(request);
    response.cookies.delete(ACCESS_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
    return response;
  }

  const tokens = result.data;

  // Make the new tokens visible to the page rendered for this same request.
  request.cookies.set(ACCESS_COOKIE, tokens.accessToken);
  request.cookies.set(REFRESH_COOKIE, tokens.refreshToken);
  const response = NextResponse.next({ request });

  // And store them in the browser.
  response.cookies.set(
    ACCESS_COOKIE,
    tokens.accessToken,
    sessionCookieOptions(accessCookieMaxAge(tokens.expiresIn)),
  );
  response.cookies.set(
    REFRESH_COOKIE,
    tokens.refreshToken,
    sessionCookieOptions(tokens.refreshExpiresIn),
  );
  return response;
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
