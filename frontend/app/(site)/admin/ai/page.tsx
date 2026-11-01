"use client";

import { Activity, Coins, Hash, RefreshCw } from "lucide-react";
import useAiManagement from "@/presentation/hooks/Admin/useAiManagement";
import AiMetricCard from "@/presentation/components/Admin/AiMetricCard";
import AiUsageChart from "@/presentation/components/Admin/AiUsageChart";
import AiStatusPanel from "@/presentation/components/Admin/AiStatusPanel";
import AiBudgetForm from "@/presentation/components/Admin/AiBudgetForm";
import { aiNumber, aiUsd } from "@/presentation/utils/aiManagement";

export default function AiManagementPage() {
  const ai = useAiManagement();
  const report = ai.report.data;
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-blue-600">
            Quản trị hệ thống
          </p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight">Quản lý AI</h1>
          <p className="mt-2 text-sm text-slate-500">
            Theo dõi mức sử dụng, chi phí và hạn mức DeepSeek.
          </p>
        </div>
        <button
          type="button"
          onClick={ai.refresh}
          disabled={ai.report.isFetching || ai.status.isFetching}
          className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-medium disabled:opacity-50"
        >
          <RefreshCw size={16} />
          Làm mới
        </button>
      </header>

      {ai.statusError && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 p-4 text-sm text-red-700"
        >
          {ai.statusError}
          {ai.notice ? " — Cấu hình đã lưu, nhưng trạng thái hạn mức chưa cập nhật. Chọn Làm mới để thử lại." : ai.status.data ? " — Số liệu bên dưới chưa cập nhật." : ""}
        </p>
      )}
      {ai.status.isPending && (
        <p role="status" className="text-sm text-slate-500">
          Đang tải hạn mức và số dư…
        </p>
      )}
      {ai.status.data && <AiStatusPanel status={ai.status.data} />}

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="font-semibold">Báo cáo sử dụng</h2>
        <form
          onSubmit={ai.applyFilters}
          className="mt-4 flex flex-wrap items-end gap-4"
        >
          <label className="text-sm text-slate-600">
            Từ ngày
            <input
              required
              type="date"
              value={ai.filterDraft.from}
              onChange={(event) =>
                ai.setFilterDraft({
                  ...ai.filterDraft,
                  from: event.target.value,
                })
              }
              className="mt-1.5 block rounded-xl border border-slate-300 px-3 py-2.5 text-slate-900"
            />
          </label>
          <label className="text-sm text-slate-600">
            Đến ngày
            <input
              required
              type="date"
              value={ai.filterDraft.to}
              onChange={(event) =>
                ai.setFilterDraft({ ...ai.filterDraft, to: event.target.value })
              }
              className="mt-1.5 block rounded-xl border border-slate-300 px-3 py-2.5 text-slate-900"
            />
          </label>
          <label className="min-w-44 text-sm text-slate-600">
            Model
            <select
              value={ai.filterDraft.model ?? ""}
              onChange={(event) =>
                ai.setFilterDraft({
                  ...ai.filterDraft,
                  model: event.target.value,
                })
              }
              className="mt-1.5 block w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-slate-900"
            >
              <option value="">Tất cả model</option>
              {report?.models.map((model) => (
                <option key={model} value={model}>
                  {model}
                </option>
              ))}
            </select>
          </label>
          <button
            type="submit"
            className="rounded-xl bg-slate-900 px-5 py-2.5 text-sm font-semibold text-white"
          >
            Áp dụng bộ lọc
          </button>
        </form>
        <p className="mt-3 text-xs text-slate-500">
          Tối đa 366 ngày · Giờ Việt Nam · Bộ lọc chỉ áp dụng cho báo cáo, không
          thay đổi hạn mức tháng.
        </p>
      </section>

      {ai.validation && (
        <p
          role="alert"
          className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900"
        >
          {ai.validation}
        </p>
      )}
      {ai.reportError && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 p-4 text-sm text-red-700"
        >
          {ai.reportError}
          {report ? " — Đang hiển thị lần tải thành công gần nhất." : ""}
        </p>
      )}
      {ai.report.isPending && (
        <p role="status" className="text-sm text-slate-500">
          Đang tải báo cáo…
        </p>
      )}
      {report && (
        <>
          <p className="text-sm text-slate-500">
            Kết quả: {report.from} → {report.to} ·{" "}
            {report.model || "Tất cả model"}
          </p>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <AiMetricCard
              label="Lần gọi AI"
              value={aiNumber(report.totals.requests)}
              detail="Bao gồm lần gọi đang chạy và thất bại"
              icon={<Activity size={18} />}
            />
            <AiMetricCard
              label="Token đầu vào"
              value={aiNumber(report.totals.inputTokens)}
              detail="Gồm token cache hit và cache miss"
              icon={<Hash size={18} />}
            />
            <AiMetricCard
              label="Token đầu ra"
              value={aiNumber(report.totals.outputTokens)}
              detail={`Tổng cộng ${aiNumber(report.totals.totalTokens)} token`}
              icon={<Hash size={18} />}
            />
            <AiMetricCard
              label="Chi phí ước tính"
              value={aiUsd(report.totals.estimatedCostUsd)}
              detail="USD · theo giá cao điểm của model"
              icon={<Coins size={18} />}
            />
          </div>
          {report.totals.requests === 0 && (
            <p
              role="status"
              className="rounded-xl bg-blue-50 p-4 text-sm text-blue-800"
            >
              Chưa có usage trong khoảng đã chọn. Hệ thống chỉ ghi nhận các lần
              gọi từ khi triển khai tính năng.
            </p>
          )}
          {report.totals.unknownUsageRequests > 0 && (
            <p
              role="status"
              className="rounded-xl bg-amber-50 p-4 text-sm text-amber-900"
            >
              Có {aiNumber(report.totals.unknownUsageRequests)} request chưa xác
              định token/chi phí. Số liệu tổng chưa bao gồm usage chưa xác định.
            </p>
          )}
          <div className="grid gap-5 xl:grid-cols-3">
            <AiUsageChart
              title="Lưu lượng request"
              metric="requests"
              days={report.days}
              color="#2563eb"
            />
            <AiUsageChart
              title="Tổng token"
              metric="totalTokens"
              days={report.days}
              color="#7c3aed"
            />
            <AiUsageChart
              title="Chi phí ước tính (USD)"
              metric="estimatedCostUsd"
              days={report.days}
              color="#059669"
            />
          </div>
        </>
      )}

      {ai.notice && (
        <p
          role="status"
          className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-800"
        >
          {ai.notice}
        </p>
      )}
      <AiBudgetForm
        budget={ai.budget.data}
        editing={ai.editingBudget}
        conflict={ai.budgetConflict}
        onOpen={ai.openBudget}
        onCancel={ai.cancelBudget}
        value={ai.form}
        onChange={ai.changeBudget}
        onSubmit={ai.submitBudget}
        onReload={() => {
          void ai.reloadBudget();
        }}
        disabled={!ai.budget.data || ai.budget.isFetching}
        saving={ai.saving}
        error={ai.saveError || ai.budgetError}
      />
      <p className="text-xs leading-relaxed text-slate-500">
        Chi phí là ước tính từ token thực tế và bảng giá cao điểm được lưu tại
        thời điểm gọi, có thể cao hơn hóa đơn DeepSeek. Báo cáo không bao gồm sử
        dụng ngoài ứng dụng hoặc lịch sử trước khi triển khai. Không quy đổi số
        dư CNY sang USD. Trang tự cập nhật báo cáo và hạn mức mỗi 60 giây khi
        đang mở.
      </p>
    </div>
  );
}
