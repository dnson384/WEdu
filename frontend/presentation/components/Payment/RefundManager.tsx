"use client";

import Link from "next/link";
import useRefunds from "@/presentation/hooks/Payment/useRefunds";
import { refundLabels } from "@/domain/entities/refund.entity";
import RefundDetail, { refundDate, refundMoney } from "@/presentation/components/Payment/RefundDetail";

export default function RefundManager({ admin }: { admin: boolean }) {
  const s = useRefunds(admin);
  if (s.isLoadingUser) return <p role="status">Đang tải tài khoản…</p>;
  if (!s.allowed) return <p role="alert">Tài khoản không có quyền truy cập chức năng này.</p>;
  return <div className="space-y-6">
    <Link href={admin ? "/admin/transactions" : "/me/bill"} className="text-blue-700 underline">← {admin ? "Quản lý giao dịch" : "Thanh toán PLUS"}</Link>
    <h1 className="text-3xl font-bold">{admin ? "Xử lý yêu cầu hoàn tiền" : "Giao dịch và hoàn tiền"}</h1>
    {s.list.data && !s.list.data.enabled && <p role="status" className="rounded-xl border border-amber-300 bg-amber-50 p-4">Tính năng gửi và xử lý hoàn tiền hiện chưa được bật. Bạn vẫn có thể xem lịch sử.</p>}
    {!admin && <section className="space-y-4 rounded-2xl border bg-white p-5">
      <h2 className="text-xl font-semibold">Gửi yêu cầu hoàn tiền</h2>
      <p>Hoàn toàn bộ trong 48 giờ kể từ khi cấp PLUS nếu chưa nhận kết quả sinh đề AI sau lần mua đó. Đơn cũ thiếu dữ liệu sẽ được ADMIN xác minh.</p>
      <p className="text-sm text-amber-800">Khi tiếp nhận yêu cầu chờ xử lý, chức năng sinh AI sẽ tạm khóa. Hạn PLUS vẫn tiếp tục chạy và không được bù thời gian chờ. Có thể hủy trước khi ADMIN bắt đầu hoàn tiền.</p>
      {s.orders.isFetching && <p role="status">Đang tải giao dịch…</p>}
      {s.ordersError && <p role="alert">{s.ordersError} <button onClick={() => void s.orders.refetch()} className="underline">Thử lại</button></p>}
      {s.orders.data && <>
        <label className="block">Chọn giao dịch của bạn<select value={s.orderId} onChange={e => s.setOrderId(e.target.value)} disabled={s.busy} className="mt-2 w-full rounded-lg border p-3">
          <option value="">Chọn đơn đã thanh toán</option>{s.orders.data.items.map(o => <option key={o.id} value={o.id} disabled={o.status !== "SUCCEEDED"}>{refundDate(o.createdAt)} · {refundMoney(o.amount)} · {o.status} · {o.id.slice(-12)}</option>)}
        </select></label>
        {!s.orders.data.items.length && <p>Chưa có giao dịch.</p>}
        <div className="flex items-center gap-3 text-sm"><button disabled={s.orderPage === 0 || s.busy} onClick={() => s.setOrderPage(s.orderPage - 1)} className="underline disabled:opacity-40">Trước</button><span>Trang giao dịch {s.orderPage + 1}</span><button disabled={!s.orders.data.hasNext || s.busy} onClick={() => s.setOrderPage(s.orderPage + 1)} className="underline disabled:opacity-40">Sau</button></div>
      </>}
      <label className="block">Lý do hoàn tiền<textarea value={s.createReason} maxLength={1000} disabled={s.busy} onChange={e => s.setCreateReason(e.target.value)} className="mt-2 w-full rounded-lg border p-3" /></label>
      <label className="flex gap-2"><input type="checkbox" checked={s.accepted} disabled={s.busy} onChange={e => s.setAccepted(e.target.checked)} />Tôi đã đọc và đồng ý điều khoản hoàn tiền, việc tạm khóa AI và thời hạn PLUS tiếp tục chạy.</label>
      <button onClick={() => void s.create()} disabled={s.busy || !s.list.data?.enabled || !s.accepted || !s.orderId || s.createReason.trim().length < 3} className="rounded-lg bg-blue-700 px-4 py-2 text-white disabled:opacity-40">Gửi yêu cầu hoàn tiền</button>
    </section>}
    {admin && <form onSubmit={e => { e.preventDefault(); s.search(); }} className="flex flex-wrap gap-3 rounded-xl border bg-white p-4">
      <label className="min-w-48 flex-1">Mã yêu cầu, mã đơn hoặc email<input value={s.draft.keyword} maxLength={254} onChange={e => s.setDraft({ ...s.draft, keyword: e.target.value })} className="mt-1 w-full rounded-lg border p-2" /></label>
      <label>Trạng thái<select value={s.draft.status} onChange={e => s.setDraft({ ...s.draft, status: e.target.value })} className="mt-1 block rounded-lg border p-2"><option value="">Tất cả</option>{Object.entries(refundLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
      <label>Từ ngày<input type="date" value={s.draft.from} onChange={e => s.setDraft({ ...s.draft, from: e.target.value })} className="mt-1 block rounded-lg border p-2" /></label>
      <label>Đến ngày<input type="date" value={s.draft.to} onChange={e => s.setDraft({ ...s.draft, to: e.target.value })} className="mt-1 block rounded-lg border p-2" /></label>
      <button className="self-end rounded-lg bg-blue-700 px-4 py-2 text-white">Tìm kiếm</button>
    </form>}
    {s.busy && <p role="status">Đang xử lý yêu cầu…</p>}
    {s.error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-700">{s.error}</p>}
    {s.selectedId && <>
      {s.detail.isPending && <p role="status">Đang tải chi tiết…</p>}
      {s.detailError && <p role="alert">{s.detailError} <button onClick={() => void s.detail.refetch()} className="underline">Tải lại</button></p>}
      {s.detail.data && <RefundDetail key={s.detail.data.id} value={s.detail.data} admin={admin} busy={s.busy} enabled={s.list.data?.enabled ?? false}
        reason={s.decisionReason} legacyException={s.legacyException} onReason={s.setDecisionReason} onLegacy={s.setLegacyException}
        onAction={action => void s.act(action)} onClose={() => s.select(null)} />}
    </>}
    <section className="space-y-3"><div className="flex justify-between gap-3"><h2 className="text-xl font-semibold">Danh sách yêu cầu</h2><button onClick={() => void s.refresh()} className="text-blue-700 underline">Làm mới</button></div>
      {s.list.isFetching && <p role="status">Đang tải danh sách…</p>}
      {s.listError && <p role="alert" className="text-red-700">{s.listError}</p>}
      {s.list.data && <><div className="overflow-x-auto rounded-xl border bg-white"><table className="w-full text-left text-sm"><thead className="bg-slate-50"><tr><th className="p-3">Ngày gửi</th><th className="p-3">{admin ? "Email / mã đơn" : "Mã đơn"}</th><th className="p-3">Số tiền</th><th className="p-3">Trạng thái</th><th className="p-3">Thao tác</th></tr></thead><tbody>
        {s.list.data.items.map(r => <tr key={r.id} className="border-t"><td className="p-3">{refundDate(r.createdAt)}</td><td className="max-w-64 break-all p-3">{admin && <p>{r.email}</p>}<p className="font-mono text-xs">{r.orderId}</p></td><td className="whitespace-nowrap p-3">{refundMoney(r.amount)}</td><td className="p-3">{refundLabels[r.status]}</td><td className="p-3"><button disabled={s.busy} onClick={() => s.select(r.id)} className="text-blue-700 underline">Xem chi tiết</button></td></tr>)}
        {!s.list.data.items.length && <tr><td colSpan={5} className="p-6 text-center text-slate-600">Không có yêu cầu hoàn tiền.</td></tr>}
      </tbody></table></div><div className="flex items-center justify-between gap-3 text-sm"><span>{s.list.data.totalElements} yêu cầu · Trang {s.filters.page + 1}/{Math.max(1, s.list.data.totalPages)}</span><div className="flex gap-3"><button disabled={s.filters.page === 0 || s.busy} onClick={() => s.page(s.filters.page - 1)} className="underline disabled:opacity-40">Trước</button><button disabled={s.filters.page + 1 >= s.list.data.totalPages || s.busy} onClick={() => s.page(s.filters.page + 1)} className="underline disabled:opacity-40">Sau</button></div></div></>}
    </section>
  </div>;
}
