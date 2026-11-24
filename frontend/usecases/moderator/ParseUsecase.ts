import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class ParseUsecase {
  constructor(private readonly repo: IModeratorRepository) {}

  async execute(formData: FormData, accessToken: string): Promise<string[]> {
    return this.repo.parse(formData, accessToken);
  }
}
