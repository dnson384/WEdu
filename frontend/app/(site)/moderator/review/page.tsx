"use client";

import { ClipboardCheck, Search } from "lucide-react";
import usePendingQuestions from "@/presentation/hooks/Moderator/usePendingQuestions";
import PendingQuestionCard from "@/presentation/components/Moderator/PendingQuestionCard";

export default function ReviewPage() {
  const {
    questions,
    search,
    setSearch,
    questionType,
    setQuestionType,
    types,
    notice,
    handleAction,
    page,
    totalPages,
    total,
    setPage,
    isPending,
    isFetching,
    isError,
    errorMessage,
    refresh,
  } = usePendingQuestions();

  return (
    <div className="mx-auto max-w-6xl">
      <header className="mb-6">
        <p className="mb-2 text-sm font-medium text-blue-600">
          Không gian kiểm duyệt
        </p>
        <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">
          Kiểm duyệt câu hỏi
        </h1>
        <p className="mt-3 text-gray-500">
          Xem nội dung, chỉnh sửa hoặc yêu cầu tạo lại câu hỏi trước khi duyệt.
        </p>
      </header>

      <p className="mb-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        Danh sách câu hỏi chờ duyệt từ hệ thống. Các thao tác duyệt, lưu chỉnh
        sửa và yêu cầu regenerate chưa được kết nối.
      </p>

      <section
        aria-label="Bộ lọc câu hỏi"
        className="mb-6 flex flex-wrap gap-4 rounded-2xl border border-gray-200 bg-white p-5"
      >
        <label className="flex min-w-0 flex-1 items-center gap-3 rounded-xl border border-gray-200 px-3 focus-within:ring-2 focus-within:ring-blue-500">
          <Search size={18} aria-hidden="true" className="text-gray-400" />
          <span className="sr-only">Tìm câu hỏi</span>
          <input
            maxLength={200}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Tìm nội dung, chương hoặc bài học…"
            className="w-full bg-transparent py-3 text-sm text-gray-900 outline-none"
          />
        </label>

        <label className="flex items-center gap-3 text-sm text-gray-600">
          Loại câu hỏi
          <select
            value={questionType}
            onChange={(event) => setQuestionType(event.target.value)}
            className="rounded-xl border border-gray-200 bg-white p-3 text-gray-900"
          >
            <option value="">Tất cả</option>
            {types.map((type) => (
              <option key={type}>{type}</option>
            ))}
          </select>
        </label>
      </section>

      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="flex items-center gap-2 font-semibold text-gray-900">
          <ClipboardCheck size={20} className="text-blue-600" />
          Câu hỏi chờ duyệt{" "}
          <span className="rounded-full bg-blue-100 px-2.5 py-1 text-xs text-blue-700">
            {isPending || isError ? "…" : total}
          </span>
        </h2>
        <button
          onClick={refresh}
          disabled={isFetching}
          className="text-sm font-medium text-blue-600 disabled:opacity-50"
        >
          Làm mới
        </button>
      </div>

      <p role="status" className="mb-4 text-sm text-blue-700">
        {notice}
      </p>

      <div className="space-y-5" aria-busy={isFetching}>
        {isError ? (
          <div role="alert" className="rounded-xl bg-red-50 p-6 text-red-700">
            {errorMessage}
            <button
              onClick={refresh}
              disabled={isFetching}
              className="ml-3 underline"
            >
              Thử lại
            </button>
          </div>
        ) : isPending ? (
          <p
            role="status"
            className="rounded-2xl bg-white p-12 text-center text-gray-500"
          >
            Đang tải câu hỏi chờ duyệt…
          </p>
        ) : (
          questions.map((question, index) => (
            <PendingQuestionCard
              key={question.id}
              question={question}
              order={page * 10 + index + 1}
              onAction={handleAction}
            />
          ))
        )}
        {!isPending && !isError && questions.length === 0 && (
          <div className="rounded-2xl border border-gray-200 bg-white px-6 py-16 text-center">
            <Search size={32} className="mx-auto mb-3 text-gray-300" />

            <h3 className="font-semibold text-gray-900">
              Không có câu hỏi phù hợp
            </h3>
            <p className="mt-2 text-sm text-gray-500">
              Thử từ khóa khác hoặc chọn tất cả loại câu hỏi.
            </p>
          </div>
        )}
      </div>

      <nav
        aria-label="Phân trang câu hỏi"
        className="mt-6 flex flex-wrap items-center justify-between gap-4 text-sm text-gray-600"
      >
        <span>
          {!isPending && !isError
            ? `${total} câu hỏi · Tối đa 10 câu/trang${totalPages > 0 && page < totalPages ? ` · Trang ${page + 1}/${totalPages}` : ""}`
            : "10 câu/trang"}
        </span>

        <div className="flex gap-3">
          <button
            onClick={() => setPage(page - 1)}
            disabled={page === 0 || isFetching}
            className="rounded-xl border border-gray-200 bg-white px-4 py-2 disabled:opacity-40"
          >
            Trang trước
          </button>
          <button
            onClick={() => setPage(page + 1)}
            disabled={isFetching || isError || page + 1 >= totalPages}
            className="rounded-xl border border-gray-200 bg-white px-4 py-2 disabled:opacity-40"
          >
            Trang sau
          </button>
        </div>
      </nav>
    </div>
  );
}
