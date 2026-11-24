export const transactionStatuses: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  SUCCEEDED: "Thành công",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
  EXPIRED: "Hết hạn",
};

export function transactionStatusClass(status: string) {
  const classes: Record<string, string> = {
    PENDING: "bg-amber-50 text-amber-700",
    SUCCEEDED: "bg-emerald-50 text-emerald-700",
    FAILED: "bg-red-50 text-red-700",
    CANCELLED: "bg-slate-100 text-slate-600",
    EXPIRED: "bg-slate-100 text-slate-600",
  };
  return classes[status] ?? "bg-slate-100 text-slate-600";
}

export function transactionDate(value: string | null) {
  return value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "medium",
        timeZone: "Asia/Ho_Chi_Minh",
      }).format(new Date(value))
    : "Chưa có thông tin";
}

export function transactionMoney(value: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
}
