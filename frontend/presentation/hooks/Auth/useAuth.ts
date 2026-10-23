"use client";

import { useQuery } from "@tanstack/react-query";
import { UserEntity } from "@/domain/entities/user.entity";
import { useMemo } from "react";
import { UserService } from "@/presentation/services/UserService";

export default function useAuth() {
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

  const user: UserEntity = data ? data : initialUserData();

  return {
    user: user,
    isLoadingUser: isLoading,
  };
}
