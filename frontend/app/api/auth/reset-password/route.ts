import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import { AuthRepositoryImpl } from "@/infrastructure/repositories/AuthRepositoryImpl";
import { ResetPasswordPayload } from "@/presentation/schemas/auth.schema";
import { ResetPasswordUsecase } from "@/usecases/auth/ResetPasswordUsecase";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const validated = ResetPasswordPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400 },
      );
    }

    const { plainToken, newPassword, confirmNewPassword } = validated.data;

    const repo = new AuthRepositoryImpl();
    const usecase = new ResetPasswordUsecase(repo);
    const response = await usecase.execute(
      plainToken,
      newPassword,
      confirmNewPassword,
    );

    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err)) {
      return NextResponse.json(
        { message: err.response?.data.message },
        { status: err.status },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
