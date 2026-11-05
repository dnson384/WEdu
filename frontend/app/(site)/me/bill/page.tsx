"use client";

import Link from "next/link";
import usePayment from "@/presentation/hooks/Payment/usePayment";
import PaymentResult from "@/presentation/components/Payment/PaymentResult";

export default function Bill() {
  const payment = usePayment();
  if (payment.isLoadingUser) return <p className="p-8">Đang tải tài khoản…</p>;
  if (!payment.teacher)
    return (
      <div className="p-8">
        <p>Tính năng nâng cấp dành cho tài khoản giáo viên.</p>
        <Link href="/" className="mt-4 inline-block text-blue-600">
          Về trang chủ
        </Link>
      </div>
    );
  return (
    <main className="mx-auto max-w-3xl space-y-6 px-4 py-10">
      <header>
        <p className="text-sm font-medium uppercase tracking-wider text-blue-600">
          WEdu PLUS
        </p>
        <h1 className="mt-2 text-3xl font-bold">Nâng cấp tài khoản</h1>
        <p className="mt-3 text-slate-600">
          Bổ sung câu hỏi bằng AI khi ngân hàng câu hỏi chưa đáp ứng đủ đề thi.
        </p>
      </header>
      <section className="rounded-2xl border border-blue-200 bg-blue-50 p-6 sm:p-8">
        <h2 className="text-xl font-semibold">Gói PLUS dành cho giáo viên</h2>
        <p className="mt-5 text-4xl font-bold">
          20.000đ{" "}
          <span className="text-base font-normal text-slate-600">
            / 30 ngày
          </span>
        </p>
        <ul className="mt-5 list-inside list-disc space-y-2 text-slate-700">
          <li>AI bổ sung câu hỏi thiếu theo luồng tạo đề hiện có.</li>
          <li>Mua thêm được cộng dồn 30 ngày vào thời hạn còn lại.</li>
          <li>Không tự động trừ tiền. Hết hạn trở về quyền FREE.</li>
        </ul>
        <p className="mt-5 text-sm">
          Tài khoản hiện tại: <strong>{payment.user.accountType}</strong>
          {payment.expiryLabel && (
            <> · Hạn PLUS: {payment.expiryLabel} (giờ Việt Nam)</>
          )}
        </p>
        {payment.missingOrderId && (
          <div role="alert" className="mt-5 space-y-2 rounded-xl border border-amber-200 bg-amber-50 p-4">
            <p>Không tìm thấy đơn thanh toán trong tài khoản hiện tại.</p>
            <p className="break-all text-sm">Mã đơn: {payment.missingOrderId}</p>
            <p className="text-sm">Nếu đã thanh toán, hãy giữ mã đơn và liên hệ hỗ trợ đối soát. Chỉ bắt đầu giao dịch mới nếu bạn muốn thực hiện một lần mua khác.</p>
            <button type="button" onClick={payment.retry} disabled={payment.loadingOrder || payment.busy} className="underline disabled:opacity-50">
              Tải lại thông tin đơn
            </button>
          </div>
        )}
        <button
          type="button"
          onClick={payment.missingOrderId ? payment.startNewPayment : payment.startPayment}
          disabled={
            !(payment.missingOrderId ? payment.canStartNewPayment : payment.canStartPayment)
          }
          className="mt-6 rounded-xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {payment.busy
            ? "Đang xử lý…"
            : payment.missingOrderId
              ? "Bắt đầu giao dịch mới"
              : payment.user.accountType === "PLUS"
              ? "Gia hạn PLUS"
              : "Nâng cấp tài khoản"}
        </button>
        <p className="mt-3 text-sm text-slate-600">
          VNPay sẽ mở trong tab mới để chọn phương thức và xác thực thanh toán.
          Giữ trang này để theo dõi kết quả.
        </p>
        {payment.loadingPlan && (
          <p className="mt-3 text-sm">Đang kiểm tra thanh toán…</p>
        )}
        {payment.plan && !payment.plan.available && (
          <p className="mt-3 text-sm">
            Thanh toán VNPay chưa sẵn sàng. Vui lòng quay lại sau.
          </p>
        )}
      </section>
      {payment.error && (
        <div
          role="alert"
          className="rounded-xl border border-amber-200 bg-amber-50 p-4"
        >
          <p>{payment.error}</p>
          <button
            type="button"
            onClick={payment.retry}
            className="mt-2 underline"
          >
            Tải lại thông tin
          </button>
        </div>
      )}
      {payment.order && (
        <PaymentResult
          order={payment.order}
          expiryLabel={payment.paidExpiryLabel}
          busy={payment.busy}
          onVerify={payment.verifyPayment}
          onContinue={payment.continuePayment}
        />
      )}
      {payment.previousOrderIds.length > 0 && (
        <section className="space-y-2 rounded-xl border border-slate-200 p-4">
          <h2 className="font-semibold">Mã đơn cũ cần đối soát</h2>
          <p className="text-sm text-slate-600">Các mã dưới đây được giữ trên trình duyệt để liên hệ hỗ trợ nếu bạn đã thanh toán. Giao dịch mới không xác nhận hoặc khôi phục các đơn này.</p>
          <ul className="space-y-2 break-all text-sm">
            {payment.previousOrderIds.map((id) => <li key={id}>{id}</li>)}
          </ul>
        </section>
      )}
    </main>
  );
}
