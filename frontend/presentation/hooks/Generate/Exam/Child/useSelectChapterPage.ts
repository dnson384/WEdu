import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  UpdateDraftParam,
  UpdateChaptersDraftPayload,
} from "@/presentation/schemas/draft.schema";
import { getAllChaptersService } from "@/presentation/services/chapter.service";
import {
  GetDraft,
  UpdateChapters,
} from "@/presentation/services/draft.service";
import { useQueries } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function useSelectChapterPage() {
  const router = useRouter();
  const pathname = usePathname();
  const draftId = pathname.split("/")[pathname.split("/").length - 2];

  const initalDraftEntity: DraftEntity = {
    id: "",
    examName: "",
    questionsCount: 0,
    questionTypes: [],
    chapters: [],
  };

  const results = useQueries({
    queries: [
      {
        queryKey: ["draft", draftId],
        queryFn: () => GetDraft(draftId),
        staleTime: 1000 * 60 * 5,
        retry: false,
        refetchOnMount: true,
        refetchOnWindowFocus: false,
        enabled: !!draftId,
      },
      {
        queryKey: ["all-chapters"],
        queryFn: () => getAllChaptersService(),
        staleTime: 1000 * 60 * 5,
      },
    ],
  });

  const [draftQuery, chapterQuery] = results;
  const draft = draftQuery.data ?? initalDraftEntity;
  const chapters = chapterQuery.data ?? [];
  const isLoading = draftQuery.isLoading || chapterQuery.isLoading;

  const isError = draftQuery.isError;
  const error = draftQuery.error || chapterQuery.error;
  const axiosError = error as AxiosError<any>;

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  useEffect(() => {
    if (axiosError) {
      setErrorMessage(axiosError.response?.data.message || null);
    }
  }, [axiosError]);

  // Handler
  const [selectedChapters, setSelectedChapters] = useState<UpdateDraftParam[]>(
    [],
  );

  useEffect(() => {
    if (draft !== initalDraftEntity && draft.chapters.length > 0) {
      setSelectedChapters(
        Object.values(draft.chapters).map((chapter) => ({
          id: chapter.id,
          name: chapter.name,
        })),
      );
    }
  }, [draft]);

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

    const payload: UpdateChaptersDraftPayload = {
      draftId: draft.id,
      add: [],
      del: [],
    };

    const prevChapterIds = draft.chapters.map((chapter) => chapter.id);

    // Thêm chương
    selectedChapters.forEach((chapter) => {
      if (!prevChapterIds.includes(chapter.id) && chapter.id) {
        payload.add.push(chapter);
      }
    });

    // Xoá chương
    const selectedChaptersId = selectedChapters.map((chapter) => chapter.id);
    prevChapterIds.forEach((chapterId) => {
      if (!selectedChaptersId.includes(chapterId)) {
        payload.del.push(chapterId);
      }
    });

    const url = pathname.replace("chapter", selectedChaptersId[0]);

    if (payload.add.length === 0 && payload.del.length === 0) {
      router.push(url);
    }

    const response = await UpdateChapters(payload);

    if (response) {
      router.push(url);
    }
  };

  const handleBackClick = () => {
    router.push(`/${pathname.split("/")[1]}/${pathname.split("/")[2]}`);
  };

  useEffect(() => {
    if (errorMessage !== null) {
      const timer = setTimeout(() => {
        setErrorMessage(null);

        if (axiosError.status === 404) {
          router.replace("/generate/exam");
        }
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [errorMessage]);

  return {
    draft,
    chapters,
    isLoadingDraft: isLoading,
    isError,
    errorMessage,
    selectedChapters,
    handleAddChapter,
    handleRemoveChapter,
    handleBackClick,
    handleContinueClick,
  };
}
