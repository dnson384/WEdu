import { changePasswordUsecase } from "@/application/usecases/user/changePasswordUsecase";
import { UpdateUsernameUsecase } from "@/application/usecases/user/updateUsernameUsecase";
import { ChangePasswordPayloadEntity } from "@/domain/entities/user.entity";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { ChangePasswordPayload } from "@/presentation/schemas/userSchema";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;
  const refreshToken = req.cookies.get("refreshToken")?.value;

  const body = await req.json();

  const validated = ChangePasswordPayload.safeParse(body);
  if (!validated.success) {
    return NextResponse.json(
      {
        message: "Dữ liệu đầu vào không hợp lệ!",
        error: validated.error.flatten().fieldErrors,
      },
      { status: 400 },
    );
  }

  const { oldPassword, newPassword, confirmNewPassword } = validated.data;
  const payload: ChangePasswordPayloadEntity = {
    oldPassword: oldPassword,
    newPassword: newPassword,
    confirmNewPassword: confirmNewPassword,
  };

  try {
    const repo = new UserRepositoryImpl();
    const usecase = new changePasswordUsecase(repo);
    const response = await usecase.execute(
      payload,
      accessToken!,
      refreshToken!,
    );

    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err) && err.response) {
      return NextResponse.json(
        { message: err.response.data.message },
        { status: err.response.status },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
