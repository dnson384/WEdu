import { Refund, RefundAction, refundLabels } from "@/domain/entities/refund.entity";

export const refundDate = (value: string | null) => value ? new Date(value).toLocaleString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" }) : "Chưa xác định";
export const refundMoney = (value: number) => value.toLocaleString("vi-VN") + "đ";

interface Props {
  value: Refund; admin: boolean; busy: boolean; enabled: boolean; reason: string; legacyException: boolean;
  onReason: (value: string) => void; onLegacy: (value: boolean) => void; onAction: (action: RefundAction) => void; onClose: () => void;
}
export default function RefundDetail(p: Props) {
  const r = p.value;
  const review = ["PENDING_REVIEW", "NEEDS_VERIFICATION"].includes(r.status);
  const mayApprove = r.entitlementVerifiable && !r.activeAi && r.eligibility !== "INELIGIBLE"
    && (r.eligibility === "ELIGIBLE" || (p.legacyException && p.reason.trim().length >= 3));
  return <section aria-label="Chi tiết yêu cầu hoàn tiền" className="space-y-4 rounded-2xl border border-blue-200 bg-blue-50 p-5">
    <div className="flex justify-between gap-4"><h2 className="text-xl font-semibold">Chi tiết yêu cầu</h2><button disabled={p.busy} onClick={p.onClose} className="underline">Đóng</button></div>
    <dl className="grid gap-3 text-sm sm:grid-cols-2">
      <div><dt>Mã yêu cầu</dt><dd className="break-all font-mono">{r.id}</dd></div>
      <div><dt>Mã đơn</dt><dd className="break-all font-mono">{r.orderId}</dd></div>
      <div><dt>Trạng thái</dt><dd className="font-semibold">{refundLabels[r.status]}</dd></div>
      <div><dt>Số tiền hoàn toàn bộ</dt><dd className="font-semibold">{refundMoney(r.amount)}</dd></div>
      <div><dt>Ngày gửi (giờ Việt Nam)</dt><dd>{refundDate(r.createdAt)}</dd></div>
      <div><dt>Ngày cấp PLUS</dt><dd>{refundDate(r.paidAt)}</dd></div>
      {p.admin && <><div><dt>Email mua hàng</dt><dd>{r.email}</dd></div><div><dt>Lần nhận kết quả AI gần nhất</dt><dd>{refundDate(r.lastAiUsedAt)}</dd></div></>}
    </dl>
    <p className="whitespace-pre-wrap break-words"><strong>Lý do yêu cầu: </strong>{r.reason}</p>
    <div className="rounded-xl bg-white p-4"><p><strong>Đánh giá: </strong>{({ ELIGIBLE: "Đủ điều kiện", INELIGIBLE: "Không đủ điều kiện", UNKNOWN: "Cần xác minh" } as Record<string, string>)[r.eligibility]}</p>
      <ul className="mt-3 space-y-2 text-sm">{r.checks?.map(check => <li key={check.key}><strong>{({ PASSED: "Đạt", FAILED: "Không đạt", UNKNOWN: "Cần xác minh" } as Record<string, string>)[check.result]}: </strong>{check.reason}</li>)}</ul>
      <p className="mt-2 whitespace-pre-wrap break-words">{r.decisionReason}</p>
      {r.activeAi && <p className="mt-2 text-amber-800">Có tác vụ AI đang chạy hoặc chưa rõ kết quả. Chưa thể duyệt hoàn tiền.</p>}
      {review && <p className="mt-2">Hạn PLUS dự kiến sau hoàn: {refundDate(r.projectedPlusExpiresAt)}.</p>}
      {r.status === "PROCESSING" && <p className="mt-2">Đang chờ bằng chứng hoàn tiền từ VNPay. Không gửi lại lệnh hoàn tiền.</p>}
      {r.refundTransactionNo && <p className="mt-2">Mã hoàn tiền VNPay: {r.refundTransactionNo}</p>}
      {p.admin && <p className="mt-2 text-sm">Phản hồi VNPay: {r.providerCode ?? "—"} · Trạng thái: {r.providerStatus ?? "—"}</p>}
    </div>
    {!p.enabled && <p>Tính năng xử lý hoàn tiền hiện chưa được bật.</p>}
    {p.admin && review && <div className="space-y-3">
      <label className="block">Lý do từ chối hoặc giải trình ngoại lệ
        <textarea maxLength={1000} value={p.reason} disabled={p.busy} onChange={e => p.onReason(e.target.value)} className="mt-1 w-full rounded-lg border bg-white p-3" />
      </label>
      {r.eligibility === "UNKNOWN" && <label className="flex gap-2"><input type="checkbox" checked={p.legacyException} disabled={p.busy} onChange={e => p.onLegacy(e.target.checked)} />Xét ngoại lệ bằng chứng AI cho đơn cũ; bắt buộc nhập giải trình.</label>}
      <p className="text-sm">Xác nhận sẽ gửi hoàn toàn bộ {refundMoney(r.amount)} qua VNPay và thu hồi phần thời hạn PLUS còn lại của đơn khi hoàn thành.</p>
      <div className="flex flex-wrap gap-3"><button disabled={p.busy || !p.enabled || !mayApprove} onClick={() => p.onAction("approve")} className="rounded-lg bg-blue-700 px-4 py-2 text-white disabled:opacity-40">Xác nhận gửi hoàn tiền</button>
        <button disabled={p.busy || !p.enabled || p.reason.trim().length < 3} onClick={() => p.onAction("reject")} className="rounded-lg border border-red-300 bg-white px-4 py-2 text-red-700 disabled:opacity-40">Từ chối yêu cầu</button></div>
    </div>}
    {!p.admin && review && <button disabled={p.busy || !p.enabled} onClick={() => p.onAction("cancel")} className="rounded-lg border bg-white px-4 py-2 disabled:opacity-40">Hủy yêu cầu và mở lại AI</button>}
    {p.admin && r.status === "PROCESSING" && <button disabled={p.busy || !p.enabled} onClick={() => p.onAction("reconcile")} className="rounded-lg bg-blue-700 px-4 py-2 text-white disabled:opacity-40">Đối soát kết quả VNPay</button>}
    <details><summary className="cursor-pointer font-medium">Lịch sử xử lý · Điều khoản {r.policyVersion}</summary>
      <ol className="mt-3 space-y-3">{r.history.map((event, i) => <li key={i} className="rounded-lg bg-white p-3 text-sm">
        <p>{refundDate(event.at)} · {refundLabels[event.status]}{p.admin && ` · ${event.actor}`}</p><p className="mt-1 whitespace-pre-wrap break-words">{event.reason}</p>
      </li>)}</ol>
    </details>
  </section>;
}
