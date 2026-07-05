import { UploadAvatarUsecase } from "@/application/usecases/user/uploadAvatarUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;
  const refreshToken = req.cookies.get("refreshToken")?.value;

  const formData: FormData = await req.formData();

  const file = formData.get("file");

  if (!file) {
    return NextResponse.json({ message: "Thiếu file ảnh" }, { status: 400 });
  }

  try {
    const repo = new UserRepositoryImpl();
    const usecase = new UploadAvatarUsecase(repo);
    const response = await usecase.execute(
      formData,
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
