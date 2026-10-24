import { icons } from "@/presentation/common/icons";
import { AuthService } from "@/presentation/services/AuthService";
import { isAxiosError } from "axios";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";

export default function useNavBar() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const authService = useMemo(() => new AuthService(), []);

  const [isLoggingOut, setIsLoggingOut] = useState<boolean>(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState<boolean>(false);

  const userSectionRef = useRef<HTMLDivElement>(null);

  const toggleUserMenu = () => {
    setIsUserMenuOpen((prev) => !prev);
  };

  const handleLogoutClick = async () => {
    if (isLoggingOut) return;

    setIsLoggingOut(true);
    setIsUserMenuOpen(false);

    try {
      const data = await authService.logout();
      if (data) {
        await queryClient.cancelQueries();
        queryClient.clear();
        router.replace("/auth/login");
        router.refresh();
      } else {
        setIsLoggingOut(false);
      }
    } catch (err) {
      if (isAxiosError(err)) {
        console.error(err.response?.data?.message || "Đăng xuất thất bại");
      } else {
        console.error("Lỗi khi đăng xuất:", err);
      }
      setIsLoggingOut(false);
    }
  };

  const menu = [
    {
      label: "Thông tin cá nhân",
      icon: icons.user,
      onClick: () => {
        router.push("/me");
      },
    },
    {
      label: "Thanh toán",
      icon: icons.creditCard,
      onClick: () => {
        router.push("/me/bill");
      },
    },
    {
      label: "Đăng xuất",
      icon: icons.logout,
      onClick: () => {
        handleLogoutClick();
      },
    },
  ];

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        userSectionRef.current &&
        !userSectionRef.current.contains(event.target as Node)
      ) {
        setIsUserMenuOpen(false);
      }
    };

    if (isUserMenuOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isUserMenuOpen]);

  return {
    isLoggingOut,
    menu,
    isUserMenuOpen,
    toggleUserMenu,
    userSectionRef,
  };
}
