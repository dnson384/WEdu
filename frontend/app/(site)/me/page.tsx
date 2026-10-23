"use client";

import Image from "next/image";

import { icons } from "@/presentation/common/icons";

import NavBar from "@/presentation/components/layout/Navbar";
import Error from "@/presentation/components/layout/Error";

import useAuth from "@/presentation/hooks/Auth/useAuth";
import useUpdateAvatar from "@/presentation/hooks/Me/useUpdateAvatar";
import useUpdateUsername from "@/presentation/hooks/Me/useUpdateUsername";
import useChangePassword from "@/presentation/hooks/Me/useChangePassword";
import Success from "@/presentation/components/layout/Success";
import Loader from "@/presentation/components/layout/Loader";

const BLANK_IMAGE =
  "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

export default function Me() {
  const { user, isLoadingUser } = useAuth();

  const {
    avatarUrl,
    isUploading,
    updateAvatarError,
    updateAvatarSuccess,
    handleAvatarChange,
  } = useUpdateAvatar({
    initialUrl: user?.avatarUrl || BLANK_IMAGE,
  });

  const {
    updateMeError,
    updateMeSuccess,
    isEditing,
    handleToggleEdit,
    formData,
    isSubmitting,
    handleInputChange,
    handleSaveProfile,
  } = useUpdateUsername({
    username: user.username || "",
  });

  const {
    changePasswordError,
    changePasswordSuccess,
    passwordFormData,
    handleFormPasswordChange,
    notiNewPassword,
    showPassword,
    handleShowPasswordClick,
    handleChangePasswordSubmit,
  } = useChangePassword();

  return (
    <>
      {isLoadingUser ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-blue-50/50">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-full max-w-md px-4 pointer-events-none">
            {updateAvatarError && <Error message={updateAvatarError} />}
            {updateMeError && <Error message={updateMeError} />}
            {changePasswordError && <Error message={changePasswordError} />}
            {updateAvatarSuccess && <Success message={updateAvatarSuccess} />}
            {updateMeSuccess && <Success message={updateMeSuccess} />}
            {changePasswordSuccess && (
              <Success message={changePasswordSuccess} />
            )}
          </div>

          <main className="lg:ml-60 py-15 px-5 sm:px-10 md:px-15 lg:px-20 space-y-6">
            {/* Header Trang */}
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">
                Cài đặt tài khoản
              </h1>
              <p className="text-sm text-gray-500 mt-1">
                Quản lý thông tin hồ sơ cá nhân và bảo mật tài khoản của bạn
              </p>
            </div>

            <div
              id="user-data"
              className="mt-10 bg-white p-10 w-full rounded-xl flex flex-col gap-10"
            >
              <section className="flex justify-between">
                <div className="flex gap-10">
                  <label className="group relative block w-25 h-25 overflow-hidden rounded-full cursor-pointer">
                    <Image
                      src={avatarUrl}
                      alt="avatar"
                      fill
                      sizes="200px"
                      quality={100}
                      priority
                      className="object-cover transition-transform duration-300 group-hover:scale-105"
                    />

                    <div className="absolute inset-0 bg-black/45 flex flex-col items-center justify-center text-white opacity-0 transition-opacity duration-200 group-hover:opacity-100">
                      <span className="text-xs font-semibold">
                        {isUploading ? "Đang tải..." : icons.edit}
                      </span>
                    </div>

                    {isUploading && (
                      <div className="absolute inset-0 bg-black/60 flex items-center justify-center">
                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      </div>
                    )}

                    <input
                      type="file"
                      accept="image/*"
                      disabled={isUploading}
                      className="hidden"
                      onChange={handleAvatarChange}
                    />
                  </label>

                  <div>
                    <h4 className="text-xl font-bold">{user.username}</h4>
                    <p className="text-gray-500">{user.email}</p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleToggleEdit}
                  className={`h-fit px-4 py-2 text-xs font-semibold rounded-lg border transition-all flex items-center gap-1.5 self-start sm:self-auto ${
                    isEditing
                      ? "bg-slate-100 border-slate-300 text-slate-600 hover:bg-slate-200"
                      : "bg-blue-50 border-blue-200 text-blue-600 hover:bg-blue-100"
                  }`}
                >
                  {icons?.edit} {isEditing ? "Hủy chỉnh sửa" : "Chỉnh sửa"}
                </button>
              </section>

              <section>
                <form onSubmit={handleSaveProfile}>
                  <div className="grid grid-cols-2 gap-5">
                    {/* Email */}
                    <div>
                      <label>
                        <p className="font-bold mb-1.5">
                          Địa chỉ Email <span className="text-red-500">*</span>
                        </p>
                        <input
                          type="email"
                          value={user.email}
                          disabled={true}
                          className="w-full px-4 py-2.5 rounded-lg border border-slate-300 bg-slate-100 text-slate-500 font-medium cursor-not-allowed focus:outline-none"
                        />
                        <p className="text-xs text-slate-400 mt-1 italic">
                          Email dùng để đăng nhập nên không thể tự thay đổi.
                        </p>
                      </label>
                    </div>

                    {/* Username */}
                    <div>
                      <label>
                        <p className="font-bold mb-1.5">
                          Tên người dùng <span className="text-red-500">*</span>
                        </p>
                        <input
                          type="text"
                          name="username"
                          value={formData.username}
                          onChange={handleInputChange}
                          disabled={!isEditing}
                          placeholder="Nhập họ và tên hiển thị..."
                          className={`w-full px-4 py-2.5 rounded-lg border transition-all focus:outline-none ${
                            isEditing
                              ? "border-blue-500 bg-white"
                              : "border-slate-300 bg-slate-50"
                          }`}
                        />
                      </label>
                    </div>
                  </div>

                  {isEditing && (
                    <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                      <button
                        type="button"
                        onClick={handleToggleEdit}
                        className="px-5 py-2 rounded-xl text-sm font-semibold text-slate-500 hover:bg-slate-100 transition-colors"
                      >
                        Hủy
                      </button>
                      <button
                        type="submit"
                        disabled={isSubmitting}
                        className="px-6 py-2 rounded-xl text-sm font-semibold text-white bg-blue-500 hover:bg-blue-600 shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        Lưu thay đổi
                      </button>
                    </div>
                  )}
                </form>
              </section>

              <section>
                <div className="bg-white rounded-2xl border border-gray-200 py-6 shadow-sm">
                  <div className="border-b border-gray-300">
                    <h3 className="font-bold pb-6 px-8">Đổi mật khẩu</h3>
                  </div>

                  <form
                    onSubmit={handleChangePasswordSubmit}
                    className="w-lg py-6 px-8 flex flex-col gap-3"
                  >
                    <label>
                      <p className="mb-2">Mật khẩu cũ</p>
                      <div className="relative w-lg">
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
                          name="oldPassword"
                          minLength={8}
                          maxLength={32}
                          className="w-full px-10 py-2.5 rounded-lg border border-slate-300 bg-slate-50 focus:outline-none focus:border-blue-500 transition-colors"
                          placeholder="••••••••"
                          value={passwordFormData.oldPassword}
                          onChange={handleFormPasswordChange}
                        />
                      </div>
                    </label>

                    <label>
                      <p className="mb-2">Mật khẩu mới</p>
                      <div className="relative w-full">
                        <div className="absolute h-full flex items-center justify-center w-10">
                          {icons.lock}
                        </div>

                        <input
                          type={showPassword ? "text" : "password"}
                          required
                          name="newPassword"
                          minLength={8}
                          maxLength={32}
                          className="w-lg px-10 py-2.5 rounded-lg border border-slate-300 bg-slate-50 focus:outline-none focus:border-blue-500 transition-colors"
                          placeholder="Tối thiểu 8 ký tự"
                          value={passwordFormData.newPassword}
                          onChange={handleFormPasswordChange}
                        />
                      </div>
                    </label>

                    <label>
                      <p className="mb-2">Xác nhận mật khẩu mới</p>
                      <div className="relative w-full">
                        <div className="absolute h-full flex items-center justify-center w-10">
                          {icons.lock}
                        </div>

                        <input
                          type={showPassword ? "text" : "password"}
                          required
                          name="confirmNewPassword"
                          minLength={8}
                          maxLength={32}
                          className="w-lg px-10 py-2.5 rounded-lg border border-slate-300 bg-slate-50 focus:outline-none focus:border-blue-500 transition-colors"
                          placeholder="Nhập lại mật khẩu mới"
                          value={passwordFormData.confirmNewPassword}
                          onChange={handleFormPasswordChange}
                        />
                      </div>
                      {notiNewPassword.trim().length > 0 && (
                        <p className="text-red-500 text-xs">
                          {notiNewPassword}
                        </p>
                      )}
                    </label>

                    <button
                      type="submit"
                      className="w-lg bg-blue-600 text-white font-bold py-2.5 rounded-lg hover:bg-blue-700"
                    >
                      Cập nhật mật khẩu
                    </button>
                  </form>
                </div>
              </section>
            </div>
          </main>
        </div>
      )}
    </>
  );
}