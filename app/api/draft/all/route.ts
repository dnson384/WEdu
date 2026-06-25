import { GetAllUserDraftsUsecase } from "@/application/usecases/draft/GetAllUserDraftUsecase";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/draftRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const repo = new DraftRepositoryImpl();
    const usecase = new GetAllUserDraftsUsecase(repo);
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
