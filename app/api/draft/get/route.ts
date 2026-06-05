import GetDraftUsecase from "@/application/usecases/draft/getDraft";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/draftRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const draftId = req.nextUrl.searchParams.get("draftId");

    if (!draftId) {
      return NextResponse.json({ message: "Thiếu mã nháp" }, { status: 400 });
    }

    const repo = new DraftRepositoryImpl();
    const usecase = new GetDraftUsecase(repo);
    const response = await usecase.execute(draftId);

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
