import { NextRequest, NextResponse } from "next/server";
import { VerificationRepositoryImpl } from "@/infrastructure/repositories/VerificationRepositoryImpl";
import { VerificationUsecase } from "@/usecases/auth/VerificationUsecase";
import { applyAuthCookies } from "../../flow";

export async function GET(request: NextRequest) {
  let response: NextResponse;

  try {
    const code = request.nextUrl.searchParams.get("code");
    const verifier = request.cookies.get("googleVerifier")?.value;

    if (
      !code ||
      !/^[a-f0-9]{64}$/.test(code) ||
      !verifier ||
      !/^[a-f0-9]{64}$/.test(verifier)
    ) {
      throw new Error("Invalid callback");
    }

    const result = await new VerificationUsecase(
      new VerificationRepositoryImpl(),
    ).exchange(code, verifier);

    response = applyAuthCookies(
      NextResponse.redirect(
        new URL(
          result.status === 202 ? "/auth/verify-email" : "/",
          request.url,
        ),
      ),
      result.cookies,
    );
  } catch {
    response = NextResponse.redirect(
      new URL("/auth/google-error", request.url),
    );
  }

  response.cookies.set("googleVerifier", "", {
    path: "/api/auth/google",
    maxAge: 0,
  });
  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Referrer-Policy", "no-referrer");

  return response;
}
