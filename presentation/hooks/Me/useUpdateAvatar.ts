"use client";

import { ChangeEvent, useEffect, useState } from "react";

interface Data {
  initialUrl: string;
}

export default function useUpdateAvatar({ initialUrl }: Data) {
  const [avatarUrl, setAvatarUrl] = useState(initialUrl);
  const [isUploading, setIsUploading] = useState(false);

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
      console.log("Đang upload file:", file.name);

      await new Promise((resolve) => setTimeout(resolve, 2000));
    } catch (error) {
      console.error("Lỗi upload avatar:", error);
      alert("Cập nhật ảnh thất bại!");

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

  return {
    avatarUrl,
    isUploading,
    handleAvatarChange,
  };
}
