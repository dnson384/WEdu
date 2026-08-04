"use client";

import { icons } from "@/presentation/common/icons";
// Component
import ChapterExamBlock from "@/presentation/components/Generate/ChapterExamBlock";
import ContinueBtn from "@/presentation/components/layout/ContinueBtn";
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
    handleAddChapter,
    handleRemoveChapter,
    handleBackClick,
    handleContinueClick,
  } = useSelectChapterPage();
  const { user, isLoadingUser } = useAuth();

  const chapters = categories.map((category) => ({
    id: category.id,
    name: category.chapter,
  }));

  // Chương chưa được chọn
  const unSelectedChaptersData = categories
    .map((category) => ({
      id: category.id,
      name: category.chapter,
    }))
    .filter(
      (chapter) => !selectedChapters.map((sc) => sc.id).includes(chapter.id),
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
                {/* Ô input thêm chương */}
                <div>
                  <ChapterExamBlock
                    unSelectedChaptersData={unSelectedChaptersData}
                    handleAddChapter={handleAddChapter}
                  />
                </div>

                {/* Danh sách chương đã chọn */}
                <ul className="mt-5">
                  {selectedChapters.map((chapter, index) => (
                    <li
                      key={chapter.id}
                      className="flex justify-between items-center px-4 py-2.5 bg-blue-50 border border-blue-100 rounded-xl"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-7 h-7 text-sm text-white font-bold bg-blue-500 flex items-center justify-center rounded-xl">
                          {index}
                        </div>
                        <span className="font-medium">{chapter.name}</span>
                      </div>

                      <button
                        onClick={() => handleRemoveChapter(chapter.id)}
                        className="p-2 text-gray-500 hover:text-red-500 transition-colors"
                      >
                        {icons.bin_18px}
                      </button>
                    </li>
                  ))}
                </ul>
              </article>

              <div className="mt-10 w-full flex justify-end">
                <ContinueBtn
                  enableToContinue={selectedChapters.length > 0}
                  handleContinueClick={handleContinueClick}
                  handleBackClick={handleBackClick}
                />
              </div>
            </section>
          </main>
        </div>
      )}
    </>
  );
}
