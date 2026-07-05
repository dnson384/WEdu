import {
  ChangePasswordPayloadEntity,
  UserEntity,
} from "../entities/user.entity";

export interface IUserRepository {
  getMe(accessToken: string, refreshToken: string): Promise<UserEntity>;
  uploadAvatar(
    formData: FormData,
    accessToken: string,
    refreshToken: string,
  ): Promise<string>;
  updateAvatar(
    s3Key: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  updateUsername(
    username: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  changePassword(
    payload: ChangePasswordPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  lockAccount(accessToken: string, refreshToken: string): Promise<boolean>;
  deleteAccount(accessToken: string, refreshToken: string): Promise<boolean>;
}
