import { uploadDocxFileUsecase } from "@/application/usecases/uploadFile/upload-docx-file";
import { UploadDocxFileRepositoryImpl } from "@/infrastructure/repositories/uploadFileRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  const formData: FormData = await req.formData();

  const file = formData.get("file");
  const subject = formData.get("subject") as string;

  if (!file || !subject)
    return NextResponse.json(
      { message: "Thiếu file hoặc tên môn học" },
      { status: 400 },
    );

  try {
    const repo = new UploadDocxFileRepositoryImpl();
    const usecase = new uploadDocxFileUsecase(repo);
    const response = await usecase.execute(subject,formData);
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
