import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { useEffect, useMemo, useState } from "react";

export default function useForgotPassword() {
  const authService = useMemo(() => new AuthService(), []);

  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [countdown, setCountdown] = useState<number>(0);

  // Xử lý đếm ngược 1 giây mỗi nhịp
  useEffect(() => {
    if (countdown <= 0) return;

    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [countdown]);

  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (!email.trim()) {
      setErrorMessage("Vui lòng nhập địa chỉ email!");
      return;
    }

    try {
      setIsSubmitting(true);
      const responseMessage = await authService.forgotPassword(email);
      setSuccessMessage(responseMessage);
      setCountdown(90);
    } catch (err) {
      if (isAxiosError(err)) {
        setErrorMessage(
          err.response?.data?.message ||
            "Không thể gửi yêu cầu đặt lại mật khẩu!",
        );
      } else {
        setErrorMessage("Đã có lỗi xảy ra. Vui lòng thử lại sau!");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!errorMessage && !successMessage) {
      return;
    }

    const timer = setTimeout(() => {
      setErrorMessage(null);
      setSuccessMessage(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorMessage, successMessage]);

  return {
    email,
    isSubmitting,
    errorMessage,
    successMessage,
    countdown,
    handleEmailChange,
    handleSubmit,
  };
}
