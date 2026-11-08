export const transactionStatuses: Record<string, string> = {
  PENDING: "Chờ thanh toán", SUCCEEDED: "Thành công", FAILED: "Thất bại",
  CANCELLED: "Đã hủy", EXPIRED: "Hết hạn",
};

export function transactionDate(value: string | null) {
  return value ? new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short", timeStyle: "medium", timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(value)) : "Chưa có thông tin";
}

export function transactionMoney(value: number) {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
}

export function providerStatus(code: string) {
  const labels: Record<string, string> = {
    "00": "Thành công", "01": "Chưa hoàn tất", "02": "Bị lỗi", "04": "Đảo giao dịch",
    "05": "Đang xử lý hoàn tiền", "06": "Đã gửi yêu cầu hoàn tiền", "07": "Nghi ngờ gian lận", "09": "Hoàn trả bị từ chối",
  };
  return `${labels[code] ?? "Trạng thái khác"} (${code})`;
}
