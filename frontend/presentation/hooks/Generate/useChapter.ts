import { useQuery } from "@tanstack/react-query";

import { ChapterEntity } from "@/domain/entities/chapter.entity";
import { ChapterSerivce } from "@/presentation/services/ChapterService";
import { useMemo } from "react";

export default function useChapter() {
  const chapterService = useMemo(() => new ChapterSerivce(), []);

  const { data, isLoading } = useQuery({
    queryKey: ["all-chapters"],
    queryFn: async () => chapterService.getAll(),
    staleTime: 5 * 60 * 1000,
  });

  const chapters: ChapterEntity[] = data ? data : [];

  return {
    chapters,
    isLoadingChapters: isLoading,
  };
}
