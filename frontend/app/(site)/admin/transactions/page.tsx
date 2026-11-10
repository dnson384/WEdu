import Link from "next/link";

export default function TransactionManagementPage() {
  return <div className="space-y-6">
    <h1 className="text-3xl font-bold">Quản lý giao dịch</h1>
    <Link href="/admin/transactions/search" className="block max-w-xl rounded-2xl border bg-white p-6 shadow-sm hover:border-blue-400">
      <h2 className="text-xl font-semibold text-blue-700">Tra cứu giao dịch →</h2>
      <p className="mt-3 text-slate-600">Tìm giao dịch theo mã, email, ngày tạo hoặc trạng thái và tra cứu kết quả từ VNPay.</p>
    </Link>
    <Link href="/admin/transactions/refunds" className="block max-w-xl rounded-2xl border bg-white p-6 shadow-sm hover:border-blue-400">
      <h2 className="text-xl font-semibold text-blue-700">Xử lý yêu cầu hoàn tiền →</h2>
      <p className="mt-3 text-slate-600">Kiểm tra điều kiện, duyệt hoặc từ chối yêu cầu và theo dõi kết quả hoàn tiền VNPay.</p>
    </Link>
  </div>;
}
