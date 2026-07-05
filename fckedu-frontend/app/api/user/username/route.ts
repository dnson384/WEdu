import { UpdateUsernameUsecase } from "@/application/usecases/user/updateUsernameUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;
  const refreshToken = req.cookies.get("refreshToken")?.value;

  const { username }: { username: string } = await req.json();

  if (username.trim().length === 0) {
    return NextResponse.json({ message: "Không có username" }, { status: 404 });
  }

  try {
    const repo = new UserRepositoryImpl();
    const usecase = new UpdateUsernameUsecase(repo);
    const response = await usecase.execute(
      username,
      accessToken!,
      refreshToken!,
    );

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
