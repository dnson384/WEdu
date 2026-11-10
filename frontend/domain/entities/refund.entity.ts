export const refundLabels: Record<string, string> = {
  PENDING_REVIEW: "Chờ ADMIN duyệt", NEEDS_VERIFICATION: "Cần xác minh", PROCESSING: "Đang xử lý VNPay",
  REFUNDED: "Đã hoàn tiền", REJECTED: "Đã từ chối", FAILED: "Hoàn tiền thất bại", CANCELLED: "Đã hủy",
};
export interface Refund {
  id: string; orderId: string; email: string | null; amount: number; status: string;
  reason: string; policyVersion: string; eligibility: string; decisionReason: string;
  createdAt: string; updatedAt: string; paidAt: string | null; lastAiUsedAt: string | null;
  activeAi: boolean; projectedPlusExpiresAt: string | null; entitlementVerifiable: boolean;
  providerCode: string | null; providerStatus: string | null; refundTransactionNo: string | null;
  revision: number; history: { at: string; status: string; actor: string; reason: string }[];
  checks: { key: string; result: string; reason: string }[];
}
export interface RefundFilters { keyword: string; status: string; from: string; to: string; page: number }
export interface RefundPage {
  items: Refund[]; page: number; pageSize: number; totalElements: number; totalPages: number;
  enabled: boolean; policyVersion: string;
}
export interface RefundOrder { id: string; amount: number; status: string; createdAt: string; paidAt: string | null }
export interface RefundOrders { items: RefundOrder[]; page: number; hasNext: boolean }
export interface RefundCreate { reason: string; policyVersion: string }
export interface RefundDecision { revision: number; reason?: string; legacyException?: boolean }
export type RefundAction = "approve" | "reject" | "cancel" | "reconcile";
