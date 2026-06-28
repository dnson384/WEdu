import { icons } from "@/presentation/common/icons";
import { LogoutService } from "@/presentation/services/auth.service";
import { isAxiosError } from "axios";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

export default function useNavBar() {
  const router = useRouter();

  let [isLoadLogout, setIsLoadLogout] = useState<boolean>(false);
  let [isLogedOut, setIsLogedout] = useState<boolean>(false);

  const handleLogoutClick = async () => {
    try {
      setIsLoadLogout(true);
      const data = await LogoutService();
      if (data === true) {
        setIsLogedout(true);
      }
    } catch (err) {
      if (isAxiosError(err)) {
        console.log(err.response?.data.message);
      } else {
        console.error(err);
      }
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

  const [isUserMenuOpen, setIsUserMenuOpen] = useState<boolean>(false);
  const userSectionRef = useRef<HTMLDivElement>(null);

  const toggleUserMenu = () => {
    setIsUserMenuOpen((prev) => !prev);
  };

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

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isLoadLogout && isLogedOut) {
        isLoadLogout = false;
        router.refresh();
      }
    }, 1500);

    return () => clearTimeout(timer);
  }, [isLogedOut]);

  return {
    isLoadLogout,
    menu,
    isUserMenuOpen,
    toggleUserMenu,
    userSectionRef,
  };
}
