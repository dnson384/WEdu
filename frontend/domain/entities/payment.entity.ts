export interface PaymentPlan {
  amount: number;
  currency: string;
  durationDays: number;
  available: boolean;
}

export interface PaymentOrder {
  orderId: string;
  amount: number;
  status: "PENDING" | "SUCCEEDED" | "FAILED" | "CANCELLED" | "EXPIRED";
  expiresAt: string;
  plusExpiresAt: string | null;
  paymentUrl: string | null;
  manualReview: boolean;
}
export type PaymentReturnParameters = [string, string][];
