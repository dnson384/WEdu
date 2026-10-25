"use client";

import { FileText, FolderOpen, RefreshCw, AlertCircle } from "lucide-react";
import useQuestionFiles from "@/presentation/hooks/Moderator/useQuestionFiles";
import DeleteQuestionFileDialog from "@/presentation/components/Moderator/DeleteQuestionFileDialog";
import QuestionFileRow from "@/presentation/components/Moderator/QuestionFileRow";
import QuestionFilePreviewDialog from "@/presentation/components/Moderator/QuestionFilePreviewDialog";

export default function QuestionFilesPage() {
  const {
    files,
    previewFile,
    handlePreviewClick,
    handleClosePreview,
    isPending,
    isFetching,
    isError,
    errorMessage,
    selectedFile,
    isDeleting,
    deleteError,
    notification,
    handleRefresh,
    handleDeleteClick,
    handleCancelDelete,
    handleConfirmDelete,
  } = useQuestionFiles();

  return (
    <div className="mx-auto max-w-6xl">
      {previewFile && <QuestionFilePreviewDialog key={previewFile.key} file={previewFile} onClose={handleClosePreview} />}
      <DeleteQuestionFileDialog
        file={selectedFile}
        isDeleting={isDeleting}
        error={deleteError}
        onCancel={handleCancelDelete}
        onConfirm={handleConfirmDelete}
      />
      {notification && (
        <p
          role="status"
          className="mb-5 rounded-xl bg-green-50 p-4 text-sm text-green-800"
        >
          {notification}
        </p>
      )}
      <header className="flex flex-wrap items-start justify-between gap-4 mb-8">
        <div>
          <p className="text-sm font-medium text-blue-600 mb-2">
            Không gian kiểm duyệt
          </p>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">
            Quản lý tệp tin câu hỏi
          </h1>
          <p className="mt-3 text-gray-500">
            Xem và tải xuống các tài liệu câu hỏi đã được gửi lên hệ thống.
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={isFetching}
          className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          <RefreshCw size={16} className={isFetching ? "animate-spin" : ""} />{" "}
          Làm mới
        </button>
      </header>

      <section
        aria-label="Danh sách tệp tin"
        aria-busy={isFetching}
        className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm"
      >
        <div className="border-b border-gray-100 px-6 py-5 flex items-center gap-3">
          <div className="rounded-xl bg-blue-50 p-2.5 text-blue-600">
            <FolderOpen size={22} />
          </div>
          <div>
            <h2 className="font-semibold text-gray-900">Tệp tin câu hỏi</h2>
            <p className="mt-0.5 text-sm text-gray-500">
              {isPending ? "Đang lấy danh sách…" : `${files.length} tệp tin`}
            </p>
          </div>
        </div>
        {isError ? (
          <div role="alert" className="p-10 text-center">
            <AlertCircle className="mx-auto text-red-500 mb-3" size={32} />
            <p className="text-gray-700">{errorMessage}</p>
            <button
              onClick={handleRefresh}
              disabled={isFetching}
              className="mt-4 font-medium text-blue-600 disabled:opacity-50"
            >
              Thử lại
            </button>
          </div>
        ) : isPending ? (
          <div role="status" className="p-12 text-center text-gray-500">
            <RefreshCw
              className="mx-auto mb-3 animate-spin text-blue-600"
              size={28}
            />
            Đang tải danh sách tệp tin…
          </div>
        ) : files.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <FileText size={40} className="mx-auto mb-4 text-gray-300" />
            <h3 className="font-semibold text-gray-900">
              Chưa có tệp tin câu hỏi
            </h3>
            <p className="mt-2 text-sm text-gray-500">
              Các tài liệu được tải lên sẽ xuất hiện tại đây.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500">
                <tr>
                  <th className="px-6 py-4 font-medium">Tên tệp tin</th>
                  <th className="px-4 py-4 font-medium whitespace-nowrap">
                    Dung lượng
                  </th>
                  <th className="px-4 py-4 font-medium whitespace-nowrap">
                    Cập nhật lần cuối
                  </th>
                  <th className="px-6 py-4 font-medium text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {files.map((file) => (
                  <QuestionFileRow
                    key={file.key}
                    file={file}
                    isDeleting={isDeleting}
                    onDelete={handleDeleteClick}
                    onPreview={handlePreviewClick}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
