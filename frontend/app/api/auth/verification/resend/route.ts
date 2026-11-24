import { NextRequest, NextResponse } from "next/server";
import { VerificationRepositoryImpl } from "@/infrastructure/repositories/VerificationRepositoryImpl";
import { VerificationUsecase } from "@/usecases/auth/VerificationUsecase";
import { authFlowError, requireSameOrigin } from "../../flow";

export async function POST(request: NextRequest) {
  const rejected = requireSameOrigin(request);
  if (rejected) return rejected;
  try {
    const result = await new VerificationUsecase(
      new VerificationRepositoryImpl(),
    ).resend(request.cookies.get("verificationToken")?.value);
    return NextResponse.json(result.data, {
      headers: { "Cache-Control": "private, no-store" },
    });
  } catch (error) {
    return authFlowError(error);
  }
}
