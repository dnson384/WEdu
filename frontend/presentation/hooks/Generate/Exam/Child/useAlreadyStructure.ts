import { DraftEntity } from "@/domain/entities/draft.entity";
import {
  deleteDraftService,
  GetDraft,
} from "@/presentation/services/draft.service";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { usePathname, useRouter } from "next/navigation";
import { ChangeEvent, useEffect, useState } from "react";

export default function useAlreadyStructure() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const pathname = usePathname();
  const draftId = pathname.split("/")[pathname.split("/").length - 1];

  const initialDraftEntity: DraftEntity = {
    id: "",
    examName: "",
    questionsCount: 0,
    questionTypes: [],
    chapters: [],
  };

  const { data, error, isLoading } = useQuery<DraftEntity, AxiosError<unknown>>(
    {
      queryKey: ["draft", draftId],
      queryFn: () => GetDraft(draftId),
      staleTime: 1000 * 60 * 5,
      retry: false,
      refetchOnMount: true,
      refetchOnWindowFocus: false,
      enabled: !!draftId,
    },
  );

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

  useEffect(() => {
    if (error && error.status === 404) {
      router.replace("/generate");
    }
  }, [error]);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

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
      setErrorMessage("Phải có tên bài kiểm tra");
      return;
    }

    if (!questionsCount) {
      setErrorMessage("Phải có ít nhất 1 câu hỏi");
      return;
    }

    const questionTypesArr: string[] = Object.entries(questionTypes)
      .filter(([_, value]) => value === true)
      .map(([key]) => key);

    if (questionTypesArr.length == 0) {
      setErrorMessage("Phải có ít nhất 1 loại câu hỏi");
      return;
    }

    if (draftId) {
      router.push(`${pathname}/chapter`);
    }
  };

  const [isSubmitted, setIsSubmitted] = useState<boolean>(false);

  const handleDeleteClick = async () => {
    if (isSubmitted) return;

    setIsSubmitted(true);

    try {
      const response = await deleteDraftService(draftId);

      if (response) {
        await queryClient.invalidateQueries({ queryKey: ["all-draft"] });
        router.replace("/generate");
      }
    } catch (error) {
      console.error("Lỗi khi xóa bản nháp", error);
    } finally {
      setIsSubmitted(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (error !== null) {
        setErrorMessage(null);
      }
    }, 3000);

    return () => clearTimeout(timer);
  }, [error]);

  return {
    isLoading,
    examName,
    questionsCount,
    questionTypes,
    errorMessage,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleDeleteClick,
    handleContinueClick,
  };
}
