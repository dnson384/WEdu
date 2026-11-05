import axios from "axios";
import { PaymentOrder, PaymentPlan, PaymentReturnParameters } from "@/domain/entities/payment.entity";

export class PaymentService {
  async plan(signal?: AbortSignal) {
    return (await axios.get<PaymentPlan>("/api/payment/plan", { signal })).data;
  }
  async create(key: string) {
    return (await axios.post<PaymentOrder>("/api/payment/orders", null, {
      headers: { "Idempotency-Key": key },
    })).data;
  }
  async order(id: string, signal?: AbortSignal) {
    return (await axios.get<PaymentOrder>(`/api/payment/orders/${id}`, { signal })).data;
  }
  async verify(id: string) {
    return (await axios.post<PaymentOrder>(`/api/payment/orders/${id}/verify`)).data;
  }
  async confirmReturn(id: string, parameters: PaymentReturnParameters) {
    return (await axios.post<PaymentOrder>(`/api/payment/orders/${id}/return`, { parameters })).data;
  }
}
