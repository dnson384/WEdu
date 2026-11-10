import { NextRequest } from "next/server";
import { handleRefund } from "@/app/api/refund-handler";
import { handlePayment } from "../handler";

export async function GET(req: NextRequest) { return handleRefund(req, "orders", false); }
export function POST(req: NextRequest) { return handlePayment(req, "create"); }
