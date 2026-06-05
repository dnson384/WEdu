import {
  ExportPayload,
  PracticeDetailEntity,
  PracticeEntity,
} from "../entities/practice.entity";

export interface IPracticeRepository {
  getPracticeById(id: string): Promise<PracticeDetailEntity>;
  getAllPractices(): Promise<PracticeEntity[]>;
  exportWordFile(payload: ExportPayload): Promise<any>;
}
