import { ChangeEvent, FormEvent, useState } from "react";

interface FormData {
  email: string;
  password: string;
}

export default function useLogin() {
  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });

  const handleFormChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const [showPassword, setShowPassword] = useState<boolean>(false);

  const handleShowPasswordClick = () => {
    setShowPassword((prev) => !prev);
  };

  const handleLoginSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    alert(`${formData.email} - ${formData.password}`);
  };
  return {
    formData,
    handleFormChange,
    showPassword,
    handleShowPasswordClick,
    handleLoginSubmit,
  };
}
