import { AiUsageDay } from "@/domain/entities/aiManagement.entity";
import { aiNumber, aiUsd } from "@/presentation/utils/aiManagement";

interface Props {
  title: string;
  metric: "requests" | "totalTokens" | "estimatedCostUsd";
  days: AiUsageDay[];
  color: string;
}

export default function AiUsageChart({ title, metric, days, color }: Props) {
  const values = days.map((day) => Number(day[metric]));
  const maximum = Math.max(0, ...values) || 1;
  const money = metric === "estimatedCostUsd";
  const format = (value: number) =>
    money ? aiUsd(String(value)) : aiNumber(value);
  const x = (index: number) =>
    48 + (days.length === 1 ? 230 : (index * 460) / (days.length - 1));
  const y = (value: number) => 165 - (value * 125) / maximum;
  const points = values
    .map((value, index) => `${x(index)},${y(value)}`)
    .join(" ");
  return (
    <section className="min-w-0 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="font-semibold text-slate-800">{title}</h3>
      <svg
        viewBox="0 0 540 205"
        role="img"
        aria-label={`${title} theo ngày, giờ Việt Nam`}
        className="mt-4 w-full"
      >
        {[0, 0.5, 1].map((fraction) => (
          <g key={fraction}>
            <line
              x1="48"
              x2="508"
              y1={y(maximum * fraction)}
              y2={y(maximum * fraction)}
              stroke="#e2e8f0"
              strokeDasharray="4 4"
            />
            <text
              x="44"
              y={y(maximum * fraction) - 6}
              fontSize="10"
              fill="#64748b"
            >
              {format(maximum * fraction)}
            </text>
          </g>
        ))}
        <polyline
          points={points}
          fill="none"
          stroke={color}
          strokeWidth="2.5"
        />
        {days.map((day, index) => (
          <circle
            key={day.day}
            cx={x(index)}
            cy={y(values[index])}
            r={days.length > 90 ? 1.5 : 3}
            fill={color}
          >
            <title>
              {day.day}: {format(values[index])}
            </title>
          </circle>
        ))}
        <text x="48" y="194" fontSize="11" fill="#64748b">
          {days[0]?.day}
        </text>
        <text x="508" y="194" textAnchor="end" fontSize="11" fill="#64748b">
          {days.at(-1)?.day}
        </text>
      </svg>
      <details className="text-xs text-slate-500">
        <summary className="cursor-pointer">Xem dữ liệu từng ngày</summary>
        <div className="mt-3 max-h-52 overflow-auto">
          <table className="w-full text-left">
            <caption className="sr-only">{title}</caption>
            <thead>
              <tr>
                <th className="py-1">Ngày</th>
                <th className="py-1 text-right">Giá trị</th>
              </tr>
            </thead>
            <tbody>
              {days.map((day) => (
                <tr key={day.day}>
                  <td className="py-1">{day.day}</td>
                  <td className="text-right">{format(Number(day[metric]))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </section>
  );
}
