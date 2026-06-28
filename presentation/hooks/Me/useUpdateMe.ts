"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";

interface FormData {
  email: string;
  username: string;
}

export default function useUpdateMe({ email, username }: FormData) {
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [formData, setFormData] = useState<FormData>({
    email: email,
    username: username,
  });

  const handleToggleEdit = () => {
    setIsEditing((prev) => !prev);
  };

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
  };

  const handleLockAccount = () => {};
  const handleDeleteAccount = () => {};

  useEffect(() => {
    if (email !== "") {
      setFormData((prev) => ({ ...prev, ["email"]: email }));
    }
    if (username !== "") {
      setFormData((prev) => ({ ...prev, ["username"]: username }));
    }
  }, [email, username]);

  return {
    isEditing,
    handleToggleEdit,
    formData,
    handleInputChange,
    handleSaveProfile,
    handleLockAccount,
    handleDeleteAccount,
  };
}
