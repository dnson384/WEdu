"use client";

import { ReactNode } from "react";
import Link from "next/link";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import NavBar from "@/presentation/components/layout/Navbar";
import Loader from "@/presentation/components/layout/Loader";

export default function AdminLayout({ children }: { children: ReactNode }) {
  const { user, isLoadingUser } = useAuth();
  if (isLoadingUser) return <Loader />;
  if (user.role !== "ROLE_ADMIN")
    return (
      <main className="min-h-screen grid place-items-center bg-slate-50 p-6">
        <div className="rounded-2xl bg-white border p-8 text-center">
          <h1 className="text-xl font-bold">Không có quyền truy cập</h1>
          <p className="mt-3 text-slate-600">
            Trang này dành cho quản trị viên.
          </p>
          <Link
            href={user.id ? "/" : "/auth/login"}
            className="mt-5 inline-block text-blue-600"
          >
            {user.id ? "Về trang chủ" : "Đăng nhập"}
          </Link>
        </div>
      </main>
    );
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <NavBar avatarUrl={user.avatarUrl} username={user.username} />
      <main className="lg:ml-60 px-5 py-8 sm:px-8 lg:p-10">{children}</main>
    </div>
  );
}
