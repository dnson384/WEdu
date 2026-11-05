import { NextRequest } from "next/server";
import { handlePayment } from "../handler";
export function POST(req: NextRequest) { return handlePayment(req, "create"); }
