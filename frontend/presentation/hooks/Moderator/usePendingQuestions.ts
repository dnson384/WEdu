"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { ModeratorService } from "@/presentation/services/ModeratorService";

import { PendingQuestionEntity, QuestionReviewAction } from "@/domain/entities/pendingQuestion.entity";
const service = new ModeratorService();
const types = ["Nhiều lựa chọn", "Đúng sai", "Tự luận"];

export default function usePendingQuestions() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState({
    page: 0,
    search: "",
    questionType: "",
  });
  const [search, setSearch] = useState("");
  const [notice, setNotice] = useState("");
  const processing = useRef(false);
  const mutation = useMutation({
    mutationFn: ({ question, action }: { question: PendingQuestionEntity; action: QuestionReviewAction }) =>
      service.reviewPendingQuestion(question.id, action, { updatedAt: question.updatedAt, reviewRevision: question.reviewRevision }),
    retry: false,
  });

  useEffect(() => {
    const timer = setTimeout(
      () =>
        setFilters((previous) => ({
          ...previous,
          page: 0,
          search: search.trim(),
        })),
      300,
    );
    return () => clearTimeout(timer);
  }, [search]);

  const query = useQuery({
    queryKey: ["moderator-pending-questions", user.id, filters],
    queryFn: ({ signal }) => service.getPendingQuestions(filters, signal),
    enabled: user.role === "ROLE_MODERATOR",
    staleTime: 0,
    retry: false,
  });

  const refreshList = () => queryClient.invalidateQueries({ queryKey: ["moderator-pending-questions", user.id] });

  useEffect(() => {
    if (!query.data || query.isFetching || query.isError || filters.page === 0) return;
    if (filters.page >= query.data.totalPages) {
      setFilters((previous) => ({ ...previous, page: Math.max(0, query.data.totalPages - 1) }));
    }
  }, [query.data, query.isFetching, query.isError, filters.page]);

  const handleAction = async (question: PendingQuestionEntity, action: QuestionReviewAction) => {
    if (processing.current) return;
    processing.current = true;
    setNotice(action === "reject" && question.regenerationCount < 4 ? "Đang sinh lại câu hỏi…" : "Đang xử lý…");
    try {
      const result = await mutation.mutateAsync({ question, action });
      setNotice(action === "approve" ? "Đã duyệt câu hỏi."
        : result?.status === "NEEDS_CONSIDERATION" ? "Đã chuyển câu hỏi sang cân nhắc trước khi sử dụng."
          : "Đã sinh lại câu hỏi. Hãy kiểm tra bản mới trước khi duyệt.");
    } catch (error) {
      setNotice(isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message ?? "Không thể kiểm duyệt câu hỏi. Hãy kiểm tra danh sách trước khi thử lại."
        : "Không thể kiểm duyệt câu hỏi. Hãy kiểm tra danh sách trước khi thử lại.");
    } finally {
      await refreshList();
      processing.current = false;
    }
  };

  return {
    questions: query.data?.items ?? [],
    search,
    setSearch,
    types,
    notice,
    handleAction,
    isProcessing: mutation.isPending || query.isFetching,
    handleSaved: () => {
      setNotice("Đã lưu chỉnh sửa. Câu hỏi vẫn đang chờ duyệt.");
      void queryClient.invalidateQueries({ queryKey: ["moderator-pending-questions", user.id] });
    },
    questionType: filters.questionType,
    setQuestionType: (questionType: string) =>
      setFilters((previous) => ({ ...previous, page: 0, questionType })),
    page: filters.page,
    totalPages: query.data?.totalPages ?? 0,
    total: query.data?.totalElements ?? 0,
    setPage: (page: number) =>
      setFilters((previous) => ({ ...previous, page })),
    isPending: query.isPending,
    isFetching: query.isFetching,
    isError: query.isError,
    errorMessage: isAxiosError<{ message?: string }>(query.error)
      ? (query.error.response?.data?.message ?? "Không thể tải câu hỏi")
      : "Không thể tải câu hỏi",
    refresh: () => {
      void query.refetch();
    },
  };
}
