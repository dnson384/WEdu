"use client";

import { usePathname, useRouter } from "next/navigation";
import {
  GenerateMatrixDetails,
  GetDraft,
} from "@/presentation/services/draft.service";
import { useQuery } from "@tanstack/react-query";
import { ChapterDraft, DraftEntity } from "@/domain/entities/draft.entity";
import { AxiosError, isAxiosError } from "axios";
import { useEffect, useState } from "react";

export default function useMatrix() {
  const router = useRouter();

  const pathname = usePathname();
  const pathnameSpilit = pathname.split("/");

  const draftId = pathnameSpilit[pathnameSpilit.length - 2];

  const DraftInitial = (): DraftEntity => ({
    id: "",
    questionsCount: 0,
    questionTypes: [],
    chapters: [],
  });

  const { data, isError, error, isLoading } = useQuery<
    DraftEntity,
    AxiosError<any>
  >({
    queryKey: ["draft-matrix", draftId],
    queryFn: () => GetDraft(draftId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: !!draftId,
  });

  const draft = data ?? DraftInitial();

  const [changesChapters, setChangesChapters] = useState<ChapterDraft[]>([]);

  useEffect(() => {
    if (data && draft !== DraftInitial()) {
      setChangesChapters(draft.chapters);
    }
  }, [draft]);

  const [curChapter, setCurChapter] = useState<ChapterDraft>({
    id: "",
    name: "",
    lessons: [],
  });

  useEffect(() => {
    if (changesChapters.length > 0) {
      setCurChapter(changesChapters[0]);
    }
  }, [changesChapters]);

  const handleChangeChapter = (chapterId: string) => {
    const chapter = changesChapters.find(
      (chapterData) => chapterData.id === chapterId,
    );
    if (chapter) {
      setCurChapter(chapter);
    }
  };

  const handleMatrixInputChange = (
    lessonId: string,
    questionType: string,
    difficultyLevel: string,
    newCount: number,
  ) => {
    const updatedLessons = curChapter.lessons.map((lesson) => {
      if (lesson.id === lessonId) {
        const updatedMatrix = [...lesson.matrix];
        const itemIndex = updatedMatrix.findIndex(
          (item) =>
            item.questionType === questionType &&
            item.difficultyLevel === difficultyLevel,
        );

        if (itemIndex > -1) {
          updatedMatrix[itemIndex] = {
            ...updatedMatrix[itemIndex],
            selectedCount: newCount,
          };
        }

        return { ...lesson, matrix: updatedMatrix };
      }
      return lesson;
    });

    const updatedChapter = { ...curChapter, lessons: updatedLessons };
    setCurChapter(updatedChapter);

    setChangesChapters((prev) =>
      prev.map((chap) =>
        chap.id === updatedChapter.id ? updatedChapter : chap,
      ),
    );
  };

  const handleContinueClick = async () => {
    try {
      const response = await GenerateMatrixDetails(draftId);
      if (response) {
        router.push(`${pathname}/details`);
      }
    } catch (err) {
      if (isAxiosError(err) && err.response?.data) {
      }
    }
  };

  useEffect(() => {
    if (error && error.status === 404) {
      router.replace("/generate/exam");
    }
  }, [error]);

  return {
    changesChapters,
    curChapter,
    isLoading,
    handleChangeChapter,
    handleMatrixInputChange,
    handleContinueClick,
  };
}
