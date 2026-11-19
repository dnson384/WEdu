"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { TransactionFilters } from "@/domain/entities/adminTransaction.entity";
import { AdminTransactionService } from "@/presentation/services/AdminTransactionService";
import useAuth from "@/presentation/hooks/Auth/useAuth";

const service = new AdminTransactionService();
const initial: TransactionFilters = {
  keyword: "",
  status: "",
  from: "",
  to: "",
  page: 0,
};

export default function useAdminTransactions() {
  const { user } = useAuth();
  const [draft, setDraft] = useState(initial);
  const [filters, setFilters] = useState(initial);
  const [validation, setValidation] = useState("");
  const list = useQuery({
    queryKey: ["admin-transactions", user.id, filters],
    queryFn: ({ signal }) => service.search(filters, signal),
    enabled: user.role === "ROLE_ADMIN",
    retry: false,
  });

  function search() {
    if (draft.from && draft.to && draft.from > draft.to) {
      setValidation("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
      return;
    }
    setValidation("");
    setFilters({ ...draft, keyword: draft.keyword.trim(), page: 0 });
  }

  function page(value: number) {
    setFilters((previous) => ({ ...previous, page: value }));
  }

  const listError = list.error
    ? isAxiosError<{ message?: string }>(list.error) &&
      typeof list.error.response?.data?.message === "string"
      ? list.error.response.data.message
      : "Không thể tải danh sách giao dịch"
    : "";

  return {
    draft,
    setDraft,
    filters,
    validation,
    search,
    page,
    list,
    listError,
  };
}
