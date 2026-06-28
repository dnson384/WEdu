import { UserEntity } from "../entities/user.entity";

export interface IUserRepository {
  getMe(): Promise<UserEntity>;
  uploadAvatar(formData: FormData): Promise<string>;
  updateAvatar(s3Key: string): Promise<boolean>;
  updateUsername(username: string): Promise<boolean>;
}
