"use client";

import { PendingQuestionEntity } from "@/domain/entities/pendingQuestion.entity";
import usePendingQuestionEdit from "@/presentation/hooks/Moderator/usePendingQuestionEdit";
import QuestionContentEditor from "./QuestionContentEditor";

interface Props {
  question: PendingQuestionEntity;
  onSaved: () => void;
  onCancel: () => void;
}

export default function PendingQuestionEditForm({ question, onSaved, onCancel }: Props) {
  const edit = usePendingQuestionEdit(question, onSaved);
  return <form onSubmit={(event) => { event.preventDefault(); void edit.submit(); }} className="space-y-5" aria-busy={edit.isSaving}>
    <p className="text-sm text-gray-600">Sửa nội dung và từng tham chiếu bên dưới. Chỉ upload ảnh khi Lưu; câu hỏi vẫn chờ duyệt sau khi lưu.</p>
    {Object.entries(edit.contents).map(([section, content]) => <QuestionContentEditor key={section}
      label={section === "question" ? "Nội dung câu hỏi" : `Phương án ${Number(section.split(".")[1]) + 1}`}
      content={content} disabled={edit.isSaving}
      onTemplate={(value) => edit.setTemplate(section, value)}
      onMath={(key, value) => edit.setMath(section, key, value)}
      onDescription={(key, value) => edit.setDescription(section, key, value)}
      onImage={(key, file) => edit.selectImage(section, key, file)}
      onInsert={(kind) => edit.insertReference(section, kind)} />)}
    {edit.error && <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{edit.error}</p>}
    <div className="flex gap-3">
      <button type="submit" disabled={edit.isSaving} className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">{edit.isSaving ? "Đang lưu và tải ảnh…" : "Lưu chỉnh sửa"}</button>
      <button type="button" disabled={edit.isSaving} onClick={onCancel} className="rounded-xl border border-gray-200 px-4 py-2 text-sm text-gray-700 disabled:opacity-50">Hủy chỉnh sửa</button>
    </div>
  </form>;
}
