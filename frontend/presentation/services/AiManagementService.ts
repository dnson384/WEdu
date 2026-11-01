import axios from "axios";
import {
  AiBudget,
  AiStatus,
  AiUsageFilters,
  AiUsageReport,
} from "@/domain/entities/aiManagement.entity";

export class AiManagementService {
  async report(filters: AiUsageFilters, signal?: AbortSignal) {
    const { data } = await axios.get<AiUsageReport>("/api/admin/ai/usage", {
      params: filters,
      signal,
    });
    return data;
  }

  async status(signal?: AbortSignal) {
    const { data } = await axios.get<AiStatus>("/api/admin/ai/status", {
      signal,
    });
    return data;
  }

  async budget(signal?: AbortSignal) {
    const { data } = await axios.get<AiBudget>("/api/admin/ai/budget", {
      signal,
    });
    return data;
  }

  async updateBudget(input: AiBudget) {
    const { data } = await axios.put<AiBudget>("/api/admin/ai/budget", input);
    return data;
  }
}
