import { NextRequest } from "next/server";
import { handleRefund } from "@/app/api/refund-handler";

export async function GET(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handleRefund(req, "get", true, (await context.params).id);
}
