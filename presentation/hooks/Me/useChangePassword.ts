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

  const [notiNewPassword, setNotiNewPassword] = useState<String>("");

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

  const handleChangePasswordSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    alert(
      `oldPw: ${passwordFormData.oldPassword} \nnewPw: ${passwordFormData.newPassword}`,
    );
  };
  return {
    passwordFormData,
    handleFormPasswordChange,
    notiNewPassword,
    showPassword,
    handleShowPasswordClick,
    handleChangePasswordSubmit,
  };
}
