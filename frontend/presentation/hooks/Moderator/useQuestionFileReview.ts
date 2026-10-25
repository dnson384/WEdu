"use client";

import { useMemo, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { ModeratorService } from "@/presentation/services/ModeratorService";

type ReviewAction = "approve" | "reject" | "cleanup";
export type ReviewComplete = (key: string, message: string, needsCleanup: boolean) => void;

export default function useQuestionFileReview(key: string, onComplete: ReviewComplete) {
  const service = useMemo(() => new ModeratorService(), []);
  const [editedFile, setEditedFile] = useState<File | null>(null);
  const [reason, setReason] = useState("");
  const [rejecting, setRejecting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);
  const mutation = useMutation({
    retry: false,
    mutationFn: async (action: ReviewAction) => {
      if (action === "approve") return service.approveQuestionFile(key, editedFile);
      if (action === "reject") await service.rejectQuestionFile({ key, reason: reason.trim() });
      else await service.deleteQuestionFile(key);
      return { message: action === "reject" ? "Đã từ chối tệp tin câu hỏi" : "Đã xóa tệp gốc sau import", fileDeleted: true };
    },
    onSuccess: (result) => onComplete(key, result.message, !result.fileDeleted),
    onError: (cause) => setError(isAxiosError<{ message?: string }>(cause)
      ? cause.response?.data?.message || "Không thể hoàn tất kiểm duyệt. Kiểm tra danh sách trước khi thử lại."
      : "Không thể hoàn tất kiểm duyệt. Kiểm tra danh sách trước khi thử lại."),
    onSettled: () => { inFlight.current = false; },
  });

  const submit = (action: ReviewAction) => {
    if (inFlight.current) return;
    if (action === "reject" && (!reason.trim() || reason.trim().length > 2000)) {
      setError("Lý do từ chối phải có từ 1 đến 2000 ký tự");
      return;
    }
    setError(null);
    inFlight.current = true;
    mutation.mutate(action);
  };

  const chooseFile = (file: File | null) => {
    if (inFlight.current) return;
    setError(null);
    if (file && (!file.name.toLowerCase().endsWith(".docx") || !file.size || file.size > 50 * 1024 * 1024)) {
      setError("Chọn tệp DOCX không rỗng, tối đa 50 MB");
      return;
    }
    setEditedFile(file);
  };

  return { editedFile, chooseFile, reason, setReason, rejecting, setRejecting, error,
    isPending: mutation.isPending, submit };
}
