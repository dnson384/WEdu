import { ReactNode } from "react";

export default function AiMetricCard({
  label,
  value,
  detail,
  icon,
}: {
  label: string;
  value: string;
  detail: string;
  icon?: ReactNode;
}) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between gap-3 text-sm text-slate-500">
        <h3>{label}</h3>
        {icon}
      </div>
      <p className="mt-3 wrap-break-word text-2xl font-semibold tracking-tight text-slate-900">
        {value}
      </p>
      <p className="mt-2 text-xs text-slate-500">{detail}</p>
    </article>
  );
}
