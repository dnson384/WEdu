import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import CreateDraftUsecase from "@/usecases/draft/CreateDraftUsecase";
import { CreateDraftEntity } from "@/domain/entities/draft.entity";
import { DraftRepositoryImpl } from "@/infrastructure/repositories/DraftRepositoryImpl";
import { CreateDraftPayload } from "@/presentation/schemas/draft.schema";

export async function POST(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;

    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const body = await req.json();

    const validated = CreateDraftPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400 },
      );
    }

    const { examName, questionsCount, questionTypes } = validated.data;
    const payload: CreateDraftEntity = {
      examName: examName,
      questionsCount: questionsCount,
      questionTypes: questionTypes,
    };

    const repo = new DraftRepositoryImpl();
    const usecase = new CreateDraftUsecase(repo);
    const response = await usecase.execute(payload, accessToken);

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
