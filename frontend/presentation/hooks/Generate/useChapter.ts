import { useQuery } from "@tanstack/react-query";

import { ChapterEntity } from "@/domain/entities/chapter.entity";
import { getAllChaptersService } from "@/presentation/services/chapter.service";

export default function useChapter() {
  const { data, isLoading } = useQuery({
    queryKey: ["all-chapters"],
    queryFn: async () => getAllChaptersService(),
    staleTime: 5 * 60 * 1000,
  });

  const chapters: ChapterEntity[] = data ? data : [];

  return {
    chapters,
    isLoadingChapters: isLoading,
  };
}
