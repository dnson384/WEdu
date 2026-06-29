import { ExamExportPayload } from "../../../../presentation/schemas/export.schema";
import { NextRequest, NextResponse } from "next/server";
import { ExamsRepositoryImpl } from "@/infrastructure/repositories/exam.repository";
import { ExamExportPayloadEntity } from "@/domain/entities/exam.entity";
import { ExportExamWordFileUsecase } from "@/application/usecases/exam/exportExamWordFileUsecase";

export async function POST(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    const refreshToken = req.cookies.get("refreshToken")?.value;

    const payload: ExamExportPayload = await req.json();

    const payloadDomain: ExamExportPayloadEntity[] = payload.map((p) => ({
      questionType: p.questionType,
      questionIds: p.questionIds,
    }));

    const repo = new ExamsRepositoryImpl();
    const usecase = new ExportExamWordFileUsecase(repo);
    const response = await usecase.execute(
      payloadDomain,
      accessToken!,
      refreshToken!,
    );

    return new NextResponse(new Uint8Array(response), {
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
