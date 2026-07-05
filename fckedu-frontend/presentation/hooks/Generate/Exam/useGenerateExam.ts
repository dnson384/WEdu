import {
  LessonsPayload,
} from "@/presentation/schemas/generate";
import { useState } from "react";

export default function useGenerateExam() {
  // Tiêu đề
  const [practiceTitle, setPracticeTitle] = useState<string>("");

  // Chương
  const [selectedChapters, setSelectedChapters] = useState<string[]>([""]);

  // Bài
  const [selectedLessons, setSelectedLessons] = useState<LessonsPayload[]>([]);

  // UI
  const [error, setError] = useState<string | null>(null);

  const handleChapterSelect = (id: string, index: number) => {
    setSelectedChapters((prev) => {
      const newSelectedChapters = [...prev];
      newSelectedChapters[index] = id;
      return newSelectedChapters;
    });
  };

  const handleAddChapter = () => {
    setSelectedChapters((prev) => [...prev, ""]);
  };

  return {
    // Title
    practiceTitle,
    setPracticeTitle,
    // Chương
    selectedChapters,
    setSelectedChapters,
    handleChapterSelect,
    handleAddChapter,
    // Bài
    selectedLessons,
    setSelectedLessons,
    // UI
    error,
  };
}
