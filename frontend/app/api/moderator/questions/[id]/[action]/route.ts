import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { ReviewPendingQuestionUsecase } from "@/usecases/moderator/ReviewPendingQuestionUsecase";

export async function POST(
  req: NextRequest,
  context: { params: Promise<{ id: string; action: string }> },
) {
  const headers = { "Cache-Control": "private, no-store" };

  const token = req.cookies.get("accessToken")?.value;
  if (!token) {
    return NextResponse.json(
      { message: "Vui lòng đăng nhập lại" },
      { status: 401, headers },
    );
  }
  const { id, action } = await context.params;
  if (action !== "approve" && action !== "reject") {
    return NextResponse.json(
      { message: "Không tìm thấy thao tác" },
      { status: 404, headers },
    );
  }

  if (!/^[a-f\d]{24}$/i.test(id)) {
    return NextResponse.json(
      { message: "ID câu hỏi không hợp lệ" },
      { status: 400, headers },
    );
  }

  let input;
  try {
    input = await req.json();
  } catch {
    return NextResponse.json(
      { message: "Dữ liệu JSON không hợp lệ" },
      { status: 400, headers },
    );
  }

  if (
    !input ||
    !Number.isSafeInteger(input.reviewRevision) ||
    input.reviewRevision < 0 ||
    !(
      input.updatedAt === null ||
      (typeof input.updatedAt === "string" &&
        Number.isFinite(Date.parse(input.updatedAt)))
    )
  ) {
    return NextResponse.json(
      { message: "Phiên bản câu hỏi không hợp lệ" },
      { status: 400, headers },
    );
  }

  try {
    const repo = new ModeratorRepositoryImpl();
    const usecase = new ReviewPendingQuestionUsecase(repo);
    const result = await usecase.execute(
      id,
      action,
      {
        updatedAt: input.updatedAt,
        reviewRevision: input.reviewRevision,
      },
      token,
    );
    return action === "approve"
      ? new NextResponse(null, { status: 204, headers })
      : NextResponse.json(result, { headers });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
    const message =
      status === 409
        ? "Câu hỏi đã thay đổi hoặc không còn chờ duyệt. Danh sách sẽ được tải lại."
        : status === 401
          ? "Phiên đăng nhập đã hết hạn"
          : status === 403
            ? "Bạn không có quyền kiểm duyệt câu hỏi"
            : status === 404
              ? "Không tìm thấy câu hỏi"
              : status === 400
                ? "Dữ liệu kiểm duyệt hoặc yêu cầu sinh lại chưa hợp lệ. Hãy tải lại danh sách."
                : "Không thể hoàn tất kiểm duyệt. Hãy làm mới danh sách để kiểm tra kết quả trước khi thử lại.";
    return NextResponse.json({ message }, { status, headers });
  }
}
