"use client";

import { LoaderCircle } from "lucide-react";
import useAdminTransactions from "@/presentation/hooks/Admin/useAdminTransactions";
import AdminPageHeader from "@/presentation/components/Admin/AdminPageHeader";
import TransactionFilters from "@/presentation/components/Admin/TransactionFilters";
import TransactionTable from "@/presentation/components/Admin/TransactionTable";

export default function TransactionSearchPage() {
  const state = useAdminTransactions();
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <AdminPageHeader
        title="Quản lý giao dịch"
        description="Tra cứu lịch sử thanh toán theo mã giao dịch, email, trạng thái và ngày tạo."
        refreshing={state.list.isFetching}
        onRefresh={() => {
          void state.list.refetch();
        }}
      />
      <TransactionFilters
        value={state.draft}
        onChange={state.setDraft}
        onSearch={state.search}
        error={state.validation}
      />
      {state.listError && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-red-50 p-4 text-sm text-red-700"
        >
          <p>
            {state.listError}
            {state.list.data ? " — Danh sách có thể chưa cập nhật." : ""}
          </p>
          <button
            type="button"
            disabled={state.list.isFetching}
            onClick={() => {
              void state.list.refetch();
            }}
            className="rounded-lg border border-red-200 px-3 py-1.5 font-medium hover:bg-red-100 disabled:opacity-50"
          >
            Thử lại
          </button>
        </div>
      )}
      {state.list.isFetching && !state.list.data && (
        <div
          role="status"
          className="flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-5 py-12 text-sm text-slate-500 shadow-sm"
        >
          <LoaderCircle size={18} className="animate-spin" />
          Đang tải danh sách giao dịch…
        </div>
      )}
      {state.list.data && (
        <TransactionTable
          data={state.list.data}
          onPage={state.page}
          busy={state.list.isFetching}
        />
      )}
    </div>
  );
}
