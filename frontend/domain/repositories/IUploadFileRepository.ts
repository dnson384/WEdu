export interface IUploadDocxFileRepository {
  uploadDocxFile(
    subject: string,
    formData: FormData,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
}
