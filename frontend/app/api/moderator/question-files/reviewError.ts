import { isAxiosError } from "axios";
import { NextResponse } from "next/server";

export const reviewHeaders = { "Cache-Control": "private, no-store" };

export function reviewError(error: unknown) {
  const status = isAxiosError(error) ? (error.response?.status ?? 502) : 500;
  const message = status === 400 ? "Dữ liệu kiểm duyệt không hợp lệ. Kiểm tra tệp DOCX, tên tệp và lý do từ chối."
    : status === 401 ? "Vui lòng đăng nhập lại"
    : status === 403 ? "Bạn không có quyền kiểm duyệt tệp"
    : status === 404 ? "Tệp tin không còn tồn tại"
    : status === 413 ? "Tệp vượt quá giới hạn dung lượng"
    : "Không thể hoàn tất kiểm duyệt. Hãy kiểm tra danh sách trước khi thử lại.";
  return NextResponse.json({ message }, { status, headers: reviewHeaders });
}
