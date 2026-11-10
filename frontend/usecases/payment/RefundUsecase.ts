import { IRefundRepository } from "@/domain/repositories/IRefundRepository";
import { RefundAction, RefundCreate, RefundDecision, RefundFilters } from "@/domain/entities/refund.entity";

export class RefundUsecase {
  constructor(private readonly repository: IRefundRepository) {}
  search(admin: boolean, filters: RefundFilters, token: string) { return this.repository.search(admin, filters, token); }
  get(admin: boolean, id: string, token: string) { return this.repository.get(admin, id, token); }
  orders(page: number, token: string) { return this.repository.orders(page, token); }
  create(id: string, body: RefundCreate, key: string, token: string) { return this.repository.create(id, body, key, token); }
  decide(id: string, action: RefundAction, body: RefundDecision, token: string) { return this.repository.decide(id, action, body, token); }
}
