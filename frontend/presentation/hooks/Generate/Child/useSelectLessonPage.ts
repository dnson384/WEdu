import { useQuery } from "@tanstack/react-query";
import { AxiosError, isAxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState, useTransition } from "react";

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
    refetch: refetchDraft,
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
  const [isNavigating, startNavigation] = useTransition();
  const [selectedLessons, setSelectedLessons] = useState<UpdateDraftParam[]>(
    [],
  );

  const currentChapter = useMemo(() => {
    return chapters.find((c) => c.id === chapterId);
  }, [chapters, chapterId]);

  const [hasSelectionChanges, setHasSelectionChanges] = useState(false);
  const [syncedKey, setSyncedKey] = useState<string | null>(null);
  const currentKey = `${fetchedDraft?.id}-${chapterId}`;

  if (fetchedDraft && (fetchedDraft !== draft || currentKey !== syncedKey)) {
    setSyncedKey(currentKey);
    setDraft(fetchedDraft);

    if (currentKey !== syncedKey || !hasSelectionChanges) {
      setHasSelectionChanges(false);
      const draftChapter = fetchedDraft.chapters.find(
        (c) => c.id === chapterId,
      );
      setSelectedLessons(
        (draftChapter?.lessons ?? []).map((lesson) => ({
          id: lesson.id,
          name: lesson.name,
        })),
      );
    }
  }

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/generate");
    }
  }, [fetchError, router]);

  const handleAddLesson = (lesson: UpdateDraftParam) => {
    if (!selectedLessons.find((l) => l.id === lesson.id)) {
      setHasSelectionChanges(true);
      setSelectedLessons((prev) => [...prev, lesson]);
    }
  };

  const handleRemoveLesson = (lessonId: string) => {
    setHasSelectionChanges(true);
    setSelectedLessons((prev) => prev.filter((l) => l.id !== lessonId));
  };

  const handleBackClick = () => {
    router.push(`/generate/${draftId}/chapter`);
  };

  const handleContinueClick = async () => {
    if (isSubmitting || isNavigating) return;

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
      startNavigation(() => router.push(nextUrl));
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
        const generateMatrixResult = await draftService.generateMatrix(draftId);
        if (generateMatrixResult) {
          await refetchDraft({ throwOnError: true });
          startNavigation(() => router.push(nextUrl));
        }
      }
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể cập nhật bài học cho chương");
      } else {
        setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
      }
    } finally {
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
    isLoading: isLoadingDraft || isSubmitting || isNavigating,
    isLoadingDraft,
    isSubmitting: isSubmitting || isNavigating,
    errorMessage: errorMessage || fetchError?.response?.data?.message || null,
    selectedLessons,
    unSelectedLessons,
    handleAddLesson,
    handleRemoveLesson,
    handleBackClick,
    handleContinueClick,
  };
}
