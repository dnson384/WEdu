import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import { NewUserRequestEntity } from "@/domain/entities/auth.entity";
import RegisterUsecase from "@/usecases/auth/RegisterUsecase";
import { RegisterPayload } from "@/presentation/schemas/auth.schema";
import { AuthRepositoryImpl } from "@/infrastructure/repositories/AuthRepositoryImpl";
import { authFlowError, authFlowResponse, requireSameOrigin } from "../flow";

export async function POST(req: NextRequest) {
  const rejected = requireSameOrigin(req);
  if (rejected) return rejected;
  try {
    const body = await req.json();

    const validated = RegisterPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400, headers: { "Cache-Control": "private, no-store" } },
      );
    }

    const { email, plainPassword, confirmPassword, username, loginMethod } =
      validated.data;
    const payload: NewUserRequestEntity = {
      email: email,
      plainPassword: plainPassword,
      confirmPassword: confirmPassword,
      username: username,
      loginMethod: loginMethod,
    };

    const repo = new AuthRepositoryImpl();
    const usecase = new RegisterUsecase(repo);
    const response = await usecase.execute(payload);

    return authFlowResponse(response);
  } catch (err) {
    if (err instanceof SyntaxError)
      return NextResponse.json(
        { message: "Dữ liệu không hợp lệ" },
        { status: 400, headers: { "Cache-Control": "private, no-store" } },
      );
    return authFlowError(err);
  }
}
