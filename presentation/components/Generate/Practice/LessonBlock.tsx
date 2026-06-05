import { LessonData } from "@/domain/entities/category.entity";
import { Dispatch, SetStateAction, ChangeEvent, useState } from "react";
import LessonSelect from "../../Category/LessonSelect";
import { LessonPayload } from "@/domain/entities/generatePractice.entity";
import LessonOptionSelect from "../../Category/LessonOptionSelect";

interface LessonBlockData {
  lesson: LessonPayload;
  selectedLessons: LessonPayload[];
  index: number;
  lessonsData: LessonData[];
  handleLessonSelect(lesson: LessonData, index: number): void;
  setSelectedLessons: Dispatch<SetStateAction<LessonPayload[]>>;
  handleLessonOptionSelect(
    event: ChangeEvent<HTMLInputElement>,
    field: string,
    index: number,
  ): void;
  handleAddLesson(): void;
}

export default function LessonBlock({
  lesson,
  selectedLessons,
  index,
  lessonsData,
  setSelectedLessons,
  handleLessonSelect,
  handleLessonOptionSelect,
  handleAddLesson,
}: LessonBlockData) {
  const [search, setSearch] = useState<string>(lesson.name || "");
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const selectedLessonNames = selectedLessons.map((sl) => sl.name);

  const filteredLessons = lessonsData.filter((l: LessonData) => {
    const matchesSearch = l.name.toLowerCase().includes(search.toLowerCase());

    const isSelected =
      selectedLessonNames.includes(l.name) && l.name !== lesson.name;

    return matchesSearch && !isSelected;
  });

  const currentLesson = lessonsData.find((l: any) => l.name === lesson.name);

  const icons = {
    add: (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        viewBox="0 0 24 24"
      >
        <g fill="none">
          <path d="m12.593 23.258l-.011.002l-.071.035l-.02.004l-.014-.004l-.071-.035q-.016-.005-.024.005l-.004.01l-.017.428l.005.02l.01.013l.104.074l.015.004l.012-.004l.104-.074l.012-.016l.004-.017l-.017-.427q-.004-.016-.017-.018m.265-.113l-.013.002l-.185.093l-.01.01l-.003.011l.018.43l.005.012l.008.007l.201.093q.019.005.029-.008l.004-.014l-.034-.614q-.005-.018-.02-.022m-.715.002a.02.02 0 0 0-.027.006l-.006.014l-.034.614q.001.018.017.024l.015-.002l.201-.093l.01-.008l.004-.011l.017-.43l-.003-.012l-.01-.01z" />
          <path
            fill="currentColor"
            d="M10.5 20a1.5 1.5 0 0 0 3 0v-6.5H20a1.5 1.5 0 0 0 0-3h-6.5V4a1.5 1.5 0 0 0-3 0v6.5H4a1.5 1.5 0 0 0 0 3h6.5z"
          />
        </g>
      </svg>
    ),
  };

  return (
    <div>
      <LessonSelect
        lessons={filteredLessons}
        searchLesson={search}
        isOpen={isOpen}
        onSearchChange={(value: string) => {
          setSearch(value);
          setIsOpen(true);
          if (value.trim() === "") {
            setSelectedLessons((prev) => prev.filter((_, i) => i !== index));
          }
        }}
        onSelect={(selectedOption) => {
          handleLessonSelect(selectedOption, index);
          setSearch(selectedOption.name);
          setIsOpen(false);
        }}
        onOpen={() => setIsOpen(true)}
        onClose={() => setIsOpen(false)}
      />

      {currentLesson && (
        <>
          <div className="mt-5 flex flex-col gap-3">
            <div>
              <label className="flex items-center gap-4 w-full">
                <span className="w-40 font-semibold text-blue-500">
                  Số lượng câu hỏi
                </span>
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  placeholder="Nhập số lượng câu hỏi"
                  className="w-xl px-3 py-1 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
                  value={lesson.questionsCount > 0 ? lesson.questionsCount : ""}
                  onChange={(e) =>
                    handleLessonOptionSelect(e, "Số lượng câu hỏi", index)
                  }
                />
              </label>
            </div>

            <LessonOptionSelect
              currentLesson={currentLesson}
              title="Dạng bài"
              index={index}
              handleLessonOptionSelect={handleLessonOptionSelect}
            />
            <LessonOptionSelect
              currentLesson={currentLesson}
              title="Độ khó"
              index={index}
              handleLessonOptionSelect={handleLessonOptionSelect}
            />
            <LessonOptionSelect
              currentLesson={currentLesson}
              title="Yêu cầu cần đạt"
              index={index}
              handleLessonOptionSelect={handleLessonOptionSelect}
            />
            <LessonOptionSelect
              currentLesson={currentLesson}
              title="Dạng câu hỏi"
              index={index}
              handleLessonOptionSelect={handleLessonOptionSelect}
            />
          </div>

          {index === selectedLessons.length - 1 &&
          selectedLessons.length < lessonsData.length ? (
            <div className="mt-5 w-full flex justify-center">
              <button
                className="w-50 py-2 rounded-lg border-2 border-blue-600 text-blue-600 font-medium cursor-pointer hover:bg-blue-600  hover:text-white transition-all"
                onClick={handleAddLesson}
              >
                Thêm bài
              </button>
            </div>
          ) : (
            <div className="border border-0.5 border-gray-300 mt-5"></div>
          )}
        </>
      )}
    </div>
  );
}
