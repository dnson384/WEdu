import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { GetQuestionFilesUsecase } from "@/usecases/moderator/GetQuestionFilesUsecase";

export async function GET(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;
  const headers = { "Cache-Control": "private, no-store" };
  if (!accessToken) {
    return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers });
  }
  try {
    const usecase = new GetQuestionFilesUsecase(new ModeratorRepositoryImpl());
    return NextResponse.json(await usecase.execute(accessToken), { headers });
  } catch (error) {
    const status = isAxiosError(error) ? error.response?.status ?? 502 : 500;
    const message = status === 403
      ? "Bạn không có quyền xem danh sách tệp tin câu hỏi"
      : status === 401 ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
      : "Không thể lấy danh sách tệp tin câu hỏi. Vui lòng thử lại";
    return NextResponse.json({ message }, { status, headers });
  }
}
