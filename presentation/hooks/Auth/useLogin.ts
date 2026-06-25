import { LoginPayload } from "@/presentation/schemas/auth.schema";
import { LoginService } from "@/presentation/services/auth.service";
import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  email: string;
  password: string;
}

export default function useLogin() {
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });

  const handleFormChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const [showPassword, setShowPassword] = useState<boolean>(false);

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const payload: LoginPayload = {
      email: formData.email,
      plainPassword: formData.password,
    };

    try {
      const authorized = await LoginService(payload);
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
    formData,
    handleFormChange,
    showPassword,
    handleShowPasswordClick,
    error,
    handleLoginSubmit,
  };
}
