"use client";

import LessonExamBlock from "@/presentation/components/Generate/LessonExamBlock";
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
    setSelectedLessons,
    handleLessonSelect,
    handleAddLesson,
    handleBackClick,
    handleContinueClick,
  } = useSelectLessonPage();
  const { user, isLoadingUser } = useAuth();

  const lessons = currentChapter?.lessons ?? [];

  const lessonsData = lessons.filter(
    (lesson) =>
      !Object.values(selectedLessons)
        .map((l) => l.id)
        .includes(lesson.id),
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
                <div className="flex flex-col gap-3">
                  {selectedLessons.map((currentLesson, index) => {
                    return (
                      <LessonExamBlock
                        key={index}
                        currentLesson={currentLesson}
                        index={index}
                        lessonsData={lessonsData}
                        setSelectedLessons={setSelectedLessons}
                        handleLessonSelect={handleLessonSelect}
                      />
                    );
                  })}
                </div>

                <div className="mt-5 flex items-center justify-between">
                  <button
                    type="button"
                    onClick={handleBackClick}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
                  >
                    Quay lại
                  </button>

                  <div className="flex justify-center">
                    {selectedLessons.length < lessons.length && (
                      <button
                        className="px-4 py-2 rounded-lg border-2 border-blue-600 text-blue-600 font-medium cursor-pointer hover:bg-blue-600  hover:text-white transition-all"
                        onClick={handleAddLesson}
                      >
                        Thêm nội dung / đơn vị kiến thức
                      </button>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={handleContinueClick}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
                  >
                    Tiếp tục
                  </button>
                </div>
              </article>
            </section>
          </main>
        </div>
      )}
    </div>
  );
}
