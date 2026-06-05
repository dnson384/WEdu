import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

import { GeneratePracticePayload } from "@/domain/entities/generatePractice.entity";
import { GeneratePayloadSchema } from "@/presentation/schemas/generate";
import { QuestionRepositoryImpl } from "@/infrastructure/repositories/questionRepositoryImpl";
import { GeneratePracticeUsecase } from "@/application/usecases/question/generate-practice";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const validationResult = GeneratePayloadSchema.safeParse(body);
    if (!validationResult.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: validationResult.error.flatten().fieldErrors,
        },
        { status: 400 },
      );
    }

    const { title, chapter, lessons } = validationResult.data;
    const payload: GeneratePracticePayload = {
      title: title,
      chapter: chapter,
      lessons: lessons.map((lesson) => ({
        name: lesson.name,
        questionsCount: lesson.questionsCount,
        exerciseTypes: lesson.exerciseTypes,
        difficultyLevels: lesson.difficultyLevels,
        learningOutcomes: lesson.learningOutcomes,
        questionTypes: lesson.questionTypes,
      })),
    };

    const repo = new QuestionRepositoryImpl();
    const usecase = new GeneratePracticeUsecase(repo);
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
