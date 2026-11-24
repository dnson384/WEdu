interface Props {
  page: number;
  totalPages: number;
  pageSize: number;
  itemLabel: string;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export default function AdminPagination({
  page,
  totalPages,
  pageSize,
  itemLabel,
  onPageChange,
  disabled = false,
}: Props) {
  const pages = Math.max(1, totalPages);
  return (
    <nav
      aria-label="Phân trang danh sách"
      className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-5 py-4 text-sm"
    >
      <p className="text-slate-500">
        Trang {page}/{pages} · {pageSize} {itemLabel}/trang
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          disabled={disabled || page <= 1}
          onClick={() => onPageChange(page - 1)}
          className="rounded-lg border border-slate-200 px-3 py-1.5 text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Trước
        </button>
        <button
          type="button"
          disabled={disabled || page >= pages}
          onClick={() => onPageChange(page + 1)}
          className="rounded-lg border border-slate-200 px-3 py-1.5 text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Sau
        </button>
      </div>
    </nav>
  );
}
