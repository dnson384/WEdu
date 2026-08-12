import { DropdownFilter } from "./DropDownFilter";
import { icons } from "@/presentation/common/icons";

interface FilterBarProps {
  selectedChapterIds: string[];
  selectedLessonIds: string[];
  chapterSearch: string;
  lessonSearch: string;
  setChapterSearch: (v: string) => void;
  setLessonSearch: (v: string) => void;
  filteredChapters: any[];
  filteredLessons: any[];
  toggleChapter: (id: string) => void;
  toggleLesson: (id: string) => void;
  totalChaptersSelected: number;
  totalLessonsSelected: number;
  handleFilterClick: () => void;
}

export default function FilterBar({
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
  totalChaptersSelected,
  totalLessonsSelected,
  handleFilterClick,
}: FilterBarProps) {
  const chapterItems = filteredChapters.map((c) => ({
    id: c.id,
    name: c.chapter,
  }));
  const lessonItems = filteredLessons.map((l) => ({ id: l.id, name: l.name }));

  return (
    <div className="w-full bg-white border border-gray-100 rounded-xl p-5 shadow-sm">
      {/* Header phần bộ lọc */}
      <div className="flex items-center gap-2 mb-4">
        <span className="text-blue-500">{icons.filter}</span>
        <span className="text-gray-800 font-semibold">Bộ lọc</span>
      </div>

      {/* Grid chứa 2 Dropdown */}
      <div className="flex flex-col md:flex-row gap-4">
        {/* Dropdown chọn Chương */}
        <DropdownFilter
          label="Chương"
          placeholder="Tất cả chương"
          searchPlaceholder="Tìm kiếm chương..."
          selectedCount={totalChaptersSelected}
          searchValue={chapterSearch}
          onSearchChange={setChapterSearch}
          items={chapterItems}
          selectedIds={selectedChapterIds}
          onToggle={toggleChapter}
        />

        {/* Dropdown chọn Bài */}
        <DropdownFilter
          label="Bài"
          placeholder="Tất cả bài"
          searchPlaceholder="Tìm kiếm bài học..."
          selectedCount={totalLessonsSelected}
          searchValue={lessonSearch}
          onSearchChange={setLessonSearch}
          items={lessonItems}
          selectedIds={selectedLessonIds}
          onToggle={toggleLesson}
        />
      </div>

      <div
        className="mt-2 w-fit bg-blue-500 text-white font-bold px-10 py-2 rounded-lg hover:bg-blue-700 select-none"
        onClick={handleFilterClick}
      >
        <p>Lọc</p>
      </div>
    </div>
  );
}
