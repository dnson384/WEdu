import { NextRequest, NextResponse } from "next/server";
import axios from "axios";
import { ExporterRepositoryImpl } from "@/infrastructure/repositories/ExporterRepositoryImpl";
import { ExportMatrixPayload } from "@/presentation/schemas/export.schema";
import { ExportMatrixUsecase } from "@/usecases/exporter/ExportMatrixUsecase";

export async function POST(req: NextRequest) {
  const accessToken = req.cookies.get("accessToken")?.value;
  if (!accessToken) {
    return NextResponse.json({ message: "Chưa xác thực" }, { status: 401 });
  }
  const body: unknown = await req.json().catch(() => null);
  const parsed = ExportMatrixPayload.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ message: "Dữ liệu đầu vào không hợp lệ" }, { status: 400 });
  }
  try {
    const { examId, examName } = parsed.data;
    const buffer = await new ExportMatrixUsecase(new ExporterRepositoryImpl())
      .execute(examId, examName, accessToken);
    const fileName = encodeURIComponent(`${examName}_ma trận đề.docx`);
    return new NextResponse(new Uint8Array(buffer), {
      headers: {
        "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "Content-Disposition": `attachment; filename*=UTF-8''${fileName}`,
        "Cache-Control": "private, no-store",
      },
    });
  } catch (error) {
    let message = "Không thể xuất ma trận đề. Vui lòng thử lại sau";
    let status = 502;
    if (axios.isAxiosError(error) && error.response) {
      status = error.response.status;
      try {
        const data = JSON.parse(Buffer.from(error.response.data).toString("utf8"));
        if (typeof data.message === "string") message = data.message;
      } catch { /* Backend may return a non-JSON error body. */ }
    }
    return NextResponse.json({ message }, { status });
  }
}
