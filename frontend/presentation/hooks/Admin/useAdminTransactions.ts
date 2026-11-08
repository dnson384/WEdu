"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { AdminTransaction, TransactionFilters, TransactionLookup } from "@/domain/entities/adminTransaction.entity";
import { AdminTransactionService } from "@/presentation/services/AdminTransactionService";
import useAuth from "@/presentation/hooks/Auth/useAuth";

const service = new AdminTransactionService();
const initial: TransactionFilters = { keyword: "", status: "", from: "", to: "", page: 0 };
function message(error: unknown, fallback: string) {
  return isAxiosError<{ message?: string }>(error) && typeof error.response?.data?.message === "string"
    ? error.response.data.message : fallback;
}

export default function useAdminTransactions() {
  const { user } = useAuth();
  const [draft, setDraft] = useState(initial);
  const [filters, setFilters] = useState(initial);
  const [validation, setValidation] = useState("");
  const [selected, setSelected] = useState<AdminTransaction | null>(null);
  const [result, setResult] = useState<TransactionLookup | null>(null);
  const [lookupError, setLookupError] = useState("");
  const [lookingUp, setLookingUp] = useState(false);
  const active = useRef<AbortController | null>(null);
  const list = useQuery({
    queryKey: ["admin-transactions", user.id, filters],
    queryFn: ({ signal }) => service.search(filters, signal),
    enabled: user.role === "ROLE_ADMIN", retry: false,
  });

  useEffect(() => () => { active.current?.abort(); }, []);

  function close() {
    active.current?.abort();
    active.current = null;
    setSelected(null);
    setResult(null);
    setLookupError("");
    setLookingUp(false);
  }

  function search() {
    if (draft.from && draft.to && draft.from > draft.to) {
      setValidation("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
      return;
    }
    setValidation("");
    close();
    setFilters({ ...draft, keyword: draft.keyword.trim(), page: 0 });
  }

  function page(value: number) {
    close();
    setFilters((previous) => ({ ...previous, page: value }));
  }

  async function lookup(order: AdminTransaction) {
    if (active.current) return;
    const controller = new AbortController();
    active.current = controller;
    setSelected(order);
    setResult(null);
    setLookupError("");
    setLookingUp(true);
    try {
      const response = await service.lookup(order.orderId, controller.signal);
      if (active.current === controller) setResult(response);
    } catch (error) {
      if (active.current === controller) setLookupError(message(error, "Chưa thể tra cứu VNPay, vui lòng thử lại"));
    } finally {
      if (active.current === controller) {
        active.current = null;
        setLookingUp(false);
      }
    }
  }

  return { draft, setDraft, filters, validation, search, page, list,
    listError: list.error ? message(list.error, "Không thể tải danh sách giao dịch") : "",
    selected, result, lookupError, lookingUp, lookup, close };
}
