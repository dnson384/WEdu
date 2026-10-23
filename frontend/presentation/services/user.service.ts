import axios from "axios";

export async function getMeService() {
  const response = await axios.get("/api/user/me", { withCredentials: true });
  return response.data;
}

export async function UpdateUsernameService(
  username: string,
): Promise<boolean> {
  const { data } = await axios.put<boolean>("/api/user/username", { username });
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
