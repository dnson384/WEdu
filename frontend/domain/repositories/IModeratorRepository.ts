export interface IModeratorRepository {
  parse(formData: FormData, accessToken: string): Promise<string[]>;
}
