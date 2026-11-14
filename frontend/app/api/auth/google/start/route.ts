import { createHash, randomBytes } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  if (request.headers.get("sec-fetch-site") === "cross-site") {
    return NextResponse.json(
      { message: "Yêu cầu không hợp lệ" },
      { status: 403, headers: { "Cache-Control": "no-store" } },
    );
  }

  const backend = process.env.GOOGLE_OAUTH_BASE_URL ?? process.env.BACKEND_URL;
  if (!backend) {
    const response = NextResponse.redirect(
      new URL("/auth/google-error", request.url),
    );
    response.headers.set("Cache-Control", "no-store");
    return response;
  }

  const verifier = randomBytes(32).toString("hex");
  const url = new URL("/oauth2/authorization/google", backend);
  url.searchParams.set(
    "binding",
    createHash("sha256").update(verifier).digest("hex"),
  );

  const response = NextResponse.redirect(url);
  response.cookies.set("googleVerifier", verifier, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/api/auth/google",
    maxAge: 600,
  });

  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Referrer-Policy", "no-referrer");
  return response;
}
