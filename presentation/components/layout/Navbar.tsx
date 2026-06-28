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

export default function NavBar({ avatarUrl, username }: Data) {
  const { isLoadLogout, menu, isUserMenuOpen, toggleUserMenu, userSectionRef } =
    useNavBar();

  const pathname = usePathname();

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

  return (
    <nav className="fixed z-20 bg-white top-0 left-0 h-screen hidden lg:block lg:w-60 shadow-md px-3 py-6">
      <div className="h-full flex flex-col justify-between">
        <section id="content">
          <div className="px-3 mb-10">
            <Logo />
          </div>

          <div className="flex flex-col gap-4">
            {navLinks.map((link) => {
              if (link.href) {
                const curLink = link.href === pathname;
                return (
                  <Link
                    key={link.name}
                    href={link.href}
                    className={`flex items-center gap-3 text-sm px-3 py-2 rounded-md transition-colors ${curLink ? "bg-blue-500 text-white" : "text-gray-500 hover:bg-gray-200 hover:text-black"}`}
                  >
                    {link.icon}
                    {link.name}
                  </Link>
                );
              }
            })}
          </div>
        </section>

        <section
          id="user"
          ref={userSectionRef}
          className="border-t border-gray-200 pt-6 relative select-none"
        >
          {isUserMenuOpen && (
            <div className="absolute bottom-full left-0 mb-3 w-full bg-white rounded-lg shadow-xl border border-gray-100 py-1.5 z-30 animate-in fade-in slide-in-from-bottom-2 duration-150">
              {menu.map((item, idx) => (
                <button
                  key={idx}
                  onClick={item.onClick}
                  className="w-full flex items-center gap-3 px-3.5 py-2.5 text-sm font-medium text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition-colors text-left"
                >
                  {item.icon}
                  {item.label}
                </button>
              ))}
            </div>
          )}

          <div
            onClick={toggleUserMenu}
            className={`cursor-pointer p-1.5 -mx-1.5 rounded-lg ${pathname === "/me" ? "bg-blue-500 hover:bg-blue-500/90 transition-colors" : "hover:bg-gray-100 transition-colors"}`}
          >
            <div id="user-avatar" className="flex items-center gap-4">
              <Image
                src={avatarUrl}
                alt="avatar-user"
                width={40}
                height={40}
                className={`h-10 w-10 rounded-full object-cover ${pathname === "/me" ? "" : "border border-gray-200"}`}
              />
              <div className="overflow-hidden">
                <p className={`font-bold text-sm ${pathname === "/me" ? "text-white" : "text-gray-800 truncate"}`}>
                  {username}
                </p>
                <p className={`text-xs ${pathname === "/me" ? "text-white" : "text-gray-400"}`}>
                  Quản lý tài khoản
                </p>
              </div>
            </div>
          </div>
        </section>

        {isLoadLogout && (
          <section
            id="logout-theme"
            className="fixed inset-0 z-50 bg-black/50 h-screen w-screen flex justify-center items-center"
          >
            <div className="flex flex-col items-center gap-5">
              <h2 className="text-white text-2xl font-bold">
                Đang đăng xuất. Đợi xí!
              </h2>
              <div className="loader-white"></div>
            </div>
          </section>
        )}
      </div>
    </nav>
  );
}
