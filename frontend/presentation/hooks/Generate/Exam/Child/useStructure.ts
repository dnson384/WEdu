import { CreateDraftPayload } from "@/presentation/schemas/draft.schema";
import { CreateDraftService } from "@/presentation/services/draft.service";
import { usePathname, useRouter } from "next/navigation";
import { ChangeEvent, useEffect, useState } from "react";

export default function useStructure() {
  const router = useRouter();
  const pathname = usePathname();

  const [examName, setExamName] = useState<string>("");
  const [questionsCount, setQuestionsCount] = useState<number>(0);
  const [questionTypes, setQuestionTypes] = useState<Record<string, boolean>>({
    "Nhiều lựa chọn": true,
    "Đúng sai": true,
    "Trả lời ngắn": true,
  });

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

    const payload: CreateDraftPayload = {
      examName: examName,
      questionsCount: questionsCount,
      questionTypes: questionTypesArr,
    };

    const draftId = await CreateDraftService(payload);

    if (draftId) {
      router.push(`/generate/${draftId}`);
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
