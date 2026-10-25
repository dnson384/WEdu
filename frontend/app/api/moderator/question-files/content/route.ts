import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { GetQuestionFileContentUsecase } from "@/usecases/moderator/GetQuestionFileContentUsecase";

export async function GET(req: NextRequest) {
  const headers = { "Cache-Control": "private, no-store", "X-Content-Type-Options": "nosniff" };
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers });
  const key = req.nextUrl.searchParams.get("key");
  if (!key?.trim()) return NextResponse.json({ message: "Thiếu key tệp tin" }, { status: 400, headers });
  try {
    const data = await new GetQuestionFileContentUsecase(new ModeratorRepositoryImpl()).execute(key, token);
    return new NextResponse(new Uint8Array(data), {
      headers: { ...headers, "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
    });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
    const message = status === 404 ? "Tệp tin không còn tồn tại"
      : status === 403 ? "Bạn không có quyền xem tệp tin này"
      : status === 401 ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
      : status === 400 ? "Tệp tin câu hỏi không hợp lệ"
      : "Không thể tải nội dung tệp tin. Vui lòng thử lại";
    return NextResponse.json({ message }, { status, headers });
  }
}
