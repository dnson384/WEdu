import { ChangePasswordPayload } from "@/presentation/schemas/auth.schema";
import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";

interface FormData {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

const initialFormData: FormData = {
  oldPassword: "",
  newPassword: "",
  confirmNewPassword: "",
};

export default function useChangePassword() {
  const authService = useMemo(() => new AuthService(), []);

  const [passwordFormData, setPasswordFormData] =
    useState<FormData>(initialFormData);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const [changePasswordError, setChangePasswordError] = useState<string | null>(
    null,
  );
  const [changePasswordSuccess, setChangePasswordSuccess] = useState<
    string | null
  >(null);

  const isMismatch =
    passwordFormData.confirmNewPassword.length > 0 &&
    passwordFormData.newPassword !== passwordFormData.confirmNewPassword;

  const notiNewPassword = isMismatch
    ? "Mật khẩu xác nhận không trùng khớp!"
    : "";

  const handleFormPasswordChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setPasswordFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleChangePasswordSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (
      !passwordFormData.oldPassword ||
      !passwordFormData.newPassword ||
      passwordFormData.newPassword !== passwordFormData.confirmNewPassword
    ) {
      return;
    }

    setChangePasswordError(null);
    setChangePasswordSuccess(null);
    setIsSubmitting(true);

    try {
      const payload: ChangePasswordPayload = {
        oldPassword: passwordFormData.oldPassword,
        newPassword: passwordFormData.newPassword,
        confirmNewPassword: passwordFormData.confirmNewPassword,
      };

      const response = await authService.changePassword(payload);

      if (response) {
        setChangePasswordSuccess(
          "Đổi mật khẩu thành công! Các thiết bị khác đã bị đăng xuất",
        );
        setPasswordFormData(initialFormData);
      }
    } catch (err) {
      if (isAxiosError(err)) {
        const message = err.response?.data?.message || "Đổi mật khẩu thất bại";
        setChangePasswordError(message);
      } else {
        setChangePasswordError("Đã có lỗi xảy ra, vui lòng thử lại sau");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!changePasswordError && !changePasswordSuccess) return;

    const timer = setTimeout(() => {
      if (changePasswordSuccess) {
        setChangePasswordSuccess(null);
      } else if (changePasswordError) {
        setChangePasswordError(null);
      }
    }, 3000);

    return () => clearTimeout(timer);
  }, [changePasswordError, changePasswordSuccess]);

  return {
    passwordFormData,
    notiNewPassword,
    showPassword,
    isSubmitting,
    changePasswordError,
    changePasswordSuccess,
    handleFormPasswordChange,
    handleShowPasswordClick,
    handleChangePasswordSubmit,
  };
}
