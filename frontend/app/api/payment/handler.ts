import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { PaymentRepositoryImpl } from "@/infrastructure/repositories/PaymentRepositoryImpl";
import { PaymentUsecase } from "@/usecases/payment/PaymentUsecase";

export async function handlePayment(
  req: NextRequest,
  operation: "plan" | "create" | "order" | "verify" | "confirmReturn",
  id?: string,
) {
  const headers = { "Cache-Control": "private, no-store" };
  const token = req.cookies.get("accessToken")?.value;
  if (!token) return NextResponse.json({ message: "Vui lòng đăng nhập lại" }, { status: 401, headers });
  if ((operation === "order" || operation === "verify" || operation === "confirmReturn") && !/^[a-f0-9]{64}$/.test(id ?? ""))
    return NextResponse.json({ message: "Mã đơn không hợp lệ" }, { status: 400, headers });
  const key = req.headers.get("Idempotency-Key") ?? "";
  if (operation === "create" && !/^[a-zA-Z0-9-]{16,80}$/.test(key))
    return NextResponse.json({ message: "Mã yêu cầu không hợp lệ" }, { status: 400, headers });
  const usecase = new PaymentUsecase(new PaymentRepositoryImpl());
  let parameters: [string, string][] = [];
  if (operation === "confirmReturn") {
    try {
      const body = await req.json();
      if (!Array.isArray(body?.parameters) || !body.parameters.length || body.parameters.length > 32 ||
        body.parameters.some((pair: unknown) => !Array.isArray(pair) || pair.length !== 2 ||
          typeof pair[0] !== "string" || !/^vnp_[a-zA-Z0-9_]{1,64}$/.test(pair[0]) ||
          typeof pair[1] !== "string" || pair[1].length > 1024)) throw new Error("Invalid parameters");
      parameters = body.parameters;
      if (new Set(parameters.map(([name]) => name)).size !== parameters.length) throw new Error("Duplicate parameters");
    } catch {
      return NextResponse.json({ message: "Dữ liệu kết quả thanh toán không hợp lệ" }, { status: 400, headers });
    }
  }
  try {
    const data = operation === "plan" ? await usecase.plan(token)
      : operation === "create" ? await usecase.create(key, token)
      : operation === "confirmReturn" ? await usecase.confirmReturn(id!, parameters, token)
      : await usecase[operation](id!, token);
    return NextResponse.json(data, { headers });
  } catch (error) {
    const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
    if (operation === "verify" && status === 503) {
      return NextResponse.json({
        message: "Chưa thể đối soát với VNPay. Đơn đang chờ xác nhận; vui lòng kiểm tra lại sau.",
        code: "PAYMENT_VERIFICATION_UNAVAILABLE",
      }, { status: 503, headers: { ...headers, "Retry-After": "30" } });
    }
    const message = isAxiosError<{ message?: string }>(error) && status < 500
      ? error.response?.data?.message ?? "Yêu cầu thanh toán không hợp lệ"
      : "Chưa thể kết nối hoặc xác nhận thanh toán. Vui lòng kiểm tra lại sau.";
    return NextResponse.json({ message }, { status, headers });
  }
}
