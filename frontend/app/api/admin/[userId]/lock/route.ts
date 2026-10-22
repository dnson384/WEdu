import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { AdminRepositoryImpl } from "@/infrastructure/repositories/AdminRepositoryImpl";
import { LockUserUsecase } from "@/usecases/admin/LockUserUsecase";

export async function PUT(
  req: NextRequest,
  { params }: { params: Promise<{ userId: string }> },
) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const resolvedParams = await params;
    const userId = resolvedParams.userId;

    if (!userId) {
      return NextResponse.json({ message: "Thiếu userId" }, { status: 400 });
    }

    const repo = new AdminRepositoryImpl();
    const usecase = new LockUserUsecase(repo);
    const response = await usecase.execute(userId, accessToken);

    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err)) {
      return NextResponse.json(
        { message: err.response?.data.message },
        { status: err.status },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
