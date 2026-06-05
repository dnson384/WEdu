import { IUploadDocxFileRepository } from "@/domain/repositories/IUploadFileRepository";

export class uploadDocxFileUsecase {
  constructor(
    private readonly uploadDocxFileRepository: IUploadDocxFileRepository,
  ) {}

  async execute(subject: string, formData: FormData): Promise<boolean> {
    return this.uploadDocxFileRepository.uploadDocxFile(subject, formData);
  }
}
