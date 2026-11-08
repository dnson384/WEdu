import { AdminTransaction, TransactionPage } from "@/domain/entities/adminTransaction.entity";
import { transactionDate, transactionMoney, transactionStatuses } from "@/presentation/utils/transactionDisplay";

export default function TransactionTable({ data, onPage, onLookup, busy }: {
  data: TransactionPage; onPage: (page: number) => void; onLookup: (order: AdminTransaction) => void; busy: boolean;
}) {
  return <section className="rounded-2xl border bg-white p-5">
    <p className="mb-4 text-sm text-slate-600">{data.totalElements} giao dịch • Trạng thái lưu trong hệ thống</p>
    {!data.items.length ? <p role="status">Không tìm thấy giao dịch phù hợp</p> : <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead><tr className="border-b text-slate-500">{["Mã đơn / Email", "Số tiền", "Ngày tạo", "Trạng thái nội bộ", "Mã VNPay", "Thao tác"].map((title) => <th key={title} className="p-3">{title}</th>)}</tr></thead>
        <tbody>{data.items.map((order) => <tr key={order.orderId} className="border-b">
          <td className="p-3"><p className="max-w-64 break-all font-mono text-xs">{order.orderId}</p><p className="mt-1 break-all">{order.email ?? "Chưa có thông tin"}</p></td>
          <td className="p-3 whitespace-nowrap">{transactionMoney(order.amount)}</td>
          <td className="p-3 whitespace-nowrap">{transactionDate(order.createdAt)}</td>
          <td className="p-3">{transactionStatuses[order.status] ?? order.status}</td>
          <td className="p-3">{order.transactionNo || "Chưa có thông tin"}</td>
          <td className="p-3"><button disabled={busy} onClick={() => onLookup(order)} className="text-blue-700 disabled:opacity-50 whitespace-nowrap">Tra cứu VNPay</button></td>
        </tr>)}</tbody>
      </table>
    </div>}
    <div className="mt-4 flex items-center justify-between gap-3">
      <button disabled={data.page === 0} onClick={() => onPage(data.page - 1)} className="rounded-lg border px-3 py-2 disabled:opacity-40">Trang trước</button>
      <span className="text-sm">Trang {data.page + 1} / {Math.max(1, data.totalPages)}</span>
      <button disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)} className="rounded-lg border px-3 py-2 disabled:opacity-40">Trang sau</button>
    </div>
  </section>;
}
