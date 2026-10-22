import { ExamResponseEntity } from "@/domain/entities/exam.entity";
import { ExamService } from "@/presentation/services/ExamService";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useMemo } from "react";

export default function useAllExam() {
  const router = useRouter();
  const examService = useMemo(() => new ExamService(), []);

  const { data, isLoading } = useQuery({
    queryKey: ["user-exams"],
    queryFn: () => examService.getAllExams(),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const exams: ExamResponseEntity[] = data || [];

  const handleCardClick = (examId: string) => {
    router.push(`/exam/${examId}`);
  };

  return {
    exams,
    isLoadingExams: isLoading,
    handleCardClick,
  };
}
