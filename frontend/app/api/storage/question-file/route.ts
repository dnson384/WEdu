import { UploadFileToQuestionBankUsecase } from "@/usecases/storage/UploadFileToQuestionBankUsecase";
import { StorageRepositoryImpl } from "@/infrastructure/repositories/StorageRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;

  if (!accessToken) {
    return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401 });
  }

  let formData: FormData;
  try {
    formData = await req.formData();
  } catch {
    return NextResponse.json({ message: "Dữ liệu upload phải là multipart/form-data" }, { status: 400 });
  }

  const file = formData.get("file");
  const subject = formData.get("subject");

  if (!file || typeof file === "string" || typeof subject !== "string" || !subject.trim())
    return NextResponse.json(
      { message: "Thiếu file hoặc tên môn học" },
      { status: 400 },
    );

  if (file.size === 0 || file.size > 50 * 1024 * 1024) {
    return NextResponse.json(
      { message: file.size === 0 ? "Không thể tải lên file rỗng" : "Dung lượng file tối đa là 50 MB" },
      { status: file.size === 0 ? 400 : 413 },
    );
  }
  formData.set("subject", subject.trim());

  try {
    const repo = new StorageRepositoryImpl();
    const usecase = new UploadFileToQuestionBankUsecase(repo);
    const response = await usecase.execute(formData, accessToken);
    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err)) {
      const data = err.response?.data;
      const message = typeof data === "string" ? data : data?.message;
      return NextResponse.json(
        { message: message || "Không thể tải lên tệp tin. Vui lòng thử lại" },
        { status: err.response?.status ?? 502 },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
