export interface TransactionFilters {
  keyword: string;
  status: string;
  from: string;
  to: string;
  page: number;
}

export interface AdminTransaction {
  orderId: string;
  email: string | null;
  amount: number;
  status: string;
  createdAt: string | null;
  transactionNo: string | null;
}

export interface TransactionPage {
  items: AdminTransaction[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionLookup {
  order: AdminTransaction;
  provider: {
    transactionNo: string | null;
    amount: number;
    bankCode: string | null;
    paidAt: string | null;
    responseCode: string;
    transactionStatus: string;
  };
  amountMismatch: boolean;
  queriedAt: string;
}
