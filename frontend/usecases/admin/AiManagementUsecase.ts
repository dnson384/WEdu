import {
  AiBudget,
  AiUsageFilters,
} from "@/domain/entities/aiManagement.entity";
import { IAiManagementRepository } from "@/domain/repositories/IAiManagementRepository";

export class AiManagementUsecase {
  constructor(private readonly repository: IAiManagementRepository) {}

  report(filters: AiUsageFilters, token: string) {
    return this.repository.report(filters, token);
  }
  status(token: string) {
    return this.repository.status(token);
  }
  budget(token: string) {
    return this.repository.budget(token);
  }
  updateBudget(input: AiBudget, token: string) {
    return this.repository.updateBudget(input, token);
  }
}
