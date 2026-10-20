import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { UploadAvatarUsecase } from "@/usecases/storage/UploadAvatarUsecase";
import { StorageRepositoryImpl } from "@/infrastructure/repositories/StorageRepositoryImpl";

export async function POST(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
  }

  const formData: FormData = await req.formData();

  const file = formData.get("file");

  if (!file) {
    return NextResponse.json({ message: "Thiếu file ảnh" }, { status: 400 });
  }

  try {
    const repo = new StorageRepositoryImpl();
    const usecase = new UploadAvatarUsecase(repo);
    const response = await usecase.execute(formData, accessToken);

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
