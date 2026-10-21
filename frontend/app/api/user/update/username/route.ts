import { UpdateUsernameUsecase } from "@/usecases/user/UpdateUsernameUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/UserRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
  }

  const { username }: { username: string } = await req.json();

  if (username.trim().length === 0) {
    return NextResponse.json({ message: "Không có username" }, { status: 404 });
  }

  try {
    const repo = new UserRepositoryImpl();
    const usecase = new UpdateUsernameUsecase(repo);
    const response = await usecase.execute(username, accessToken!);

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
