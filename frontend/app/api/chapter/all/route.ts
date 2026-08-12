import { GetAllChaptersUsecase } from "@/application/usecases/chapter/GetAllChaptersUsecase";
import { ChapterRepositoryImpl } from "@/infrastructure/repositories/ChapterRepositoryImpl";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  try {
    const accessToken = req.cookies.get("accessToken")?.value;
    const refreshToken = req.cookies.get("refreshToken")?.value;

    const repo = new ChapterRepositoryImpl();
    const usecase = new GetAllChaptersUsecase(repo);
    const response = await usecase.execute(accessToken!, refreshToken!);
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
