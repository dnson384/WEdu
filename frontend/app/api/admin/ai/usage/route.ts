import { NextRequest } from "next/server";
import { handleAiManagement } from "../handler";

export async function GET(req: NextRequest) {
  return handleAiManagement(req, "usage");
}
