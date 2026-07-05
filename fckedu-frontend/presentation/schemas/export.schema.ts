import { z } from "zod";

export const ExportPayload = z.object({
  examId: z.string(),
  examName: z.string(),
});

export type ExportPayload = z.infer<typeof ExportPayload>;
