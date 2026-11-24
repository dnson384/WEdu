import { Search } from "lucide-react";
import { TransactionFilters as Filters } from "@/domain/entities/adminTransaction.entity";
import { transactionStatuses } from "@/presentation/utils/transactionDisplay";

interface Props {
  value: Filters;
  onChange: (value: Filters) => void;
  onSearch: () => void;
  error: string;
}

export default function TransactionFilters({
  value,
  onChange,
  onSearch,
  error,
}: Props) {
  const inputClass =
    "mt-1.5 block w-full min-w-0 rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition-colors focus:border-blue-500 focus:ring-2 focus:ring-blue-100";
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSearch();
      }}
      className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
    >
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <label className="min-w-0 text-sm text-slate-600">
          Từ khóa
          <input
            type="search"
            className={inputClass}
            value={value.keyword}
            maxLength={254}
            placeholder="Mã đơn, mã VNPay hoặc email"
            onChange={(event) =>
              onChange({ ...value, keyword: event.target.value })
            }
          />
        </label>
        <label className="min-w-0 text-sm text-slate-600">
          Trạng thái
          <select
            className={inputClass}
            value={value.status}
            onChange={(event) =>
              onChange({ ...value, status: event.target.value })
            }
          >
            <option value="">Tất cả trạng thái</option>
            {Object.entries(transactionStatuses).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="min-w-0 text-sm text-slate-600">
          Từ ngày
          <input
            type="date"
            className={inputClass}
            value={value.from}
            onChange={(event) =>
              onChange({ ...value, from: event.target.value })
            }
          />
        </label>
        <label className="min-w-0 text-sm text-slate-600">
          Đến ngày
          <input
            type="date"
            className={inputClass}
            value={value.to}
            onChange={(event) => onChange({ ...value, to: event.target.value })}
          />
        </label>
      </div>
      {error && (
        <p
          role="alert"
          className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-900"
        >
          {error}
        </p>
      )}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-xs leading-relaxed text-slate-500">
          Tìm mã chính xác hoặc một phần email · Giờ Việt Nam
        </p>
        <button
          type="submit"
          className="flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
        >
          <Search size={16} />
          Tìm kiếm
        </button>
      </div>
    </form>
  );
}
