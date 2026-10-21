"use client";

import { ResetPasswordPayload } from "@/presentation/schemas/auth.schema";
import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

export default function useResetPassword() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";

  const authService = useMemo(() => new AuthService(), []);

  const [formData, setFormData] = useState({
    newPassword: "",
    confirmPassword: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const notiPassword = useMemo(() => {
    if (
      formData.confirmPassword.length > 0 &&
      formData.newPassword !== formData.confirmPassword
    ) {
      return "Mật khẩu xác nhận không trùng khớp!";
    }
    return "";
  }, [formData.newPassword, formData.confirmPassword]);

  const toggleShowPassword = () => setShowPassword((prev) => !prev);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!token) {
      setErrorMessage("Liên kết không hợp lệ hoặc thiếu mã xác thực (token)!");
      return;
    }

    if (formData.newPassword.length < 8) {
      setErrorMessage("Mật khẩu mới phải có tối thiểu 8 ký tự!");
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setErrorMessage("Mật khẩu xác nhận không khớp!");
      return;
    }

    const payload: ResetPasswordPayload = {
      plainToken: token,
      newPassword: formData.newPassword,
      confirmNewPassword: formData.confirmPassword,
    };

    try {
      setIsSubmitting(true);
      const message = await authService.resetPassword(payload);

      setSuccessMessage(message || "Đặt lại mật khẩu thành công!");
      // Chuyển hướng về trang đăng nhập sau 2 giây
      setTimeout(() => {
        router.push("/auth/login");
      }, 2000);
    } catch (err) {
      if (isAxiosError(err)) {
        setErrorMessage(
          err.response?.data?.message ||
            "Không thể đặt lại mật khẩu. Liên kết có thể đã hết hạn!",
        );
      } else {
        setErrorMessage("Đã có lỗi xảy ra. Vui lòng thử lại sau!");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!errorMessage && !successMessage && !token) {
      return;
    } else {
      const timer = setTimeout(() => {
        setErrorMessage(null);
        setSuccessMessage(null);
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [errorMessage, successMessage, token]);

  return {
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
  };
}
