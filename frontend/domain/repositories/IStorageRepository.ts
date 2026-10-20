export interface IStorageRepository {
  uploadAvatar(formData: FormData, accessToken: string): Promise<string>;
}
