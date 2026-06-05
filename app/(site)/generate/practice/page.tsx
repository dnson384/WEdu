"use client";

import ChapterSelect from "@/presentation/components/Category/ChapterSelect";
import LessonBlock from "@/presentation/components/Generate/Practice/LessonBlock";
import NavBar from "@/presentation/components/layout/NavBar";
import useCategory from "@/presentation/hooks/Generate/useCategory";
import useGeneratePractice from "@/presentation/hooks/Generate/Practice/useGeneratePractice";

export default function GeneratePractice() {
  const { categories } = useCategory();
  const {
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
    handleLessonOptionSelect,
    handleAddLesson,
    // UI
    error,
    // Func
    handleGeneratePractice,
  } = useGeneratePractice();

  const chapters = categories.map((category) => category.chapter);
  const filteredChapters = chapters.filter((chapter) =>
    chapter.toLowerCase().includes(searchChapter.toLowerCase()),
  );

  const lessonsData =
    categories.find((category) => category.chapter === selectedChapter)
      ?.lessons || [];

  const icons = {
    error: (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
      >
        <path
          fill="#fb2c36"
          d="M12 4c-4.419 0-8 3.582-8 8s3.581 8 8 8s8-3.582 8-8s-3.581-8-8-8m3.707 10.293a.999.999 0 1 1-1.414 1.414L12 13.414l-2.293 2.293a.997.997 0 0 1-1.414 0a1 1 0 0 1 0-1.414L10.586 12L8.293 9.707a.999.999 0 1 1 1.414-1.414L12 10.586l2.293-2.293a.999.999 0 1 1 1.414 1.414L13.414 12z"
        />
      </svg>
    ),
  };

  return (
    <>
      <NavBar />

      <main className="max-w-6xl p-4 mx-auto mt-26 mb-50">
        {error && (
          <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
            <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
              {icons.error}
              <p className="text-red-500">{error}</p>
            </div>
          </div>
        )}

        <div className="flex flex-col gap-3 items-center">
          <label htmlFor="practice_name" className="flex items-center gap-4">
            <span className="w-40 text-lg font-semibold text-blue-600">
              Tiêu đề
            </span>
            <input
              type="text"
              id="practice_name"
              placeholder="Tiêu đề ôn tập"
              className="w-xl px-3 py-1 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
              value={practiceTitle}
              onChange={(e) => {
                setPracticeTitle(e.target.value);
              }}
            />
          </label>

          <ChapterSelect
            chapters={filteredChapters}
            searchChapter={searchChapter}
            isOpen={isOpenChapter}
            onSearchChange={(value) => {
              setSearchChapter(value);
              setIsOpenChapter(true);
              if (value.trim() === "") {
                setSelectedChapter("");
                setSelectedLessons([]);
              }
            }}
            onSelect={handleChapterSelect}
            onOpen={() => setIsOpenChapter(true)}
            onClose={() => setIsOpenChapter(false)}
          />

          {selectedChapter.trim() && (
            <div className="flex flex-col gap-10">
              {selectedLessons.map((lesson, index) => {
                return (
                  <LessonBlock
                    key={index}
                    lesson={lesson}
                    selectedLessons={selectedLessons}
                    index={index}
                    lessonsData={lessonsData}
                    setSelectedLessons={setSelectedLessons}
                    handleLessonSelect={handleLessonSelect}
                    handleLessonOptionSelect={handleLessonOptionSelect}
                    handleAddLesson={handleAddLesson}
                  />
                );
              })}
            </div>
          )}
        </div>
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-50">
          <div className="max-w-6xl mx-auto flex justify-end">
            <button
              type="button"
              onClick={handleGeneratePractice}
              className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors"
            >
              Tạo đề ôn tập
            </button>
          </div>
        </div>
      </main>
    </>
  );
}
