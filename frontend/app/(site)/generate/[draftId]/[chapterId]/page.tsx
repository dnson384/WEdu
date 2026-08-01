"use client";

import LessonExamBlock from "@/presentation/components/Generate/LessonExamBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useChapter from "@/presentation/hooks/Generate/Exam/Child/useChapter";

export default function GenerateExam() {
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
  } = useChapter();
  const { user, isLoadingUser } = useAuth();

  const lessons = currentChapter?.lessons ?? [];

  const lessonsData = lessons.filter(
    (lesson) =>
      !Object.values(selectedLessons)
        .map((l) => l.id)
        .includes(lesson.id),
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
    <div className="bg-blue-50/50 h-screen pt-20">
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
        <div className="mt-32 h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <div className="bg-blue-50/50 h-screen pt-20">
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
