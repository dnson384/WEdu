"use client";

import { Search, Users } from "lucide-react";
import AdminPageHeader from "@/presentation/components/Admin/AdminPageHeader";
import AdminPagination from "@/presentation/components/Admin/AdminPagination";
import useAdminUsers from "@/presentation/hooks/Admin/useAdminUsers";
import AdminUserRow from "@/presentation/components/Admin/AdminUserRow";
import AdminUserActionDialog from "@/presentation/components/Admin/AdminUserActionDialog";

export default function AdminUsersPage() {
  const admin = useAdminUsers();
  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <AdminPageHeader
        title="Quản lý tài khoản người dùng"
        description="Tìm kiếm, phân quyền và quản lý trạng thái tài khoản."
        refreshing={admin.query.isFetching}
        disabled={admin.pending}
        onRefresh={() => {
          void admin.query.refetch();
        }}
      />
      <form
        onSubmit={admin.applySearch}
        className="flex flex-wrap gap-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <label className="min-w-0 flex-1">
          <span className="sr-only">Tìm tài khoản theo tên hoặc email</span>
          <input
            type="search"
            value={admin.search}
            onChange={(event) => admin.setSearch(event.target.value)}
            placeholder="Tìm theo tên hoặc email…"
            className="w-full rounded-xl border border-slate-300 px-4 py-2.5 text-sm"
          />
        </label>
        <button
          type="submit"
          className="flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white"
        >
          <Search size={16} />
          Tìm kiếm
        </button>
      </form>
      {admin.notice && (
        <p
          role="status"
          className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-800"
        >
          {admin.notice}
        </p>
      )}
      {admin.error && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 p-4 text-sm text-red-700"
        >
          {admin.error}
          {admin.query.data ? " — Danh sách có thể chưa cập nhật." : ""}
        </p>
      )}
      <section
        className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
        aria-busy={admin.query.isFetching}
      >
        <div className="flex items-center gap-2 px-5 py-4 text-sm font-medium">
          <Users size={18} />
          {admin.query.isPending
            ? "Đang tải danh sách…"
            : `${admin.total} tài khoản${admin.keyword ? " phù hợp" : ""}`}
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-190 text-left">
            <caption className="sr-only">
              Danh sách tài khoản người dùng
            </caption>
            <thead className="bg-slate-50 text-xs text-slate-500">
              <tr>
                <th className="px-5 py-3 font-medium">Người dùng</th>
                <th className="px-5 py-3 font-medium">Vai trò</th>
                <th className="px-5 py-3 font-medium">Trạng thái</th>
                <th className="px-5 py-3 font-medium">Loại tài khoản</th>
                <th className="px-5 py-3 font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {admin.users.map((user) => (
                <AdminUserRow
                  key={user.id}
                  user={user}
                  canManage={admin.canManage(user)}
                  disabled={admin.pending || admin.query.isFetching}
                  onAction={admin.openAction}
                />
              ))}
            </tbody>
          </table>
        </div>
        {!admin.query.isPending && !admin.error && admin.total === 0 && (
          <p
            role="status"
            className="border-t border-slate-100 px-5 py-12 text-center text-sm text-slate-500"
          >
            {admin.keyword
              ? "Không tìm thấy tài khoản phù hợp."
              : "Chưa có tài khoản người dùng."}
          </p>
        )}
        <AdminPagination
          page={admin.page}
          totalPages={admin.totalPages}
          pageSize={10}
          itemLabel="tài khoản"
          onPageChange={admin.setPage}
          disabled={admin.query.isFetching || admin.pending}
        />
      </section>
      <p className="text-xs text-slate-500">
        Không thể thao tác lên tài khoản quản trị hoặc bản thân. Việc phân quyền
        hiện hỗ trợ giáo viên và kiểm duyệt viên theo API giao diện hiện có.
      </p>
      <AdminUserActionDialog
        action={admin.action}
        nextRole={admin.nextRole}
        onRoleChange={admin.setNextRole}
        pending={admin.pending}
        error={admin.actionError}
        onCancel={admin.cancelAction}
        onConfirm={admin.confirmAction}
      />
    </div>
  );
}
