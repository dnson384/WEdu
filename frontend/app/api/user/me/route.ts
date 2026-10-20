import { GetMeUsecase } from "@/usecases/user/GetMeUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const repo = new UserRepositoryImpl();
    const usecase = new GetMeUsecase(repo);
    const response = await usecase.execute(accessToken);

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
