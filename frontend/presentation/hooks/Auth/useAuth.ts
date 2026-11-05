"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { UserEntity } from "@/domain/entities/user.entity";
import { useEffect, useMemo } from "react";
import { UserService } from "@/presentation/services/UserService";

export default function useAuth() {
  const queryClient = useQueryClient();
  const userService = useMemo(() => new UserService(), []);

  const initialUserData = (): UserEntity => ({
    id: "",
    email: "",
    username: "",
    role: "",
    avatarUrl: "",
    isActived: true,
    accountType: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["user"],
    queryFn: async () => userService.getMe(),
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (data?.accountType !== "PLUS" || !data.plusExpiresAt) return;
    const expire = () => {
      queryClient.setQueryData<UserEntity>(["user"], (current) =>
        current && current.plusExpiresAt === data.plusExpiresAt ? { ...current, accountType: "FREE" } : current,
      );
      void queryClient.invalidateQueries({ queryKey: ["user"] });
    };
    let timer: ReturnType<typeof setTimeout>;
    const schedule = () => {
      const remaining = new Date(data.plusExpiresAt!).getTime() - Date.now();
      if (remaining <= 0) { expire(); return; }
      timer = setTimeout(schedule, Math.min(remaining + 50, 2147483647));
    };
    schedule();
    return () => clearTimeout(timer);
  }, [data, queryClient]);

  const user: UserEntity = data ? data : initialUserData();

  return {
    user: user,
    isLoadingUser: isLoading,
  };
}
