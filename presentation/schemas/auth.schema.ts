import { z } from "zod";

export const LoginPayload = z.object({
  email: z.email({ error: "Email không đúng định dạng" }),
  plainPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
});

export type LoginPayload = z.infer<typeof LoginPayload>;
