import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  UpdateDraftParam,
  UpdateChaptersDraftPayload,
} from "@/presentation/schemas/draft.schema";
import { getAllCategoriesService } from "@/presentation/services/category.service";
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
        queryKey: ["categories"],
        queryFn: () => getAllCategoriesService(),
        staleTime: 1000 * 60 * 5,
      },
    ],
  });

  const [draftQuery, categoriesQuery] = results;
  const draft = draftQuery.data ?? initalDraftEntity;
  const categories = categoriesQuery.data ?? [];
  const isLoading = draftQuery.isLoading || categoriesQuery.isLoading;

  const isError = draftQuery.isError;
  const error = draftQuery.error || categoriesQuery.error;
  const axiosError = error as AxiosError<any>;

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  useEffect(() => {
    if (axiosError) {
      setErrorMessage(axiosError.response?.data.message || null);
    }
  }, [axiosError]);

  // Handler
  const [selectedChapters, setSelectedChapters] = useState<UpdateDraftParam[]>([
    { id: "", name: "" },
  ]);

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

  const handleChapterSelect = (
    curId: string,
    id: string,
    name: string,
    index: number,
  ) => {
    setSelectedChapters((prev) => {
      const newSelectedChapters = [...prev];
      if (curId !== id) {
        newSelectedChapters[index] = { id: id, name: name };
      }
      return newSelectedChapters;
    });
  };

  const handleAddChapter = () => {
    setSelectedChapters((prev) => [...prev, { id: "", name: "" }]);
  };

  const handleContinueClick = async () => {
    if (!selectedChapters[0].id) {
      setErrorMessage("Chưa chọn chương nào");
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
    categories,
    isLoadingDraft: isLoading,
    isError,
    errorMessage,
    selectedChapters,
    setSelectedChapters,
    handleChapterSelect,
    handleAddChapter,
    handleBackClick,
    handleContinueClick,
  };
}
