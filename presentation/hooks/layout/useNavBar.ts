import { icons } from "@/presentation/common/icons";
import axios from "axios";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";

export default function useNavBar() {
  const router = useRouter();

  const handleLogoutClick = async () => {
    try {
      const response = axios
    }
  }

  const menu = [
    {
      label: "Thông tin cá nhân",
      icon: icons.user,
      onClick: () => {
        router.push("/user/me");
      },
    },
    {
      label: "Đăng xuất",
      icon: icons.logout,
      onclick: () => {

      }
    },
  ];
  const [isUserMenuOpen, setIsUserMenuOpen] = useState<boolean>(false);
  const userSectionRef = useRef<HTMLDivElement>(null);
  return {};
}
