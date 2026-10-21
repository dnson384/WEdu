import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { AuthRepositoryImpl } from "@/infrastructure/repositories/AuthRepositoryImpl";
import LogoutUsecase from "@/usecases/auth/LogoutUsecase";

export async function POST(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const repo = new AuthRepositoryImpl();
    const usecase = new LogoutUsecase(repo);
    const response = await usecase.execute(accessToken);

    const nextResponse = NextResponse.json(response, { status: 200 });

    nextResponse.cookies.delete("accessToken");

    nextResponse.cookies.delete("refreshToken");

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
