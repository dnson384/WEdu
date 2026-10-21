import { useQuery } from "@tanstack/react-query";
import { AxiosError, isAxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  UpdateDraftParam,
  UpdateDraftPayload,
} from "@/presentation/schemas/draft.schema";
import { DraftService } from "@/presentation/services/DraftService";
import { ChapterEntity } from "@/domain/entities/chapter.entity";

const initalDraftEntity: DraftEntity = {
  id: "",
  examName: "",
  questionsCount: 0,
  questionTypes: [],
  chapters: [],
};

export default function useSelectChapterPage(chapters: ChapterEntity[]) {
  const router = useRouter();
  const params = useParams();
  const draftService = useMemo(() => new DraftService(), []);

  const draftId = (params?.draftId as string) || "";

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
  const [selectedChapters, setSelectedChapters] = useState<UpdateDraftParam[]>(
    [],
  );

  const [syncedDraftId, setSyncedDraftId] = useState<string | null>(null);
  if (fetchedDraft && fetchedDraft.id !== syncedDraftId) {
    setSyncedDraftId(fetchedDraft.id);
    setDraft(fetchedDraft);

    if (fetchedDraft.chapters.length > 0) {
      setSelectedChapters(
        Object.values(fetchedDraft.chapters).map((chapter) => ({
          id: chapter.id,
          name: chapter.name,
        })),
      );
    }
  }

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/generate/exam");
    }
  }, [fetchError, router]);

  useEffect(() => {
    if (errorMessage !== null) {
      const timer = setTimeout(() => {
        setErrorMessage(null);

        if (fetchError?.response?.status === 404) {
          router.replace("/generate/exam");
        }
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [fetchError, errorMessage, router]);

  const handleAddChapter = (chapter: UpdateDraftParam) => {
    if (!selectedChapters.find((c) => c.id === chapter.id)) {
      setSelectedChapters((prev) => [...prev, chapter]);
    }
  };

  const handleRemoveChapter = (chapterId: string) => {
    setSelectedChapters((prev) => prev.filter((c) => c.id !== chapterId));
  };

  const handleContinueClick = async () => {
    if (selectedChapters.length === 0) {
      setErrorMessage("Vui lòng chọn chương");
      return;
    }

    const prevChapterIds = draft.chapters.map((chapter) => chapter.id);
    const selectedChapterIds = selectedChapters.map((chapter) => chapter.id);

    const payload: UpdateDraftPayload = {
      add: selectedChapters.filter(
        (chapter) => !prevChapterIds.includes(chapter.id),
      ),
      del: prevChapterIds.filter((id) => !selectedChapterIds.includes(id)),
    };

    const targetUrl = `/generate/${draftId}/${selectedChapters[0].id}`;

    if (payload.add.length === 0 && payload.del.length === 0) {
      router.push(targetUrl);
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const response = await draftService.updateChapters(draftId, payload);
      if (response) {
        router.push(targetUrl);
      }
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể cập nhật danh sách chương");
      } else {
        setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
      }
      setIsSubmitting(false);
    }
  };

  const handleBackClick = () => {
    router.push(`/generate/${draftId}`);
  };

  const unSelectedChaptersData = useMemo(() => {
    const selectedIds = new Set(selectedChapters.map((sc) => sc.id));
    return chapters
      .filter((chapter) => !selectedIds.has(chapter.id))
      .map((chapter) => ({
        id: chapter.id,
        name: chapter.name,
      }));
  }, [chapters, selectedChapters]);

  useEffect(() => {
    if (!errorMessage) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorMessage]);

  return {
    draft,
    isLoadingDraft,
    isSubmitting,
    errorMessage: errorMessage || fetchError?.response?.data?.message || null,
    selectedChapters,
    unSelectedChaptersData,
    handleAddChapter,
    handleRemoveChapter,
    handleBackClick,
    handleContinueClick,
  };
}
