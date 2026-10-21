import { UploadFileToQuestionBankUsecase } from "@/usecases/storage/UploadFileToQuestionBankUsecase";
import { StorageRepositoryImpl } from "@/infrastructure/repositories/StorageRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
  }

  const formData: FormData = await req.formData();

  const file = formData.get("file");
  const subject = formData.get("subject") as string;

  if (!file || !subject)
    return NextResponse.json(
      { message: "Thiếu file hoặc tên môn học" },
      { status: 400 },
    );

  try {
    const repo = new StorageRepositoryImpl();
    const usecase = new UploadFileToQuestionBankUsecase(repo);
    const response = await usecase.execute(formData, accessToken);
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
