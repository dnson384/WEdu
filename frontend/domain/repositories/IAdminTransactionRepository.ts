import { TransactionFilters, TransactionLookup, TransactionPage } from "@/domain/entities/adminTransaction.entity";

export interface IAdminTransactionRepository {
  search(filters: TransactionFilters, token: string): Promise<TransactionPage>;
  lookup(id: string, token: string): Promise<TransactionLookup>;
}
