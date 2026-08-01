import { LessonData } from "@/domain/entities/category.entity";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import ExamLessonSelect from "./ExamLessonSelect";

interface LessonBlockData {
  currentLesson: { id: string; name: string };
  index: number;
  lessonsData: LessonData[];
  setSelectedLessons: Dispatch<SetStateAction<{ id: string; name: string }[]>>;
  handleLessonSelect(
    currentId: string,
    id: string,
    name: string,
    index: number,
  ): void;
}

export default function LessonExamBlock({
  currentLesson,
  index,
  lessonsData,
  setSelectedLessons,
  handleLessonSelect,
}: LessonBlockData) {
  const [search, setSearch] = useState<string>(currentLesson.name);
  const [isOpen, setIsOpen] = useState<boolean>(false);

  useEffect(() => {
    setSearch(currentLesson.name);
  }, [currentLesson.name]);

  const filteredLessons = lessonsData.filter((lesson: LessonData) =>
    lesson.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div>
      <ExamLessonSelect
        filteredLessons={filteredLessons}
        searchLesson={search}
        isOpen={isOpen}
        onSearchChange={(value: string) => {
          setSearch(value);
          setIsOpen(true);
          if (value.trim() === "") {
            setSelectedLessons((prev) => prev.filter((_, i) => i !== index));
          }
        }}
        onSelect={(id: string, name: string) => {
          handleLessonSelect(currentLesson.id, id, name, index);
          setSearch(lessonsData.find((lesson) => lesson.id === id)?.name ?? "");
          setIsOpen(false);
        }}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
      />

      
    </div>
  );
}
