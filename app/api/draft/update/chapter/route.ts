import UpdateChaptersUsecase from "@/application/usecases/draft/updateChapters";
import { UpdateChaptersDraftPayloadEntity } from "@/domain/entities/draft.entity";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/draftRepositoryImpl";
import { UpdateChaptersDraftPayload } from "@/presentation/schemas/draft.schema";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function PUT(req: NextRequest) {
  try {
    const body = await req.json();

    const validated = UpdateChaptersDraftPayload.safeParse(body);

    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: validated.error.flatten().fieldErrors,
        },
        { status: 400 },
      );
    }

    const { draftId, add, del } = validated.data;

    const payload: UpdateChaptersDraftPayloadEntity = {
      draftId: draftId,
      add: add,
      del: del,
    };

    const repo = new DraftRepositoryImpl();
    const usecase = new UpdateChaptersUsecase(repo);
    const response = await usecase.execute(payload);

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
