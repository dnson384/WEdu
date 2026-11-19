import { RefreshCw } from "lucide-react";

interface Props {
  title: string;
  description: string;
  onRefresh: () => void;
  refreshing?: boolean;
  disabled?: boolean;
}

export default function AdminPageHeader({
  title,
  description,
  onRefresh,
  refreshing = false,
  disabled = false,
}: Props) {
  return (
    <header className="flex flex-wrap items-center justify-between gap-4">
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-blue-600">
          Quản trị hệ thống
        </p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
          {title}
        </h1>
        <p className="mt-2 text-sm text-slate-500">{description}</p>
      </div>
      <button
        type="button"
        onClick={onRefresh}
        disabled={refreshing || disabled}
        className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <RefreshCw
          size={16}
          className={refreshing ? "animate-spin" : undefined}
        />
        Làm mới
      </button>
    </header>
  );
}
