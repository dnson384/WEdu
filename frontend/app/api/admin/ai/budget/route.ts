import { NextRequest } from "next/server";
import { handleAiManagement } from "../handler";

export async function GET(req: NextRequest) {
  return handleAiManagement(req, "budget");
}
export async function PUT(req: NextRequest) {
  return handleAiManagement(req, "updateBudget");
}
