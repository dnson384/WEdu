"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { RefundService } from "@/presentation/services/RefundService";
import { RefundAction, RefundFilters } from "@/domain/entities/refund.entity";

const initial: RefundFilters = { keyword: "", status: "", from: "", to: "", page: 0 };
function message(error: unknown) {
  return isAxiosError<{ message?: string }>(error) ? error.response?.data?.message ?? "Không thể kết nối. Vui lòng tải lại trạng thái." : "Không thể xử lý yêu cầu.";
}

export default function useRefunds(admin: boolean) {
  const { user, isLoadingUser } = useAuth();
  const allowed = user.role === (admin ? "ROLE_ADMIN" : "ROLE_TEACHER");
  const service = useMemo(() => new RefundService(), []);
  const cache = useQueryClient();
  const [draft, setDraft] = useState(initial);
  const [filters, setFilters] = useState(initial);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [orderPage, setOrderPage] = useState(0);
  const [orderId, setOrderId] = useState("");
  const [createReason, setCreateReason] = useState("");
  const [accepted, setAccepted] = useState(false);
  const [decisionReason, setDecisionReason] = useState("");
  const [legacyException, setLegacyException] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const inFlight = useRef(false);
  const attemptKeys = useRef<Record<string, string>>({});
  const list = useQuery({
    queryKey: ["refunds", user.id, admin, filters], enabled: allowed,
    queryFn: ({ signal }) => service.search(admin, filters, signal),
  });
  const detail = useQuery({
    queryKey: ["refund-detail", user.id, admin, selectedId], enabled: allowed && !!selectedId,
    queryFn: ({ signal }) => service.get(admin, selectedId!, signal),
    refetchInterval: (query) => ["PENDING_REVIEW", "NEEDS_VERIFICATION", "PROCESSING"].includes(query.state.data?.status ?? "") ? 10000 : false,
  });
  const orders = useQuery({ queryKey: ["refund-orders", user.id, orderPage], enabled: allowed && !admin, queryFn: () => service.orders(orderPage) });

  useEffect(() => {
    if (detail.data?.status === "REFUNDED") void cache.invalidateQueries({ queryKey: ["user"] });
  }, [detail.data?.status, cache]);

  function select(id: string | null) { setSelectedId(id); setDecisionReason(""); setLegacyException(false); setError(""); }
  async function refresh() {
    await Promise.all([cache.invalidateQueries({ queryKey: ["refunds", user.id] }),
      cache.invalidateQueries({ queryKey: ["refund-detail", user.id] }), cache.invalidateQueries({ queryKey: ["refund-orders", user.id] })]);
  }
  async function create() {
    if (inFlight.current || !accepted || !orderId || createReason.trim().length < 3) return;
    inFlight.current = true; setBusy(true); setError("");
    const storageKey = `refund-attempt:${user.id}:${orderId}`;
    try {
      let key = attemptKeys.current[storageKey];
      try { key ||= sessionStorage.getItem(storageKey) ?? ""; } catch { /* Memory fallback for restricted storage. */ }
      key ||= crypto.randomUUID();
      attemptKeys.current[storageKey] = key;
      try { sessionStorage.setItem(storageKey, key); } catch { /* Retain the in-memory key. */ }
      const refund = await service.create(orderId, { reason: createReason.trim(), policyVersion: list.data?.policyVersion ?? "plus-refund-v1" }, key);
      delete attemptKeys.current[storageKey];
      try { sessionStorage.removeItem(storageKey); } catch { /* Request is already safely persisted. */ }
      select(refund.id); setCreateReason(""); setAccepted(false); await refresh();
    } catch (failure) { setError(message(failure)); await refresh(); }
    finally { inFlight.current = false; setBusy(false); }
  }
  async function act(action: RefundAction) {
    if (inFlight.current || !detail.data) return;
    inFlight.current = true; setBusy(true); setError("");
    try { await service.decide(detail.data, action, decisionReason.trim(), legacyException); await refresh(); }
    catch (failure) { setError(message(failure)); await refresh(); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return {
    allowed, isLoadingUser, list, detail, orders, selectedId, select, draft, setDraft, filters,
    search: () => { setFilters({ ...draft, page: 0 }); select(null); }, page: (page: number) => setFilters({ ...filters, page }),
    orderPage, setOrderPage, orderId, setOrderId, createReason, setCreateReason, accepted, setAccepted,
    decisionReason, setDecisionReason, legacyException, setLegacyException, busy, error,
    listError: list.error ? message(list.error) : "", detailError: detail.error ? message(detail.error) : "",
    ordersError: orders.error ? message(orders.error) : "", create, act, refresh,
  };
}
