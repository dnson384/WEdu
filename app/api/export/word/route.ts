import { ExportPayload } from "@/domain/entities/practice.entity";
import { LessonExportPayload } from "../../../../presentation/schemas/export.schema";
import { NextRequest, NextResponse } from "next/server";
import { PracticeRepositoryImpl } from "@/infrastructure/repositories/practiceRepositoryImpl";
import { ExportWordFileUsecase } from "@/application/usecases/practice/exportWordFile";

export async function POST(req: NextRequest) {
  try {
    const {
      title,
      questionsSorted,
    }: { title: string; questionsSorted: LessonExportPayload } =
      await req.json();

    const payload: ExportPayload = {
      title: title,
      questionsSorted: questionsSorted,
    };

    const repo = new PracticeRepositoryImpl();
    const usecase = new ExportWordFileUsecase(repo);
    const response = await usecase.execute(payload);

    return new NextResponse(response, {
      status: 200,
      headers: {
        "Content-Type":
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "Content-Disposition": 'attachment; filename="export.docx"',
      },
    });
  } catch (error: any) {
    console.error("Lỗi tại Next.js BFF:", error.message);
    return NextResponse.json(
      { error: "Lỗi khi xuất file từ Backend" },
      { status: 500 },
    );
  }
}
