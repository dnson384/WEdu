import { z } from "zod";

export const LoginPayload = z.object({
  email: z.email({ error: "Email không đúng định dạng" }),
  plainPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
});

export const RegisterPayload = z.object({
  email: z.email({ error: "Email không đúng định dạng" }),
  plainPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
  confirmPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
  username: z.string().min(1, "Tên người dùng tối thiểu 1 ký tự"),
  loginMethod: z.enum(["LOCAL", "GOOGLE"]),
});

export type LoginPayload = z.infer<typeof LoginPayload>;
export type RegisterPayload = z.infer<typeof RegisterPayload>;
