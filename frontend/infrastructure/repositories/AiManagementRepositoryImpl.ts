import axios from "axios";
import {
  AiBudget,
  AiStatus,
  AiUsageFilters,
  AiUsageReport,
} from "@/domain/entities/aiManagement.entity";
import { IAiManagementRepository } from "@/domain/repositories/IAiManagementRepository";

export class AiManagementRepositoryImpl implements IAiManagementRepository {
  private readonly baseUrl = `${
    process.env.NODE_ENV === "development"
      ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL
      : process.env.NEXT_PUBLIC_BACKEND_PROD_URL
  }/admin/ai`;

  async report(filters: AiUsageFilters, token: string): Promise<AiUsageReport> {
    const { data } = await axios.get<AiUsageReport>(`${this.baseUrl}/usage`, {
      params: filters,
      headers: { Authorization: `Bearer ${token}` },
    });
    return data;
  }

  async status(token: string): Promise<AiStatus> {
    const { data } = await axios.get<AiStatus>(`${this.baseUrl}/status`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return data;
  }

  async budget(token: string): Promise<AiBudget> {
    const { data } = await axios.get<AiBudget>(`${this.baseUrl}/budget`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return data;
  }

  async updateBudget(input: AiBudget, token: string): Promise<AiBudget> {
    const { data } = await axios.put<AiBudget>(
      `${this.baseUrl}/budget`,
      input,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    return data;
  }
}
