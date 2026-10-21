import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { AxiosError, isAxiosError } from "axios";

import { ChapterDraft, DraftEntity } from "@/domain/entities/draft.entity";
import { DraftService } from "@/presentation/services/DraftService";
import { ExamService } from "@/presentation/services/ExamService";
import { ApiError } from "next/dist/server/api-utils";

const initalDraftEntity = (): DraftEntity => ({
  id: "",
  examName: "",
  questionsCount: 0,
  questionTypes: [],
  chapters: [],
});

export default function useMatrixDetails(accountType: string) {
  const router = useRouter();
  const params = useParams();
  const draftService = useMemo(() => new DraftService(), []);
  const examService = useMemo(() => new ExamService(), []);

  const draftId = (params?.draftId as string) || "";

  const {
    data: fetchedDraft,
    error: fetchError,
    isLoading: isLoadingDraft,
  } = useQuery<DraftEntity, AxiosError<ApiError>>({
    queryKey: ["draft-generated-matrix-detail", draftId],
    queryFn: async () => draftService.get(draftId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: Boolean(draftId),
  });

  const [draft, setDraft] = useState<DraftEntity>(initalDraftEntity);
  const [changesChapters, setChangesChapters] = useState<ChapterDraft[]>([]);
  const [selectedChapterId, setSelectedChapterId] = useState<string>("");

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const [syncedDraftId, setSyncedDraftId] = useState<string | null>(null);
  if (fetchedDraft && fetchedDraft.id !== syncedDraftId) {
    setSyncedDraftId(fetchedDraft.id);
    setDraft(fetchedDraft);
    setChangesChapters(fetchedDraft.chapters || []);

    if (fetchedDraft.chapters && fetchedDraft.chapters.length > 0) {
      setSelectedChapterId(fetchedDraft.chapters[0].id);
    }
  }

  const curChapter = useMemo(() => {
    return (
      changesChapters.find((chap) => chap.id === selectedChapterId) ||
      changesChapters[0] || {
        id: "",
        name: "",
        lessons: [],
      }
    );
  }, [changesChapters, selectedChapterId]);

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/generate");
    }
  }, [fetchError, router]);

  const handleChangeChapter = (chapterId: string) => {
    setSelectedChapterId(chapterId);
  };

  // Handle BTN
  const handleBackClick = () => {
    router.replace(`/generate/${draftId}/matrix`);
  };

  const handleContinueClick = async () => {
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const response = await examService.generate(draftId);
      if (response) {
        if (response.errors.length > 0) {
          const params =
            accountType === "PLUS"
              ? "noti=Các câu hỏi thiếu đã được tạo bằng AI"
              : `errors=${response.errors.join("_||_")}`;
          router.replace(`/exam/${response.examId}?${params}`);
        } else {
          router.replace(`/exam/${response.examId}`);
        }
      }
    } catch (err) {
      if (isAxiosError(err) && err.response?.data) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể tạo bài kiểm tra từ ma trận");
      } else {
        setErrorMessage("Đã có lỗi xảy ra trong quá trình tạo đề thi");
      }
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!errorMessage) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorMessage]);

  return {
    draft,
    changesChapters,
    curChapter,
    isLoadingDraft,
    isSubmitting,
    errorMessage: errorMessage || fetchError?.response?.data?.message || null,
    handleChangeChapter,
    handleBackClick,
    handleContinueClick,
  };
}
