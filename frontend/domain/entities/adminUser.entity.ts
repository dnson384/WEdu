export interface AdminUserEntity {
  id: string;
  email: string;
  username: string;
  role: string;
  avatarUrl: string | null;
  isActive: boolean | null;
  accountType: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export type AdminUserActionType = "role" | "lock" | "unlock" | "delete";

export interface AdminUserAction {
  type: AdminUserActionType;
  user: AdminUserEntity;
}
