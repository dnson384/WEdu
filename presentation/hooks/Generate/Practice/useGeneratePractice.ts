import { LessonData } from "@/domain/entities/category.entity";
import { LessonPayload } from "@/domain/entities/generatePractice.entity";
import {
  GeneratePayloadSchema,
  LessonsPayload,
} from "@/presentation/schemas/generate";
import { generatePracticeService } from "@/presentation/services/question.service";
import { useRouter } from "next/navigation";
import { ChangeEvent, useEffect, useState } from "react";

export default function useGeneratePractice() {
  const router = useRouter();

  // Tiêu đề
  const [practiceTitle, setPracticeTitle] = useState<string>("");

  // Chương
  const [searchChapter, setSearchChapter] = useState<string>("");
  const [selectedChapter, setSelectedChapter] = useState<string>("");
  const [isOpenChapter, setIsOpenChapter] = useState(false);

  // Bài
  const [selectedLessons, setSelectedLessons] = useState<LessonPayload[]>([]);

  // UI
  const [error, setError] = useState<string | null>(null);

  const handleChapterSelect = (chapter: string) => {
    setSelectedChapter(chapter);
    setSearchChapter(chapter);
    setIsOpenChapter(false);
  };

  const newLessonData = (): LessonPayload => ({
    name: "",
    questionsCount: 10,
    exerciseTypes: [],
    difficultyLevels: [],
    learningOutcomes: [],
    questionTypes: [],
  });

  const handleLessonSelect = (lesson: LessonData, index: number) => {
    setSelectedLessons((prev) => {
      const lessons = [...prev];
      lessons[index].name = lesson.name;
      return lessons;
    });
  };

  const handleLessonOptionSelect = (
    event: ChangeEvent<HTMLInputElement>,
    field: string,
    index: number,
  ) => {
    const { checked, value } = event.target;
    setSelectedLessons((prev) => {
      const lessonsCopy = [...prev];
      const currentLesson = { ...lessonsCopy[index] };

      if (field === "Dạng bài") {
        if (checked) {
          currentLesson.exerciseTypes = [...currentLesson.exerciseTypes, value];
        } else {
          currentLesson.exerciseTypes = currentLesson.exerciseTypes.filter(
            (type) => type != value,
          );
        }
      } else if (field === "Số lượng câu hỏi") {
        if (Number(value) >= 0) currentLesson.questionsCount = Number(value);
      } else if (field === "Độ khó") {
        if (checked) {
          currentLesson.difficultyLevels = [
            ...currentLesson.difficultyLevels,
            value,
          ];
        } else {
          currentLesson.difficultyLevels =
            currentLesson.difficultyLevels.filter((type) => type !== value);
        }
      } else if (field === "Yêu cầu cần đạt") {
        if (checked) {
          currentLesson.learningOutcomes = [
            ...currentLesson.learningOutcomes,
            value,
          ];
        } else {
          currentLesson.learningOutcomes =
            currentLesson.learningOutcomes.filter((type) => type !== value);
        }
      } else if (field === "Dạng câu hỏi") {
        if (checked) {
          currentLesson.questionTypes = [...currentLesson.questionTypes, value];
        } else {
          currentLesson.questionTypes = currentLesson.questionTypes.filter(
            (type) => type !== value,
          );
        }
      }

      lessonsCopy[index] = currentLesson;
      return lessonsCopy;
    });
  };

  const handleAddLesson = () => {
    setSelectedLessons((prev) => [...prev, newLessonData()]);
  };

  const handleGeneratePractice = async () => {
    const titleError = practiceTitle.trim().length === 0;
    if (titleError) {
      setError("Tiêu đề chưa được nhập");
      return;
    }

    const chapterError = selectedChapter.trim().length === 0;
    if (chapterError) {
      setError("Chương chưa được chọn");
      return;
    }

    const lessonsError = selectedLessons.some((lesson) => {
      if (lesson.name.trim().length === 0) {
        setError("Phải có ít nhất 1 bài");
        return true;
      } else if (!lesson.questionsCount) {
        setError(`Bài ${lesson.name} phải có ít nhất 1 câu hỏir`);
        return true;
      } else if (lesson.difficultyLevels.length === 0) {
        setError(`Độ khó của bài ${lesson.name} chưa được chọn`);
        return true;
      } else if (lesson.exerciseTypes.length === 0) {
        setError(`Dạng bài của bài ${lesson.name} chưa được chọn`);
        return true;
      } else if (lesson.learningOutcomes.length === 0) {
        setError(`Yêu cầu cần đạt của bài ${lesson.name} chưa được chọn`);
        return true;
      } else if (lesson.questionTypes.length === 0) {
        setError(`Dạng câu hỏi của bài ${lesson.name} chưa được chọn`);
        return true;
      }
    });

    if (!titleError && !lessonsError && !chapterError) {
      const lessonsPayload: LessonsPayload[] = selectedLessons.map(
        (lesson) => ({
          name: lesson.name,
          questionsCount: lesson.questionsCount,
          exerciseTypes: lesson.exerciseTypes,
          difficultyLevels: lesson.difficultyLevels,
          learningOutcomes: lesson.learningOutcomes,
          questionTypes: lesson.questionTypes,
        }),
      );

      const payload: GeneratePayloadSchema = {
        title: practiceTitle,
        chapter: selectedChapter,
        lessons: lessonsPayload,
      };

      const practiceId: string = await generatePracticeService(payload);
      if (practiceId) {
        router.push(`/practice/${practiceId}`);
      }
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (error) {
        setError(null);
      }
    }, 5000);

    return () => clearTimeout(timer);
  }, [error]);

  useEffect(() => {
    if (selectedChapter.trim().length > 0) {
      setSelectedLessons(() => {
        const lesson = [];
        lesson.push(newLessonData());
        return lesson;
      });
    }
  }, [selectedChapter]);

  return {
    // Title
    practiceTitle,
    setPracticeTitle,
    // Chương
    searchChapter,
    selectedChapter,
    isOpenChapter,
    setIsOpenChapter,
    setSearchChapter,
    setSelectedChapter,
    handleChapterSelect,
    // Bài
    selectedLessons,
    setSelectedLessons,
    handleLessonSelect,
    // Các opiton của bài
    handleLessonOptionSelect,
    handleAddLesson,
    // UI
    error,
    // Func
    handleGeneratePractice,
  };
}
