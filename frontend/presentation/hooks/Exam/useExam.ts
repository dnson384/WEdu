import { ExamDetailReponseEntity } from "@/domain/entities/exam.entity";
import { ExportPayload } from "@/presentation/schemas/export.schema";
import { ExamService } from "@/presentation/services/ExamService";
import { ExportService } from "@/presentation/services/ExportService";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

const initialExamDetail = (): ExamDetailReponseEntity => ({
  id: "",
  name: "",
  groups: [],
});

export default function useExam() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const params = useParams();
  const searchParams = useSearchParams();
  const examService = useMemo(() => new ExamService(), []);
  const exportService = useMemo(() => new ExportService(), []);

  const examId = (params?.examId as string) || "";

  const errorParam = searchParams.get("errors");
  const notiParam = searchParams.get("noti");

  const [errorsList, setErrorsList] = useState<string[]>(() =>
    errorParam ? errorParam.split("_||_") : [],
  );
  const [noti, setNoti] = useState<string | null>(notiParam);

  const {
    data: fetchedExamDetail,
    error: fetchError,
    isLoading: isLoadingExam,
  } = useQuery<ExamDetailReponseEntity, AxiosError<ApiError>>({
    queryKey: ["exam", examId],
    queryFn: () => examService.get(examId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: Boolean(examId),
  });

  const [examDetail, setExamDetail] =
    useState<ExamDetailReponseEntity>(initialExamDetail);
  const [isSubmitted, setIsSubmitted] = useState<boolean>(false);

  const [syncedExamId, setSyncedExamId] = useState<string | null>(null);
  if (fetchedExamDetail && fetchedExamDetail.id !== syncedExamId) {
    setSyncedExamId(fetchedExamDetail.id);
    setExamDetail(fetchedExamDetail);
  }

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/exam/all");
    }
  }, [fetchError, router]);

  const handleExportExam = async () => {
    setIsSubmitted(true);

    const payload: ExportPayload = {
      examId: examDetail.id,
      examName: examDetail.name,
    };

    try {
      const blob = await exportService.exportExam(payload);

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${examDetail.name}.docx`;
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

  const handleExportMatrix = async () => {
    setIsSubmitted(true);
  };

  const handleDeleteClick = async () => {
    if (isSubmitted) return;

    setIsSubmitted(true);

    try {
      const response = await examService.delete(examId);

      if (response) {
        await queryClient.invalidateQueries({ queryKey: ["user-exams"] });
        router.replace("/exam/all");
      }
    } catch (error) {
      console.error("Lỗi khi xóa đề", error);
      setIsSubmitted(false);
    }
  };

  useEffect(() => {
    if (errorsList.length === 0) return;

    const timer = setTimeout(() => {
      setErrorsList([]);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorsList]);

  useEffect(() => {
    if (noti === null || noti.length === 0) return;

    const timer = setTimeout(() => {
      setNoti(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [noti]);

  return {
    examDetail,
    isLoadingExam,
    errorsList,
    noti,
    handleExportExam,
    handleExportMatrix,
    handleDeleteClick,
  };
}
