import { z } from "zod";

export const LessonsPayloadSchema = z.object({
  name: z.string({ error: "Tên bài học không được để trống" }),
  questionsCount: z.number().min(1, "Số lượng câu hỏi phải lớn hơn 0"),
  exerciseTypes: z.array(z.string()).min(1, "Vui lòng chọn ít nhất 1 dạng bài"),
  difficultyLevels: z
    .array(z.string())
    .min(1, "Vui lòng chọn ít nhất 1 độ khó"),
  learningOutcomes: z
    .array(z.string())
    .min(1, "Vui lòng chọn ít nhất 1 yêu cầu cần đạt"),
  questionTypes: z
    .array(z.string())
    .min(1, "Vui lòng chọn ít nhất 1 dạng câu hỏi"),
});

export const GeneratePayloadSchema = z.object({
  title: z.string({ error: "Tiêu đề không được để trống" }),
  chapter: z.string({
    error: "Chương không được để trống",
  }),
  lessons: z.array(LessonsPayloadSchema).min(1, "Vui lòng chọn ít nhất 1 bài"),
});

export type LessonsPayload = z.infer<typeof LessonsPayloadSchema>;
export type GeneratePayloadSchema = z.infer<typeof GeneratePayloadSchema>;
