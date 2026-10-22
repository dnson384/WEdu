import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { GetAllUsersUsecase } from "@/usecases/admin/GetAllUsersUsecase";
import { AdminRepositoryImpl } from "@/infrastructure/repositories/AdminRepositoryImpl";

export async function GET(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const repo = new AdminRepositoryImpl();
    const usecase = new GetAllUsersUsecase(repo);
    const response = await usecase.execute(accessToken);
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
