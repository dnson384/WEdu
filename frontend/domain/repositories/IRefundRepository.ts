import { Refund, RefundAction, RefundCreate, RefundDecision, RefundFilters, RefundOrders, RefundPage } from "@/domain/entities/refund.entity";

export interface IRefundRepository {
  search(admin: boolean, filters: RefundFilters, token: string): Promise<RefundPage>;
  get(admin: boolean, id: string, token: string): Promise<Refund>;
  orders(page: number, token: string): Promise<RefundOrders>;
  create(orderId: string, body: RefundCreate, key: string, token: string): Promise<Refund>;
  decide(id: string, action: RefundAction, body: RefundDecision, token: string): Promise<Refund>;
}
