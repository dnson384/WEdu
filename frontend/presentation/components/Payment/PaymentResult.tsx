import { PaymentOrder } from "@/domain/entities/payment.entity";

interface Props {
  order: PaymentOrder;
  expiryLabel: string | null;
  busy: boolean;
  onVerify: () => void;
  onContinue: () => void;
}

export default function PaymentResult({ order, expiryLabel, busy, onVerify, onContinue }: Props) {
  const messages = {
    PENDING: "Đang chờ xác nhận thanh toán",
    SUCCEEDED: order.manualReview ? "Đã nhận thanh toán — cần hỗ trợ cấp quyền" : "Nâng cấp tài khoản thành công",
    FAILED: "Thanh toán không thành công",
    CANCELLED: "Thanh toán đã bị hủy",
    EXPIRED: "Đã hết thời gian thanh toán",
  };
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6" aria-live="polite">
      <h2 className="text-lg font-semibold">{messages[order.status]}</h2>
      <p className="mt-2 break-all text-sm text-slate-500">Mã đơn: {order.orderId}</p>
      {order.status === "SUCCEEDED" ? (
        <p className="mt-3">{order.manualReview
          ? "Vui lòng liên hệ hỗ trợ với mã đơn để xử lý tài khoản."
          : `PLUS có hiệu lực đến ${expiryLabel} (giờ Việt Nam).`}</p>
      ) : (
        <>
          <p className="mt-3 text-sm text-slate-600">
            Nếu đã thanh toán, hãy kiểm tra lại. WEdu chỉ nâng cấp sau khi nhận xác nhận từ VNPay.
            Mỗi lần đối soát cách nhau ít nhất 30 giây.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button type="button" disabled={busy} onClick={onVerify}
              className="rounded-lg border border-slate-300 px-4 py-2 disabled:opacity-50">Kiểm tra thanh toán</button>
            {order.status === "PENDING" && order.paymentUrl && (
              <button type="button" disabled={busy} onClick={onContinue}
                className="rounded-lg bg-blue-600 px-4 py-2 text-white disabled:opacity-50">Tiếp tục tại VNPay (tab mới)</button>
            )}
          </div>
        </>
      )}
      <p className="mt-4 text-sm text-slate-500">Kết quả thanh toán sẽ được gửi đến email tài khoản.</p>
    </section>
  );
}
