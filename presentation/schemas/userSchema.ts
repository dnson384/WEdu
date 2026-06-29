import { z } from "zod";

export const ChangePasswordPayload = z.object({
  oldPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
  newPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
  confirmNewPassword: z
    .string()
    .min(8, "Mật khảu phải từ 8 ký tự")
    .max(32, "Mật khẩu tối đa 32 ký tự"),
});

export type ChangePasswordPayload = z.infer<typeof ChangePasswordPayload>;
