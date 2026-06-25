import { LessonData } from "@/domain/entities/category.entity";
import { Dispatch, SetStateAction, useEffect, useState } from "react";
import ExamLessonSelect from "./ExamLessonSelect";

interface LessonBlockData {
  currentLesson: { id: string; name: string };
  lessonsCount: number;
  selectedLessons: { id: string; name: string }[];
  index: number;
  lessonsData: LessonData[];
  setSelectedLessons: Dispatch<SetStateAction<{ id: string; name: string }[]>>;
  handleLessonSelect(
    currentId: string,
    id: string,
    name: string,
    index: number,
  ): void;
  handleAddLesson(): void;
}

export default function LessonExamBlock({
  currentLesson,
  lessonsCount,
  selectedLessons,
  index,
  lessonsData,
  setSelectedLessons,
  handleLessonSelect,
  handleAddLesson,
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

      <div className="flex justify-center mt-5">
        {index === selectedLessons.length - 1 &&
          selectedLessons.length !== lessonsCount && (
            <button
              className="px-4 py-2 rounded-lg border-2 border-blue-600 text-blue-600 font-medium cursor-pointer hover:bg-blue-600  hover:text-white transition-all"
              onClick={handleAddLesson}
            >
              Thêm nội dung / đơn vị kiến thức
            </button>
          )}
      </div>
    </div>
  );
}
