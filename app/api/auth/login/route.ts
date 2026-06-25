import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import { LoginPayload } from "@/presentation/schemas/auth.schema";
import { LoginPayloadEntity } from "@/domain/entities/auth.entity";
import { authRepositoryImpl } from "@/infrastructure/repositories/authRepositoryImpl";
import LoginUsecase from "@/application/usecases/auth/loginUsecase";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const validated = LoginPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400 },
      );
    }

    const { email, plainPassword } = validated.data;
    const payload: LoginPayloadEntity = {
      email: email,
      plainPassword: plainPassword,
    };

    const repo = new authRepositoryImpl();
    const usecase = new LoginUsecase(repo);
    const response = await usecase.execute(payload);

    const nextResponse = NextResponse.json(
      { user: response.user },
      { status: 200 },
    );
    
    nextResponse.cookies.set("accessToken", response.accessToken, {
      httpOnly: true,
      secure: false,
      path: "/",
      maxAge: 15 * 60,
      sameSite: "lax",
    });

    nextResponse.cookies.set("refreshToken", response.refreshToken, {
      httpOnly: true,
      secure: false,
      path: "/",
      maxAge: 7 * 24 * 60 * 60,
      sameSite: "lax",
    });

    return nextResponse;
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
