import { NextRequest } from "next/server";
import { handlePayment } from "../../handler";
export async function GET(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handlePayment(req, "order", (await context.params).id);
}
