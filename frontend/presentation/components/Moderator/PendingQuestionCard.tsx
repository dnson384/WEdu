"use client";

import { useState } from "react";
import PendingQuestionEditForm from "./PendingQuestionEditForm";
import { Check, Pencil, RefreshCw } from "lucide-react";
import { PendingQuestionEntity, QuestionReviewAction } from "@/domain/entities/pendingQuestion.entity";
import QuestionContent from "@/presentation/components/Exam/QuestionContent";
import MultipleChoiceContent from "@/presentation/components/Exam/MultipleChoiceContent";

interface Props {
  question: PendingQuestionEntity;
  order: number;
  onAction: (question: PendingQuestionEntity, action: QuestionReviewAction) => void;
  isProcessing: boolean;
  onSaved: () => void;
}

export default function PendingQuestionCard({
  question,
  order,
  onAction,
  onSaved,
  isProcessing,
}: Props) {
  const [editing, setEditing] = useState(false);
  const displayed = question.latestRegeneratedQuestion
    ? { ...question, ...question.latestRegeneratedQuestion } : question;
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
        <span className="text-gray-500">
          Đã từ chối {question.regenerationCount}/5
          {question.latestRegeneratedQuestion ? ` · Bản sinh lại ${question.regenerationCount}` : ""}
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
          <PendingQuestionEditForm key={`${question.updatedAt}-${question.reviewRevision}`} question={displayed} onCancel={() => setEditing(false)}
            onSaved={() => { setEditing(false); onSaved(); }} />
        ) : (
          <>
            <QuestionContent order={order} {...displayed.question} />
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {displayed.options.map((option, index) => (
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
          disabled={editing || isProcessing}
          onClick={() => onAction(question, "approve")}
          className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-40"
        >
          <Check size={16} /> Duyệt
        </button>
        <button
          disabled={editing || isProcessing}
          onClick={() => setEditing(true)}
          className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40"
        >
          <Pencil size={16} /> Chỉnh sửa
        </button>
        <button
          disabled={editing || isProcessing}
          onClick={() => onAction(question, "reject")}
          className="inline-flex items-center gap-2 rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-40"
        >
          <RefreshCw size={16} /> {question.regenerationCount >= 4 ? "Từ chối – chuyển sang cân nhắc" : "Từ chối – sinh lại"}
        </button>
      </footer>
    </article>
  );
}
