"use client";

import { ExamDetailReponseEntity } from "@/domain/entities/exam.entity";
import { ExportPayload } from "@/presentation/schemas/export.schema";
import {
  deleteExamService,
  exportWordFileService,
  GetExamService,
} from "@/presentation/services/exam.service";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

export default function useExam() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const pathname = usePathname();
  const pathnameSplitted = pathname.split("/");
  const examId = pathnameSplitted[pathnameSplitted.length - 1];

  const searchParams = useSearchParams();
  const errorParam = searchParams.get("errors");
  const [errorsList, setErrorsList] = useState<string[]>(() =>
    errorParam ? errorParam.split("_||_") : [],
  );

  useEffect(() => {
    if (errorsList.length === 0) return;

    const timer = setTimeout(() => {
      setErrorsList([]);
    }, 5000);

    return () => clearTimeout(timer);
  }, []);

  const initialResponse = (): ExamDetailReponseEntity => ({
    id: "",
    name: "",
    groups: [],
  });

  const { data, isLoading } = useQuery({
    queryKey: ["exam", examId],
    queryFn: () => GetExamService(examId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: !!examId,
  });

  const details = data || initialResponse();

  const [isSubmitted, setIsSubmitted] = useState<boolean>(false);

  const handleExportDocx = async () => {
    if (isSubmitted) return;

    setIsSubmitted(true);

    const payload: ExportPayload = {
      examId: details.id,
      examName: details.name,
    };

    try {
      const blob = await exportWordFileService(payload);

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${details.name}.docx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Lỗi khi tải file UI:", err);
    } finally {
      setIsSubmitted(false);
    }
  };

  const handleDeleteClick = async () => {
    if (isSubmitted) return;

    setIsSubmitted(true);

    try {
      const response = await deleteExamService(examId);

      if (response) {
        await queryClient.invalidateQueries({ queryKey: ["all_exam"] })
        router.replace("/exam/all");
      }
    } catch (error) {
      console.error("Lỗi khi xóa đề", error);
    } finally {
      setIsSubmitted(false);
    }
  };

  return {
    details,
    isLoading,
    errorsList,
    handleExportDocx,
    handleDeleteClick,
  };
}
