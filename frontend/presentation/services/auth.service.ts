import axios from "axios";

export async function LogoutService(): Promise<boolean> {
  const { data } = await axios.post("/api/auth/logout");
  return data;
}
