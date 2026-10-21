export interface IStorageRepository {
  uploadFileToQuestionBank(
    formData: FormData,
    accessToken: string,
  ): Promise<string>;

  uploadAvatar(formData: FormData, accessToken: string): Promise<string>;
}
