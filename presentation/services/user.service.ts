import axios from "axios";

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
