import { Dispatch, SetStateAction, useState } from "react";
import ExamChapterSelect from "./ExamChapterSelect";

interface ChapterBlockProps {
  currentChapter: { id: string; name: string };
  selectedChapters: { id: string; name: string }[];
  index: number;
  chaptersData: { id: string; name: string }[];
  handleChapterSelect: (
    curId: string,
    id: string,
    name: string,
    index: number,
  ) => void;
  setSelectedChapters: Dispatch<SetStateAction<{ id: string; name: string }[]>>;
  handleAddChapter: () => void;
}

export default function ChapterExamBlock({
  currentChapter,
  selectedChapters,
  index,
  chaptersData,
  handleChapterSelect,
  setSelectedChapters,
  handleAddChapter,
}: ChapterBlockProps) {
  const [search, setSearch] = useState<string>("");
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const filteredChapters = chaptersData.filter((chapter) =>
    chapter.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div>
      <ExamChapterSelect
        filteredChapters={filteredChapters}
        search={search}
        curValue={currentChapter.name}
        index={index}
        isOpen={isOpen}
        onSearchChange={(value) => {
          setSearch(value);
          setIsOpen(true);
          if (value.trim().length === 0) {
            if (selectedChapters.length > 1) {
              setSelectedChapters((prev) => prev.filter((_, i) => i !== index));
              setIsOpen(false);
            } else {
              setSelectedChapters([{ id: "", name: "" }]);
            }
          }
        }}
        onSelect={(id: string, name: string, index: number) => {
          handleChapterSelect(currentChapter.id, id, name, index);
          setSearch(chaptersData.find((chapter) => chapter.id === id)?.name!);
          setIsOpen(false);
        }}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
      />

      <div className="flex justify-center mt-5">
        {index === selectedChapters.length - 1 && chaptersData.length !== 0 && (
          <button
            className="w-50 py-2 rounded-lg border-2 border-blue-600 text-blue-600 font-medium cursor-pointer hover:bg-blue-600  hover:text-white transition-all"
            onClick={handleAddChapter}
          >
            Thêm chương / chủ đề
          </button>
        )}
      </div>
    </div>
  );
}
