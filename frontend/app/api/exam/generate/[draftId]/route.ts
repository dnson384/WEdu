import { GenerateExamUsecase } from "@/usecases/exam/GenerateExamUsecase";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/ExamsRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ draftId: string }> },
) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const resolvedParams = await params;
    const draftId = resolvedParams.draftId;

    if (!draftId) {
      return NextResponse.json({ message: "Thiếu draftId" }, { status: 400 });
    }

    const repo = new ExamsRepositoryImpl();
    const usecase = new GenerateExamUsecase(repo);
    const response = await usecase.execute(draftId, accessToken);

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
