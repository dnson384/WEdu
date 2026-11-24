import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { AdminTransactionRepositoryImpl } from "@/infrastructure/repositories/AdminTransactionRepositoryImpl";
import { AdminTransactionUsecase } from "@/usecases/admin/AdminTransactionUsecase";

export async function handleAdminTransactions(req: NextRequest) {
  const headers = { "Cache-Control": "private, no-store" };
  const fail = (status: number, message: string, code?: string) =>
    NextResponse.json({ message, code }, { status, headers });
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return fail(401, "Vui lòng đăng nhập lại");
  const usecase = new AdminTransactionUsecase(
    new AdminTransactionRepositoryImpl(),
  );
  try {
    const params = req.nextUrl.searchParams;
    const rawPage = params.get("page") ?? "0";
    const keyword = (params.get("keyword") ?? "").trim();
    if (
      !/^\d+$/.test(rawPage) ||
      Number(rawPage) > 2147483647 ||
      keyword.length > 254
    )
      return fail(400, "Bộ lọc giao dịch không hợp lệ");
    const result = await usecase.search(
      {
        keyword,
        page: Number(rawPage),
        status: params.get("status") ?? "",
        from: params.get("from") ?? "",
        to: params.get("to") ?? "",
      },
      token,
    );
    return NextResponse.json(result, { headers });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 503) : 500;
    const data = isAxiosError<{ message?: string; code?: string }>(error)
      ? error.response?.data
      : undefined;
    return fail(
      status,
      status < 500 && typeof data?.message === "string"
        ? data.message
        : "Không thể tải danh sách giao dịch",
    );
  }
}
