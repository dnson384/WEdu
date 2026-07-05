import { GetAllExamsUsecase } from "@/application/usecases/exam/getAllExamUsecase";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/exam.repository";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    const refreshToken = req.cookies.get("refreshToken")?.value;

    const repo = new ExamsRepositoryImpl();
    const usecase = new GetAllExamsUsecase(repo);
    const response = await usecase.execute(accessToken!, refreshToken!);

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
