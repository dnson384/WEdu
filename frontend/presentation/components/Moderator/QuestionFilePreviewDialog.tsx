"use client";

import { useEffect, useRef } from "react";
import type { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import useQuestionFilePreview, { previewDocument } from "@/presentation/hooks/Moderator/useQuestionFilePreview";

export default function QuestionFilePreviewDialog({ file, onClose }: {
  file: QuestionFileEntity;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const { frameRef, status, error, handleFrameLoad, handleRetry } = useQuestionFilePreview(file.key);

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
      onCancel={(event) => { event.preventDefault(); onClose(); }}
      className="fixed inset-0 m-auto h-[90vh] w-[calc(100%-2rem)] max-w-6xl rounded-2xl border-0 bg-white p-0 shadow-xl backdrop:bg-black/40">
      <div className="flex h-full flex-col">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 p-4">
          <div className="min-w-0 flex-1">
            <h2 id="preview-file-title" className="break-all font-semibold text-gray-900">{file.fileName}</h2>
            <p className="mt-1 text-sm text-gray-500">Xem trước · Chỉ đọc</p>
          </div>
          <a href={file.presignedUrl} target="_blank" rel="noopener noreferrer"
            className="rounded-lg bg-blue-50 px-4 py-2 text-sm font-medium text-blue-600">Tải xuống</a>
          <button ref={closeRef} type="button" onClick={onClose}
            className="rounded-lg border border-gray-200 px-4 py-2 text-sm">Đóng</button>
        </header>
        <div className="relative min-h-0 flex-1" aria-busy={status === "loading"}>
          {status === "loading" && <p role="status" className="absolute inset-0 z-10 bg-gray-50 p-10 text-center text-gray-600">Đang tải bản xem trước…</p>}
          {status === "error" && <div role="alert" className="absolute inset-0 z-10 bg-gray-50 p-10 text-center">
            <p className="text-red-700">{error}</p>
            <button type="button" onClick={handleRetry} className="mt-4 font-medium text-blue-600">Thử lại</button>
          </div>}
          <iframe ref={frameRef} title={`Nội dung ${file.fileName}`} sandbox="allow-same-origin"
            srcDoc={previewDocument} onLoad={(event) => {
              const frameDocument = event.currentTarget.contentDocument;
              if (frameDocument) frameDocument.onkeydown = (keyEvent) => {
                if (keyEvent.key === "Escape") { keyEvent.preventDefault(); onClose(); }
              };
              handleFrameLoad();
            }}
            className={`h-full w-full border-0 ${status !== "ready" ? "invisible" : ""}`} />
        </div>
      </div>
    </dialog>
  );
}
