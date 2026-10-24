import { LoginPayload } from "@/presentation/schemas/auth.schema";
import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useRouter } from "next/navigation";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { UserService } from "@/presentation/services/UserService";

interface FormData {
  email: string;
  password: string;
}

export default function useLogin() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const authService = useMemo(() => new AuthService(), []);
  const userService = useMemo(() => new UserService(), []);

  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });

  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const handleFormChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmedEmail = formData.email.trim();
    const password = formData.password;

    if (!trimmedEmail || !password) {
      setError("Vui lòng nhập đầy đủ email và mật khẩu");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    const payload: LoginPayload = {
      email: trimmedEmail,
      plainPassword: password,
    };

    try {
      const authorized = await authService.login(payload);
      if (authorized) {
        await queryClient.cancelQueries();
        queryClient.clear();
        const user = await queryClient.fetchQuery({
          queryKey: ["user"],
          queryFn: () => userService.getMe(),
          staleTime: 0,
        });
        router.replace(user.role === "ROLE_MODERATOR" ? "/moderator/question-files" : "/");
        router.refresh();
      }
    } catch (err) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setError(
          resMessage || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin",
        );
      } else {
        setError("Đã có lỗi xảy ra. Vui lòng thử lại sau");
      }
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
    showPassword,
    error,
    isSubmitting,
    handleFormChange,
    handleShowPasswordClick,
    handleLoginSubmit,
  };
}
