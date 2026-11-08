"use client";

import Link from "next/link";
import useAdminTransactions from "@/presentation/hooks/Admin/useAdminTransactions";
import TransactionFilters from "@/presentation/components/Admin/TransactionFilters";
import TransactionTable from "@/presentation/components/Admin/TransactionTable";
import TransactionDetail from "@/presentation/components/Admin/TransactionDetail";

export default function TransactionSearchPage() {
  const state = useAdminTransactions();
  return <div className="space-y-6">
    <Link href="/admin/transactions" className="text-sm text-blue-700">← Quản lý giao dịch</Link>
    <h1 className="text-3xl font-bold">Tra cứu giao dịch</h1>
    <p className="text-slate-600">Tìm giao dịch trong hệ thống, sau đó chọn “Tra cứu VNPay” để kiểm tra kết quả mới nhất.</p>
    <TransactionFilters value={state.draft} onChange={state.setDraft} onSearch={state.search} error={state.validation} />
    {state.selected && <TransactionDetail selected={state.selected} result={state.result} busy={state.lookingUp}
      error={state.lookupError} onClose={state.close} onRetry={() => { if (state.selected) void state.lookup(state.selected); }} />}
    {state.list.isFetching && <p role="status">Đang tải giao dịch…</p>}
    {state.listError && <div role="alert" className="text-red-700"><p>{state.listError}</p><button onClick={() => void state.list.refetch()} className="mt-2 text-blue-700">Tải lại danh sách</button></div>}
    {!state.list.isFetching && !state.listError && state.list.data && <TransactionTable data={state.list.data} onPage={state.page} onLookup={(order) => void state.lookup(order)} busy={state.lookingUp} />}
  </div>;
}
