import { TransactionFilters } from "@/domain/entities/adminTransaction.entity";
import { IAdminTransactionRepository } from "@/domain/repositories/IAdminTransactionRepository";

export class AdminTransactionUsecase {
  constructor(private readonly repository: IAdminTransactionRepository) {}

  search(filters: TransactionFilters, token: string) {
    return this.repository.search(filters, token);
  }

  lookup(id: string, token: string) {
    return this.repository.lookup(id, token);
  }
}
