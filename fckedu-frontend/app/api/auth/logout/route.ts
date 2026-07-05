import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { authRepositoryImpl } from "@/infrastructure/repositories/authRepositoryImpl";
import LogoutUsecase from "@/application/usecases/auth/logoutUsecase";

export async function POST(req: NextRequest) {
  try {
    const repo = new authRepositoryImpl();
    const usecase = new LogoutUsecase(repo);
    const response = await usecase.execute();

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
