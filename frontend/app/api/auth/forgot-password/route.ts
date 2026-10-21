import { ForgotPasswordUsecase } from "@/usecases/auth/ForgotPasswordUsecase";
import { AuthRepositoryImpl } from "@/infrastructure/repositories/AuthRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const searchParams = req.nextUrl.searchParams;

    const email = searchParams.get("email");
    console.log("Email received for forgot password:", email);

    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    if (!email || email.trim().length === 0 || !emailRegex.test(email)) {
      return NextResponse.json(
        { message: "Email không hợp lệ" },
        { status: 400 },
      );
    }

    const repo = new AuthRepositoryImpl();
    const usecase = new ForgotPasswordUsecase(repo);
    const response = await usecase.execute(email);

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
