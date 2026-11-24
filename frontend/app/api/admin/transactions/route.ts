import { NextRequest } from "next/server";
import { handleAdminTransactions } from "./handler";

export function GET(req: NextRequest) {
  return handleAdminTransactions(req);
}
