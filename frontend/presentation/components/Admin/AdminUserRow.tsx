import {
  AdminUserActionType,
  AdminUserEntity,
} from "@/domain/entities/adminUser.entity";
import { adminRoleLabel } from "@/presentation/hooks/Admin/useAdminUsers";

interface Props {
  user: AdminUserEntity;
  canManage: boolean;
  disabled: boolean;
  onAction: (type: AdminUserActionType, user: AdminUserEntity) => void;
}

export default function AdminUserRow({
  user,
  canManage,
  disabled,
  onAction,
}: Props) {
  return (
    <tr className="border-t border-slate-100">
      <td className="px-5 py-4">
        <p className="font-medium text-slate-900">
          {user.username || "Chưa cập nhật tên"}
        </p>
        <p className="mt-1 break-all text-xs text-slate-500">{user.email}</p>
      </td>
      <td className="px-5 py-4 text-sm">{adminRoleLabel(user.role)}</td>
      <td className="px-5 py-4">
        <span
          className={`rounded-full px-2.5 py-1 text-xs font-medium ${user.isActive === true ? "bg-emerald-50 text-emerald-700" : user.isActive === false ? "bg-red-50 text-red-700" : "bg-slate-100 text-slate-600"}`}
        >
          {user.isActive === true
            ? "Hoạt động"
            : user.isActive === false
              ? "Đã khóa"
              : "Chưa xác định"}
        </span>
      </td>
      <td className="px-5 py-4 text-sm text-slate-600">
        {user.accountType || "—"}
      </td>
      <td className="px-5 py-4">
        {canManage ? (
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={disabled}
              onClick={() => onAction("role", user)}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-50 disabled:opacity-50"
            >
              Phân quyền
            </button>
            <button
              type="button"
              disabled={
                disabled ||
                user.isActive === null ||
                user.isActive === undefined
              }
              onClick={() =>
                onAction(user.isActive === true ? "lock" : "unlock", user)
              }
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium hover:bg-slate-50 disabled:opacity-50"
            >
              {user.isActive === false ? "Mở khóa" : "Khóa"}
            </button>
            <button
              type="button"
              disabled={disabled}
              onClick={() => onAction("delete", user)}
              className="rounded-lg border border-red-100 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
            >
              Xóa
            </button>
          </div>
        ) : (
          <span className="text-xs text-slate-400">
            Tài khoản quản trị được bảo vệ
          </span>
        )}
      </td>
    </tr>
  );
}
