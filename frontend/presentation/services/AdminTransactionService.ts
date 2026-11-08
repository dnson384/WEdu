import axios from "axios";
import { TransactionFilters, TransactionLookup, TransactionPage } from "@/domain/entities/adminTransaction.entity";

export class AdminTransactionService {
  async search(filters: TransactionFilters, signal?: AbortSignal) {
    return (await axios.get<TransactionPage>("/api/admin/transactions", { params: filters, signal })).data;
  }

  async lookup(id: string, signal?: AbortSignal) {
    return (await axios.post<TransactionLookup>(`/api/admin/transactions/${encodeURIComponent(id)}/lookup`, null, { signal })).data;
  }
}
