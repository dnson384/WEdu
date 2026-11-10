import { NextRequest } from "next/server";
import { handleRefund } from "@/app/api/refund-handler";

export async function GET(req: NextRequest) {
  return handleRefund(req, "list", false);
}
