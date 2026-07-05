"use client";
import { RegisterPayload } from "@/presentation/schemas/auth.schema";
import { RegisterService } from "@/presentation/services/auth.service";
import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export default function useRegister() {
  const [error, setError] = useState<string | null>(null);

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

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const payload: RegisterPayload = {
      email: formData.email,
      plainPassword: formData.password,
      confirmPassword: formData.confirmPassword,
      username: formData.username ,
      loginMethod: "LOCAL",
    };

    try {
      const authorized = await RegisterService(payload);
      if (authorized) {
        window.location.replace("/");
      }
    } catch (err) {
      if (isAxiosError(err)) {
        setError(err.response?.data.message);
      }
    }
  };

  useEffect(() => {
    if (error !== null) {
      const timer = setTimeout(() => {
        setError(null);
      }, 2000);

      return () => clearTimeout(timer);
    }
  }, [error]);

  return {
    // Data input
    formData,
    handleFormChange,

    // Password notification
    notiPassword,

    // Error
    error,

    // Show password
    showPassword,
    handleShowPasswordClick,
    handleRegisterSubmit,
  };
}
