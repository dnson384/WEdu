import { LessonData } from "@/domain/entities/category.entity";
import { useEffect, useState } from "react";
import ExamLessonSelect from "./ExamLessonSelect";

interface LessonBlockData {
  unSelectedLessons: LessonData[];
  handleAddLesson: (lesson: { id: string; name: string }) => void;
}

export default function LessonExamBlock({
  unSelectedLessons,
  handleAddLesson,
}: LessonBlockData) {
  const [search, setSearch] = useState<string>("");
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [stagedLesson, setStagedLesson] = useState<{
    id: string;
    name: string;
  } | null>(null);

  const filteredLessons = unSelectedLessons.filter((lesson: LessonData) =>
    lesson.name.toLowerCase().includes(search.toLowerCase()),
  );

  const handleAddClick = () => {
    if (stagedLesson) {
      handleAddLesson(stagedLesson);
      setSearch("");
      setStagedLesson(null);
    }
  };

  return (
    <div className="flex items-center gap-3">
      <ExamLessonSelect
        filteredLessons={filteredLessons}
        searchLesson={search}
        isOpen={isOpen}
        onSearchChange={(value: string) => {
          setSearch(value);
          setStagedLesson(null);
          setIsOpen(true);
        }}
        onSelect={(id: string, name: string) => {
          setSearch(name);
          setStagedLesson({ id, name });
          setIsOpen(false);
        }}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
      />

      <button
        type="button"
        disabled={!stagedLesson}
        onClick={handleAddClick}
        className="bg-blue-500 hover:bg-blue-700 disabled:bg-blue-200 disabled:cursor-not-allowed text-white px-6 py-2.5 rounded-xl font-medium shadow-md transition-colors whitespace-nowrap"
      >
        Thêm nội dung
      </button>
    </div>
  );
}
