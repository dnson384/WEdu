import { useState } from "react";
import ExamChapterSelect from "./ExamChapterSelect";

interface ChapterBlockProps {
  unSelectedChaptersData: { id: string; name: string }[];
  handleAddChapter: (chapter: { id: string; name: string }) => void;
}

export default function ChapterExamBlock({
  unSelectedChaptersData,
  handleAddChapter,
}: ChapterBlockProps) {
  const [search, setSearch] = useState<string>("");
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [stagedChapter, setStagedChapter] = useState<{
    id: string;
    name: string;
  } | null>(null);

  const filteredChapters = unSelectedChaptersData.filter((chapter) =>
    chapter.name.toLowerCase().includes(search.toLowerCase()),
  );

  const handleAddClick = () => {
    if (stagedChapter) {
      handleAddChapter(stagedChapter);
      setSearch("");
      setStagedChapter(null);
    }
  };

  return (
    <div className="flex items-center gap-3">
      <ExamChapterSelect
        filteredChapters={filteredChapters}
        search={search}
        isOpen={isOpen}
        onSearchChange={(value) => {
          setSearch(value);
          setStagedChapter(null);
          setIsOpen(true);
        }}
        onSelect={(id: string, name: string) => {
          setSearch(name);
          setStagedChapter({ id, name });
          setIsOpen(false);
        }}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
      />

      <button
        type="button"
        disabled={!stagedChapter}
        onClick={handleAddClick}
        className="bg-blue-500 hover:bg-blue-700 disabled:bg-blue-200 disabled:cursor-not-allowed text-white px-6 py-2.5 rounded-xl font-medium shadow-md transition-colors whitespace-nowrap"
      >
        Thêm
      </button>
    </div>
  );
}
