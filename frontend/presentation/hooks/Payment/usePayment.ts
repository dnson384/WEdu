"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { PaymentService } from "@/presentation/services/PaymentService";
import { PaymentOrder, PaymentReturnParameters } from "@/domain/entities/payment.entity";

const formatDate = (value?: string | null) =>
  value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Ho_Chi_Minh",
      }).format(new Date(value))
    : null;

function errorMessage(error: unknown) {
  return isAxiosError<{ message?: string }>(error)
    ? (error.response?.data?.message ??
        "Chưa thể kết nối. Vui lòng kiểm tra lại.")
    : "Chưa thể xử lý thanh toán. Vui lòng thử lại.";
}

function isMissingOrder(error: unknown) {
  return (
    isAxiosError<{ message?: string }>(error) &&
    error.response?.status === 404 &&
    error.response.data?.message === "Không tìm thấy đơn thanh toán"
  );
}

const popupBlockedMessage =
  "Trình duyệt đã chặn tab thanh toán. Vui lòng cho phép mở cửa sổ bật lên cho WEdu rồi thử lại.";

function openPaymentTab(): Window | null {
  let tab: Window | null = null;
  try {
    // Open synchronously during the click; waiting for the API can lose user activation.
    tab = window.open("about:blank", "_blank");
    if (tab) tab.opener = null;
    return tab;
  } catch {
    tab?.close();
    return null;
  }
}

export default function usePayment() {
  const { user, isLoadingUser } = useAuth();
  const teacher = user.role === "ROLE_TEACHER";
  const service = useMemo(() => new PaymentService(), []);
  const queryClient = useQueryClient();
  const [orderId, setOrderId] = useState<string | null>(null);
  const [previousOrderIds, setPreviousOrderIds] = useState<string[]>([]);
  const [error, setError] = useState("");
  const [verificationError, setVerificationError] = useState("");
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const key = useRef<string | null>(null);
  const keyOrder = useRef<string | null>(null);
  const verified = useRef<{ id: string; startedAt: number } | null>(null);
  const verificationInFlight = useRef(false);
  const pendingReturn = useRef<{ id: string; parameters: PaymentReturnParameters } | null>(null);
  const storageKey = `wedu-payment-${user.id}`;

  const updateOrder = useCallback(
    (result: PaymentOrder) => {
      queryClient.setQueryData<PaymentOrder>(
        ["payment-order", user.id, result.orderId],
        (current) => (current?.status === "SUCCEEDED" ? current : result),
      );
    },
    [queryClient, user.id],
  );

  useEffect(() => {
    if (!teacher || !user.id) return;
    const params = new URLSearchParams(window.location.search);
    const returnedId = params.get("vnp_TxnRef");
    if (returnedId && /^[a-f0-9]{64}$/.test(returnedId)) {
      pendingReturn.current = {
        id: returnedId,
        parameters: [...params.entries()].filter(([name]) => name.startsWith("vnp_")),
      };
    }
    let saved: string | null = null;
    key.current = null;
    keyOrder.current = null;
    try {
      saved = localStorage.getItem(storageKey);
    } catch { /* Session retry data can still be available. */ }
    try {
      const attempt = JSON.parse(
        sessionStorage.getItem(storageKey + "-key") ?? "null",
      );
      if (
        attempt &&
        typeof attempt.key === "string" &&
        /^[a-zA-Z0-9-]{16,80}$/.test(attempt.key)
      ) {
        key.current = attempt.key;
        keyOrder.current =
          typeof attempt.orderId === "string" && /^[a-f0-9]{64}$/.test(attempt.orderId)
            ? attempt.orderId : null;
      }
    } catch {
      /* Storage is optional. */
    }
    try {
      const previous: unknown = JSON.parse(
        localStorage.getItem(storageKey + "-missing-orders") ?? "[]",
      );
      setPreviousOrderIds(Array.isArray(previous) ? previous.filter(
        (id): id is string => typeof id === "string" && /^[a-f0-9]{64}$/.test(id),
      ) : []);
    } catch { /* Invalid reference history must not discard the retry key. */ }
    const id = [returnedId, saved, keyOrder.current].find(
      (value) => value && /^[a-f0-9]{64}$/.test(value),
    );
    if (id && /^[a-f0-9]{64}$/.test(id)) {
      setOrderId(id);
      try {
        localStorage.setItem(storageKey, id);
      } catch {
        /* Storage is optional. */
      }
    }
    // Keep the signed receipt in memory for backend validation; never persist or trust its status here.
    if (params.has("vnp_TxnRef"))
      window.history.replaceState(null, "", "/me/bill");
  }, [teacher, user.id, storageKey]);

  const plan = useQuery({
    queryKey: ["payment-plan", user.id],
    queryFn: ({ signal }) => service.plan(signal),
    enabled: teacher,
    retry: false,
  });
  const order = useQuery({
    queryKey: ["payment-order", user.id, orderId],
    queryFn: ({ signal }) => service.order(orderId!, signal),
    enabled: teacher && !!orderId,
    retry: false,
    refetchInterval: (query) =>
      !isMissingOrder(query.state.error) && query.state.data?.status === "PENDING"
        ? 5000 : false,
  });
  const missingOrderId = orderId && isMissingOrder(order.error) ? orderId : null;
  const canStartPayment = !busy && !!plan.data?.available &&
    (!orderId || (!order.isPending && !order.isError && order.data?.status !== "PENDING"));
  const canStartNewPayment = !busy && !!plan.data?.available && !!missingOrderId && !order.isFetching;

  const reconcileOrder = useCallback(async (id: string) => {
    const receipt = pendingReturn.current;
    if (receipt?.id !== id) return service.verify(id);
    try {
      const result = await service.confirmReturn(id, receipt.parameters);
      if (pendingReturn.current === receipt) pendingReturn.current = null;
      return result;
    } catch (e) {
      // Invalid receipts are not retried; transient failures keep the receipt in memory.
      if (isAxiosError(e) && [400, 403, 404].includes(e.response?.status ?? 0) && pendingReturn.current === receipt) {
        pendingReturn.current = null;
      }
      throw e;
    }
  }, [service]);

  useEffect(() => {
    const id = order.data?.orderId;
    const status = order.data?.status;
    if (!teacher || !id || missingOrderId) return;
    if (status === "SUCCEEDED") {
      setVerificationError("");
      void queryClient.invalidateQueries({ queryKey: ["user"] });
      return;
    }
    let active = true;
    const verify = async () => {
      if (lock.current || verificationInFlight.current) return;
      if (
        verified.current?.id === id &&
        Date.now() - verified.current.startedAt < 30000
      )
        return;
      verified.current = { id, startedAt: Date.now() };
      verificationInFlight.current = true;
      try {
        const result = await reconcileOrder(id);
        if (active) {
          updateOrder(result);
          setVerificationError("");
        }
      } catch (e) {
        if (active) setVerificationError(errorMessage(e));
      } finally {
        verificationInFlight.current = false;
      }
    };
    void verify();
    if (status !== "PENDING")
      return () => {
        active = false;
      };
    const interval = setInterval(() => {
      void verify();
    }, 30000);
    const onFocus = () => {
      void verify();
    };
    window.addEventListener("focus", onFocus);
    return () => {
      active = false;
      clearInterval(interval);
      window.removeEventListener("focus", onFocus);
    };
  }, [
    order.data?.orderId,
    order.data?.status,
    teacher,
    queryClient,
    service,
    updateOrder,
    missingOrderId,
    reconcileOrder,
  ]);

  const beginPayment = async (replaceMissingOrder: boolean) => {
    if (
      lock.current || !teacher ||
      !(replaceMissingOrder ? canStartNewPayment : canStartPayment)
    ) return;
    lock.current = true;
    setBusy(true);
    setError("");
    setVerificationError("");
    let paymentTab: Window | null = null;
    try {
      paymentTab = openPaymentTab();
      if (!paymentTab) {
        setError(popupBlockedMessage);
        return;
      }
      if (replaceMissingOrder && missingOrderId) {
        // Retain the reference for reconciliation; only an explicit new purchase retires its key.
        const previous = [...new Set([...previousOrderIds, missingOrderId])];
        setPreviousOrderIds(previous);
        setOrderId(null);
        key.current = null;
        keyOrder.current = null;
        try {
          localStorage.setItem(storageKey + "-missing-orders", JSON.stringify(previous));
          localStorage.removeItem(storageKey);
        } catch { /* Keep the reference in memory when storage is unavailable. */ }
      }
      if (
        !key.current ||
        (order.data &&
          keyOrder.current === order.data.orderId &&
          order.data.status !== "PENDING")
      ) {
        key.current = crypto.randomUUID();
        keyOrder.current = null;
      }
      try {
        sessionStorage.setItem(
          storageKey + "-key",
          JSON.stringify({ key: key.current, orderId: keyOrder.current }),
        );
      } catch {
        /* Keep in-memory key. */
      }
      const result = await service.create(key.current);
      keyOrder.current = result.orderId;
      try {
        sessionStorage.setItem(
          storageKey + "-key",
          JSON.stringify({ key: key.current, orderId: result.orderId }),
        );
      } catch {
        /* Storage is optional. */
      }
      setOrderId(result.orderId);
      updateOrder(result);
      try {
        localStorage.setItem(storageKey, result.orderId);
      } catch {
        /* Storage is optional. */
      }
      if (result.paymentUrl) {
        if (paymentTab.closed) {
          setError(
            "Tab thanh toán đã đóng. Chọn “Tiếp tục tại VNPay” để mở lại đơn đã tạo.",
          );
        } else {
          paymentTab.location.replace(result.paymentUrl);
        }
      } else {
        paymentTab.close();
        key.current = null;
      }
    } catch (e) {
      paymentTab?.close();
      setError(errorMessage(e));
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };

  const continuePayment = () => {
    if (lock.current || !teacher || missingOrderId || !order.data?.paymentUrl) return;
    setError("");
    const paymentTab = openPaymentTab();
    if (!paymentTab) {
      setError(popupBlockedMessage);
      return;
    }
    try {
      paymentTab.location.replace(order.data.paymentUrl);
    } catch {
      paymentTab.close();
      setError("Chưa thể mở tab thanh toán. Vui lòng thử lại.");
    }
  };

  const verifyPayment = async () => {
    if (lock.current || verificationInFlight.current || missingOrderId || !orderId) return;
    lock.current = true;
    verificationInFlight.current = true;
    verified.current = { id: orderId, startedAt: Date.now() };
    setBusy(true);
    setError("");
    setVerificationError("");
    try {
      const result = await reconcileOrder(orderId);
      updateOrder(result);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      lock.current = false;
      verificationInFlight.current = false;
      setBusy(false);
    }
  };

  return {
    user,
    teacher,
    isLoadingUser,
    plan: plan.data,
    loadingPlan: plan.isLoading,
    order: missingOrderId ? undefined : order.data,
    missingOrderId,
    previousOrderIds,
    canStartPayment,
    canStartNewPayment,
    loadingOrder: order.isFetching,
    busy,
    error:
      error ||
      verificationError ||
      (plan.error
        ? errorMessage(plan.error)
        : order.error && !missingOrderId
          ? errorMessage(order.error)
          : ""),
    startPayment: () => beginPayment(false),
    startNewPayment: () => beginPayment(true),
    verifyPayment,
    continuePayment,
    expiryLabel: formatDate(user.plusExpiresAt),
    paidExpiryLabel: formatDate(order.data?.plusExpiresAt),
    retry: () => {
      setError("");
      setVerificationError("");
      void plan.refetch();
      if (orderId) void order.refetch();
    },
  };
}
