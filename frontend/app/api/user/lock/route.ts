import { lockAccountUsecase } from "@/application/usecases/user/lockAccountUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    const refreshToken = req.cookies.get("refreshToken")?.value;

    const repo = new UserRepositoryImpl();
    const usecase = new lockAccountUsecase(repo);
    const response = await usecase.execute(accessToken!, refreshToken!);

    const nextResponse = NextResponse.json(response, { status: 200 });

    nextResponse.cookies.delete("accessToken");
    nextResponse.cookies.delete("refreshToken");

    return nextResponse;
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
