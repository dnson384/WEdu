"use client";

import { useParams, useRouter } from "next/navigation";
import { DraftService } from "@/presentation/services/DraftService";
import { useQuery } from "@tanstack/react-query";
import { ChapterDraft, DraftEntity } from "@/domain/entities/draft.entity";
import { AxiosError, isAxiosError } from "axios";
import { useEffect, useMemo, useState } from "react";
import { ApiError } from "next/dist/server/api-utils";

const initalDraftEntity: DraftEntity = {
  id: "",
  examName: "",
  questionsCount: 0,
  questionTypes: [],
  chapters: [],
};

export default function useMatrix() {
  const router = useRouter();
  const params = useParams();
  const draftService = useMemo(() => new DraftService(), []);

  const draftId = (params?.draftId as string) || "";

  const {
    data: fetchedDraft,
    error: fetchError,
    isLoading: isLoadingDraft,
  } = useQuery<DraftEntity, AxiosError<ApiError>>({
    queryKey: ["draft-generated-matrix", draftId],
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
      router.replace("/generate/exam");
    }
  }, [fetchError, router]);

  const handleChangeChapter = (chapterId: string) => {
    setSelectedChapterId(chapterId);
  };

  // Dùng để chỉnh số lượng câu hỏi cho từng phần (chưa áp dụng)
  const handleMatrixInputChange = (
    lessonId: string,
    questionType: string,
    difficultyLevel: string,
    newCount: number,
  ) => {
    setChangesChapters((prevChapters) =>
      prevChapters.map((chap) => {
        if (chap.id !== curChapter.id) return chap;

        const updatedLessons = chap.lessons.map((lesson) => {
          if (lesson.id !== lessonId) return lesson;

          const updatedMatrix = (lesson.matrix || []).map((item) => {
            if (
              item.questionType === questionType &&
              item.difficultyLevel === difficultyLevel
            ) {
              return { ...item, selectedCount: Math.max(0, newCount) };
            }
            return item;
          });

          return { ...lesson, matrix: updatedMatrix };
        });

        return { ...chap, lessons: updatedLessons };
      }),
    );
  };

  // Handle BTN
  const handleBackClick = () => {
    const lastestChapterId = draft.chapters[draft.chapters.length - 1].id;

    if (lastestChapterId) {
      router.push(`/generate/${draftId}/${lastestChapterId}/lesson`);
    } else {
      router.push(`/generate/${draftId}/chapter`);
    }
  };

  const handleContinueClick = async () => {
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await draftService.generateMatrixDetails(draftId);
      router.push(`/generate/${draftId}/details`);
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể lưu cấu trúc ma trận đề thi");
      } else {
        setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
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
    handleMatrixInputChange,
    handleBackClick,
    handleContinueClick,
  };
}
