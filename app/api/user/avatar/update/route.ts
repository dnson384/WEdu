import { UpdateAvatarUsecase } from "@/application/usecases/user/updateAvatarUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(req: NextRequest) {
  const { s3Key }: { s3Key: string } = await req.json();

  if (s3Key.trim().length === 0) {
    return NextResponse.json({ message: "Không có s3Key" }, { status: 404 });
  }

  try {
    const repo = new UserRepositoryImpl();
    const usecase = new UpdateAvatarUsecase(repo);
    const response = await usecase.execute(s3Key);

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
