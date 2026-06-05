import { LessonExportPayload } from "../../schemas/export.schema";
import { PracticeDetailEntity } from "@/domain/entities/practice.entity";
import {
  exportWordFile,
  getPracticeDetailByIdService,
} from "@/presentation/services/practice.service";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

export default function usePractice() {
  const pathname = usePathname();

  const [practice, setPractice] = useState<PracticeDetailEntity | null>(null);

  const questionsSorted: LessonExportPayload = {};
  practice?.questions.forEach((question) => {
    if (!questionsSorted[question.lesson]) {
      questionsSorted[question.lesson] = [];
    }

    questionsSorted[question.lesson].push({
      id: question.id,
      questionType: question.questionType,
      question: question.question,
      options: question.options,
    });
  });

  const handleExportDocx = async () => {
    if (!practice || Object.keys(questionsSorted).length === 0) return;

    try {
      const blob = await exportWordFile(practice.title, questionsSorted);

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${practice.title}.docx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Lỗi khi tải file UI:", error);
    }
  };

  useEffect(() => {
    const fetchData = async () => {
      const id = pathname.split("/")[2];
      const response = await getPracticeDetailByIdService(id);

      if (response) {
        setPractice({
          title: response.title,
          chapter: response.chapter,
          questionsCount: response.questionsCount,
          questions: response.questions,
        });
      }
    };
    fetchData();
  }, []);

  return { practice, questionsSorted, handleExportDocx };
}
