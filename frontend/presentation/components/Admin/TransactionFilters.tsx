import { TransactionFilters as Filters } from "@/domain/entities/adminTransaction.entity";
import { transactionStatuses } from "@/presentation/utils/transactionDisplay";

export default function TransactionFilters({ value, onChange, onSearch, error }: {
  value: Filters; onChange: (value: Filters) => void; onSearch: () => void; error: string;
}) {
  const input = "mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 bg-white";
  return <form onSubmit={(event) => { event.preventDefault(); onSearch(); }} className="rounded-2xl border bg-white p-5 space-y-4">
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <label className="text-sm font-medium">Từ khóa
        <input className={input} value={value.keyword} maxLength={254} placeholder="Mã đơn, mã VNPay hoặc email"
          onChange={(e) => onChange({ ...value, keyword: e.target.value })} />
      </label>
      <label className="text-sm font-medium">Trạng thái nội bộ
        <select className={input} value={value.status} onChange={(e) => onChange({ ...value, status: e.target.value })}>
          <option value="">Tất cả trạng thái</option>
          {Object.entries(transactionStatuses).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
        </select>
      </label>
      <label className="text-sm font-medium">Ngày tạo từ
        <input type="date" className={input} value={value.from} onChange={(e) => onChange({ ...value, from: e.target.value })} />
      </label>
      <label className="text-sm font-medium">Ngày tạo đến
        <input type="date" className={input} value={value.to} onChange={(e) => onChange({ ...value, to: e.target.value })} />
      </label>
    </div>
    <p className="text-sm text-slate-500">Tìm mã chính xác hoặc một phần email. Ngày và giờ theo múi giờ Việt Nam.</p>
    {error && <p role="alert" className="text-red-700">{error}</p>}
    <button className="rounded-lg bg-blue-600 px-5 py-2 text-white" type="submit">Tìm giao dịch</button>
  </form>;
}
