"use client";

import { icons } from "@/presentation/common/icons";
// COMPONENT
import LessonExamBlock from "@/presentation/components/Generate/LessonExamBlock";
import NavigationBTN from "@/presentation/components/layout/NavigationBTN";
import Error from "@/presentation/components/layout/Error";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";
// HOOK
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useSelectLessonPage from "@/presentation/hooks/Generate/Child/useSelectLessonPage";
import useChapter from "@/presentation/hooks/Generate/useChapter";

export default function SelectLesson() {
  const { user, isLoadingUser } = useAuth();
  const { chapters, isLoadingChapters } = useChapter();
  const {
    currentChapter,
    isLoading,
    isLoadingDraft,
    isSubmitting,
    errorMessage,
    selectedLessons,
    unSelectedLessons,
    handleAddLesson,
    handleRemoveLesson,
    handleBackClick,
    handleContinueClick,
  } = useSelectLessonPage(chapters);

  const isGlobalLoading =
    isLoadingUser || isLoading || isLoadingChapters || isLoadingDraft;

  return (
    <div className="min-h-screen bg-blue-500/5">
      {/* Thông báo lỗi */}
      {errorMessage && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center pointer-events-none">
          <Error message={errorMessage} />
        </div>
      )}

      {isGlobalLoading ? (
        <Loader />
      ) : (
        <div className="min-h-screen pt-20">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          <main className="ml-60">
            <section className="bg-white rounded-2xl shadow-lg w-4xl mx-auto py-10 px-15">
              <h1 className="text-4xl font-bold">
                Nội dung / Đơn vị kiến thức
              </h1>
              <h2 className="mt-10 text-3xl font-bold text-blue-600">
                {currentChapter?.name}
              </h2>

              <article className="mt-5">
                {/* Ô input thêm bài theo chương */}
                <LessonExamBlock
                  unSelectedLessons={unSelectedLessons}
                  handleAddLesson={handleAddLesson}
                />

                {/* Danh sách bài đã chọn */}
                <ul className="space-y-3 mt-5">
                  {selectedLessons.map((lesson, index) => (
                    <li
                      key={lesson.id}
                      className="flex justify-between items-center px-4 py-2.5 bg-blue-50 border border-blue-100 rounded-xl"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-7 h-7 text-sm text-white font-bold bg-blue-500 flex items-center justify-center rounded-xl">
                          {index + 1}
                        </div>
                        <span>{lesson.name}</span>
                      </div>

                      <button
                        onClick={() => handleRemoveLesson(lesson.id)}
                        className="p-2 text-gray-500 hover:text-red-500 transition-colors"
                      >
                        {icons.bin_18px}
                      </button>
                    </li>
                  ))}
                </ul>

                <div className="mt-10 w-full flex justify-end">
                  <NavigationBTN
                    enableToContinue={
                      selectedLessons.length > 0 && !isSubmitting
                    }
                    handleContinueClick={handleContinueClick}
                    handleBackClick={handleBackClick}
                  />
                </div>
              </article>
            </section>
          </main>
        </div>
      )}
    </div>
  );
}
