import Link from "next/link";
import GoogleAuthButton from "@/presentation/components/Auth/GoogleAuthButton";

export default function GoogleErrorPage() {
  return (
    <main className="min-h-screen bg-gray-50 px-4 py-30">
      <section className="mx-auto max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-bold">Chưa thể đăng nhập bằng Google</h1>

        <p role="alert" className="my-5 text-sm text-gray-600">
          Bạn có thể đã hủy xác nhận, phiên đã hết hạn, tài khoản không đủ điều
          kiện hoặc dịch vụ tạm thời gặp lỗi. Chỉ tài khoản Giáo viên đang hoạt
          động được sử dụng tính năng này.
        </p>

        <GoogleAuthButton />

        <Link
          className="mt-5 block text-center text-sm text-blue-600"
          href="/auth/login"
        >
          Về trang đăng nhập
        </Link>
      </section>
    </main>
  );
}
