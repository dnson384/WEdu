import { getMeUsecase } from "@/application/usecases/user/getMeUsecase";
import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";
import { isAxiosError } from "axios";
import { NextResponse } from "next/server";

export async function GET() {
  try {
    const repo = new UserRepositoryImpl();
    const usecase = new getMeUsecase(repo);
    const response = await usecase.execute();
    
    return NextResponse.json(response, { status: 200 });
  } catch (err) {
    if (isAxiosError(err) && err.response) {
      return NextResponse.json(
        { message: err.response.data.message },
        { status: err.response.status },
      );
    }
    return NextResponse.json({ message: "Lỗi Server" }, { status: 500 });
  }
}
