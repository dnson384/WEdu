import axios from "axios";
import { PaymentOrder, PaymentPlan, PaymentReturnParameters } from "@/domain/entities/payment.entity";
import { IPaymentRepository } from "@/domain/repositories/IPaymentRepository";

export class PaymentRepositoryImpl implements IPaymentRepository {
  private readonly baseUrl = `${process.env.BACKEND_URL || (
    process.env.NODE_ENV === "development"
      ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL
      : process.env.NEXT_PUBLIC_BACKEND_PROD_URL
  )}/payment`;

  async plan(token: string) {
    return (await axios.get<PaymentPlan>(`${this.baseUrl}/plan`, {
      headers: { Authorization: `Bearer ${token}` }, timeout: 15000,
    })).data;
  }

  async create(key: string, token: string) {
    return (await axios.post<PaymentOrder>(`${this.baseUrl}/orders`, null, {
      headers: { Authorization: `Bearer ${token}`, "Idempotency-Key": key }, timeout: 15000,
    })).data;
  }

  async order(id: string, token: string) {
    return (await axios.get<PaymentOrder>(`${this.baseUrl}/orders/${encodeURIComponent(id)}`, {
      headers: { Authorization: `Bearer ${token}` }, timeout: 15000,
    })).data;
  }

  async verify(id: string, token: string) {
    return (await axios.post<PaymentOrder>(`${this.baseUrl}/orders/${encodeURIComponent(id)}/verify`, null, {
      headers: { Authorization: `Bearer ${token}` }, timeout: 20000,
    })).data;
  }

  async confirmReturn(id: string, parameters: PaymentReturnParameters, token: string) {
    return (await axios.post<PaymentOrder>(`${this.baseUrl}/orders/${encodeURIComponent(id)}/return`, { parameters }, {
      headers: { Authorization: `Bearer ${token}` }, timeout: 15000,
    })).data;
  }
}
