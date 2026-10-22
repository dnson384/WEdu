"use client";
import { useEffect, useMemo, useState } from "react";
import type { ChapterEntity } from "@/domain/entities/chapter.entity";
import { ExamResponseEntity } from "@/domain/entities/exam.entity";

interface UseFilterProps {
  chapters: ChapterEntity[];
  exams: ExamResponseEntity[];
}

export default function useExamFilter({ chapters, exams }: UseFilterProps) {
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
    return chapters.filter((c) =>
      c.name.toLowerCase().includes(chapterSearch.toLowerCase()),
    );
  }, [chapters, chapterSearch]);

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
      return chapters.flatMap((c) => c.lessons);
    }
    return chapters
      .filter((c) => selectedChapterIds.includes(c.id))
      .flatMap((c) => c.lessons);
  }, [chapters, selectedChapterIds]);

  // Danh sách Bài học sau khi tìm kiếm
  const filteredLessons = useMemo(() => {
    return availableLessons.filter((l) =>
      l.name.toLowerCase().includes(lessonSearch.toLowerCase()),
    );
  }, [availableLessons, lessonSearch]);

  // -------------------- Exam Filtered --------------------
  const [appliedFilters, setAppliedFilters] = useState<{
    chapterIds: string[];
    lessonIds: string[];
  }>({
    chapterIds: [],
    lessonIds: [],
  });

  const handleFilterClick = () => {
    setAppliedFilters({
      chapterIds: selectedChapterIds,
      lessonIds: selectedLessonIds,
    });
  };

  const examFiltered: ExamResponseEntity[] = useMemo(() => {
    const { chapterIds, lessonIds } = appliedFilters;

    if (chapterIds.length === 0 && lessonIds.length === 0) {
      return exams;
    }

    return exams.filter((exam) => {
      const matchesChapter = chapterIds.every((selectedId) =>
        exam.chapters.some((chapter) => chapter.id === selectedId),
      );

      const allLessonIdsInExam = exam.chapters.flatMap(
        (chapter) => chapter.lessonIds,
      );

      const matchesLesson = lessonIds.every((selectedId) =>
        allLessonIdsInExam.includes(selectedId),
      );

      console.log("lessonIds", lessonIds);
      console.log("allLessonIdsInExam", allLessonIdsInExam);

      return matchesChapter && matchesLesson;
    });
  }, [exams, appliedFilters]);

  useEffect(() => {
    console.log("chapterIds", selectedChapterIds);
  }, [selectedChapterIds]);

  useEffect(() => {
    console.log("lessonIds", selectedLessonIds);
  }, [selectedLessonIds]);

  useEffect(() => {
    console.log("appliedFilters", appliedFilters);
  }, [appliedFilters]);

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
    examFiltered,
    handleFilterClick,
  };
}
