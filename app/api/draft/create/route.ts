import CreateDraftUsecase from "@/application/usecases/draft/createDraft";
import { CreateDraftPayloadEntity } from "@/domain/entities/draft.entity";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/draftRepositoryImpl";
import { CreateDraftPayload } from "@/presentation/schemas/draft.schema";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const validated = CreateDraftPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: validated.error.flatten().fieldErrors,
        },
        { status: 400 },
      );
    }

    const { examName, questionsCount, questionTypes } = validated.data;
    const payload: CreateDraftPayloadEntity = {
      examName: examName,
      questionsCount: questionsCount,
      questionTypes: questionTypes,
    };

    const repo = new DraftRepositoryImpl();
    const usecase = new CreateDraftUsecase(repo);
    const response = await usecase.execute(payload);

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
