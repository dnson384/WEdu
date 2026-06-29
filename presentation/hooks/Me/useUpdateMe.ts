"use client";

import { UpdateUsernameService } from "@/presentation/services/user.service";
import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  username: string;
}

export default function useUpdateMe({ username }: FormData) {
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [formData, setFormData] = useState<FormData>({
    username: username,
  });

  const [updateMeError, setUpdateMeError] = useState<string | null>(null);
  const [updateMeSuccess, setUpdateMeSuccess] = useState<string | null>(null);

  const handleToggleEdit = () => {
    setIsEditing((prev) => !prev);
  };

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    try {
      const response = await UpdateUsernameService(formData.username);

      if (response) {
        setUpdateMeSuccess("Cập nhật tài khoản thành công");
      }
    } catch (err) {
      if (isAxiosError(err)) {
        const message = err.response?.data.message;
        setUpdateMeError(message);
      }
    }
  };

  useEffect(() => {
    if (username !== "") {
      setFormData((prev) => ({ ...prev, ["username"]: username }));
    }
  }, [username]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (updateMeSuccess && updateMeSuccess.trim().length > 0) {
        setUpdateMeSuccess(null);
        window.location.reload();
      } else if (updateMeError && updateMeError.trim().length > 0) {
        setUpdateMeError(null);
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [updateMeSuccess, updateMeError]);

  return {
    updateMeError,
    updateMeSuccess,
    isEditing,
    handleToggleEdit,
    formData,
    handleInputChange,
    handleSaveProfile,
  };
}
