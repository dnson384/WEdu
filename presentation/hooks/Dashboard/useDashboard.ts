"use client";

import { GetRecentDraftService } from "@/presentation/services/draft.service";
import { getRecentExamService } from "@/presentation/services/exam.service";
import { useQueries } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

export default function useDashboard() {
  const router = useRouter();

  const results = useQueries({
    queries: [
      {
        queryKey: ["draft-recent"],
        queryFn: () => GetRecentDraftService(),
        staleTime: 1000 * 60 * 5,
        retry: false,
        refetchOnMount: true,
        refetchOnWindowFocus: false,
      },
      {
        queryKey: ["exam-recent"],
        queryFn: () => getRecentExamService(),
        staleTime: 1000 * 60 * 5,
        retry: false,
        refetchOnMount: true,
        refetchOnWindowFocus: false,
      },
    ],
  });

  // Data
  const [draftQuery, examQuery] = results;
  const recentDrafts = draftQuery.data ?? [];
  const recentExams = examQuery.data ?? [];

  const handleExamCardClick = (examId: string) => {
    router.push(`/exam/${examId}`);
  };

  const handleDraftCardClick = (draftId: string) => {
    router.push(`/generate/${draftId}`);
  };
  return {
    recentDrafts,
    recentExams,
    handleExamCardClick,
    handleDraftCardClick,
  };
}
