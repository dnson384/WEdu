import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";

import { ExportExamUsecase } from "@/usecases/exporter/ExportExamUsecase";
import { ExporterRepositoryImpl } from "@/infrastructure/repositories/ExporterRepositoryImpl";
import { ExportPayload } from "@/presentation/schemas/export.schema";

export async function POST(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    if (!accessToken) {
      return NextResponse.json({ message: "Thiếu AT" }, { status: 400 });
    }

    const body = await req.json();

    const validated = ExportPayload.safeParse(body);
    if (!validated.success) {
      return NextResponse.json(
        {
          message: "Dữ liệu đầu vào không hợp lệ!",
          error: z.flattenError(validated.error).fieldErrors,
        },
        { status: 400 },
      );
    }

    const { examId, examName } = validated.data;

    const repo = new ExporterRepositoryImpl();
    const usecase = new ExportExamUsecase(repo);
    const response = await usecase.execute(examId, examName, accessToken!);

    const uint8Array = new Uint8Array(response as unknown as ArrayBuffer);

    const encodedFileName = encodeURIComponent(`${examName}.docx`);

    return new NextResponse(uint8Array, {
      status: 200,
      headers: {
        "Content-Type":
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "Content-Disposition": `attachment; filename="${encodedFileName}"; filename*=UTF-8''${encodedFileName}`,
      },
    });
  } catch (error: unknown) {
    const errorMessage =
      error instanceof Error ? error.message : "Lỗi không xác định";
    console.error("Lỗi tại Next.js BFF:", errorMessage);
    return NextResponse.json(
      { error: "Lỗi khi xuất file từ Backend" },
      { status: 500 },
    );
  }
}
