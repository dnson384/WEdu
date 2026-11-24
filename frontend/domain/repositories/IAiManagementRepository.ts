import {
  AiBudget,
  AiStatus,
  AiUsageFilters,
  AiUsageReport,
} from "@/domain/entities/aiManagement.entity";

export interface IAiManagementRepository {
  report(filters: AiUsageFilters, token: string): Promise<AiUsageReport>;
  status(token: string): Promise<AiStatus>;
  budget(token: string): Promise<AiBudget>;
  updateBudget(input: AiBudget, token: string): Promise<AiBudget>;
}
