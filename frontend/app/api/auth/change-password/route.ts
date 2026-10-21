import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import { ChangePasswordUsecase } from "@/usecases/auth/ChangePasswordUsecase";
import { ChangePasswordPayload } from "@/presentation/schemas/auth.schema";
import { ChangePasswordEntity } from "@/domain/entities/auth.entity";
import { AuthRepositoryImpl } from "@/infrastructure/repositories/AuthRepositoryImpl";

export async function PUT(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
  }

  const body = await req.json();

  const validated = ChangePasswordPayload.safeParse(body);
  if (!validated.success) {
    return NextResponse.json(
      {
        message: "Dữ liệu đầu vào không hợp lệ!",
        error: z.flattenError(validated.error).fieldErrors,
      },
      { status: 400 },
    );
  }

  const { oldPassword, newPassword, confirmNewPassword } = validated.data;
  const payload: ChangePasswordEntity = {
    oldPassword: oldPassword,
    newPassword: newPassword,
    confirmNewPassword: confirmNewPassword,
  };

  try {
    const repo = new AuthRepositoryImpl();
    const usecase = new ChangePasswordUsecase(repo);
    const response = await usecase.execute(payload, accessToken);

    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err) && err.response) {
      return NextResponse.json(
        { message: err.response.data.message },
        { status: err.response.status },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
