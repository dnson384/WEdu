"use client";

import { ReactNode } from "react";
import Link from "next/link";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import NavBar from "@/presentation/components/layout/Navbar";
import Loader from "@/presentation/components/layout/Loader";

export default function ModeratorLayout({ children }: { children: ReactNode }) {
  const { user, isLoadingUser } = useAuth();
  if (isLoadingUser) return <Loader />;
  if (user.role !== "ROLE_MODERATOR") {
    return <main className="min-h-screen bg-blue-50/60 flex items-center justify-center p-6">
      <div className="rounded-2xl bg-white border border-gray-200 p-8 text-center max-w-md">
        <h1 className="text-xl font-bold text-gray-900">Không có quyền truy cập</h1>
        <p className="mt-3 text-gray-500">Trang này dành cho người kiểm duyệt câu hỏi.</p>
        <Link href={user.id ? "/" : "/auth/login"} className="mt-6 inline-block text-blue-600 font-medium">
          {user.id ? "Về trang chủ" : "Đăng nhập"}
        </Link>
      </div>
    </main>;
  }
  return <div className="min-h-screen bg-blue-50/60">
    <NavBar avatarUrl={user.avatarUrl} username={user.username} />
    <main className="lg:ml-60 px-5 py-8 sm:px-10 lg:p-12">{children}</main>
  </div>;
}
