"use client";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export default function useRegister() {
  const [formData, setFormData] = useState<FormData>({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const handleFormChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const [notiPassword, setNotiPassword] = useState<String>("");

  useEffect(() => {
    if (
      formData.password != formData.confirmPassword &&
      formData.confirmPassword.trim().length > 0
    ) {
      setNotiPassword("Mật khẩu xác nhận không trùng khớp!");
    } else {
      setNotiPassword("");
    }
  }, [formData.confirmPassword]);

  const [showPassword, setShowPassword] = useState<boolean>(false);

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleRegisterSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (formData.confirmPassword != formData.password) {
      return;
    }
    alert(`${formData.email} - ${formData.password}`);
  };

  return {
    // Data input
    formData,
    handleFormChange,

    // Password notification
    notiPassword,

    // Show password
    showPassword,
    handleShowPasswordClick,
    handleRegisterSubmit,
  };
}
