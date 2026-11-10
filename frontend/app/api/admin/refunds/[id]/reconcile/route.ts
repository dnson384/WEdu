import { NextRequest } from "next/server";
import { handleRefund } from "@/app/api/refund-handler";

export async function POST(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handleRefund(req, "reconcile", true, (await context.params).id);
}
