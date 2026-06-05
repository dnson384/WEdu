import { PracticeEntity } from "@/domain/entities/practice.entity";
import { getAllPracticesService } from "@/presentation/services/practice.service";
import { useQuery } from "@tanstack/react-query";
import { AxiosError } from "axios";

export default function useAllPractices() {
  const {
    data: practices = [],
    isLoading,
    error,
  } = useQuery<PracticeEntity[], AxiosError<any>>({
    queryKey: ["categories"],
    queryFn: getAllPracticesService,
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const errorMessage: string | undefined = error?.response?.data.message;

  return { practices, isLoading, errorMessage };
}
