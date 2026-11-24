export interface AiBudget {
  revision: number;
  monthlyBudgetUsd: string | null;
  monthlyTokenQuota: number | null;
}

export interface AiUsageDay {
  day: string | null;
  requests: number;
  unknownUsageRequests: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  estimatedCostUsd: string;
}

export interface AiUsageFilters {
  from: string;
  to: string;
  model?: string;
}

export interface AiUsageReport extends AiUsageFilters {
  timezone: string;
  currency: string;
  models: string[];
  totals: AiUsageDay;
  days: AiUsageDay[];
}

export interface AiBalance {
  available: boolean | null;
  updatedAt: string | null;
  stale: boolean;
  balances: {
    currency: string;
    totalBalance: string;
    grantedBalance: string;
    toppedUpBalance: string;
  }[];
}

export interface AiStatus {
  month: string;
  budget: AiBudget;
  usage: AiUsageDay;
  remainingBudgetUsd: string | null;
  remainingTokens: number | null;
  budgetUsedPercent: number | null;
  tokensUsedPercent: number | null;
  level: "NORMAL" | "WARNING" | "BLOCKED";
  reasons: string[];
  balance: AiBalance;
}

export interface AiBudgetDraft {
  revision: number;
  amount: string;
  tokens: string;
}
