"use client";

import { FormEvent, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AiBudgetDraft,
  AiUsageFilters,
} from "@/domain/entities/aiManagement.entity";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { AiManagementService } from "@/presentation/services/AiManagementService";

const service = new AiManagementService();

function currentMonth(): AiUsageFilters {
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const value = (type: string) =>
    parts.find((part) => part.type === type)?.value ?? "";
  const month = `${value("year")}-${value("month")}`;
  return { from: `${month}-01`, to: `${month}-${value("day")}`, model: "" };
}

function errorMessage(error: unknown): string | null {
  if (!error) return null;
  return isAxiosError<{ message?: string }>(error)
    ? (error.response?.data?.message ?? "Không thể kết nối dịch vụ quản lý AI")
    : error instanceof Error
      ? error.message
      : "Có lỗi xảy ra";
}

export default function useAiManagement() {
  const { user } = useAuth();
  const enabled = user.role === "ROLE_ADMIN";
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState(currentMonth);
  const [filterDraft, setFilterDraft] = useState(currentMonth);
  const [draft, setDraft] = useState<AiBudgetDraft | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [validation, setValidation] = useState<string | null>(null);
  const savingRef = useRef(false);
  const key = ["admin-ai", user.id];
  const report = useQuery({
    queryKey: [...key, "usage", filters],
    queryFn: ({ signal }) => service.report(filters, signal),
    enabled,
    refetchInterval: 60000,
    retry: false,
    staleTime: 0,
  });
  const status = useQuery({
    queryKey: [...key, "status"],
    queryFn: ({ signal }) => service.status(signal),
    enabled,
    refetchInterval: 60000,
    retry: false,
    staleTime: 0,
  });
  const budget = useQuery({
    queryKey: [...key, "budget"],
    queryFn: ({ signal }) => service.budget(signal),
    enabled,
    retry: false,
    staleTime: 0,
  });
  const form: AiBudgetDraft = draft ?? {
    revision: budget.data?.revision ?? 0,
    amount: budget.data?.monthlyBudgetUsd ?? "",
    tokens: budget.data?.monthlyTokenQuota?.toString() ?? "",
  };
  const save = useMutation({
    mutationFn: service.updateBudget.bind(service),
    retry: false,
    onSuccess: (data) => {
      queryClient.setQueryData([...key, "budget"], data);
      setDraft(null);
      setNotice(
        "Đã lưu hạn mức. Cấu hình có hiệu lực ngay và áp dụng cho các tháng tiếp theo.",
      );
      void queryClient.invalidateQueries({ queryKey: [...key, "status"] });
    },
    onSettled: () => {
      savingRef.current = false;
    },
  });

  const applyFilters = (event: FormEvent) => {
    event.preventDefault();
    const duration = Date.parse(filterDraft.to) - Date.parse(filterDraft.from);
    if (
      !Number.isFinite(duration) ||
      duration < 0 ||
      duration >= 366 * 86400000
    ) {
      setValidation("Chọn khoảng thời gian từ 1 đến 366 ngày.");
      return;
    }
    setValidation(null);
    setFilters({ ...filterDraft });
  };

  const submitBudget = (event: FormEvent) => {
    event.preventDefault();
    if (savingRef.current || !budget.data) return;
    const amount = form.amount.trim();
    const tokens = form.tokens.trim();
    if (
      (amount &&
        (!/^\d{1,12}(\.\d{1,8})?$/.test(amount) || Number(amount) <= 0)) ||
      (tokens &&
        (!/^\d+$/.test(tokens) ||
          !Number.isSafeInteger(Number(tokens)) ||
          Number(tokens) <= 0))
    ) {
      setValidation(
        "Budget phải lớn hơn 0, tối đa 8 số thập phân; quota phải là số nguyên dương an toàn.",
      );
      return;
    }
    setValidation(null);
    setNotice(null);
    savingRef.current = true;
    save.mutate({
      revision: form.revision,
      monthlyBudgetUsd: amount || null,
      monthlyTokenQuota: tokens ? Number(tokens) : null,
    });
  };

  const reloadBudget = async () => {
    const result = await budget.refetch();
    if (result.isSuccess) {
      setDraft(null);
      save.reset();
      setValidation(null);
    }
  };

  return {
    report,
    status,
    budget,
    filterDraft,
    setFilterDraft,
    applyFilters,
    form,
    changeBudget: (field: "amount" | "tokens", value: string) => {
      setDraft({ ...form, [field]: value });
      setNotice(null);
    },
    submitBudget,
    reloadBudget,
    saving: save.isPending,
    notice,
    validation,
    saveError: errorMessage(save.error),
    reportError: errorMessage(report.error),
    statusError: errorMessage(status.error),
    budgetError: errorMessage(budget.error),
    refresh: () => {
      void report.refetch();
      void status.refetch();
    },
  };
}
