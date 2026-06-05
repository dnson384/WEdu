"use client";
import MatrixDetailsBlock from "@/presentation/components/Generate/Exam/MatrixDetailsBlock";
import ContinueBtn from "@/presentation/components/layout/ContinueBtn";
import NavBar from "@/presentation/components/layout/Navbar";
import useMatrixDetails from "@/presentation/hooks/Generate/Exam/Child/useMatrixDetails";
import { transformMatrixDetailsToUI } from "@/presentation/utils/transformMatrixDetailsToUI";

export default function MatrixDetails() {
  const {
    isLoading,
    changesChapters,
    curChapter,
    handleChangeChapter,
    handleContinueClick,
  } = useMatrixDetails();

  const chaptersCount = changesChapters.length;

  return (
    <>
      <NavBar />

      {isLoading ? (
        <div className="mt-32 h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <main className="mt-32 w-6xl mx-auto px-4">
          <h1 className="text-4xl font-bold text-center text-blue-600">
            Đặc tả ma trận
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

          <section className="mt-10 mb-30">
            {curChapter.lessons.map((lesson) => {
              const groupedMatrixDetails = transformMatrixDetailsToUI(
                lesson.matrixDetails,
              );

              return (
                <article key={lesson.id} className="mt-10">
                  {lesson.matrixDetails.length > 0 && (
                    <>
                      <h2 className="text-3xl text-center font-semibold text-blue-600">
                        {lesson.name}
                      </h2>

                      <div className="flex flex-col gap-4">
                        {Object.entries(groupedMatrixDetails).map(
                          ([level, levelData]) => {
                            return (
                              <MatrixDetailsBlock
                                key={level}
                                lessonId={lesson.id}
                                level={level}
                                levelData={levelData}
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

          <ContinueBtn
            handleContinueClick={handleContinueClick}
            content="Tạo đề"
          />
        </main>
      )}
    </>
  );
}
