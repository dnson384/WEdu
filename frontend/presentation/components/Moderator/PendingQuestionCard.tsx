"use client";

import { useState } from "react";
import { Check, Pencil, RefreshCw } from "lucide-react";
import { PendingQuestionEntity } from "@/domain/entities/pendingQuestion.entity";
import QuestionContent from "@/presentation/components/Exam/QuestionContent";
import MultipleChoiceContent from "@/presentation/components/Exam/MultipleChoiceContent";
import { ReviewPreviewAction } from "@/presentation/hooks/Moderator/usePendingQuestions";

interface Props {
  question: PendingQuestionEntity;
  order: number;
  onAction: (action: ReviewPreviewAction) => void;
}

export default function PendingQuestionCard({
  question,
  order,
  onAction,
}: Props) {
  const [editing, setEditing] = useState(false);
  const [template, setTemplate] = useState(question.question.template);
  const [options, setOptions] = useState(
    question.options.map((option) => option.template),
  );
  const cancelEdit = () => {
    setTemplate(question.question.template);
    setOptions(question.options.map((option) => option.template));
    setEditing(false);
  };
  return (
    <article
      className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm"
      aria-label={`Câu hỏi ${order}`}
    >
      <div className="flex flex-wrap items-center gap-2 border-b border-gray-100 px-6 py-4 text-xs">
        <span className="rounded-full bg-amber-50 px-3 py-1 font-medium text-amber-700">
          Chờ duyệt
        </span>
        <span className="rounded-full bg-blue-50 px-3 py-1 text-blue-700">
          {question.questionType}
        </span>
        <span className="rounded-full bg-gray-100 px-3 py-1 text-gray-600">
          {question.difficultyLevel}
        </span>
        <span className="ml-auto text-gray-400">
          {question.createdAt
            ? new Date(question.createdAt).toLocaleDateString("vi-VN", {
                timeZone: "Asia/Ho_Chi_Minh",
              })
            : "Chưa có ngày tạo"}
        </span>
      </div>

      <div className="p-6">
        <p className="mb-4 text-sm text-gray-500">
          {question.chapter} / {question.lesson}
        </p>
        {editing ? (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              onAction("edit");
            }}
            className="space-y-4"
          >
            <p className="text-sm text-blue-700">
              Chỉnh sửa thử trên giao diện, chưa lưu vào hệ thống. Giữ nguyên
              các placeholder công thức/ảnh.
            </p>
            <label className="block text-sm font-medium text-gray-700">
              Nội dung câu hỏi
              <textarea
                required
                value={template}
                onChange={(event) => setTemplate(event.target.value)}
                rows={4}
                className="mt-2 w-full rounded-xl border border-gray-300 p-3"
              />
            </label>

            {options.map((option, index) => (
              <label
                key={index}
                className="block text-sm font-medium text-gray-700"
              >
                Phương án {index + 1}
                <textarea
                  required
                  value={option}
                  onChange={(event) =>
                    setOptions(
                      options.map((text, optionIndex) =>
                        optionIndex === index ? event.target.value : text,
                      ),
                    )
                  }
                  rows={2}
                  className="mt-2 w-full rounded-xl border border-gray-300 p-3"
                />
              </label>
            ))}

            <div className="flex gap-3">
              <button
                type="submit"
                className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                Lưu chỉnh sửa (xem trước)
              </button>
              <button
                type="button"
                onClick={cancelEdit}
                className="rounded-xl border border-gray-200 px-4 py-2 text-sm text-gray-700"
              >
                Hủy chỉnh sửa
              </button>
            </div>
          </form>
        ) : (
          <>
            <QuestionContent order={order} {...question.question} />
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {question.options.map((option, index) => (
                <div
                  key={index}
                  className="rounded-xl bg-gray-50 p-3 text-sm text-gray-700"
                >
                  <span className="mr-2 font-semibold">
                    {question.questionType === "Đúng sai"
                      ? String.fromCharCode(97 + index)
                      : String.fromCharCode(65 + index)}
                    .
                  </span>
                  <MultipleChoiceContent {...option} />
                </div>
              ))}
            </div>
          </>
        )}
        <p className="mt-4 text-xs text-gray-500">
          Dạng bài: {question.exerciseType} · Yêu cầu cần đạt:{" "}
          {question.learningOutcomes.join(", ")}
        </p>
      </div>

      <footer className="flex flex-wrap gap-3 border-t border-gray-100 bg-gray-50/50 px-6 py-4">
        <button
          disabled={editing}
          onClick={() => onAction("approve")}
          className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-40"
        >
          <Check size={16} /> Duyệt thẳng
        </button>
        <button
          disabled={editing}
          onClick={() => setEditing(true)}
          className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40"
        >
          <Pencil size={16} /> Chỉnh sửa
        </button>
        <button
          disabled={editing}
          onClick={() => onAction("regenerate")}
          className="inline-flex items-center gap-2 rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-40"
        >
          <RefreshCw size={16} /> Từ chối – yêu cầu regenerate
        </button>
      </footer>
    </article>
  );
}
