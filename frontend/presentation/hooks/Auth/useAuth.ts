"use client";

import { useQuery } from "@tanstack/react-query";
import { getMeService } from "@/presentation/services/user.service";
import { UserEntity } from "@/domain/entities/user.entity";

export const useAuth = () => {
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
    queryFn: async () => getMeService(),
    staleTime: 5 * 60 * 1000,
  });

  const user: UserEntity = data ? data : initialUserData();

  return {
    user: user,
    isLoadingUser: isLoading,
  };
};
