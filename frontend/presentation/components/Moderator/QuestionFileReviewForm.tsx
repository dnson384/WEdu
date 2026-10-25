"use client";

interface Props {
  isPending: boolean;
  previewReady: boolean;
  needsCleanup: boolean;
  editedFile: File | null;
  onChooseFile: (file: File | null) => void;
  rejecting: boolean;
  onRejectingChange: (value: boolean) => void;
  reason: string;
  onReasonChange: (value: string) => void;
  error: string | null;
  onSubmit: (action: "approve" | "reject" | "cleanup") => void;
}

export default function QuestionFileReviewForm(props: Props) {
  const { isPending, previewReady, needsCleanup, editedFile, onChooseFile, rejecting,
    onRejectingChange, reason, onReasonChange, error, onSubmit } = props;
  const buttonClass = "rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium disabled:opacity-50";
  return (
    <footer className="max-h-[40vh] shrink-0 overflow-y-auto border-t border-gray-200 p-4" aria-busy={isPending}>
      {error && <p role="alert" className="mb-3 text-sm text-red-700">{error}</p>}
      <fieldset disabled={isPending}>
        <legend className="sr-only">Kiểm duyệt tệp câu hỏi</legend>
        {needsCleanup ? <div className="flex flex-wrap items-center gap-3">
          <p role="status" className="text-sm text-amber-800">Đã import câu hỏi. Chỉ xóa tệp gốc, không duyệt lại.</p>
          <button type="button" className={buttonClass} onClick={() => onSubmit("cleanup")}>Thử lại xóa tệp gốc</button>
        </div> : rejecting ? <form onSubmit={(event) => { event.preventDefault(); onSubmit("reject"); }}>
          <label htmlFor="rejection-reason" className="block text-sm font-medium">Lý do từ chối</label>
          <textarea id="rejection-reason" required maxLength={2000} value={reason}
            onChange={(event) => onReasonChange(event.target.value)} rows={3}
            className="mt-2 w-full rounded-lg border border-gray-300 p-2" />
          <div className="mt-2 flex gap-3">
            <button type="button" className={buttonClass} onClick={() => onRejectingChange(false)}>Hủy từ chối</button>
            <button type="submit" disabled={isPending || !reason.trim()} className={`${buttonClass} bg-red-600 text-white`}>Xác nhận từ chối</button>
          </div>
        </form> : <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm font-medium">Tải lên tệp chỉnh sửa
            <input type="file" accept=".docx" className="ml-2 max-w-64 text-sm"
              onChange={(event) => {
                onChooseFile(event.target.files?.[0] ?? null);
                event.target.value = "";
              }} />
          </label>
          {editedFile && <button type="button" className={buttonClass} onClick={() => onChooseFile(null)}>Dùng lại bản gốc</button>}
          <button type="button" disabled={isPending || !previewReady}
            className={`${buttonClass} bg-green-700 text-white`} onClick={() => onSubmit("approve")}>
            {editedFile ? "Duyệt bản chỉnh sửa" : "Duyệt ngay"}
          </button>
          <button type="button" className={`${buttonClass} text-red-700`} onClick={() => onRejectingChange(true)}>Từ chối</button>
        </div>}
      </fieldset>
      {isPending && <p role="status" className="mt-2 text-sm text-gray-600">Đang xử lý, vui lòng chờ…</p>}
    </footer>
  );
}
