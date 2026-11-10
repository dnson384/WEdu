import { NextRequest } from "next/server";
import { handleRefund } from "@/app/api/refund-handler";

export async function POST(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handleRefund(req, "create", false, (await context.params).id);
}
