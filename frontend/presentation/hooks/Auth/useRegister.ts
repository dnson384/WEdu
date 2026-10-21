"use client";
import { RegisterPayload } from "@/presentation/schemas/auth.schema";
import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { useRouter } from "next/navigation";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";

interface FormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export default function useRegister() {
  const router = useRouter();
  const authService = useMemo(() => new AuthService(), []);

  const [formData, setFormData] = useState<FormData>({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const handleFormChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const notiPassword = useMemo(() => {
    if (
      formData.confirmPassword.length > 0 &&
      formData.password !== formData.confirmPassword
    ) {
      return "Mật khẩu xác nhận không trùng khớp!";
    }
    return "";
  }, [formData.password, formData.confirmPassword]);

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmedUsername = formData.username.trim();
    const trimmedEmail = formData.email.trim();
    const { password, confirmPassword } = formData;

    // 1. Kiểm tra rỗng
    if (!trimmedUsername || !trimmedEmail || !password || !confirmPassword) {
      setError("Vui lòng điền đầy đủ tất cả các trường");
      return;
    }

    // 2. Kiểm tra khớp mật khẩu
    if (password !== confirmPassword) {
      setError("Mật khẩu xác nhận không trùng khớp");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    const payload: RegisterPayload = {
      username: trimmedUsername,
      email: trimmedEmail,
      plainPassword: password,
      confirmPassword: confirmPassword,
      loginMethod: "LOCAL",
    };

    try {
      const authorized = await authService.register(payload);
      if (authorized) {
        router.replace("/");
      }
    } catch (err) {
      if (isAxiosError(err)) {
        setError(err.response?.data.message);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!error) return;

    const timer = setTimeout(() => {
      setError(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [error]);

  return {
    formData,
    handleFormChange,
    notiPassword,
    error,
    showPassword,
    isSubmitting,
    handleShowPasswordClick,
    handleRegisterSubmit,
  };
}
