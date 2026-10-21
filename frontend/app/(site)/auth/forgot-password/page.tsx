"use client";

import Error from "@/presentation/components/layout/Error";
import Success from "@/presentation/components/layout/Success";
import useForgotPassword from "@/presentation/hooks/Auth/useForgotPassword";
import Link from "next/link";

export default function ForgotPassword() {
  const {
    email,
    isSubmitting,
    errorMessage,
    successMessage,
    countdown,
    handleEmailChange,
    handleSubmit,
  } = useForgotPassword();

  return (
    <div className="min-h-screen flex justify-center bg-blue-50/50 px-4 sm:px-6 lg:px-8 py-15">
      {/* Toast thông báo lỗi nổi ở trên cùng */}
      <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-full max-w-md px-4 pointer-events-none">
        {errorMessage && <Error message={errorMessage} />}
        {successMessage && <Success message={successMessage} />}
      </div>

      <div className="h-fit max-w-md bg-white border border-gray-200/80 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="text-center mb-6">
          <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"
              />
            </svg>
          </div>

          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">
            Quên mật khẩu?
          </h1>
          <p className="text-sm text-gray-500 mt-1.5 leading-relaxed">
            Nhập email liên kết với tài khoản của bạn để nhận liên kết đặt lại
            mật khẩu.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1.5">
              Địa chỉ Email <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-gray-400">
                <svg
                  className="w-5 h-5"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207"
                  />
                </svg>
              </div>
              <input
                type="email"
                required
                disabled={isSubmitting}
                placeholder="name@example.com"
                value={email}
                onChange={(e) => handleEmailChange(e)}
                className="w-full pl-10 pr-4 py-2.5 text-sm rounded-lg border border-gray-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition disabled:bg-gray-100 disabled:cursor-not-allowed"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting || countdown > 0}
            className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 px-4 rounded-lg shadow-sm transition active:scale-98 disabled:opacity-50 disabled:pointer-events-none cursor-pointer text-sm"
          >
            {isSubmitting ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Đang gửi liên kết...</span>
              </>
            ) : countdown > 0 ? (
              <span>Gửi lại sau ({countdown}s)</span>
            ) : (
              <span>Gửi liên kết đặt lại mật khẩu</span>
            )}
          </button>
        </form>

        <div className="mt-6 pt-4 border-t border-gray-100 text-center">
          <Link
            href="/auth/login"
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-blue-600 font-medium transition"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
            Quay lại đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
}
