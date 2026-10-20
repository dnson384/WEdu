"use client";

import { useQuery } from "@tanstack/react-query";
import { UserService } from "@/presentation/services/UserService";
import { useMemo } from "react";
import { UserEntity } from "@/domain/entities/user.entity";

export const useAuth = () => {
  const userService = useMemo(() => new UserService(), []);

  const initialUserData = (): UserEntity => ({
    id: "",
    email: "",
    username: "",
    role: "",
    avatarUrl: "",
    accountType: ""
  });

  const { data, isLoading } = useQuery({
    queryKey: ["user-profile"],
    queryFn: async () => userService.getMe(),
    staleTime: 5 * 60 * 1000,
  });

  const user: UserEntity = data ? data : initialUserData();

  return {
    user: user,
    isLoadingUser: isLoading,
  };
};
