"use client";

import { icons } from "@/presentation/common/icons";
import Error from "@/presentation/components/layout/Error";
import Success from "@/presentation/components/layout/Success";
import useResetPassword from "@/presentation/hooks/Auth/useResetPassword";
import Link from "next/link";

export default function ResetPassword() {
  const {
    token,
    formData,
    showPassword,
    isSubmitting,
    errorMessage,
    successMessage,
    notiPassword,
    handleFormChange,
    toggleShowPassword,
    handleSubmit,
  } = useResetPassword();

  return (
    <div className="min-h-screen flex justify-center bg-blue-50/50 px-4 sm:px-6 lg:px-8 py-15">
      {/* Thông báo lỗi nổi ở trên cùng */}
      <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-full max-w-md px-4 pointer-events-none">
        {errorMessage && <Error message={errorMessage} />}
        {successMessage && <Success message={successMessage} />}
      </div>

      <div className="h-fit w-full md:w-lg bg-white border border-gray-200/80 rounded-2xl p-6 sm:p-8 shadow-sm">
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
            Thiết lập mật khẩu mới
          </h1>
        </div>

        {!token && (
          <div className="text-center space-y-4">
            <div className="p-4 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-sm leading-relaxed">
              Mã xác thực không tồn tại hoặc đường dẫn đặt lại mật khẩu không
              hợp lệ. Vui lòng gửi lại yêu cầu.
            </div>
            <Link
              href="/auth/forgot-password"
              className="inline-block text-sm font-semibold text-blue-600 hover:text-blue-700"
            >
              Yêu cầu liên kết mới
            </Link>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Mật khẩu mới */}
          <label className="block">
            <span className="font-semibold text-sm text-gray-700 block mb-1.5">
              Mật khẩu mới
            </span>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 flex items-center justify-center w-10 text-gray-400 pointer-events-none">
                {icons.lock}
              </div>

              <input
                type={showPassword ? "text" : "password"}
                required
                name="newPassword"
                minLength={8}
                maxLength={32}
                disabled={isSubmitting}
                className="w-full pl-10 pr-12 py-2.5 rounded-lg bg-gray-50 border border-gray-300 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors disabled:opacity-50"
                placeholder="Nhập mật khẩu mới"
                value={formData.newPassword}
                onChange={handleFormChange}
              />

              <button
                type="button"
                tabIndex={-1}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                className="absolute inset-y-0 right-0 flex items-center justify-center w-10 text-gray-500 hover:text-gray-700 cursor-pointer"
                onClick={toggleShowPassword}
              >
                {showPassword ? icons.eyeClose : icons.eye}
              </button>
            </div>
          </label>

          {/* Xác nhận mật khẩu mới */}
          <label className="block">
            <span className="font-semibold text-sm text-gray-700 block mb-1.5">
              Xác nhận mật khẩu mới
            </span>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 flex items-center justify-center w-10 text-gray-400 pointer-events-none">
                {icons.lock}
              </div>
              <input
                type={showPassword ? "text" : "password"}
                required
                name="confirmPassword"
                minLength={8}
                maxLength={32}
                disabled={isSubmitting}
                className="w-full pl-10 pr-12 py-2.5 rounded-lg bg-gray-50 border border-gray-300 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors disabled:opacity-50"
                placeholder="Nhập lại mật khẩu mới"
                value={formData.confirmPassword}
                onChange={handleFormChange}
              />
            </div>
            {notiPassword && (
              <p className="text-red-500 text-xs mt-1 font-medium">
                {notiPassword}
              </p>
            )}
          </label>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 px-4 rounded-lg shadow-sm transition active:scale-98 disabled:opacity-50 disabled:pointer-events-none cursor-pointer text-sm mt-2"
          >
            {isSubmitting ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>Đang xử lý...</span>
              </>
            ) : (
              <span>Cập nhật mật khẩu</span>
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
