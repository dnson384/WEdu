export interface IUploadDocxFileRepository {
  uploadDocxFile(subject: string, formData: FormData): Promise<boolean>;
}
