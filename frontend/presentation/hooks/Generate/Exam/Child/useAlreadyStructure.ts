import { DraftEntity } from "@/domain/entities/draft.entity";
import { CreateDraftPayload } from "@/presentation/schemas/draft.schema";
import {
  CreateDraftService,
  GetDraft,
} from "@/presentation/services/draft.service";
import { useQueries, useQuery } from "@tanstack/react-query";
import { usePathname, useRouter } from "next/navigation";
import { ChangeEvent, useEffect, useState } from "react";

export default function useAlreadyStructure() {
  const router = useRouter();
  const pathname = usePathname();
  const draftId = pathname.split("/")[pathname.split("/").length - 1];

  const initialDraftEntity: DraftEntity = {
    id: "",
    examName: "",
    questionsCount: 0,
    questionTypes: [],
    chapters: [],
  };

  const { data, isLoading } = useQuery({
    queryKey: ["draft", draftId],
    queryFn: () => GetDraft(draftId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    enabled: !!draftId,
  });

  const draft = data ?? initialDraftEntity;

  const [examName, setExamName] = useState<string>("");
  const [questionsCount, setQuestionsCount] = useState<number>(0);
  const [questionTypes, setQuestionTypes] = useState<Record<string, boolean>>({
    "Nhiều lựa chọn": true,
    "Đúng sai": true,
    "Trả lời ngắn": true,
  });

  useEffect(() => {
    if (draft) {
      setExamName(draft.examName);
      setQuestionsCount(draft.questionsCount);
    }
  }, [draft]);

  const [error, setError] = useState<string | null>(null);

  const handleExamNameChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setExamName(value);
  };

  const handleQuestionsCountChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuestionsCount(Number(value));
  };

  const handleQuestionTypesChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { id, checked } = e.target;
    setQuestionTypes((prev) => ({ ...prev, [id]: checked }));
  };

  const handleContinueClick = async () => {
    if (examName.trim().length === 0) {
      setError("Phải có tên bài kiểm tra");
      return;
    }

    if (!questionsCount) {
      setError("Phải có ít nhất 1 câu hỏi");
      return;
    }

    const questionTypesArr: string[] = Object.entries(questionTypes)
      .filter(([_, value]) => value === true)
      .map(([key]) => key);

    if (questionTypesArr.length == 0) {
      setError("Phải có ít nhất 1 loại câu hỏi");
      return;
    }

    if (draftId) {
      router.push(`${pathname}/chapter`);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (error !== null) {
        setError(null);
      }
    }, 3000);

    return () => clearTimeout(timer);
  }, [error]);

  return {
    isLoading,
    examName,
    questionsCount,
    questionTypes,
    error,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleContinueClick,
  };
}
