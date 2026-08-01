"use client";

import { ExamDetailReponseEntity } from "@/domain/entities/exam.entity";
import { ExportPayload } from "@/presentation/schemas/export.schema";
import {
  exportWordFileService,
  GetExamService,
} from "@/presentation/services/exam.service";
import { useQuery } from "@tanstack/react-query";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

export default function useExam() {
  const pathname = usePathname();
  const pathnameSplitted = pathname.split("/");
  const examId = pathnameSplitted[pathnameSplitted.length - 1];

  const searchParams = useSearchParams();
  const errorParam = searchParams.get("errors");
  const [errorsList, setErrorsList] = useState<string[]>(() =>
    errorParam ? errorParam.split("_||_") : [],
  );

  useEffect(() => {
    if (errorsList.length === 0) return;

    const timer = setTimeout(() => {
      setErrorsList([]);
    }, 5000);

    return () => clearTimeout(timer);
  }, []);

  const initialResponse = (): ExamDetailReponseEntity => ({
    id: "",
    name: "",
    groups: [],
  });

  const { data, isLoading } = useQuery({
    queryKey: ["exam", examId],
    queryFn: () => GetExamService(examId),
    staleTime: 1000 * 60 * 5,
    retry: false,
    refetchOnWindowFocus: false,
    enabled: !!examId,
  });

  const details = data || initialResponse();

  const handleExportDocx = async () => {
    const payload: ExportPayload = {
      examId: details.id,
      examName: details.name,
    };

    try {
      const rawData = await exportWordFileService(payload);
      const blob = new Blob([new Uint8Array(rawData)], {
        type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      });

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${details.name}.docx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Lỗi khi tải file UI:", err);
    }
  };
  return { details, isLoading, errorsList, handleExportDocx };
}
