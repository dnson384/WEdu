import axios from "axios";
import {
  TransactionFilters,
  TransactionPage,
} from "@/domain/entities/adminTransaction.entity";
import { IAdminTransactionRepository } from "@/domain/repositories/IAdminTransactionRepository";

export class AdminTransactionRepositoryImpl implements IAdminTransactionRepository {
  private readonly baseUrl = `${
    process.env.BACKEND_URL ||
    (process.env.NODE_ENV === "development"
      ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL
      : process.env.NEXT_PUBLIC_BACKEND_PROD_URL)
  }/admin/transactions`;

  async search(filters: TransactionFilters, token: string) {
    return (
      await axios.get<TransactionPage>(this.baseUrl, {
        params: filters,
        headers: { Authorization: `Bearer ${token}` },
        timeout: 15000,
      })
    ).data;
  }
}
