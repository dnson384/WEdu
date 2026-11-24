import { NextRequest, NextResponse } from "next/server";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { ApproveQuestionFileUsecase } from "@/usecases/moderator/ApproveQuestionFileUsecase";
import { reviewError, reviewHeaders } from "../reviewError";

export async function POST(req: NextRequest) {
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers: reviewHeaders });
  let data: FormData;
  try { data = await req.formData(); } catch {
    return NextResponse.json({ message: "Dữ liệu multipart không hợp lệ" }, { status: 400, headers: reviewHeaders });
  }
  const key = data.get("key");
  const file = data.get("file");
  if (typeof key !== "string" || !key.trim() || (file !== null && !(file instanceof File))) {
    return NextResponse.json({ message: "Thiếu key hoặc tệp không hợp lệ" }, { status: 400, headers: reviewHeaders });
  }
  // Forward only the documented fields; subject is derived by the backend.
  const payload = new FormData();
  payload.set("key", key);
  if (file instanceof File) payload.set("file", file);
  try {
    const usecase = new ApproveQuestionFileUsecase(new ModeratorRepositoryImpl());
    return NextResponse.json(await usecase.execute(payload, token), { headers: reviewHeaders });
  } catch (error) { return reviewError(error); }
}
