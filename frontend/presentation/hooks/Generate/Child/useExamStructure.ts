import { DraftEntity } from "@/domain/entities/draft.entity";
import { CreateDraftPayload } from "@/presentation/schemas/draft.schema";
import { DraftService } from "@/presentation/services/DraftService";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError, isAxiosError } from "axios";
import { ApiError } from "next/dist/server/api-utils";
import { useParams, useRouter } from "next/navigation";
import { ChangeEvent, useEffect, useMemo, useState } from "react";

const initialDraftEntity: DraftEntity = {
  id: "",
  examName: "",
  questionsCount: 0,
  questionTypes: ["Nhiều lựa chọn", "Đúng sai", "Trả lời ngắn"],
  chapters: [],
};

export default function useExamStructure() {
  const router = useRouter();
  const params = useParams();
  const queryClient = useQueryClient();

  const draftService = useMemo(() => new DraftService(), []);

  const draftId = (params?.draftId as string) || "";
  const isEditMode = Boolean(draftId);

  const {
    data: fetchedDraft,
    error: fetchError,
    isLoading: isLoadingDraft,
  } = useQuery<DraftEntity, AxiosError<ApiError>>({
    queryKey: ["draft", draftId],
    queryFn: () => draftService.get(draftId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    enabled: isEditMode,
  });

  const [draft, setDraft] = useState<DraftEntity>(initialDraftEntity);

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [syncedDraftId, setSyncedDraftId] = useState<string | null>(null);
  if (isEditMode && fetchedDraft && fetchedDraft.id !== syncedDraftId) {
    setSyncedDraftId(fetchedDraft.id);
    setDraft(fetchedDraft);
  }

  useEffect(() => {
    if (fetchError?.response?.status === 404) {
      router.replace("/generate");
    }
  }, [fetchError, router]);

  const handleExamNameChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setDraft((prev) => ({
      ...prev,
      examName: value,
    }));
  };

  const handleQuestionsCountChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (value === "") {
      setDraft((prev) => ({
        ...prev,
        questionsCount: 0,
      }));
      return;
    }

    const parsed = parseInt(value, 10);
    const questionsCount = isNaN(parsed) || parsed < 0 ? 0 : parsed;
    setDraft((prev) => ({
      ...prev,
      questionsCount: questionsCount,
    }));
  };

  const handleQuestionTypesChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { id, checked } = e.target;

    setDraft((prev) => {
      const currentTypes = prev.questionTypes || [];
      const updatedTypes = checked
        ? [...new Set([...currentTypes, id])]
        : currentTypes.filter((type) => type !== id);

      return {
        ...prev,
        questionTypes: updatedTypes,
      };
    });
  };

  const handleContinueClick = async () => {
    const examName = draft.examName;
    const questionsCount = draft.questionsCount;
    const questionTypes = draft.questionTypes;

    if (examName.trim().length === 0) {
      setErrorMessage("Phải có tên bài kiểm tra");
      return;
    }

    if (!questionsCount || questionsCount < 0) {
      setErrorMessage("Phải có ít nhất 1 câu hỏi");
      return;
    }

    if (questionTypes.length == 0) {
      setErrorMessage("Phải có ít nhất 1 loại câu hỏi");
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const payload: CreateDraftPayload = {
        examName: examName,
        questionsCount: questionsCount,
        questionTypes: questionTypes,
      };

      const draftId = await draftService.create(payload);

      if (draftId) {
        router.push(`/generate/${draftId}/chapter`);
      }
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        setErrorMessage(
          err.response?.data?.message || "Không thể tạo bản nháp bài kiểm tra",
        );
      } else {
        setErrorMessage("Đã có lỗi xảy ra, vui lòng thử lại sau");
      }
      setIsSubmitting(false);
    }
  };

  const handleDeleteClick = async () => {
    if (!isEditMode || isDeleting) return;

    setIsDeleting(true);
    try {
      const response = await draftService.delete(draftId);
      if (response) {
        await queryClient.invalidateQueries({ queryKey: ["all-draft"] });
        router.replace("/generate");
      }
    } catch (err: unknown) {
      if (isAxiosError(err)) {
        const resMessage = (err.response?.data as ApiError)?.message;
        setErrorMessage(resMessage || "Không thể xóa bản nháp");
      } else {
        setErrorMessage("Đã có lỗi xảy ra khi xóa");
      }
      setIsDeleting(false);
    }
  };

  useEffect(() => {
    if (!errorMessage) return;

    const timer = setTimeout(() => {
      setErrorMessage(null);
    }, 3000);

    return () => clearTimeout(timer);
  }, [errorMessage]);

  return {
    isEditMode,
    draftId,
    draft,
    errorMessage,
    isLoadingDraft,
    isSubmitting,
    isDeleting,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleContinueClick,
    handleDeleteClick,
  };
}
