import { jwtVerify } from "jose";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.BACKEND_URL;

const publicPaths = [
  "/auth/login",
  "/auth/register",
  "/auth/forgot-password",
  "/auth/reset-password",
  "/landing-page",
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
];

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (
    publicPaths.some((p) => pathname.startsWith(p)) ||
    pathname.includes(".")
  ) {
    return NextResponse.next();
  }

  let accessToken = request.cookies.get("accessToken")?.value;
  const refreshToken = request.cookies.get("refreshToken")?.value;

  let accountId: string | null = null;
  let isTokensRegenerated = false;
  let newAccessToken: string | undefined = undefined;

  const binaryString = atob(process.env.JWT_ACCESS_SECRET!);
  const accessSecret = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    accessSecret[i] = binaryString.charCodeAt(i);
  }

  // Kiểm tra AT
  if (accessToken) {
    try {
      const { payload } = await jwtVerify(accessToken, accessSecret);

      accountId = payload.sub as string;
    } catch (error: unknown) {
      const isJwtExpired =
        typeof error === "object" &&
        error !== null &&
        "code" in error &&
        (error as Record<string, unknown>).code === "ERR_JWT_EXPIRED";

      if (isJwtExpired) {
        accessToken = undefined;
      } else {
        return forceLogout(request);
      }
    }
  }

  // Refresh AT
  if (!accessToken) {
    if (!refreshToken) {
      return forceLogout(request);
    }

    try {
      const response = await fetch(
        `${BACKEND_URL}/auth/regenerate-access-token`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Cookie: `refreshToken=${refreshToken}`,
          },
          cache: "no-store",
        },
      );

      if (response.status === 401 || response.status === 403) {
        return forceLogout(request);
      }
      if (!response.ok) {
        throw new Error("Spring Boot từ chối cấp lại token");
      }

      const data: { accessToken: string } = await response.json();
      newAccessToken = data.accessToken;

      const { payload } = await jwtVerify(newAccessToken, accessSecret);
      accountId = payload.sub as string;

      isTokensRegenerated = true;
    } catch (err) {
      console.error("Lỗi gia hạn token tại Middleware:", err);
      return forceLogout(request);
    }
  }

  // Gán accout_id & AT mới
  const requestHeaders = new Headers(request.headers);

  requestHeaders.set("x-account-id", accountId!);

  if (isTokensRegenerated && newAccessToken) {
    requestHeaders.set(
      "cookie",
      `accessToken=${newAccessToken}; refreshToken=${refreshToken}`,
    );
  }

  const response = NextResponse.next({
    request: { headers: requestHeaders },
  });

  if (isTokensRegenerated && newAccessToken) {
    response.cookies.set("accessToken", newAccessToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      path: "/",
      maxAge: 5 * 60,
      sameSite: "lax",
    });
  }

  return response;
}

function forceLogout(request: NextRequest) {
  const loginUrl = new URL("/landing-page", request.url);

  const response = NextResponse.redirect(loginUrl);
  response.cookies.delete("accessToken");
  response.cookies.delete("refreshToken");
  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
