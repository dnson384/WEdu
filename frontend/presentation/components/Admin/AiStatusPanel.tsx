import { AiStatus } from "@/domain/entities/aiManagement.entity";
import {
  aiNumber,
  aiTimestamp,
  aiUsd,
} from "@/presentation/utils/aiManagement";

function LimitProgress({
  label,
  used,
  remaining,
  percent,
  limit,
}: {
  label: string;
  used: string;
  remaining: string;
  percent: number | null;
  limit: string;
}) {
  return (
    <div>
      <div className="flex flex-wrap justify-between gap-2 text-sm">
        <h3 className="font-medium">{label}</h3>
        <span>
          {percent === null ? "Chưa đặt hạn mức" : `${aiNumber(percent)}%`}
        </span>
      </div>
      <div
        role="progressbar"
        aria-label={label}
        aria-valuenow={percent === null ? undefined : Math.min(100, percent)}
        aria-valuemin={0}
        aria-valuemax={100}
        className="mt-3 h-2 overflow-hidden rounded-full bg-slate-100"
      >
        <div
          className={`h-full rounded-full ${percent !== null && percent >= 100 ? "bg-red-500" : percent !== null && percent >= 80 ? "bg-amber-500" : "bg-blue-600"}`}
          style={{ width: `${Math.min(100, percent ?? 0)}%` }}
        />
      </div>
      <p className="mt-2 text-xs text-slate-600">
        Giới hạn {limit} · Đã dùng {used} · Còn lại {remaining}
      </p>
    </div>
  );
}

export default function AiStatusPanel({ status }: { status: AiStatus }) {
  const warning = status.level !== "NORMAL";
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">
            Hạn mức tháng {status.month}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Toàn hệ thống, tất cả model · Giờ Việt Nam
          </p>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-xs font-semibold ${status.level === "BLOCKED" ? "bg-red-50 text-red-700" : warning ? "bg-amber-50 text-amber-800" : "bg-emerald-50 text-emerald-700"}`}
        >
          {status.level === "BLOCKED"
            ? "Đang chặn AI mới"
            : warning
              ? "Sắp hết hạn mức"
              : "Đang hoạt động"}
        </span>
      </div>
      {warning && (
        <p
          role="alert"
          className={`mt-4 rounded-xl p-3 text-sm ${status.level === "BLOCKED" ? "bg-red-50 text-red-800" : "bg-amber-50 text-amber-900"}`}
        >
          {status.level === "BLOCKED"
            ? "Đã đạt hạn mức hoặc tài khoản DeepSeek không đủ số dư. Các lần gọi AI mới bị chặn; request đang chạy vẫn hoàn tất."
            : "Đã sử dụng từ 80% budget hoặc quota token. Hệ thống chặn lần gọi AI mới khi đạt 100%."}
        </p>
      )}
      <div className="mt-6 grid gap-6 md:grid-cols-2">
        <LimitProgress
          label="Budget USD · chi phí ước tính"
          limit={status.budget.monthlyBudgetUsd === null ? "Chưa đặt" : aiUsd(status.budget.monthlyBudgetUsd)}
          used={aiUsd(status.usage.estimatedCostUsd)}
          remaining={
            status.remainingBudgetUsd === null
              ? "Chưa đặt"
              : aiUsd(status.remainingBudgetUsd)
          }
          percent={status.budgetUsedPercent}
        />
        <LimitProgress
          label="Quota tổng token"
          limit={status.budget.monthlyTokenQuota === null ? "Chưa đặt" : `${aiNumber(status.budget.monthlyTokenQuota)} token`}
          used={aiNumber(status.usage.totalTokens)}
          remaining={
            status.remainingTokens === null
              ? "Chưa đặt"
              : aiNumber(status.remainingTokens)
          }
          percent={status.tokensUsedPercent}
        />
      </div>
      {status.usage.unknownUsageRequests > 0 && (
        <p role="status" className="mt-4 text-sm text-amber-800">
          Có {aiNumber(status.usage.unknownUsageRequests)} request chưa xác định
          usage trong tháng, gồm request đang chạy hoặc lỗi. Tổng token và chi
          phí có thể chưa đầy đủ.
        </p>
      )}
      <div className="mt-6 border-t border-slate-100 pt-5">
        <h3 className="font-medium">Số dư tài khoản DeepSeek</h3>
        <div className="mt-2 flex flex-wrap gap-4">
          {status.balance.balances.map((balance) => (
            <p key={balance.currency} className="text-xl font-semibold">
              {balance.totalBalance}{" "}
              <span className="text-sm font-normal text-slate-500">
                {balance.currency}
              </span>
            </p>
          ))}
        </div>
        <p className="mt-2 text-xs text-slate-500">
          Cập nhật: {aiTimestamp(status.balance.updatedAt)} · Số dư của tài
          khoản platform, không phân bổ theo model.
        </p>
        {status.balance.stale && (
          <p role="status" className="mt-2 text-sm text-amber-800">
            Chưa cập nhật được số dư.{" "}
            {status.balance.updatedAt
              ? "Đang hiển thị bản gần nhất."
              : "Chưa có dữ liệu số dư."}{" "}
            Hạn mức nội bộ vẫn được kiểm tra.
          </p>
        )}
      </div>
    </section>
  );
}
