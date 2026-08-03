"use client";

// Component
import ChapterExamBlock from "@/presentation/components/Generate/ChapterExamBlock";
import Error from "@/presentation/components/layout/Error";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";

// Hook
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useSelectChapterPage from "@/presentation/hooks/Generate/Exam/Child/useSelectChapterPage";

export default function SelectChapter() {
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
  } = useSelectChapterPage();
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

  return (
    <>
      {/* Thông báo lỗi */}
      {errorMessage && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <Error message={errorMessage} />
        </div>
      )}

      {isLoadingUser || isLoadingDraft ? (
        <Loader />
      ) : (
        <div className="bg-blue-50/50 h-screen pt-20">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

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
        </div>
      )}
    </>
  );
}
