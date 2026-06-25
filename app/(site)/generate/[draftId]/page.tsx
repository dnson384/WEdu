"use client";

import ChapterExamBlock from "@/presentation/components/Generate/ChapterExamBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useDraft from "@/presentation/hooks/Generate/Exam/Child/useDraft";

export default function GenerateExam() {
  const {
    draft,
    categories,
    isLoadingDraft,
    isError,
    errorMessage,
    selectedChapters,
    setSelectedChapters,
    handleChapterSelect,
    handleAddChapter,
    handleContinueClick,
  } = useDraft();
  const { user, isLoadingUser } = useAuth();

  const chapters = categories.map((category) => ({
    id: category.id,
    name: category.chapter,
  }));

  const chaptersData = chapters.filter(
    (chapter) =>
      !Object.values(selectedChapters)
        .map((chapter) => chapter.id)
        .includes(chapter.id),
  );

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
      {/* Thông báo lỗi */}
      {errorMessage && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
            {icons.error}
            <p className="text-red-500">{errorMessage}</p>
          </div>
        </div>
      )}

      {isLoadingUser ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          <NavBar avatarUrl={user.avatarUrl} />

          {isLoadingDraft ? (
            <div className="h-screen mx-auto px-4 flex justify-center items-center">
              <div className="loader"></div>
            </div>
          ) : (
            <main className="mt-32 w-6xl mx-auto px-4">
              <h1 className="text-4xl font-bold text-center">
                Chương / chủ đề
              </h1>
              <section className="mt-10">
                {selectedChapters.map((currentChapter, index) => {
                  return (
                    <ChapterExamBlock
                      currentChapter={{
                        id: currentChapter?.id || "",
                        name: currentChapter?.name || "",
                      }}
                      key={index}
                      selectedChapters={selectedChapters}
                      index={index}
                      chaptersData={chaptersData}
                      handleChapterSelect={handleChapterSelect}
                      setSelectedChapters={setSelectedChapters}
                      handleAddChapter={handleAddChapter}
                    />
                  );
                })}

                <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-50">
                  <div className="max-w-6xl mx-auto flex justify-end">
                    <button
                      type="button"
                      onClick={handleContinueClick}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
                    >
                      Tiếp tục
                    </button>
                  </div>
                </div>
              </section>
            </main>
          )}
        </>
      )}
    </>
  );
}
