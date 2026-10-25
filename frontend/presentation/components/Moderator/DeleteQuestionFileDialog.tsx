"use client";

import { useEffect, useRef } from "react";
import { AlertTriangle, LoaderCircle } from "lucide-react";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

interface Props {
  file: QuestionFileEntity | null;
  isDeleting: boolean;
  error: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}

export default function DeleteQuestionFileDialog({
  file,
  isDeleting,
  error,
  onCancel,
  onConfirm,
}: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (file && dialog && !dialog.open) {
      dialog.showModal();
      cancelRef.current?.focus();
    } else if (!file && dialog?.open) {
      dialog.close();
    }
  }, [file]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="delete-file-title"
      aria-describedby="delete-file-description"
      aria-busy={isDeleting}
      onCancel={(event) => {
        event.preventDefault();
        if (!isDeleting) onCancel();
      }}
      className="fixed inset-0 m-auto w-[calc(100%-2rem)] max-w-md rounded-2xl border-0 bg-white p-6 shadow-xl backdrop:bg-black/40"
    >
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-50 text-red-600">
        <AlertTriangle size={24} />
      </div>
      <h2 id="delete-file-title" className="text-xl font-bold text-gray-900">
        Xóa tệp tin câu hỏi?
      </h2>
      <div id="delete-file-description" className="mt-3 text-sm text-gray-600">
        <p>Bạn có chắc muốn xóa tệp sau khỏi hệ thống?</p>
        <p className="mt-2 break-all font-semibold text-gray-900">
          {file?.fileName}
        </p>
      </div>
      {error && (
        <p
          role="alert"
          className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700"
        >
          {error}
        </p>
      )}
      <div className="mt-6 flex justify-end gap-3">
        <button
          ref={cancelRef}
          type="button"
          onClick={onCancel}
          disabled={isDeleting}
          className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Hủy
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={isDeleting}
          className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
        >
          {isDeleting && <LoaderCircle size={16} className="animate-spin" />}
          {isDeleting ? "Đang xóa…" : "Xóa tệp"}
        </button>
      </div>
    </dialog>
  );
}
