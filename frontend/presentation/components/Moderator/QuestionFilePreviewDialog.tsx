"use client";

import { useEffect, useRef } from "react";
import type { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import useQuestionFilePreview, { previewDocument } from "@/presentation/hooks/Moderator/useQuestionFilePreview";
import useQuestionFileReview, { ReviewComplete } from "@/presentation/hooks/Moderator/useQuestionFileReview";
import QuestionFileReviewForm from "@/presentation/components/Moderator/QuestionFileReviewForm";

export default function QuestionFilePreviewDialog({ file, onClose, onComplete, needsCleanup }: {
  file: QuestionFileEntity;
  onClose: () => void;
  onComplete: ReviewComplete;
  needsCleanup: boolean;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const review = useQuestionFileReview(file.key, onComplete);
  const { frameRef, status, error, handleFrameLoad, handleRetry } = useQuestionFilePreview(file.key, review.editedFile);
  const handleClose = () => { if (!review.isPending) onClose(); };

  useEffect(() => {
    const document = frameRef.current?.contentDocument;
    if (!document) return;
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        if (!review.isPending) onClose();
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => { document.removeEventListener("keydown", handleKey); };
  });

  useEffect(() => {
    const previousFocus = document.activeElement;
    const dialog = dialogRef.current;
    dialog?.showModal();
    closeRef.current?.focus();
    return () => {
      dialog?.close();
      if (previousFocus instanceof HTMLElement) previousFocus.focus();
    };
  }, []);

  return (
    <dialog ref={dialogRef} aria-labelledby="preview-file-title"
      onCancel={(event) => { event.preventDefault(); handleClose(); }}
      className="fixed inset-0 m-auto h-[90vh] w-[calc(100%-2rem)] max-w-6xl rounded-2xl border-0 bg-white p-0 shadow-xl backdrop:bg-black/40">
      <div className="flex h-full flex-col">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 p-4">
          <div className="min-w-0 flex-1">
            <h2 id="preview-file-title" className="break-all font-semibold text-gray-900">{file.fileName}</h2>
            <p className="mt-1 break-all text-sm text-gray-500">{review.editedFile ? `Bản chỉnh sửa: ${review.editedFile.name}` : "Xem trước bản gốc · Chỉ đọc"}</p>
          </div>
          <a href={file.presignedUrl} target="_blank" rel="noopener noreferrer"
            className="rounded-lg bg-blue-50 px-4 py-2 text-sm font-medium text-blue-600">Tải xuống</a>
          <button ref={closeRef} type="button" onClick={handleClose} disabled={review.isPending}
            className="rounded-lg border border-gray-200 px-4 py-2 text-sm">Đóng</button>
        </header>
        <div className="relative min-h-0 flex-1" aria-busy={status === "loading"}>
          {status === "loading" && <p role="status" className="absolute inset-0 z-10 bg-gray-50 p-10 text-center text-gray-600">Đang tải bản xem trước…</p>}
          {status === "error" && <div role="alert" className="absolute inset-0 z-10 bg-gray-50 p-10 text-center">
            <p className="text-red-700">{error}</p>
            <button type="button" onClick={handleRetry} className="mt-4 font-medium text-blue-600">Thử lại</button>
          </div>}
          <iframe ref={frameRef} title={`Nội dung ${file.fileName}`} sandbox="allow-same-origin"
            srcDoc={previewDocument} onLoad={() => {
              handleFrameLoad();
            }}
            className={`h-full w-full border-0 ${status !== "ready" ? "invisible" : ""}`} />
        </div>
        <QuestionFileReviewForm isPending={review.isPending} previewReady={status === "ready"}
          needsCleanup={needsCleanup} editedFile={review.editedFile} onChooseFile={review.chooseFile}
          rejecting={review.rejecting} onRejectingChange={review.setRejecting}
          reason={review.reason} onReasonChange={review.setReason} error={review.error} onSubmit={review.submit} />
      </div>
    </dialog>
  );
}
