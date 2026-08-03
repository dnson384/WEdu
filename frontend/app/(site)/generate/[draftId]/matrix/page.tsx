"use client";

import MatrixBlock from "@/presentation/components/Generate/MatrixBlock";
import ContinueBtn from "@/presentation/components/layout/ContinueBtn";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useMatrix from "@/presentation/hooks/Generate/Exam/Child/useMatrix";
import { transformMatrixToUI } from "@/presentation/utils/transformMatrixToUI";

export default function Matrix() {
  const {
    changesChapters,
    curChapter,
    isLoading,
    handleChangeChapter,
    handleMatrixInputChange,
    handleBackClick,
    handleContinueClick,
  } = useMatrix();

  const { user, isLoadingUser } = useAuth();

  const chaptersCount = changesChapters.length;

  return (
    <>
      {isLoadingUser || isLoading ? (
        <Loader />
      ) : (
        <div className="bg-blue-50/10 pt-20 flex justify-center">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="ml-60 w-6xl">
            <h1 className="text-4xl font-bold text-center text-blue-500">
              Ma trận đề thi
            </h1>

            {/* Sub navigation */}
            <div
              className={`mt-5 grid gap-5`}
              style={{
                gridTemplateColumns: `repeat(${chaptersCount}, minmax(0, 1fr))`,
              }}
            >
              {changesChapters.map((chapterData) => {
                const isCurrent = curChapter.id === chapterData.id;

                return (
                  <div
                    key={chapterData.id}
                    className={`flex items-center gap-2 px-3 py-1.5 rounded-md ${isCurrent ? "bg-blue-100" : "bg-gray-100"} cursor-pointer`}
                    onClick={() => handleChangeChapter(chapterData.id)}
                  >
                    <p>{chapterData.name}</p>
                  </div>
                );
              })}
            </div>

            {/* Current Chapter Matrix */}
            <section className="mt-10">
              {curChapter.lessons.map((lesson) => {
                const groupedMatrix = transformMatrixToUI(lesson.matrix);

                return (
                  <article key={lesson.id} className="mt-10">
                    {lesson.matrix.length > 0 && (
                      <>
                        <h2 className="text-2xl font-semibold text-center text-blue-500">
                          {lesson.name}
                        </h2>

                        <div className="flex flex-col gap-4">
                          {Object.entries(groupedMatrix).map(
                            ([questionType, questionTypeData]) => {
                              return (
                                <MatrixBlock
                                  key={questionType}
                                  lessonId={lesson.id}
                                  questionType={questionType}
                                  questionTypeData={questionTypeData}
                                  handleMatrixInputChange={
                                    handleMatrixInputChange
                                  }
                                />
                              );
                            },
                          )}
                        </div>
                      </>
                    )}
                  </article>
                );
              })}
            </section>

            <div className="my-10 w-full flex justify-end">
              <ContinueBtn
                handleContinueClick={handleContinueClick}
                handleBackClick={handleBackClick}
              />
            </div>
          </main>
        </div>
      )}
    </>
  );
}
