import { DraftEntity } from "@/domain/entities/draft.entity";
import { getAllDraftService } from "@/presentation/services/draft.service";
import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

const EMPTY_DRAFTS: DraftEntity[] = [];

export default function useAllDraft() {
  const router = useRouter();

  const { data, isLoading } = useQuery({
    queryKey: ["all-draft"],
    queryFn: () => getAllDraftService(),
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
