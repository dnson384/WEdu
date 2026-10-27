import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { ModeratorRepositoryImpl } from "@/infrastructure/repositories/ModeratorRepositoryImpl";
import { GetPendingQuestionsUsecase } from "@/usecases/moderator/GetPendingQuestionsUsecase";

export async function GET(req: NextRequest) {
  const headers = { "Cache-Control": "private, no-store" };
  const accessToken = req.cookies.get("accessToken")?.value;
  if (!accessToken)
    return NextResponse.json(
      { message: "Vui lòng đăng nhập lại" },
      { status: 401, headers },
    );

  const rawPage = req.nextUrl.searchParams.get("page") ?? "0";
  const search = (req.nextUrl.searchParams.get("search") ?? "").trim();
  const questionType = (
    req.nextUrl.searchParams.get("questionType") ?? ""
  ).trim();
  const page = Number(rawPage);

  if (
    !/^\d+$/.test(rawPage) ||
    !Number.isSafeInteger(page) ||
    page > 2147483647 ||
    search.length > 200 ||
    questionType.length > 100
  ) {
    return NextResponse.json(
      { message: "Trang hoặc bộ lọc không hợp lệ" },
      { status: 400, headers },
    );
  }

  try {
    const usecase = new GetPendingQuestionsUsecase(
      new ModeratorRepositoryImpl(),
    );
    return NextResponse.json(
      await usecase.execute({ page, search, questionType }, accessToken),
      { headers },
    );
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
    const message =
      status === 401
        ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại"
        : status === 403
          ? "Bạn không có quyền xem câu hỏi chờ duyệt"
          : status === 400
            ? "Trang hoặc bộ lọc không hợp lệ"
            : "Không thể tải câu hỏi chờ duyệt. Vui lòng thử lại";
    return NextResponse.json({ message }, { status, headers });
  }
}
