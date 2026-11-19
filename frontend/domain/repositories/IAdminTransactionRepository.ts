import {
  TransactionFilters,
  TransactionPage,
} from "@/domain/entities/adminTransaction.entity";

export interface IAdminTransactionRepository {
  search(filters: TransactionFilters, token: string): Promise<TransactionPage>;
}
