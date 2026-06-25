import { UserEntity } from "../entities/user.entity";

export interface IUserRepository {
  getMe(): Promise<UserEntity>;
}
