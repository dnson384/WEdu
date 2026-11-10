import axios from "axios";
import { IRefundRepository } from "@/domain/repositories/IRefundRepository";
import { Refund, RefundAction, RefundCreate, RefundDecision, RefundFilters, RefundOrders, RefundPage } from "@/domain/entities/refund.entity";

export class RefundRepositoryImpl implements IRefundRepository {
  private readonly base = process.env.BACKEND_URL || (process.env.NODE_ENV === "development"
    ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL : process.env.NEXT_PUBLIC_BACKEND_PROD_URL);
  private config(token: string) { return { headers: { Authorization: `Bearer ${token}` }, timeout: 30000 }; }
  async search(admin: boolean, filters: RefundFilters, token: string) {
    return (await axios.get<RefundPage>(`${this.base}/${admin ? "admin" : "payment"}/refunds`, { ...this.config(token), params: filters })).data;
  }
  async get(admin: boolean, id: string, token: string) {
    return (await axios.get<Refund>(`${this.base}/${admin ? "admin" : "payment"}/refunds/${id}`, this.config(token))).data;
  }
  async orders(page: number, token: string) {
    return (await axios.get<RefundOrders>(`${this.base}/payment/orders`, { ...this.config(token), params: { page } })).data;
  }
  async create(orderId: string, body: RefundCreate, key: string, token: string) {
    return (await axios.post<Refund>(`${this.base}/payment/orders/${orderId}/refunds`, body,
      { ...this.config(token), headers: { Authorization: `Bearer ${token}`, "Idempotency-Key": key } })).data;
  }
  async decide(id: string, action: RefundAction, body: RefundDecision, token: string) {
    return (await axios.post<Refund>(`${this.base}/${action === "cancel" ? "payment" : "admin"}/refunds/${id}/${action}`, body, this.config(token))).data;
  }
}
