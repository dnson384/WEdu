import { ExamResponseEntity } from "@/domain/entities/exam.entity";
import { getAllExamsService } from "@/presentation/services/exam.service";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

const EMPTY_EXAMS: ExamResponseEntity[] = [];

export default function useAllExam() {
  const router = useRouter();

  const { data, isLoading } = useQuery({
    queryKey: ["all_exam"],
    queryFn: () => getAllExamsService(),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const exams: ExamResponseEntity[] = data || EMPTY_EXAMS;

  const handleCardClick = (examId: string) => {
    router.push(`/exam/${examId}`);
  };

  return {
    exams,
    isLoading,
    handleCardClick,
  };
}
