import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { AuthFlowResult } from "@/domain/entities/verification.entity";

export function requireSameOrigin(request: NextRequest) {
  const origin = request.headers.get("origin");
  if (
    origin !== request.nextUrl.origin ||
    request.headers.get("sec-fetch-site") === "cross-site"
  ) {
    return NextResponse.json(
      { message: "Yêu cầu không hợp lệ" },
      { status: 403, headers: { "Cache-Control": "private, no-store" } },
    );
  }
}

export function applyAuthCookies(response: NextResponse, cookies: string[]) {
  const maxAges: Record<string, number> = {
    accessToken: 300,
    refreshToken: 604800,
    verificationToken: 1800,
  };
  for (const cookie of cookies) {
    const pair = cookie.split(";", 1)[0];
    const separator = pair.indexOf("=");
    const name = pair.slice(0, separator);
    if (!(name in maxAges)) continue;
    const value = pair.slice(separator + 1);
    response.cookies.set(name, value, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: /(?:^|;)\s*Max-Age=0(?:;|$)/i.test(cookie) ? 0 : maxAges[name],
    });
  }
  response.headers.set("Cache-Control", "private, no-store");
  response.headers.set("Referrer-Policy", "no-referrer");
  return response;
}

export function authFlowResponse(result: AuthFlowResult) {
  return applyAuthCookies(
    NextResponse.json(
      result.status === 202 ? result.data : { user: result.data },
      {
        status: result.status,
        headers: { "Cache-Control": "private, no-store" },
      },
    ),
    result.cookies,
  );
}

export function authFlowError(error: unknown) {
  if (isAxiosError(error) && error.response) {
    const status = error.response.status;
    const data = error.response.data;
    const retryAfter = Number(
      error.response.headers["retry-after"] ?? data?.retryAfter ?? 0,
    );
    return NextResponse.json(
      {
        message:
          typeof data?.message === "string"
            ? data.message
            : "Yêu cầu xác thực không hợp lệ",
        code: typeof data?.code === "string" ? data.code : undefined,
        retryAfter: Number.isFinite(retryAfter) ? retryAfter : 0,
      },
      {
        status,
        headers: {
          "Cache-Control": "private, no-store",
          ...(retryAfter > 0 ? { "Retry-After": String(retryAfter) } : {}),
        },
      },
    );
  }
  return NextResponse.json(
    { message: "Không thể xác thực. Vui lòng thử lại sau" },
    {
      status: 503,
      headers: { "Cache-Control": "private, no-store" },
    },
  );
}
