"use client";

import { FormEvent, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AdminUserAction,
  AdminUserActionType,
  AdminUserEntity,
} from "@/domain/entities/adminUser.entity";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { AdminService } from "@/presentation/services/AdminService";

const service = new AdminService();
const PAGE_SIZE = 10;
export const manageableRoles = [
  { value: "ROLE_TEACHER", label: "Giáo viên" },
  { value: "ROLE_MODERATOR", label: "Kiểm duyệt viên" },
];

export function adminRoleLabel(role: string) {
  return role === "ROLE_ADMIN"
    ? "Quản trị viên"
    : (manageableRoles.find((option) => option.value === role)?.label ?? role);
}

function message(error: unknown): string | null {
  if (!error) return null;
  if (isAxiosError<{ message?: string }>(error))
    return (
      error.response?.data?.message ??
      "Không thể xác nhận kết quả. Hãy làm mới danh sách trước khi thử lại."
    );
  return error instanceof Error ? error.message : "Có lỗi xảy ra";
}

export default function useAdminUsers() {
  const { user: currentUser } = useAuth();
  const client = useQueryClient();
  const key = ["admin-users", currentUser.id];
  const [search, setSearch] = useState("");
  const [keyword, setKeyword] = useState("");
  const [requestedPage, setRequestedPage] = useState(1);
  const [action, setAction] = useState<AdminUserAction | null>(null);
  const [nextRole, setNextRole] = useState("ROLE_TEACHER");
  const [notice, setNotice] = useState<string | null>(null);
  const submitting = useRef(false);
  const query = useQuery({
    queryKey: [...key, keyword],
    queryFn: ({ signal }) =>
      keyword
        ? service.findUsers(keyword, signal)
        : service.getAllUsers(signal),
    enabled: currentUser.role === "ROLE_ADMIN",
    staleTime: 0,
    retry: false,
  });
  const mutation = useMutation({
    retry: false,
    mutationFn: async ({
      target,
      role,
    }: {
      target: AdminUserAction;
      role: string;
    }) => {
      if (target.type === "role")
        return service.updateUserRole(target.user.id, role);
      if (target.type === "lock") return service.lockUser(target.user.id);
      if (target.type === "unlock") return service.unLockUser(target.user.id);
      if (!(await service.deleteUser(target.user.id)))
        throw new Error(
          "Chưa xác nhận xóa thành công. Hãy làm mới danh sách để kiểm tra.",
        );
    },
    onSuccess: (_, input) => {
      const labels = {
        role: "cập nhật vai trò",
        lock: "khóa tài khoản",
        unlock: "mở khóa tài khoản",
        delete: "xóa tài khoản",
      };
      setNotice(`Đã ${labels[input.target.type]} ${input.target.user.email}.`);
      setAction(null);
    },
    onSettled: async () => {
      await client.invalidateQueries({ queryKey: key });
      submitting.current = false;
    },
  });

  const canManage = (target: AdminUserEntity) =>
    target.id !== currentUser.id && target.role !== "ROLE_ADMIN";
  const openAction = (type: AdminUserActionType, target: AdminUserEntity) => {
    if (submitting.current || !canManage(target)) return;
    if (type === "lock" && target.isActive !== true) return;
    if (type === "unlock" && target.isActive !== false) return;
    mutation.reset();
    setNextRole(
      manageableRoles.some((option) => option.value === target.role)
        ? target.role
        : "ROLE_TEACHER",
    );
    setAction({ type, user: target });
    setNotice(null);
  };
  const confirmAction = () => {
    if (!action || submitting.current || !canManage(action.user)) return;
    if (
      action.type === "role" &&
      (!manageableRoles.some((option) => option.value === nextRole) ||
        nextRole === action.user.role)
    )
      return;
    submitting.current = true;
    mutation.mutate({ target: action, role: nextRole });
  };
  const applySearch = (event: FormEvent) => {
    event.preventDefault();
    setKeyword(search.trim());
    setRequestedPage(1);
  };
  const users = query.data ?? [];
  const totalPages = Math.max(1, Math.ceil(users.length / PAGE_SIZE));
  const page = Math.min(requestedPage, totalPages);
  return {
    users: users.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    total: users.length,
    page,
    totalPages,
    setPage: setRequestedPage,
    search,
    setSearch,
    keyword,
    applySearch,
    query,
    error: message(query.error),
    notice,
    canManage,
    openAction,
    action,
    nextRole,
    setNextRole,
    confirmAction,
    cancelAction: () => {
      if (!submitting.current) {
        setAction(null);
        mutation.reset();
      }
    },
    pending: mutation.isPending,
    actionError: message(mutation.error),
  };
}
