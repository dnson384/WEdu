import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { DeleteDraftUsecase } from "@/usecases/draft/DeleteDraftUsecase";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/DraftRepositoryImpl";

export async function DELETE(
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

    const repo = new DraftRepositoryImpl();
    const usecase = new DeleteDraftUsecase(repo);
    const response = await usecase.execute(draftId, accessToken);

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
