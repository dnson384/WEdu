import axios from "axios";

export async function getMeService() {
  const response = await axios.get("/api/user/me", { withCredentials: true });
  return response.data;
}

// export async function getUserByIdService(
//   userId: string,
// ): Promise<UserResponseEntity> {
//   const response = await axios.get("/api/user", { params: userId });
//   return response.data;
// }
