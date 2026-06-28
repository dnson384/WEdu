"use client";

import { updateAvatarService } from "@/presentation/services/user.service";
import { ChangeEvent, useEffect, useState } from "react";

interface Data {
  initialUrl: string;
}

export default function useUpdateAvatar({ initialUrl }: Data) {
  const [avatarUrl, setAvatarUrl] = useState(initialUrl);
  const [isUploading, setIsUploading] = useState(false);

  const [updateAvatarError, setUpdateAvatarError] = useState<string | null>(
    null,
  );
  const [updateAvatarSuccess, setUpdateAvatarSuccess] = useState<string | null>(
    null,
  );

  const handleAvatarChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      alert("Vui lòng chọn ảnh nhỏ hơn 5MB");
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarUrl(previewUrl);

    try {
      setIsUploading(true);

      const updated = await updateAvatarService(file);
      if (updated) {
        setUpdateAvatarSuccess("Cập nhật ảnh đại diện thành công");
      }
    } catch (error) {
      setAvatarUrl(initialUrl);
    } finally {
      setIsUploading(false);
    }
  };

  useEffect(() => {
    if (initialUrl) {
      setAvatarUrl(initialUrl);
    }
  }, [initialUrl]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (
        (updateAvatarError !== null && updateAvatarError.trim().length > 0) ||
        (updateAvatarSuccess !== null && updateAvatarSuccess.trim().length > 0)
      ) {
        setUpdateAvatarError(null);
        setUpdateAvatarSuccess(null);
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [updateAvatarError, updateAvatarSuccess]);

  return {
    avatarUrl,
    isUploading,
    handleAvatarChange,
  };
}
