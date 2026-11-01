import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { AiManagementRepositoryImpl } from "@/infrastructure/repositories/AiManagementRepositoryImpl";
import { AiManagementUsecase } from "@/usecases/admin/AiManagementUsecase";

export async function handleAiManagement(
  req: NextRequest,
  operation: "usage" | "status" | "budget" | "updateBudget",
) {
  const headers = { "Cache-Control": "private, no-store" };
  const token = req.cookies.get("accessToken")?.value;
  if (!token)
    return NextResponse.json(
      { message: "Vui lòng đăng nhập lại" },
      { status: 401, headers },
    );
  const usecase = new AiManagementUsecase(new AiManagementRepositoryImpl());
  try {
    let result;
    if (operation === "usage") {
      const params = req.nextUrl.searchParams;
      result = await usecase.report(
        {
          from: params.get("from") ?? "",
          to: params.get("to") ?? "",
          model: params.get("model") || undefined,
        },
        token,
      );
    } else if (operation === "updateBudget") {
      let input;
      try {
        input = await req.json();
      } catch {
        return NextResponse.json(
          { message: "JSON không hợp lệ" },
          { status: 400, headers },
        );
      }
      if (
        !input ||
        !Number.isSafeInteger(input.revision) ||
        input.revision < 0 ||
        !(
          input.monthlyBudgetUsd === null ||
          typeof input.monthlyBudgetUsd === "string"
        ) ||
        !(
          input.monthlyTokenQuota === null ||
          (Number.isSafeInteger(input.monthlyTokenQuota) &&
            input.monthlyTokenQuota > 0)
        )
      ) {
        return NextResponse.json(
          { message: "Cấu hình hạn mức không hợp lệ" },
          { status: 400, headers },
        );
      }
      result = await usecase.updateBudget(
        {
          revision: input.revision,
          monthlyBudgetUsd: input.monthlyBudgetUsd,
          monthlyTokenQuota: input.monthlyTokenQuota,
        },
        token,
      );
    } else {
      result = await usecase[operation](token);
    }
    return NextResponse.json(result, { headers });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
    const message =
      isAxiosError<{ message?: string }>(error) && status < 500
        ? (error.response?.data?.message ?? "Yêu cầu không hợp lệ")
        : "Không thể tải hoặc lưu dữ liệu quản lý AI";
    return NextResponse.json({ message }, { status, headers });
  }
}
