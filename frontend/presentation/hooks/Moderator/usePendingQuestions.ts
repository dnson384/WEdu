"use client";

import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { ModeratorService } from "@/presentation/services/ModeratorService";

export type ReviewPreviewAction = "approve" | "regenerate";
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

  const handleAction = (action: ReviewPreviewAction) => {
    const labels = {
      approve: "Duyệt thẳng",
      regenerate: "Từ chối duyệt – yêu cầu regenerate",
    };
    setNotice(
      `${labels[action]} chưa được kết nối. Chưa lưu thay đổi hoặc gửi yêu cầu lên hệ thống.`,
    );
  };

  return {
    questions: query.data?.items ?? [],
    search,
    setSearch,
    types,
    notice,
    handleAction,
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
