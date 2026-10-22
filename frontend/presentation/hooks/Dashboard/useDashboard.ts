"use client";

import { DraftService } from "@/presentation/services/DraftService";
import { ExamService } from "@/presentation/services/ExamService";
import { useQueries } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useMemo } from "react";

export default function useDashboard() {
  const router = useRouter();
  const draftService = useMemo(() => new DraftService(), []);
  const examService = useMemo(() => new ExamService(), []);

  const results = useQueries({
    queries: [
      {
        queryKey: ["draft-recent"],
        queryFn: () => draftService.getRecent(),
        staleTime: 1000 * 60 * 5,
        retry: false,
        refetchOnMount: true,
        refetchOnWindowFocus: false,
      },
      {
        queryKey: ["exam-recent"],
        queryFn: () => examService.getRecent(),
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
