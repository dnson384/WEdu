import { NextRequest } from "next/server";
import { handleAdminTransactions } from "../../handler";

export async function POST(req: NextRequest, context: { params: Promise<{ id: string }> }) {
  return handleAdminTransactions(req, (await context.params).id);
}
