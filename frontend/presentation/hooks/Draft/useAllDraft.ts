import { DraftEntity } from "@/domain/entities/draft.entity";
import { DraftService } from "@/presentation/services/DraftService";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useMemo } from "react";

const EMPTY_DRAFTS: DraftEntity[] = [];

export default function useAllDraft() {
  const router = useRouter();
  const draftService = useMemo(() => new DraftService(), []);

  const { data, isLoading } = useQuery({
    queryKey: ["all-draft"],
    queryFn: () => draftService.getAll(),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const drafts: DraftEntity[] = data || EMPTY_DRAFTS;

  const handleDraftCardClick = (draftId: string) => {
    router.push(`/generate/${draftId}`);
  };

  return {
    drafts,
    isLoading,
    handleDraftCardClick,
  };
}
