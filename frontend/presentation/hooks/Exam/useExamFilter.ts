"use client";
import { useEffect, useMemo, useState } from "react";
import type { CategoryEntity } from "@/domain/entities/category.entity";
import {
  ExamChapterReposneEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";

interface UseFilterProps {
  categories: CategoryEntity[];
  exams: ExamResponseEntity[];
}

export default function useExamFilter({ categories, exams }: UseFilterProps) {
  // -------------------- Filter - Chapter --------------------
  const [selectedChapterIds, setSelectedChapterIds] = useState<string[]>([]);
  const [chapterSearch, setChapterSearch] = useState("");

  // Xử lý logic chọn Chương (Chapter)
  const toggleChapter = (id: string) => {
    setSelectedChapterIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  // Danh sách Chương sau khi tìm kiếm
  const filteredChapters = useMemo(() => {
    return categories.filter((c) =>
      c.chapter.toLowerCase().includes(chapterSearch.toLowerCase()),
    );
  }, [categories, chapterSearch]);

  // -------------------- Filter - Lesson --------------------
  const [selectedLessonIds, setSelectedLessonIds] = useState<string[]>([]);

  const [lessonSearch, setLessonSearch] = useState("");

  // Xử lý logic chọn Bài học (Lesson)
  const toggleLesson = (id: string) => {
    setSelectedLessonIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  // Danh sách Bài học hiển thị dựa theo Chương được chọn (nếu không chọn chương nào -> hiển thị tất cả)
  const availableLessons = useMemo(() => {
    if (selectedChapterIds.length === 0) {
      return categories.flatMap((c) => c.lessons);
    }
    return categories
      .filter((c) => selectedChapterIds.includes(c.id))
      .flatMap((c) => c.lessons);
  }, [categories, selectedChapterIds]);

  // Danh sách Bài học sau khi tìm kiếm
  const filteredLessons = useMemo(() => {
    return availableLessons.filter((l) =>
      l.name.toLowerCase().includes(lessonSearch.toLowerCase()),
    );
  }, [availableLessons, lessonSearch]);

  // -------------------- Exam Filtered --------------------
  const [examFiltered, setExamFiltered] = useState<ExamResponseEntity[]>([]);
  useEffect(() => {
    setExamFiltered(exams);
  }, [exams]);

  const handleFilterClick = () => {
    const filtered = exams.filter((exam) => {
      const matchesChapter = selectedChapterIds.every((selectedChampId) =>
        exam.chapters.some((chapter) => chapter.id === selectedChampId),
      );

      const allLessonIdsInExam = exam.chapters.flatMap(
        (chapter) => chapter.lessonIds,
      );

      const matchesLesson = selectedLessonIds.every((selectedLessonId) =>
        allLessonIdsInExam.includes(selectedLessonId),
      );

      return matchesChapter && matchesLesson;
    });

    setExamFiltered(filtered);
  };

  return {
    selectedChapterIds,
    selectedLessonIds,
    chapterSearch,
    lessonSearch,
    setChapterSearch,
    setLessonSearch,
    filteredChapters,
    filteredLessons,
    toggleChapter,
    toggleLesson,
    totalChaptersSelected: selectedChapterIds.length,
    totalLessonsSelected: selectedLessonIds.length,
    // Filtered
    examFiltered,
    handleFilterClick,
  };
}
