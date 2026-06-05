import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  UpdateDraftParam,
  UpdateLessonssDraftPayload,
} from "@/presentation/schemas/draft.schema";
import { getAllCategoriesService } from "@/presentation/services/category.service";
import {
  GenerateMatrix,
  GetDraft,
  UpdateLessons,
} from "@/presentation/services/draft.service";
import { useQueries } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function useChapter() {
  const router = useRouter();

  const pathname = usePathname();
  const pathnameSplited = pathname.split("/");

  const draftId = pathnameSplited[pathnameSplited.length - 2];
  const chapterId = pathnameSplited[pathnameSplited.length - 1];

  const initalDraftEntity: DraftEntity = {
    id: "",
    questionsCount: 0,
    questionTypes: [],
    chapters: [],
  };

  const results = useQueries({
    queries: [
      {
        queryKey: ["draft", draftId, chapterId],
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

  // Data
  const [draftQuery, categoriesQuery] = results;
  const draft = draftQuery.data ?? initalDraftEntity;

  const categories = categoriesQuery.data ?? [];
  const currentChapter = categories.find(
    (category) => category.id === chapterId,
  );

  // Error & UI
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

  // Handlers
  const newSelectedLesson = (): UpdateDraftParam => ({
    id: "",
    name: "",
  });

  const [selectedLessons, setSelectedLessons] = useState<UpdateDraftParam[]>([
    newSelectedLesson(),
  ]);

  useEffect(() => {
    const draftLessons = draft.chapters.find(
      (chapter) => chapter.id === chapterId,
    )?.lessons;
    if (draftLessons && Object.keys(draftLessons).length > 0) {
      setSelectedLessons(
        Object.values(draftLessons).map((lesson) => ({
          id: lesson.id,
          name: lesson.name,
        })),
      );
    }
  }, [draft, chapterId]);

  const handleLessonSelect = (
    currentId: string,
    id: string,
    name: string,
    index: number,
  ) => {
    setSelectedLessons((prev) => {
      const newSelectedLessons = [...prev];
      if (currentId !== id) {
        newSelectedLessons[index] = { id: id, name: name };
      }
      return newSelectedLessons;
    });
  };

  const handleAddLesson = () => {
    setSelectedLessons((prev) => [...prev, newSelectedLesson()]);
  };

  const handleContinueClick = async () => {
    if (!selectedLessons[0].id) {
      return setErrorMessage("Vui lòng chọn nội dung");
    }

    // Tiếp tục mà không thay đổi
    const curChapterIndex = draft.chapters.findIndex(
      (chapter) => chapterId === chapter.id,
    );

    const newChapterId =
      curChapterIndex < draft.chapters.length - 1
        ? draft.chapters[curChapterIndex + 1].id
        : draft.chapters[curChapterIndex].id;

    if (
      selectedLessons.length ===
        draft.chapters[curChapterIndex].lessons.length &&
      curChapterIndex < draft.chapters.length - 1
    ) {
      router.push(`${pathname.replace(chapterId, newChapterId)}`);
    }

    const prevLessonIds: string[] = draft.chapters[curChapterIndex].lessons.map(
      (lesson) => lesson.id,
    );
    if (!prevLessonIds) {
      return setErrorMessage("Chương hiện tại chưa tồn tại trong bản nháp");
    }

    // Cập nhật nội dung
    const payload: UpdateLessonssDraftPayload = {
      draftId: draft.id,
      chapterId: chapterId,
      add: [],
      del: [],
    };

    // Thêm bài
    selectedLessons.forEach((chapter) => {
      if (!prevLessonIds.includes(chapter.id)) {
        payload.add.push(chapter);
      }
    });

    // Xoá bài
    const selectedLessonsId = selectedLessons.map((chapter) => chapter.id);
    prevLessonIds.forEach((chapterId) => {
      if (!selectedLessonsId.includes(chapterId)) {
        payload.del.push(chapterId);
      }
    });

    let response;
    let isGenerated = false;

    response = await UpdateLessons(payload);

    if (curChapterIndex === draft.chapters.length - 1) {
      response = await GenerateMatrix(draftId);
      if (response) isGenerated = true;
    }

    if (response && !isGenerated) {
      router.push(`${pathname.replace(chapterId, newChapterId)}`);
    } else if (response && isGenerated) {
      router.push(`${pathname.replace(chapterId, "matrix")}`);
    }
  };

  useEffect(() => {
    if (errorMessage !== null) {
      const timer = setTimeout(() => {
        setErrorMessage(null);

        if (axiosError && axiosError.status === 404) {
          router.replace("/generate/exam");
        }
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [errorMessage]);

  return {
    // Data
    currentChapter,
    draft,
    // Error & UI
    isLoading,
    isError,
    errorMessage,
    // Handlers
    selectedLessons,
    setSelectedLessons,
    handleLessonSelect,
    handleAddLesson,
    handleContinueClick,
  };
}
