"use client";

import Link from "next/link";
import Image from "next/image";

import Logo from "../common/Logo";
import { icons } from "@/presentation/common/icons";
import { usePathname } from "next/navigation";
import useNavBar from "@/presentation/hooks/layout/useNavBar";

interface Data {
  avatarUrl: string;
  username: string;
}

const DEFAULT_AVATAR = "/frontend/public/default-avatar-user.png";

const navLinks = [
  {
    name: "Tổng quan",
    href: "/",
    icon: icons.dashboard,
  },
  {
    name: "Tạo đề kiểm tra",
    href: "/generate",
    icon: icons.generateNavGray,
  },
  {
    name: "Danh sách đề",
    href: "/exam/all",
    icon: icons.book,
  },
  {
    name: "Tải lên câu hỏi",
    href: "/upload",
    icon: icons.database,
  },
];

export default function NavBar({ avatarUrl, username }: Data) {
  const { isLoggingOut, menu, isUserMenuOpen, toggleUserMenu, userSectionRef } =
    useNavBar();

  const pathname = usePathname();

  const isLinkActive = (href: string) => {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  };

  return (
    <nav className="fixed z-40 bg-white top-0 left-0 h-screen hidden lg:block lg:w-60 shadow-md px-3 py-6">
      <div className="h-full flex flex-col justify-between">
        <section id="content">
          <div className="px-3 mb-10">
            <Logo />
          </div>

          <div className="flex flex-col gap-2">
            {navLinks.map((link) => {
              const active = isLinkActive(link.href);

              return (
                <Link
                  key={link.name}
                  href={link.href}
                  className={`flex items-center gap-3 text-sm px-3 py-2.5 rounded-lg font-medium transition-colors ${
                    active
                      ? "bg-blue-600 text-white shadow-xs"
                      : "text-gray-600 hover:bg-gray-100 hover:text-black"
                  }`}
                >
                  <span className={active ? "text-white" : "text-gray-500"}>
                    {link.icon}
                  </span>
                  <span>{link.name}</span>
                </Link>
              );
            })}
          </div>
        </section>

        <section
          id="user"
          ref={userSectionRef}
          className="relative select-none"
        >
          {isUserMenuOpen && (
            <div className="absolute bottom-full left-0 mb-3 w-full bg-white rounded-xl shadow-xl border border-gray-100 py-1.5 z-30 animate-in fade-in slide-in-from-bottom-2 duration-150">
              {menu.map((item, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={item.onClick}
                  className="w-full flex items-center gap-3 px-3.5 py-2.5 text-sm font-medium text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition-colors text-left cursor-pointer"
                >
                  <span className="text-gray-500">{item.icon}</span>
                  <span>{item.label}</span>
                </button>
              ))}
            </div>
          )}

          <div
            onClick={toggleUserMenu}
            className={`cursor-pointer p-2 rounded-xl transition-colors ${
              pathname.startsWith("/me")
                ? "bg-blue-600 text-white"
                : "hover:bg-gray-100 text-gray-800"
            }`}
          >
            <div id="user-avatar" className="flex items-center gap-3">
              <div className="relative h-10 w-10 shrink-0">
                <Image
                  src={
                    avatarUrl && avatarUrl.trim() !== ""
                      ? avatarUrl
                      : DEFAULT_AVATAR
                  }
                  alt={username}
                  width={40}
                  height={40}
                  quality={90}
                  className={`h-10 w-10 rounded-full object-cover ${
                    pathname.startsWith("/me")
                      ? "border-2 border-white"
                      : "border border-gray-200"
                  }`}
                />
              </div>

              <div className="overflow-hidden">
                <p
                  className={`font-semibold text-sm truncate ${
                    pathname.startsWith("/me") ? "text-white" : "text-gray-900"
                  }`}
                >
                  {username}
                </p>
                <p
                  className={`text-xs truncate ${
                    pathname.startsWith("/me")
                      ? "text-blue-100"
                      : "text-gray-500"
                  }`}
                >
                  Quản lý tài khoản
                </p>
              </div>
            </div>
          </div>
        </section>

        {isLoggingOut && (
          <div
            id="logout-theme"
            className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex justify-center items-center"
          >
            <div className="flex flex-col items-center gap-4 bg-gray-900/80 px-8 py-6 rounded-2xl shadow-2xl">
              <div className="loader-white"></div>
              <h2 className="text-white text-lg font-medium">
                Đang đăng xuất...
              </h2>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}
