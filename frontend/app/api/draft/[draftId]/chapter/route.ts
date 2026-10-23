import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import UpdateChaptersDraftUsecase from "@/usecases/draft/UpdateChaptersDraftUsecase";
import { UpdateDraftEntity } from "@/domain/entities/draft.entity";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/DraftRepositoryImpl";
import { UpdateDraftPayload } from "@/presentation/schemas/draft.schema";

export async function PUT(
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

    const body = await req.json();

    const validated = UpdateDraftPayload.safeParse(body);

    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400 },
      );
    }

    const { add, del } = validated.data;

    const payload: UpdateDraftEntity = {
      add: add,
      del: del,
    };

    const repo = new DraftRepositoryImpl();
    const usecase = new UpdateChaptersDraftUsecase(repo);
    const response = await usecase.execute(draftId, payload, accessToken);

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
