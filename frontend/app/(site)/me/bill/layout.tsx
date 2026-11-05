"use client";

import type { ReactNode } from "react";
import NavBar from "@/presentation/components/layout/Navbar";
import useAuth from "@/presentation/hooks/Auth/useAuth";

export default function BillingLayout({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-blue-50/50">
      <NavBar avatarUrl={user.avatarUrl} username={user.username} />
      <div className="lg:ml-60">{children}</div>
    </div>
  );
}
