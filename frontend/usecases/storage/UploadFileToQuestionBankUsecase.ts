import { IStorageRepository } from "@/domain/repositories/IStorageRepository";

export class UploadFileToQuestionBankUsecase {
  constructor(private readonly repo: IStorageRepository) {}

  async execute(formData: FormData, accessToken: string): Promise<string> {
    return this.repo.uploadFileToQuestionBank(formData, accessToken);
  }
}
