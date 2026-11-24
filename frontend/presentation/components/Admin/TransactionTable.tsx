import { ReceiptText } from "lucide-react";
import { TransactionPage } from "@/domain/entities/adminTransaction.entity";
import AdminPagination from "@/presentation/components/Admin/AdminPagination";
import {
  transactionDate,
  transactionMoney,
  transactionStatuses,
  transactionStatusClass,
} from "@/presentation/utils/transactionDisplay";

interface Props {
  data: TransactionPage;
  onPage: (page: number) => void;
  busy?: boolean;
}

export default function TransactionTable({
  data,
  onPage,
  busy = false,
}: Props) {
  return (
    <section
      aria-busy={busy}
      className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
    >
      <div className="flex flex-wrap items-center justify-between gap-2 px-5 py-4 text-sm">
        <p className="flex items-center gap-2 font-medium text-slate-900">
          <ReceiptText size={18} />
          {data.totalElements.toLocaleString("vi-VN")} giao dịch
        </p>
        <p role="status" className="text-xs text-slate-500">
          {busy
            ? "Đang cập nhật danh sách…"
            : "Trạng thái đã lưu trong hệ thống"}
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-200 text-left text-sm">
          <caption className="sr-only">Danh sách giao dịch thanh toán</caption>
          <thead className="bg-slate-50 text-xs text-slate-500">
            <tr>
              {[
                "Mã đơn / Email",
                "Số tiền",
                "Ngày tạo",
                "Trạng thái",
                "Mã VNPay",
              ].map((title) => (
                <th key={title} scope="col" className="px-5 py-3 font-medium">
                  {title}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.items.map((order) => (
              <tr
                key={order.orderId}
                className="border-t border-slate-100 transition-colors hover:bg-slate-50/70"
              >
                <td className="px-5 py-4">
                  <p className="max-w-64 break-all font-mono text-xs text-slate-700">
                    {order.orderId}
                  </p>
                  <p className="mt-1 break-all text-xs text-slate-500">
                    {order.email || "Chưa có thông tin"}
                  </p>
                </td>
                <td className="whitespace-nowrap px-5 py-4 font-semibold tabular-nums text-slate-900">
                  {transactionMoney(order.amount)}
                </td>
                <td className="whitespace-nowrap px-5 py-4 text-slate-600">
                  {transactionDate(order.createdAt)}
                </td>
                <td className="whitespace-nowrap px-5 py-4">
                  <span
                    className={
                      "inline-flex rounded-full px-2.5 py-1 text-xs font-medium " +
                      transactionStatusClass(order.status)
                    }
                  >
                    {transactionStatuses[order.status] ?? order.status}
                  </span>
                </td>
                <td className="px-5 py-4 font-mono text-xs text-slate-600">
                  {order.transactionNo || "Chưa có thông tin"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!data.items.length && (
        <div
          role="status"
          className="border-t border-slate-100 px-5 py-12 text-center"
        >
          <ReceiptText size={28} className="mx-auto text-slate-300" />
          <p className="mt-3 text-sm font-medium text-slate-600">
            Không tìm thấy giao dịch phù hợp
          </p>
          <p className="mt-1 text-xs text-slate-500">
            Thử thay đổi từ khóa, trạng thái hoặc khoảng ngày.
          </p>
        </div>
      )}
      <AdminPagination
        page={data.page + 1}
        totalPages={data.totalPages}
        pageSize={data.pageSize}
        itemLabel="giao dịch"
        onPageChange={(page) => onPage(page - 1)}
        disabled={busy}
      />
    </section>
  );
}
