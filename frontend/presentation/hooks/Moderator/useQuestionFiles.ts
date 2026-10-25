"use client";

import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import { ModeratorService } from "@/presentation/services/ModeratorService";

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function useQuestionFiles() {
  const { user } = useAuth();
  const service = useMemo(() => new ModeratorService(), []);
  const queryClient = useQueryClient();
  const queryKey = ["moderator-question-files", user.id] as const;
  const [selectedFile, setSelectedFile] = useState<QuestionFileEntity | null>(
    null,
  );
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [previewFile, setPreviewFile] = useState<QuestionFileEntity | null>(null);
  const [notification, setNotification] = useState<string | null>(null);
  const deleteInFlight = useRef(false);

  const deletion = useMutation({
    mutationFn: (key: string) => service.deleteQuestionFile(key),
    retry: false,
    onSuccess: async (_, key) => {
      setSelectedFile(null);
      setNotification("Đã xóa tệp tin câu hỏi thành công");
      await queryClient.cancelQueries({ queryKey, exact: true });
      queryClient.setQueryData<QuestionFileEntity[]>(queryKey, (previous) =>
        previous?.filter((file) => file.key !== key),
      );
      await queryClient.invalidateQueries({ queryKey, exact: true });
    },
    onError: (error) => {
      setDeleteError(
        isAxiosError<{ message?: string }>(error)
          ? error.response?.data?.message ||
              "Không thể xóa tệp tin. Vui lòng thử lại"
          : "Không thể xóa tệp tin. Vui lòng thử lại",
      );
    },
    onSettled: () => {
      deleteInFlight.current = false;
    },
  });

  const { data, isPending, isFetching, isError, error, refetch } = useQuery({
    queryKey,
    queryFn: () => service.getQuestionFiles(),
    enabled: user.role === "ROLE_MODERATOR",
    staleTime: 0,
    refetchOnMount: "always",
    retry: false,
  });
  const files = useMemo(
    () =>
      (data ?? []).map((file) => ({
        ...file,
        sizeLabel: formatSize(file.size),
        lastModifiedLabel: file.lastModified
          ? new Date(file.lastModified).toLocaleString("vi-VN")
          : "—",
      })),
    [data],
  );
  const errorMessage =
    (isAxiosError<{ message: string }>(error)
      ? error.response?.data?.message
      : undefined) || "Không thể tải danh sách tệp tin. Vui lòng thử lại.";

  const handleRefresh = () => {
    void refetch();
  };

  const handleDeleteClick = (file: QuestionFileEntity) => {
    if (deleteInFlight.current) return;
    setSelectedFile(file);
    setDeleteError(null);
    setNotification(null);
  };

  const handleCancelDelete = () => {
    if (deleteInFlight.current) return;
    setSelectedFile(null);
    setDeleteError(null);
  };

  const handleConfirmDelete = () => {
    if (!selectedFile || deleteInFlight.current) return;
    deleteInFlight.current = true;
    setDeleteError(null);
    deletion.mutate(selectedFile.key);
  };

  return {
    previewFile,
    handlePreviewClick: (file: QuestionFileEntity) => setPreviewFile(file),
    handleClosePreview: () => setPreviewFile(null),
    files,
    isPending,
    isFetching,
    isError,
    errorMessage,
    selectedFile,
    deleteError,
    notification,
    isDeleting: deletion.isPending,
    handleRefresh,
    handleDeleteClick,
    handleCancelDelete,
    handleConfirmDelete,
  };
}
