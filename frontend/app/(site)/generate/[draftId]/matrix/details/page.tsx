"use client";
import MatrixDetailsBlock from "@/presentation/components/Generate/MatrixDetailsBlock";
import NavigationBTN from "@/presentation/components/layout/NavigationBTN";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useMatrixDetails from "@/presentation/hooks/Generate/Exam/Child/useMatrixDetails";
import { transformMatrixDetailsToUI } from "@/presentation/utils/transformMatrixDetailsToUI";

export default function MatrixDetails() {
  const {
    isLoading,
    changesChapters,
    curChapter,
    handleChangeChapter,
    handleBackClick,
    handleContinueClick,
  } = useMatrixDetails();

  const { user, isLoadingUser } = useAuth();

  const chaptersCount = changesChapters.length;

  return (
    <>
      {isLoadingUser || isLoading ? (
        <Loader />
      ) : (
        <div className="pt-20 flex justify-center">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="ml-60 w-6xl">
            <h1 className="text-4xl font-bold text-center text-blue-500">
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

            <section className="mt-10">
              {curChapter.lessons.map((lesson) => {
                const groupedMatrixDetails = transformMatrixDetailsToUI(
                  lesson.matrixDetails,
                );

                return (
                  <article key={lesson.id} className="mt-10">
                    {lesson.matrixDetails.length > 0 && (
                      <>
                        <h2 className="text-2xl font-semibold text-center text-blue-500">
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

            <div className="my-10 w-full flex justify-end">
              <NavigationBTN
                enableToContinue={true}
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
