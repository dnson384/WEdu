import { NextRequest, NextResponse } from "next/server";
import { isAxiosError } from "axios";
import { RefundUsecase } from "@/usecases/payment/RefundUsecase";
import { RefundRepositoryImpl } from "@/infrastructure/repositories/RefundRepositoryImpl";
import { RefundAction } from "@/domain/entities/refund.entity";

type Operation = "list" | "get" | "orders" | "create" | RefundAction;
export async function handleRefund(req: NextRequest, operation: Operation, admin: boolean, id?: string) {
  const headers = { "Cache-Control": "private, no-store" };
  const fail = (status: number, message: string) => NextResponse.json({ message }, { status, headers });
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return fail(401, "Vui lòng đăng nhập lại.");
  if (id !== undefined && !(operation === "create" ? /^[a-f0-9]{64}$/ : /^[a-f0-9]{32}$/).test(id)) return fail(400, "Mã yêu cầu không hợp lệ.");
  const usecase = new RefundUsecase(new RefundRepositoryImpl());
  try {
    let data;
    if (operation === "orders" || operation === "list") {
      const params = req.nextUrl.searchParams;
      const rawPage = params.get("page") ?? "0";
      if (!/^\d{1,7}$/.test(rawPage) || Number(rawPage) > 1000000 || (params.get("keyword")?.length ?? 0) > 254) return fail(400, "Bộ lọc không hợp lệ.");
      data = operation === "orders" ? await usecase.orders(Number(rawPage), token) : await usecase.search(admin, {
        page: Number(rawPage), keyword: params.get("keyword") ?? "", status: params.get("status") ?? "",
        from: params.get("from") ?? "", to: params.get("to") ?? "",
      }, token);
    } else if (operation === "get") data = await usecase.get(admin, id!, token);
    else if (operation === "reconcile") data = await usecase.decide(id!, operation, { revision: 0 }, token);
    else {
      let body;
      try {
        const raw = await req.text();
        if (raw.length > 8192) return fail(400, "Dữ liệu quá lớn.");
        body = JSON.parse(raw);
      } catch { return fail(400, "Dữ liệu không hợp lệ."); }
      if (!body || typeof body !== "object") return fail(400, "Dữ liệu không hợp lệ.");
      if (operation === "create") {
        const key = req.headers.get("Idempotency-Key") ?? "";
        if (!/^[a-zA-Z0-9-]{16,80}$/.test(key) || typeof body.reason !== "string" || body.reason.trim().length < 3
          || body.reason.length > 1000 || body.policyVersion !== "plus-refund-v1") return fail(400, "Vui lòng nhập lý do và xác nhận điều khoản.");
        data = await usecase.create(id!, { reason: body.reason, policyVersion: body.policyVersion }, key, token);
      } else {
        if (!Number.isSafeInteger(body.revision) || body.revision < 0 || (body.reason !== undefined && (typeof body.reason !== "string" || body.reason.length > 1000))
          || (body.legacyException !== undefined && typeof body.legacyException !== "boolean")) return fail(400, "Dữ liệu xử lý không hợp lệ.");
        data = await usecase.decide(id!, operation, { revision: body.revision, reason: body.reason, legacyException: body.legacyException }, token);
      }
    }
    return NextResponse.json(data, { headers });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 503) : 500;
    const message = isAxiosError<{ message?: string }>(error) && status < 500 ? error.response?.data?.message : undefined;
    return fail(status, typeof message === "string" ? message : "Chưa thể xác nhận kết quả. Hãy tải lại trạng thái trước khi thử lại.");
  }
}
