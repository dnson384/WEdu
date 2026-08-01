"use client";

import ChapterExamBlock from "@/presentation/components/Generate/ChapterExamBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useDraft from "@/presentation/hooks/Generate/Exam/Child/useDraft";

export default function GenerateExam() {
  const {
    categories,
    isLoadingDraft,
    errorMessage,
    selectedChapters,
    setSelectedChapters,
    handleChapterSelect,
    handleAddChapter,
    handleBackClick,
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
        <div className="bg-blue-50/50 h-screen pt-20">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          {isLoadingDraft ? (
            <div className="h-screen mx-auto px-4 flex justify-center items-center">
              <div className="loader"></div>
            </div>
          ) : (
            <main className="ml-60">
              <section className="bg-white rounded-2xl shadow-lg w-4xl mx-auto py-10 px-15">
                <h1 className="text-4xl font-bold">Chương / chủ đề</h1>
                <article className="mt-10">
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

                  <div className="mt-5 flex items-center justify-between">
                    <button
                      type="button"
                      onClick={handleBackClick}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
                    >
                      Quay lại
                    </button>

                    <div className="flex justify-center ">
                      {selectedChapters.length === chapters.length - 1 &&
                        chaptersData.length !== 0 && (
                          <button
                            className="w-50 py-2 rounded-lg border-2 border-blue-600 text-blue-600 font-medium cursor-pointer hover:bg-blue-600  hover:text-white transition-all"
                            onClick={handleAddChapter}
                          >
                            Thêm chương / chủ đề
                          </button>
                        )}
                    </div>

                    <button
                      type="button"
                      disabled={!selectedChapters[0]?.id}
                      onClick={handleContinueClick}
                      className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer disabled:bg-gray-400 disabled:text-gray-200 disabled:opacity-60 disabled:cursor-not-allowed"
                    >
                      Tiếp tục
                    </button>
                  </div>
                </article>
              </section>
            </main>
          )}
        </div>
      )}
    </>
  );
}
