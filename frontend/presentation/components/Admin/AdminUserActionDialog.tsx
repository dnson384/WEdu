"use client";

import { useEffect, useRef } from "react";
import { AdminUserAction } from "@/domain/entities/adminUser.entity";
import { manageableRoles } from "@/presentation/hooks/Admin/useAdminUsers";

interface Props {
  action: AdminUserAction | null;
  nextRole: string;
  onRoleChange: (role: string) => void;
  pending: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

const titles = {
  role: "Thay đổi vai trò",
  lock: "Khóa tài khoản",
  unlock: "Mở khóa tài khoản",
  delete: "Xóa tài khoản",
};

export default function AdminUserActionDialog({
  action,
  nextRole,
  onRoleChange,
  pending,
  error,
  onCancel,
  onConfirm,
}: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (action && dialog && !dialog.open) {
      dialog.showModal();
      cancelRef.current?.focus();
    } else if (!action && dialog?.open) dialog.close();
  }, [action]);
  return (
    <dialog
      ref={ref}
      aria-labelledby="admin-user-action-title"
      aria-describedby="admin-user-action-description"
      aria-busy={pending}
      onCancel={(event) => {
        event.preventDefault();
        if (!pending) onCancel();
      }}
      className="fixed inset-0 m-auto w-[calc(100%-2rem)] max-w-md rounded-2xl border-0 bg-white p-6 shadow-xl backdrop:bg-black/40"
    >
      <h2 id="admin-user-action-title" className="text-xl font-semibold">
        {action ? titles[action.type] : "Quản lý tài khoản"}
      </h2>
      <p
        id="admin-user-action-description"
        className="mt-3 break-all text-sm text-slate-600"
      >
        {action?.user.email}
      </p>
      {action?.type === "role" ? (
        <label className="mt-5 block text-sm font-medium">
          Vai trò mới
          <select
            value={nextRole}
            onChange={(event) => onRoleChange(event.target.value)}
            disabled={pending}
            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"
          >
            {manageableRoles.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      ) : (
        <p className="mt-4 text-sm text-slate-600">
          {action?.type === "delete"
            ? "Bạn có chắc muốn xóa tài khoản này khỏi hệ thống?"
            : action?.type === "lock"
              ? "Người dùng sẽ bị khóa tài khoản theo chính sách truy cập hiện có."
              : "Bạn có chắc muốn mở khóa tài khoản này?"}
        </p>
      )}
      {error && (
        <p
          role="alert"
          className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700"
        >
          {error}
        </p>
      )}
      <div className="mt-6 flex justify-end gap-3">
        <button
          ref={cancelRef}
          type="button"
          disabled={pending}
          onClick={onCancel}
          className="rounded-xl border border-slate-200 px-4 py-2 text-sm disabled:opacity-50"
        >
          Hủy
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={
            !action ||
            pending ||
            (action.type === "role" && nextRole === action.user.role)
          }
          className={`rounded-xl px-4 py-2 text-sm font-semibold text-white disabled:opacity-50 ${action?.type === "delete" ? "bg-red-600 hover:bg-red-700" : "bg-blue-600 hover:bg-blue-700"}`}
        >
          {pending ? "Đang xử lý…" : "Xác nhận"}
        </button>
      </div>
    </dialog>
  );
}
