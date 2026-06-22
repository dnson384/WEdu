import { UserEntity } from "@/domain/entities/user.entity";
import { getMeService } from "@/presentation/services/user.service";
import { useQuery } from "@tanstack/react-query";

export default function useNavBar() {
  const initialUserData = (): UserEntity => ({
    id: "",
    email: "",
    username: "",
    role: "",
    avatarUrl: "",
  });

  const { data, isLoading } = useQuery<UserEntity>({
    queryKey: ["user-profile"],
    queryFn: () => getMeService(),
  });

  const user: UserEntity = data ? data : initialUserData();
  return { user, isLoading };
}
