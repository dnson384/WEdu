"use client";

import LessonExamBlock from "@/presentation/components/Generate/LessonExamBlock";
import NavigationBTN from "@/presentation/components/layout/NavigationBTN";
import Error from "@/presentation/components/layout/Error";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useSelectLessonPage from "@/presentation/hooks/Generate/Exam/Child/useSelectLessonPage";

export default function SelectLesson() {
  const {
    currentChapter,
    isLoading,
    errorMessage,
    selectedLessons,
    handleAddLesson,
    handleRemoveLesson,
    handleBackClick,
    handleContinueClick,
  } = useSelectLessonPage();
  
  const { user, isLoadingUser } = useAuth();

  const lessons = currentChapter?.lessons ?? [];

  const unSelectedLessons = lessons.filter(
    (lesson) => !selectedLessons.map((l) => l.id).includes(lesson.id),
  );

  return (
    <div className="h-screen">
      {/* Thông báo lỗi */}
      {errorMessage && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <Error message={errorMessage} />
        </div>
      )}

      {isLoadingUser || isLoading ? (
        <Loader />
      ) : (
        <div className="bg-blue-500/5 h-screen pt-20">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          <main className="ml-60">
            <section className="bg-white rounded-2xl shadow-lg w-4xl mx-auto py-10 px-15">
              <h1 className="text-4xl font-bold">
                Nội dung / Đơn vị kiến thức
              </h1>
              <h2 className="mt-10 text-3xl font-bold text-blue-600">
                {currentChapter?.chapter}
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
                        className="text-red-500 hover:text-white hover:bg-red-500 border border-red-500 px-4 py-1 rounded-md text-sm transition-colors"
                      >
                        Xóa
                      </button>
                    </li>
                  ))}
                </ul>

                <div className="mt-10 w-full flex justify-end">
                  <NavigationBTN
                    enableToContinue={selectedLessons.length > 0}
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
