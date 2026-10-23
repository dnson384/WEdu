import {
  deleteAccoutService,
  lockAccoutService,
} from "@/presentation/services/user.service";
import { isAxiosError } from "axios";
import { useEffect, useState } from "react";

export default function useAccount() {
  const [accountError, setAccoutError] = useState<string | null>(null);
  const [accountSuccess, setAccoutSuccess] = useState<string | null>(null);

  const handleLockAccountClick = async () => {
    if (confirm("Xác nhận khóa tài khoản")) {
      try {
        const response = await lockAccoutService();

        if (response) {
          setAccoutSuccess(
            "Khóa thành công. Để mở lại xin vui lòng liên hệ xxx!",
          );
        }
      } catch (err) {
        if (isAxiosError(err)) {
          const message = err.response?.data.message;
          setAccoutError(message);
        }
      }
    }
  };

  const handleDeleteAccountClick = async () => {
    if (confirm("Xác nhận xóa tài khoản")) {
      try {
        const response = await deleteAccoutService();

        if (response) {
          setAccoutSuccess("Đã xóa tài khoản thành công!");
        }
      } catch (err) {
        if (isAxiosError(err)) {
          const message = err.response?.data.message;
          setAccoutError(message);
        }
      }
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (accountError) {
        setAccoutError(null);
      } else if (accountSuccess) {
        setAccoutSuccess(null);
        window.location.reload();
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [accountError, accountSuccess]);

  return {
    accountError,
    accountSuccess,
    handleLockAccountClick,
    handleDeleteAccountClick,
  };
}
