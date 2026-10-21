"use client";
import {
  useState,
  useRef,
  ChangeEvent,
  DragEvent,
  useEffect,
  useMemo,
} from "react";
import { StorageService } from "@/presentation/services/StorageService";

export default function useUploadToQuestionBank() {
  const storageService = useMemo(() => new StorageService(), []);

  const hiddenFileInput = useRef<HTMLInputElement | null>(null);

  const [subject, setSubject] = useState<string>("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [isSuccess, setIsSuccess] = useState<boolean | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Trạng thái kéo thả
  const [isDragging, setIsDragging] = useState(false);

  // Mở trình duyệt file Explorer
  const handleInputClick = () => {
    hiddenFileInput.current?.click();
  };

  // Kiểm tra định dạng file DOCX
  const validateAndSetFile = (file: File) => {
    const isDocx =
      file.name.endsWith(".docx") ||
      file.type ===
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    if (!isDocx) {
      setError("Chỉ chấp nhận định dạng file Word (.docx)!");
      return false;
    }

    setSelectedFile(file);
    setError(null);
    return true;
  };

  // Xử lý khi chọn file qua hộp thoại Explorer
  const onFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSetFile(e.target.files[0]);
    }
    // Reset value input để có thể chọn lại cùng 1 file nếu muốn
    e.target.value = "";
  };

  // Sự kiện kéo thả
  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  // Hủy file đã chọn
  const handleRemoveFile = () => {
    setSelectedFile(null);
    if (hiddenFileInput.current) {
      hiddenFileInput.current.value = "";
    }
  };

  // Bấm nút Tải lên (Submit)
  const handleSubmitUpload = async () => {
    if (!subject.trim()) {
      setError("Vui lòng nhập tên môn học!");
      return;
    }

    if (!selectedFile) {
      setError("Vui lòng chọn hoặc kéo thả tài liệu câu hỏi (.docx)!");
      return;
    }

    try {
      setIsUploading(true);
      setError(null);

      const formData = new FormData();
      formData.append("file", selectedFile);

      const data = await storageService.uploadFileToQuestionBank(
        subject.trim(),
        formData,
      );

      if (data) {
        setIsSuccess(true);
        // Reset form sau khi thành công
        setSelectedFile(null);
        setSubject("");
      }
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setIsSuccess(false);
        setError("Có lỗi trong quá trình tải lên câu hỏi.");
      }
    } finally {
      setIsUploading(false);
    }
  };

  // Tự động ẩn thông báo sau 2.5 giây
  useEffect(() => {
    if (error !== null || isSuccess !== null) {
      const timer = setTimeout(() => {
        setError(null);
        setIsSuccess(null);
      }, 2500);
      return () => clearTimeout(timer);
    }
  }, [error, isSuccess]);

  return {
    hiddenFileInput,
    subject,
    selectedFile,
    selectedFileName: selectedFile?.name ?? null,
    isUploading,
    isSuccess,
    error,
    isDragging,
    setSubject,
    handleInputClick,
    onFileChange,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    handleRemoveFile,
    handleSubmitUpload,
  };
}
