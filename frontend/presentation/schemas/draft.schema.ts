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

// Update chương và update bài
export const UpdateDraftPayload = z.object({
  add: z.array(UpdateDraftParam),
  del: z.array(z.string()),
});

export type CreateDraftPayload = z.infer<typeof CreateDraftPayload>;
export type UpdateDraftParam = z.infer<typeof UpdateDraftParam>;
export type UpdateDraftPayload = z.infer<typeof UpdateDraftPayload>;
