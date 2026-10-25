import { NextRequest, NextResponse } from "next/server";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { RejectQuestionFileUsecase } from "@/usecases/moderator/RejectQuestionFileUsecase";
import { reviewError, reviewHeaders } from "../reviewError";

export async function POST(req: NextRequest) {
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers: reviewHeaders });
  let input;
  try { input = await req.json(); } catch {
    return NextResponse.json({ message: "JSON không hợp lệ" }, { status: 400, headers: reviewHeaders });
  }
  if (!input || typeof input.key !== "string" || !input.key.trim()
      || typeof input.reason !== "string" || !input.reason.trim() || input.reason.trim().length > 2000) {
    return NextResponse.json({ message: "Thiếu key hoặc lý do phải có từ 1 đến 2000 ký tự" }, { status: 400, headers: reviewHeaders });
  }
  try {
    const usecase = new RejectQuestionFileUsecase(new ModeratorRepositoryImpl());
    await usecase.execute({ key: input.key, reason: input.reason.trim() }, token);
    return new NextResponse(null, { status: 204, headers: reviewHeaders });
  } catch (error) { return reviewError(error); }
}
