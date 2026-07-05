import { GetExamByIdUsecase } from "@/application/usecases/exam/getExamById.usecase";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/exam.repository";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    const refreshToken = req.cookies.get("refreshToken")?.value;

    const examId = req.nextUrl.searchParams.get("examId");
    if (!examId) {
      return NextResponse.json(
        { message: "Thiếu mã bài kiểm tra" },
        { status: 400 },
      );
    }

    const repo = new ExamsRepositoryImpl();
    const usecase = new GetExamByIdUsecase(repo);
    const response = await usecase.execute(examId, accessToken!, refreshToken!);

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
