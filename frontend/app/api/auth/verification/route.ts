import { NextRequest, NextResponse } from "next/server";
import { VerificationRepositoryImpl } from "@/infrastructure/repositories/VerificationRepositoryImpl";
import { VerificationUsecase } from "@/usecases/auth/VerificationUsecase";
import { authFlowError } from "../flow";

export async function GET(request: NextRequest) {
  try {
    const result = await new VerificationUsecase(
      new VerificationRepositoryImpl(),
    ).metadata(request.cookies.get("verificationToken")?.value);
    return NextResponse.json(result.data, {
      headers: { "Cache-Control": "private, no-store" },
    });
  } catch (error) {
    return authFlowError(error);
  }
}
