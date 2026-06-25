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
  const {} = useNavBar();

  const pathname = usePathname();

  const imgUrl = `/api/image${avatarUrl}`;

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

        <section id="user" className="border-t border-gray-200 pt-6">
          <div>
            <div id="user-avatar" className="flex items-center gap-5">
              <Image
                src={imgUrl}
                alt="avatar-user"
                width={0}
                height={0}
                className="h-auto w-10"
              />
              <p className="font-bold">{username}</p>
            </div>
          </div>
        </section>
      </div>
    </nav>
  );
}
