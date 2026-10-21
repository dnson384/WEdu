import { isAxiosError } from "axios";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { UserService } from "@/presentation/services/UserService";
import { useRouter } from "next/navigation";
import { QueryClient } from "@tanstack/react-query";

interface FormData {
  username: string;
}

export default function useUpdateUsername({ username }: FormData) {
  const router = useRouter();
  const queryClient = new QueryClient();
  const userService = useMemo(() => new UserService(), []);

  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [formData, setFormData] = useState<FormData>({ username });

  const [prevUsername, setPrevUsername] = useState<string>(username);
  if (username !== prevUsername) {
    setPrevUsername(username);
    if (username !== "") {
      setFormData((prev) => ({ ...prev, username }));
    }
  }

  const [updateMeError, setUpdateMeError] = useState<string | null>(null);
  const [updateMeSuccess, setUpdateMeSuccess] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const handleToggleEdit = () => {
    setIsEditing((prev) => {
      const nextState = !prev;

      if (!nextState) {
        setFormData({ username });
        setUpdateMeError(null);
      }
      return nextState;
    });
  };

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveProfile = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setUpdateMeError(null);
    setUpdateMeSuccess(null);
    setIsSubmitting(true);

    try {
      const response = await userService.updateUsername(formData.username);

      if (response) {
        await queryClient.invalidateQueries({ queryKey: ["user-profile"] });
        setUpdateMeSuccess("Cập nhật tài khoản thành công");
      }
    } catch (err) {
      if (isAxiosError(err)) {
        const message = err.response?.data?.message || "Đã có lỗi xảy ra";
        setUpdateMeError(message);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!updateMeSuccess && !updateMeError) return;

    const timer = setTimeout(() => {
      if (updateMeSuccess) {
        setUpdateMeSuccess(null);
        router.refresh();
      } else if (updateMeError) {
        setUpdateMeError(null);
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [updateMeSuccess, updateMeError, router]);

  return {
    updateMeError,
    updateMeSuccess,
    isEditing,
    handleToggleEdit,
    isSubmitting,
    formData,
    handleInputChange,
    handleSaveProfile,
  };
}
