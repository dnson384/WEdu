import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { EditPendingQuestionUsecase } from "@/usecases/moderator/EditPendingQuestionUsecase";

export async function PATCH(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  const headers = { "Cache-Control": "private, no-store" };
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers });
  const { id } = await context.params;
  if (!/^[a-f\d]{24}$/i.test(id)) return NextResponse.json({ message: "ID câu hỏi không hợp lệ" }, { status: 400, headers });
  let form: FormData;
  try { form = await req.formData(); } catch {
    return NextResponse.json({ message: "Dữ liệu multipart không hợp lệ" }, { status: 400, headers });
  }
  const payload = form.getAll("payload");
  if (payload.length !== 1 || !(payload[0] instanceof File) || payload[0].type !== "application/json" || payload[0].size > 1024 * 1024) {
    return NextResponse.json({ message: "Thiếu hoặc sai dữ liệu câu hỏi" }, { status: 400, headers });
  }
  let images = 0;
  for (const [name, value] of form.entries()) {
    if (name === "payload") continue;
    if (!/^(question|options\.\d+)\.img_\d+$/.test(name) || !(value instanceof File) || value.size === 0 || value.size > 5 * 1024 * 1024 || form.getAll(name).length !== 1) {
      return NextResponse.json({ message: "Tệp ảnh hoặc tham chiếu không hợp lệ (tối đa 5 MiB/ảnh)" }, { status: 400, headers });
    }
    images++;
  }
  if (images > 10) return NextResponse.json({ message: "Chỉ tải tối đa 10 ảnh trong một lần lưu" }, { status: 400, headers });
  try {
    await new EditPendingQuestionUsecase(new ModeratorRepositoryImpl()).execute(id, form, token);
    return new NextResponse(null, { status: 204, headers });
  } catch (error) {
    const status = isAxiosError(error) ? error.response?.status ?? 502 : 500;
    const message = status === 409 ? "Câu hỏi đã thay đổi hoặc không còn chờ duyệt. Hãy tải lại danh sách."
      : status === 401 ? "Phiên đăng nhập đã hết hạn"
        : status === 403 ? "Bạn không có quyền chỉnh sửa câu hỏi"
          : status === 404 ? "Không tìm thấy câu hỏi"
            : status === 400 ? "Nội dung, công thức hoặc ảnh chưa hợp lệ. Kiểm tra các tham chiếu và tải ảnh còn thiếu."
              : "Không thể lưu câu hỏi. Vui lòng tải lại danh sách để kiểm tra trước khi thử lại.";
    return NextResponse.json({ message }, { status, headers });
  }
}
