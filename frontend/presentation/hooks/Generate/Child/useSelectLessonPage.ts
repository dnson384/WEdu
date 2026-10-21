import { useQuery } from "@tanstack/react-query";
import { AxiosError, isAxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { ChapterEntity } from "@/domain/entities/chapter.entity";
import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  UpdateDraftParam,
  UpdateDraftPayload,
} from "@/presentation/schemas/draft.schema";
import { DraftService } from "@/presentation/services/DraftService";

const initalDraftEntity: DraftEntity = {
  id: "",
  examName: "",
  questionsCount: 0,
  questionTypes: [],
  chapters: [],
};

export default function useSelectLessonPage(chapters: ChapterEntity[]) {
  const router = useRouter();
  const params = useParams();
  const draftService = useMemo(() => new DraftService(), []);

  const draftId = (params?.draftId as string) || "";
  const chapterId = (params?.chapterId as string) || "";

  const {
    data: fetchedDraft,
    error: fetchError,
    isLoading: isLoadingDraft,
  } = useQuery<DraftEntity, AxiosError<ApiError>>({
    queryKey: ["draft", draftId],
    queryFn: () => draftService.get(draftId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    enabled: Boolean(draftId),
  });

  const [draft, setDraft] = useState<DraftEntity>(initalDraftEntity);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [selectedLessons, setSelectedLessons] = useState<UpdateDraftParam[]>(
    [],
  );

  const currentChapter = useMemo(() => {
    return chapters.find((c) => c.id === chapterId);
  }, [chapters, chapterId]);

  const [syncedKey, setSyncedKey] = useState<string | null>(null);
  const currentKey = `${fetchedDraft?.id}-${chapterId}`;

  if (fetchedDraft && currentKey !== syncedKey) {
    setSyncedKey(currentKey);
    setDraft(fetchedDraft);

    const draftChapter = fetchedDraft.chapters?.find((c) => c.id === chapterId);
    if (draftChapter?.lessons?.length) {
      setSelectedLessons(
        draftChapter.lessons.map((lesson) => ({
          id: lesson.id,
          name: lesson.name,
        })),
      );
    } else {
      setSelectedLessons([]);
    }
  }

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/generate");
    }
  }, [fetchError, router]);

  const handleAddLesson = (lesson: UpdateDraftParam) => {
    if (!selectedLessons.find((l) => l.id === lesson.id)) {
      setSelectedLessons((prev) => [...prev, lesson]);
    }
  };

  const handleRemoveLesson = (lessonId: string) => {
    setSelectedLessons((prev) => prev.filter((l) => l.id !== lessonId));
  };

  const handleBackClick = () => {
    router.push(`/generate/${draftId}/chapter`);
  };

  const handleContinueClick = async () => {
    if (selectedLessons.length === 0) {
      return setErrorMessage("Vui lòng chọn nội dung");
    }

    const curChapterIndex = draft.chapters.findIndex((c) => c.id === chapterId);
    if (curChapterIndex === -1) {
      setErrorMessage("Chương không tồn tại trong bản nháp");
      return;
    }

    const isLastChapter = curChapterIndex === draft.chapters.length - 1;
    const nextChapterId = !isLastChapter
      ? draft.chapters[curChapterIndex + 1].id
      : null;
    const nextUrl = isLastChapter
      ? `/generate/${draftId}/matrix`
      : `/generate/${draftId}/${nextChapterId}`;

    const prevLessons = draft.chapters[curChapterIndex].lessons || [];
    const prevLessonIds = prevLessons.map((l) => l.id);
    const selectedLessonIds = selectedLessons.map((l) => l.id);

    const payload: UpdateDraftPayload = {
      add: selectedLessons.filter(
        (lesson) => !prevLessonIds.includes(lesson.id),
      ),
      del: prevLessonIds.filter((id) => !selectedLessonIds.includes(id)),
    };

    if (payload.add.length === 0 && payload.del.length === 0) {
      router.push(nextUrl);
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const updateLessonsResult = await draftService.updateLessons(
        draftId,
        chapterId,
        payload,
      );
      if (updateLessonsResult) {
        try {
          const generateMatrixResult =
            await draftService.generateMatrix(draftId);
          if (generateMatrixResult) {
            router.push(nextUrl);
          }
        } catch (err: unknown) {
          if (isAxiosError(err)) {
            const resMessage = (err.response?.data as ApiError)?.message;
            setErrorMessage(
              resMessage || "Không thể cập nhật bài học cho chương",
            );
          } else {
            setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
          }
          setIsSubmitting(false);
        }
      }
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể cập nhật bài học cho chương");
      } else {
        setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
      }
      setIsSubmitting(false);
    }
  };

  const unSelectedLessons = useMemo(() => {
    const lessons = currentChapter?.lessons ?? [];
    const selectedIds = new Set(selectedLessons.map((l) => l.id));
    return lessons.filter((lesson) => !selectedIds.has(lesson.id));
  }, [currentChapter, selectedLessons]);

  useEffect(() => {
    if (!errorMessage) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorMessage]);

  return {
    currentChapter,
    draft,
    isLoading: isLoadingDraft || isSubmitting,
    isLoadingDraft,
    isSubmitting,
    errorMessage: errorMessage || fetchError?.response?.data?.message || null,
    selectedLessons,
    unSelectedLessons,
    handleAddLesson,
    handleRemoveLesson,
    handleBackClick,
    handleContinueClick,
  };
}
