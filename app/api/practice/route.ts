import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { PracticeRepositoryImpl } from "@/infrastructure/repositories/practiceRepositoryImpl";
import { GetPracticeByIdUsecase } from "@/application/usecases/practice/getPracticeById";

export async function GET(req: NextRequest) {
  try {
    const id = req.nextUrl.searchParams.get("id");

    const repo = new PracticeRepositoryImpl();
    const usecase = new GetPracticeByIdUsecase(repo);
    const response = await usecase.execute(id!);
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
