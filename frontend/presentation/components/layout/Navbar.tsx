"use client";

import Link from "next/link";
import Image from "next/image";

import Logo from "../common/Logo";
import { icons } from "@/presentation/common/icons";
import { usePathname } from "next/navigation";
import useNavBar from "@/presentation/hooks/layout/useNavBar";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { useQueryClient } from "@tanstack/react-query";
import { Files, ShieldCheck, Library } from "lucide-react";

interface Data {
  avatarUrl: string;
  username: string;
}

const DEFAULT_AVATAR = "/default-avatar-user.png";

function resolveAvatarSrc(value: string | undefined) {
  const source = value?.trim();
  if (!source) return DEFAULT_AVATAR;
  if (source.startsWith("/") && !source.startsWith("//")) return source;
  try {
    const url = new URL(source);
    return url.protocol === "https:" || url.protocol === "http:" ? source : DEFAULT_AVATAR;
  } catch {
    return DEFAULT_AVATAR;
  }
}

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
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const isModerator = user.role === "ROLE_MODERATOR";
  const links = isModerator ? [
    { name: "Quản lý tệp tin câu hỏi", href: "/moderator/question-files", icon: <Files size={20} /> },
    { name: "Kiểm duyệt câu hỏi", href: "/moderator/review", icon: <ShieldCheck size={20} /> },
    { name: "Quản lý ngân hàng câu hỏi", href: "/moderator/question-bank", icon: <Library size={20} /> },
  ] : navLinks;
  const { isLoggingOut, menu, isUserMenuOpen, toggleUserMenu, userSectionRef } =
    useNavBar();

  const pathname = usePathname();

  const isLinkActive = (href: string) => {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  };

  return (
    <nav aria-label="Điều hướng chính" className={`${isModerator ? "relative lg:fixed lg:h-screen" : "fixed h-screen hidden lg:block"} z-40 bg-white top-0 left-0 lg:w-60 shadow-md px-3 py-6`}>
      <div className="h-full flex flex-col justify-between">
        <section id="content">
          <div className="px-3 mb-10">
            <Logo />
          </div>

          <div className="flex flex-col gap-2">
            {links.map((link) => {
              const active = isLinkActive(link.href);

              return (
                <Link
                  key={link.name}
                  href={link.href}
                  prefetch={false}
                  aria-current={active ? "page" : undefined}
                  onClick={() => {
                    if (link.href === "/moderator/question-files") {
                      void queryClient.invalidateQueries({ queryKey: ["moderator-question-files"] });
                    }
                  }}
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
                  src={resolveAvatarSrc(avatarUrl)}
                  alt={username}
                  width={40}
                  height={40}
                  quality={75}
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
