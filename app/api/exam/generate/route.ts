import { GenerateExamUsecase } from "@/application/usecases/exam/generateExam.usecase";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/exam.repository";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const { draftId } = await req.json();
    if (!draftId) {
      return NextResponse.json({ message: "Thiếu mã nháp" }, { status: 400 });
    }

    const repo = new ExamsRepositoryImpl();
    const usecase = new GenerateExamUsecase(repo);
    const response = await usecase.execute(draftId);

    return NextResponse.json(response, { status: 201 });
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
