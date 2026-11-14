import { NextRequest, NextResponse } from "next/server";
import { VerificationRepositoryImpl } from "@/infrastructure/repositories/VerificationRepositoryImpl";
import { VerificationUsecase } from "@/usecases/auth/VerificationUsecase";
import { authFlowError, authFlowResponse, requireSameOrigin } from "../../flow";

export async function POST(request: NextRequest) {
  const rejected = requireSameOrigin(request);
  if (rejected) return rejected;
  try {
    const body = await request.json();
    if (typeof body?.otp !== "string" || !/^[0-9]{6}$/.test(body.otp)) {
      return NextResponse.json(
        { message: "OTP phải gồm 6 chữ số" },
        { status: 400, headers: { "Cache-Control": "private, no-store" } },
      );
    }
    return authFlowResponse(
      await new VerificationUsecase(new VerificationRepositoryImpl()).verify(
        body.otp,
        request.cookies.get("verificationToken")?.value,
      ),
    );
  } catch (error) {
    if (error instanceof SyntaxError)
      return NextResponse.json(
        { message: "Dữ liệu không hợp lệ" },
        { status: 400, headers: { "Cache-Control": "private, no-store" } },
      );
    return authFlowError(error);
  }
}
