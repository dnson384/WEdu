import axios from "axios";
import { ChangePasswordPayload } from "../schemas/userSchema";

export async function getMeService() {
  const response = await axios.get("/api/user/me", { withCredentials: true });
  return response.data;
}

export async function updateAvatarService(file: File): Promise<boolean> {
  const formData = new FormData();
  formData.append("file", file);

  const responseUpload = await axios.post<string>(
    "/api/user/avatar/upload",
    formData,
  );

  const s3Key = responseUpload.data;

  const responseUpdate = await axios.put<boolean>(`/api/user/avatar/update`, {
    s3Key,
  });

  return responseUpdate.data;
}

export async function UpdateUsernameService(
  username: string,
): Promise<boolean> {
  const { data } = await axios.put<boolean>("/api/user/username", { username });
  return data;
}

export async function ChangePasswordService(
  payload: ChangePasswordPayload,
): Promise<boolean> {
  const { data } = await axios.put<boolean>(
    "/api/user/changePassword",
    payload,
  );

  return data;
}

export async function lockAccoutService() {
  const { data } = await axios.post<boolean>("/api/user/lock");
  return data;
}

export async function deleteAccoutService() {
  const { data } = await axios.delete<boolean>("/api/user/delete");
  return data;
}
