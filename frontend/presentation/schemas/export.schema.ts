import { z } from "zod";

export const ExportPayload = z.object({
  examId: z.string(),
  examName: z.string(),
});

export type ExportPayload = z.infer<typeof ExportPayload>;

export const ExportMatrixPayload = z.object({
  examId: z.string().trim().min(1),
  examName: z.string().refine((value) => value.trim().length > 0),
});

export type ExportMatrixPayload = z.infer<typeof ExportMatrixPayload>;
