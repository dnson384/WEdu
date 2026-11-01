import { FormEvent } from "react";
import { AiBudgetDraft } from "@/domain/entities/aiManagement.entity";

interface Props {
  value: AiBudgetDraft;
  onChange: (field: "amount" | "tokens", value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onReload: () => void;
  disabled: boolean;
  saving: boolean;
  error: string | null;
}

export default function AiBudgetForm({
  value,
  onChange,
  onSubmit,
  onReload,
  disabled,
  saving,
  error,
}: Props) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold">Cấu hình hạn mức tháng</h2>
      <p className="mt-1 text-sm text-slate-500">
        Có hiệu lực ngay, tự áp dụng lại mỗi tháng. Để trống một ô để tắt giới
        hạn đó.
      </p>
      <form onSubmit={onSubmit} className="mt-5">
        <fieldset
          disabled={disabled || saving}
          className="grid gap-4 md:grid-cols-2"
        >
          <label className="text-sm font-medium">
            Budget ước tính (USD)
            <input
              type="text"
              inputMode="decimal"
              value={value.amount}
              onChange={(event) => onChange("amount", event.target.value)}
              placeholder="Chưa giới hạn"
              className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-normal"
            />
          </label>
          <label className="text-sm font-medium">
            Quota tổng token
            <input
              type="text"
              inputMode="numeric"
              value={value.tokens}
              onChange={(event) => onChange("tokens", event.target.value)}
              placeholder="Chưa giới hạn"
              className="mt-2 w-full rounded-xl border border-slate-300 px-3 py-2.5 font-normal"
            />
          </label>
        </fieldset>
        {error && (
          <p role="alert" className="mt-3 text-sm text-red-700">
            {error}
          </p>
        )}
        <div className="mt-5 flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={disabled || saving}
            className="rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? "Đang lưu…" : "Lưu hạn mức"}
          </button>
          <button
            type="button"
            onClick={onReload}
            disabled={saving}
            className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm disabled:opacity-50"
          >
            Tải lại cấu hình
          </button>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Cảnh báo ở 80%, chặn ở 100%. Không hủy request đang chạy; có thể vượt
          hạn mức khi gọi đồng thời.
        </p>
      </form>
    </section>
  );
}
