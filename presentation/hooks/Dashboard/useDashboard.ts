import { useRouter } from "next/navigation";
import React from "react";

export default function useDashboard() {
  const router = useRouter();

  const redirect = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    router.push(`/${event.currentTarget.id}`);
  };

  return { redirect };
}
