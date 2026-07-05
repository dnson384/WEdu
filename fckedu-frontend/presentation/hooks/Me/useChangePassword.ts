import { ChangePasswordPayload } from "@/presentation/schemas/userSchema";
import { ChangePasswordService } from "@/presentation/services/user.service";
import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export default function useChangePassword() {
  const [passwordFormData, setPasswordFormData] = useState<FormData>({
    oldPassword: "",
    newPassword: "",
    confirmNewPassword: "",
  });

  const handleFormPasswordChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setPasswordFormData((prev) => ({ ...prev, [name]: value }));
  };

  const [notiNewPassword, setNotiNewPassword] = useState<string>("");

  const [changePasswordError, setChangePasswordError] = useState<string | null>(
    null,
  );
  const [changePasswordSuccess, setChangePasswordSuccess] = useState<
    string | null
  >(null);

  useEffect(() => {
    if (
      passwordFormData.newPassword != passwordFormData.confirmNewPassword &&
      passwordFormData.newPassword.trim().length > 0
    ) {
      setNotiNewPassword("Mật khẩu xác nhận không trùng khớp!");
    } else {
      setNotiNewPassword("");
    }
  }, [passwordFormData.confirmNewPassword]);

  const [showPassword, setShowPassword] = useState<boolean>(false);

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleChangePasswordSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (passwordFormData.newPassword !== passwordFormData.confirmNewPassword) {
      return;
    }

    try {
      const payload: ChangePasswordPayload = {
        oldPassword: passwordFormData.oldPassword,
        newPassword: passwordFormData.newPassword,
        confirmNewPassword: passwordFormData.confirmNewPassword,
      };

      const response = await ChangePasswordService(payload);

      if (response === true || response === false) {
        setChangePasswordSuccess(
          "Đổi mật khẩu thành công! Các thiết bị khác đã bị đăng xuất",
        );
      }
    } catch (err) {
      if (isAxiosError(err)) {
        const message = err.response?.data.message;
        setChangePasswordError(message);
      }
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (changePasswordError) {
        setChangePasswordError(null);
      } else if (changePasswordSuccess) {
        setChangePasswordSuccess(null);
      }
      setPasswordFormData({
        oldPassword: "",
        newPassword: "",
        confirmNewPassword: "",
      });
    }, 2000);

    return () => clearTimeout(timer);
  }, [changePasswordError, changePasswordSuccess]);

  return {
    changePasswordError,
    changePasswordSuccess,
    passwordFormData,
    handleFormPasswordChange,
    notiNewPassword,
    showPassword,
    handleShowPasswordClick,
    handleChangePasswordSubmit,
  };
}
