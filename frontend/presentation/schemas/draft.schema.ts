import { z } from "zod";

export const CreateDraftPayload = z.object({
  examName: z.string({ error: "Thiếu tên bài kiểm tra" }),
  questionsCount: z.number().min(1, "Bắt buộc có ít nhất 1 câu hỏi"),
  questionTypes: z
    .array(z.string())
    .min(3, "Bắt buộc chọn ít nhất 3 dạng câu hỏi"),
});

export const UpdateDraftParam = z.object({
  id: z.string({ error: "Thiếu Id" }),
  name: z.string({ error: "Thiếu Name" }),
});

// Update chương
export const UpdateChaptersDraftPayload = z.object({
  draftId: z.string(),
  add: z.array(UpdateDraftParam),
  del: z.array(z.string()),
});

// Update bài
export const UpdateLessonssDraftPayload = z.object({
  draftId: z.string(),
  chapterId: z.string(),
  add: z.array(UpdateDraftParam),
  del: z.array(z.string()),
});

export type CreateDraftPayload = z.infer<typeof CreateDraftPayload>;
export type UpdateDraftParam = z.infer<typeof UpdateDraftParam>;
export type UpdateChaptersDraftPayload = z.infer<
  typeof UpdateChaptersDraftPayload
>;
export type UpdateLessonssDraftPayload = z.infer<
  typeof UpdateLessonssDraftPayload
>;
