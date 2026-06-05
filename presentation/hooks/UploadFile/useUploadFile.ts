"use client";
import { uploadDocxFile } from "@/presentation/services/uploadFile.service";
import { useState, useRef, ChangeEvent, useEffect } from "react";

export default function useDocxUpload() {
  const hiddenFileInput = useRef<HTMLInputElement | null>(null);
  const [subject, setSubject] = useState<string>("");
  const [isSuccess, setIsSuccess] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleInputClick = () => {
    hiddenFileInput.current?.click();
  };

  const handleSelectedFile = async (event: ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files || event.target.files.length === 0) return;

    const file: File = event.target.files[0];
    try {
      const formData = new FormData();
      formData.append("file", file);
      if (subject.trim().length === 0)
        throw new Error("Vui lòng nhập tên môn học");

      const data = await uploadDocxFile(subject, formData);
      if (data) {
        setIsSuccess(true);
        setError(null);
      }
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setIsSuccess(false);
        setError(null);
      }
    } finally {
      event.target.value = "";
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (error !== null) {
        setError(null);
      }
      if (isSuccess !== null) {
        setIsSuccess(null);
      }
    }, 3000);

    return () => clearTimeout(timer);
  }, [error, isSuccess]);

  return {
    hiddenFileInput,
    subject,
    isSuccess,
    error,
    setSubject,
    handleInputClick,
    handleSelectedFile,
  };
}
