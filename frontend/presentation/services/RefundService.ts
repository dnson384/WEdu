import axios from "axios";
import { Refund, RefundAction, RefundCreate, RefundFilters, RefundOrders, RefundPage } from "@/domain/entities/refund.entity";

export class RefundService {
  async search(admin: boolean, filters: RefundFilters, signal?: AbortSignal) {
    return (await axios.get<RefundPage>(`/api/${admin ? "admin" : "payment"}/refunds`, { params: filters, signal })).data;
  }
  async get(admin: boolean, id: string, signal?: AbortSignal) {
    return (await axios.get<Refund>(`/api/${admin ? "admin" : "payment"}/refunds/${id}`, { signal })).data;
  }
  async orders(page: number) { return (await axios.get<RefundOrders>("/api/payment/orders", { params: { page } })).data; }
  async create(id: string, body: RefundCreate, key: string) {
    return (await axios.post<Refund>(`/api/payment/orders/${id}/refunds`, body, { headers: { "Idempotency-Key": key } })).data;
  }
  async decide(refund: Refund, action: RefundAction, reason: string, legacyException: boolean) {
    return (await axios.post<Refund>(`/api/${action === "cancel" ? "payment" : "admin"}/refunds/${refund.id}/${action}`,
      { revision: refund.revision, reason, legacyException })).data;
  }
}
