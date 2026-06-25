import { GetRecentExamUsecase } from "@/application/usecases/exam/getRecentExamUsecase";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/exam.repository";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const repo = new ExamsRepositoryImpl();
    const usecase = new GetRecentExamUsecase(repo);
    const response = await usecase.execute();

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
