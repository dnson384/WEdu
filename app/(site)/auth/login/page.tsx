"use client";
import Link from "next/link";

import { icons } from "@/presentation/common/icons";
import useLogin from "@/presentation/hooks/Auth/useLogin";
import Error from "@/presentation/components/layout/Error";

export default function Login() {
  const {
    formData,
    handleFormChange,
    showPassword,
    handleShowPasswordClick,
    error,
    handleLoginSubmit,
  } = useLogin();

  return (
    <main className="grid grid-cols-2 h-screen">
      <section className="bg-blue-500 p-20 relative">
        <Link
          href={"/"}
          className="absolute flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors mb-8 text-sm"
        >
          <span className="text-white">{icons.arrowBack}</span>
          <span className="text-white">Về trang chủ</span>
        </Link>

        <div className="flex items-center h-full">
          <div>
            <h2 className="w-sm text-4xl text-white font-bold">
              Nền tảng tạo đề chuyên nghiệp nhất cho giáo viên
            </h2>
            <h4 className="text-white text-2xl mt-8">
              Tuân thủ 100% chuẩn 7991 của Bộ Giáo dục. Tạo đề hoàn toàn miễn
              phí, nhanh chóng và chính xác.
            </h4>

            <div className="my-20 grid grid-cols-2 gap-4">
              {[
                {
                  icon: icons.shield24,
                  label: "Chuẩn 7991",
                  sub: "Bộ Giáo dục",
                },
                {
                  icon: icons.shield24,
                  label: "Tiết kiệm 80%",
                  sub: "Thời gian ra đề",
                },
                {
                  icon: icons.education,
                  label: "Miễn phí",
                  sub: "Tạo đề không giới hạn",
                },
                {
                  icon: icons.check24,
                  label: "Chất lượng",
                  sub: "Đảm bảo chuẩn đầu ra",
                },
              ].map((item) => (
                <div
                  key={item.label}
                  className="bg-white/10 rounded-xl p-4 flex flex-col gap-1"
                >
                  <div className="text-white">{item.icon}</div>
                  <p className="text-white font-medium text-sm">{item.label}</p>
                  <p className="text-blue-200 text-xs">{item.sub}</p>
                </div>
              ))}
            </div>

            <p className="text-blue-200 text-sm">
              © 2026 Hệ Thống Tạo Đề Tự Động
            </p>
          </div>
        </div>
      </section>

      <section className="flex justify-center items-center h-full relative select-none">
        <div className="absolute top-20">
          <Error message={error} />
        </div>

        <div>
          <div className="mb-8">
            <h4 className="text-2xl font-bold">Chào mừng trở lại</h4>
            <p className="text-gray-500 text-lg">Đăng nhập để tiếp tục</p>
          </div>

          {/* Form Đăng nhập */}
          <form className="flex flex-col gap-4" onSubmit={handleLoginSubmit}>
            {/* Email */}
            <label>
              <p className="font-bold mb-2">Email</p>
              <div className="relative">
                <div className="absolute h-full flex items-center justify-center w-10">
                  {icons.mail}
                </div>
                <input
                  type="email"
                  required
                  name="email"
                  maxLength={255}
                  className="w-lg pl-10 pr-4 py-2.5 rounded-lg bg-gray-100 border border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-colors"
                  placeholder="giaovien@truong.edu.vn"
                  value={formData.email}
                  onChange={handleFormChange}
                />
              </div>
            </label>

            {/* Mật khẩu */}
            <label>
              <p className="font-bold mb-2">Mật khẩu</p>
              <div className="relative">
                <div className="absolute h-full flex items-center justify-center w-10">
                  {icons.lock}
                </div>

                <div
                  className="absolute right-3 h-full flex items-center justify-center w-10 cursor-pointer z-10"
                  onClick={handleShowPasswordClick}
                >
                  {showPassword ? icons.eyeClose : icons.eye}
                </div>

                <input
                  type={showPassword ? "text" : "password"}
                  required
                  name="password"
                  minLength={8}
                  maxLength={32}
                  className="w-lg px-10 py-2.5 rounded-lg bg-gray-100 border border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-colors"
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={handleFormChange}
                />
              </div>
            </label>

            <button
              type="submit"
              className="bg-blue-500 text-white font-bold py-2.5 rounded-lg hover:bg-blue-600"
            >
              Đăng nhập
            </button>
          </form>

          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-background px-3 text-sm text-muted-foreground">
                hoặc
              </span>
            </div>
          </div>

          {/* Google */}
          <button
            type="button"
            className="w-full py-2.5 border border-gray-400 rounded-lg flex items-center justify-center gap-2 font-medium hover:bg-gray-100 transition-colors"
          >
            {icons.google}
            Tiếp tục với Google
          </button>

          <div className="mt-6 flex justify-center gap-1">
            <p>Chưa có tài khoản?</p>
            <Link href={"/auth/register"}>
              <p className="text-blue-500 font-medium hover:underline">
                Đăng ký miễn phí
              </p>
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
