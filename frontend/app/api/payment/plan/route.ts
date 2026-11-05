import { NextRequest } from "next/server";
import { handlePayment } from "../handler";
export function GET(req: NextRequest) { return handlePayment(req, "plan"); }
