import { NextRequest } from "next/server";
import { handlePayment } from "../../../handler";
export async function POST(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handlePayment(req, "verify", (await context.params).id);
}
