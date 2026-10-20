import { QueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { StorageService } from "@/presentation/services/StorageService";
import { UserService } from "@/presentation/services/UserService";

interface Data {
  initialUrl: string;
}

export default function useUpdateAvatar({ initialUrl }: Data) {
  const router = useRouter();
  const queryClient = new QueryClient();
  const storageService = new StorageService();
  const userService = new UserService();

  const [avatarUrl, setAvatarUrl] = useState(initialUrl);
  const [isUploading, setIsUploading] = useState(false);

  const previewUrlRef = useRef<string | null>(null);

  const [prevInitialUrl, setPrevInitialUrl] = useState(initialUrl);
  if (initialUrl !== prevInitialUrl) {
    setPrevInitialUrl(initialUrl);
    setAvatarUrl(initialUrl);
  }

  const [updateAvatarError, setUpdateAvatarError] = useState<string | null>(
    null,
  );
  const [updateAvatarSuccess, setUpdateAvatarSuccess] = useState<string | null>(
    null,
  );

  const cleanupPreviewUrl = () => {
    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }
  };

  const handleAvatarChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert("Vui lòng chọn ảnh nhỏ hơn 5MB");
      return;
    }

    cleanupPreviewUrl();

    const previewUrl = URL.createObjectURL(file);
    previewUrlRef.current = previewUrl;
    setAvatarUrl(previewUrl);

    setUpdateAvatarError(null);
    setUpdateAvatarSuccess(null);

    try {
      setIsUploading(true);

      const s3Key = await storageService.uploadAvatar(file);

      const updateStatus = await userService.updateAvatar(s3Key);

      if (updateStatus) {
        await queryClient.invalidateQueries({ queryKey: ["user-profile"] });
        setUpdateAvatarSuccess("Cập nhật ảnh đại diện thành công");
      }
    } catch (error: unknown) {
      console.error(error);
      if (isAxiosError(error)) {
        setUpdateAvatarError(
          error.response?.data?.message || "Cập nhật ảnh thất bại",
        );
      } else {
        setUpdateAvatarError("Có lỗi xảy ra khi tải ảnh lên");
      }

      setAvatarUrl(initialUrl);
      cleanupPreviewUrl();
    } finally {
      setIsUploading(false);
      event.target.value = "";
    }
  };

  useEffect(() => {
    return () => cleanupPreviewUrl();
  }, []);

  useEffect(() => {
    if (!updateAvatarSuccess && !updateAvatarError) return;

    const timer = setTimeout(() => {
      if (updateAvatarSuccess) {
        setUpdateAvatarSuccess(null);
        router.refresh();
      } else if (updateAvatarError) {
        setUpdateAvatarError(null);
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [updateAvatarError, updateAvatarSuccess, router]);

  return {
    avatarUrl,
    isUploading,
    updateAvatarError,
    updateAvatarSuccess,
    handleAvatarChange,
  };
}
