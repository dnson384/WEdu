"use client";
import Link from "next/link";

import { icons } from "@/presentation/common/icons";
import useLogin from "@/presentation/hooks/Auth/useLogin";
import Error from "@/presentation/components/layout/Error";

export default function Login() {
  const {
    formData,
    showPassword,
    error,
    isSubmitting,
    handleFormChange,
    handleShowPasswordClick,
    handleLoginSubmit,
  } = useLogin();

  return (
    <main className="min-h-screen bg-gray-50 pt-30 sm:px-6 lg:px-8 relative">
      <div className="absolute top-6 left-6 z-20">
        <Link
          href="/landing-page"
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium text-gray-600 hover:text-blue-600 hover:bg-white transition-all shadow-xs border border-transparent hover:border-gray-200"
        >
          {icons.arrowBack}
          <span>Về trang giới thiệu</span>
        </Link>
      </div>

      {error && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 z-50 pointer-events-none">
          <Error message={error} />
        </div>
      )}

      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Chào mừng trở lại
          </h1>
        </div>

        <div className="bg-white py-8 px-6 shadow-sm rounded-2xl border border-gray-200 sm:px-10">
          <form className="flex flex-col gap-4" onSubmit={handleLoginSubmit}>
            {/* Email */}
            <label className="block">
              <span className="font-semibold text-sm text-gray-700 block mb-1.5">
                Email
              </span>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 flex items-center justify-center w-10 text-gray-400 pointer-events-none">
                  {icons.mail}
                </div>
                <input
                  type="email"
                  required
                  name="email"
                  maxLength={255}
                  disabled={isSubmitting}
                  className="w-full pl-10 pr-4 py-2.5 rounded-lg bg-gray-50 border border-gray-300 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors disabled:opacity-50"
                  placeholder="giaovien@truong.edu.vn"
                  value={formData.email}
                  onChange={handleFormChange}
                />
              </div>
            </label>

            {/* Mật khẩu */}
            <div>
              <label className="block">
                <span className="font-semibold text-sm text-gray-700 block mb-1.5">
                  Mật khẩu
                </span>

                <div id="password-input" className="relative">
                  <div className="absolute inset-y-0 left-0 flex items-center justify-center w-10 text-gray-400 pointer-events-none">
                    {icons.lock}
                  </div>

                  <button
                    type="button"
                    tabIndex={-1}
                    aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                    className="absolute inset-y-0 right-0 flex items-center justify-center w-10 text-gray-500 hover:text-gray-700 cursor-pointer"
                    onClick={handleShowPasswordClick}
                  >
                    {showPassword ? icons.eyeClose : icons.eye}
                  </button>

                  <input
                    type={showPassword ? "text" : "password"}
                    required
                    name="password"
                    minLength={8}
                    maxLength={32}
                    disabled={isSubmitting}
                    className="w-full pl-10 pr-12 py-2.5 rounded-lg bg-gray-50 border border-gray-300 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-colors disabled:opacity-50"
                    placeholder="Tối thiểu 8 ký tự"
                    value={formData.password}
                    onChange={handleFormChange}
                  />
                </div>
              </label>

              <div>
                <Link
                  href={"/auth/forgot-password"}
                  className="italic text-sm text-gray-700"
                >
                  Quên mật khẩu
                </Link>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full mt-2 bg-blue-600 text-white font-semibold py-2.5 rounded-lg hover:bg-blue-700 transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center cursor-pointer text-sm"
            >
              {isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}
            </button>
          </form>

          {/* Đường phân cách */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-white px-3 text-gray-500 font-medium">
                hoặc
              </span>
            </div>
          </div>

          {/* Đăng ký với Google */}
          <button
            type="button"
            disabled={isSubmitting}
            className="w-full py-2.5 border border-gray-300 rounded-lg flex items-center justify-center gap-2 font-medium text-gray-700 hover:bg-gray-50 transition-colors cursor-pointer disabled:opacity-50 text-sm"
          >
            {icons.google}
            <span>Tiếp tục với Google</span>
          </button>

          <div className="mt-6 flex justify-center gap-1.5 text-sm">
            <span className="text-gray-600">Đã có tài khoản?</span>
            <Link
              href="/auth/register"
              className="text-blue-600 font-semibold hover:underline"
            >
              Đăng ký ngay
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
