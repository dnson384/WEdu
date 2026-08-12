"use client";
import Link from "next/link";

import { icons } from "@/presentation/common/icons";
import useRegister from "@/presentation/hooks/Auth/useRegister";
import Error from "@/presentation/components/layout/Error";

export default function Register() {
  const {
    formData,
    handleFormChange,
    notiPassword,
    error,
    showPassword,
    handleShowPasswordClick,
    handleRegisterSubmit,
  } = useRegister();

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
              Bắt đầu hành trình tạo đề chuyên nghiệp
            </h2>
            <h4 className="text-white text-2xl mt-8">
              Đăng ký miễn phí và tận hưởng đầy đủ tính năng tạo đề, không giới
              hạn.
            </h4>

            <div className="my-20 flex flex-col gap-2">
              {[
                {
                  num: "01",
                  label: "Tạo tài khoản",
                  sub: "Đăng ký chỉ mất 1 phút",
                },
                {
                  num: "02",
                  label: "Tạo đề ngay",
                  sub: "Hệ thống tự tạo đề theo Công văn 7991",
                },
                {
                  num: "03",
                  label: "Quản lý đề",
                  sub: "Quản lý toàn bộ đề đã tạo",
                },
              ].map((item) => (
                <div key={item.label} className="flex gap-3">
                  <div className="text-white/80 text-xl">{item.num}</div>
                  <div>
                    <p className="text-white font-medium text-lg">
                      {item.label}
                    </p>
                    <p className="text-white/50">{item.sub}</p>
                  </div>
                </div>
              ))}
            </div>

            <p className="text-blue-200 text-sm">
              © 2026 Hệ Thống Tạo Đề Tự Động
            </p>
          </div>
        </div>
      </section>

      <section className="m-auto select-none">
        {error && (
          <div className="absolute top-20">
            <Error message={error} />
          </div>
        )}

        <div>
          <div className="mb-8">
            <h4 className="text-2xl font-bold">Tạo tài khoản mới</h4>
            <p className="text-gray-500 text-lg">
              Điền thông tin cơ bản để bắt đầu
            </p>
          </div>

          {/* Form Đăng nhập */}
          <form className="flex flex-col gap-4" onSubmit={handleRegisterSubmit}>
            {/* Họ và tên */}
            <label>
              <p className="font-bold mb-2">Họ và tên</p>
              <div className="relative">
                <div className="absolute h-full flex items-center justify-center w-10">
                  {icons.user}
                </div>
                <input
                  type="text"
                  required
                  name="username"
                  className="w-lg pl-10 pr-4 py-2.5 rounded-lg bg-gray-100 border border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-colors"
                  placeholder="Nguyen Van A"
                  value={formData.username}
                  onChange={handleFormChange}
                />
              </div>
            </label>

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
                  className="w-lg pl-10 pr-4 py-2.5 rounded-lg bg-gray-100 border border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-colors"
                  placeholder="Tối thiểu 8 ký tự"
                  value={formData.password}
                  onChange={handleFormChange}
                />
              </div>
            </label>

            {/* Xác nhận mật khẩu */}
            <label>
              <p className="font-bold mb-2">Xác nhận mật khẩu</p>
              <div className="relative">
                <div className="absolute h-full flex items-center justify-center w-10">
                  {icons.lock}
                </div>
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  name="confirmPassword"
                  minLength={8}
                  maxLength={32}
                  className="w-lg pl-10 pr-4 py-2.5 rounded-lg bg-gray-100 border border-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-colors"
                  placeholder="Nhập lại mật khẩu"
                  value={formData.confirmPassword}
                  onChange={handleFormChange}
                />
              </div>
              {notiPassword.trim().length > 0 && (
                <p className="text-red-500 text-xs">{notiPassword}</p>
              )}
            </label>

            <button
              type="submit"
              className="bg-blue-500 text-white font-bold py-2.5 rounded-lg hover:bg-blue-600"
            >
              Đăng ký
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
            <p>Đã có tài khoản?</p>
            <Link href={"/auth/login"}>
              <p className="text-blue-500 font-medium hover:underline">
                Đăng nhập ngay
              </p>
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
