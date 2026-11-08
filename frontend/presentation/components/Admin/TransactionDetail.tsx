import { AdminTransaction, TransactionLookup } from "@/domain/entities/adminTransaction.entity";
import { providerStatus, transactionDate, transactionMoney, transactionStatuses } from "@/presentation/utils/transactionDisplay";

export default function TransactionDetail({ selected, result, busy, error, onClose, onRetry }: {
  selected: AdminTransaction; result: TransactionLookup | null; busy: boolean; error: string;
  onClose: () => void; onRetry: () => void;
}) {
  const order = result?.order ?? selected;
  return <section aria-label="Chi tiết tra cứu" aria-live="polite" className="rounded-2xl border border-blue-200 bg-white p-5 space-y-4">
    <div className="flex justify-between gap-4"><h2 className="text-xl font-semibold">Kết quả tra cứu VNPay</h2><button onClick={onClose} className="text-slate-600">Đóng</button></div>
    <p className="break-all font-mono text-sm">{order.orderId}</p>
    <p>Nội bộ: {transactionStatuses[order.status] ?? order.status} • {transactionMoney(order.amount)}</p>
    {busy && <p role="status">Đang lấy thông tin từ VNPay…</p>}
    {error && <div role="alert"><p className="text-red-700">{error}</p><button onClick={onRetry} className="mt-2 text-blue-700">Thử lại</button></div>}
    {result && <>
      {result.amountMismatch && <p role="alert" className="rounded-lg bg-amber-50 p-3 text-amber-900">Số tiền VNPay khác số tiền lưu trong hệ thống. Cần kiểm tra giao dịch.</p>}
      <dl className="grid gap-4 sm:grid-cols-2">
        {[
          ["Trạng thái VNPay", providerStatus(result.provider.transactionStatus)],
          ["Số tiền VNPay", transactionMoney(result.provider.amount)],
          ["Mã giao dịch VNPay", result.provider.transactionNo || "Chưa có thông tin"],
          ["Ngân hàng", result.provider.bankCode || "Chưa có thông tin"],
          ["Thời gian thanh toán", transactionDate(result.provider.paidAt)],
          ["Thời điểm tra cứu", transactionDate(result.queriedAt)],
        ].map(([label, value]) => <div key={label}><dt className="text-sm text-slate-500">{label}</dt><dd className="mt-1 break-all font-medium">{value}</dd></div>)}
      </dl>
      <p className="text-sm text-slate-500">Tra cứu chỉ hiển thị kết quả. Trạng thái đơn và quyền PLUS không thay đổi.</p>
    </>}
  </section>;
}
