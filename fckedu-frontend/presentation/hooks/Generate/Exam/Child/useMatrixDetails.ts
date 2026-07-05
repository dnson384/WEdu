"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { AxiosError, isAxiosError } from "axios";

import { ChapterDraft, DraftEntity } from "@/domain/entities/draft.entity";
import { GetDraft } from "@/presentation/services/draft.service";
import { GenerateExamService } from "@/presentation/services/exam.service";

export default function useMatrixDetails() {
  const router = useRouter();

  const pathname = usePathname();
  const pathnameSpilit = pathname.split("/");

  const draftId = pathnameSpilit[pathnameSpilit.length - 3];

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
    queryKey: ["draft-details", draftId],
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

  const handleContinueClick = async () => {
    try {
      const response = await GenerateExamService(draftId);
      if (response) {
        router.replace(`/exam/${response}`);
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
    draft,
    changesChapters,
    curChapter,
    isLoading,
    handleChangeChapter,
    handleContinueClick,
  };
}
