"use client";
import NavBar from "@/presentation/components/layout/Navbar";
import ExamCard from "@/presentation/components/Exam/ExamCard";
import FilterBar from "@/presentation/components/Exam/Filter/FilterBar";

import useAllExam from "@/presentation/hooks/Exam/useAllExam";
import useChapter from "@/presentation/hooks/Generate/useChapter";
import useExamFilter from "@/presentation/hooks/Exam/useExamFilter";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";

export default function AllExam() {
  const { user, isLoadingUser } = useAuth();

  const { exams, isLoading, handleCardClick } = useAllExam();
  const { chapters, isLoadingChapters } = useChapter();
  const {
    selectedChapterIds,
    selectedLessonIds,
    chapterSearch,
    lessonSearch,
    setChapterSearch,
    setLessonSearch,
    filteredChapters,
    filteredLessons,
    toggleChapter,
    toggleLesson,
    totalChaptersSelected,
    totalLessonsSelected,
    // Filtered
    examFiltered,
    handleFilterClick,
  } = useExamFilter({ chapters, exams });

  return (
    <>
      {isLoadingUser ? (
        <div className="h-screen mx-auto my-15 px-20 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          {isLoading ? (
            <div className="h-screen mx-auto my-15 px-20 flex justify-center items-center">
              <div className="loader"></div>
            </div>
          ) : (
            <main className="lg:ml-60 my-15 px-20 mx-auto">
              <h3 className="text-4xl font-bold text-center text-blue-500 mb-5">
                Danh sách đề
              </h3>

              <section id="filter" className="mb-5">
                <FilterBar
                  selectedChapterIds={selectedChapterIds}
                  selectedLessonIds={selectedLessonIds}
                  chapterSearch={chapterSearch}
                  lessonSearch={lessonSearch}
                  setChapterSearch={setChapterSearch}
                  setLessonSearch={setLessonSearch}
                  filteredChapters={filteredChapters}
                  filteredLessons={filteredLessons}
                  toggleChapter={toggleChapter}
                  toggleLesson={toggleLesson}
                  totalChaptersSelected={totalChaptersSelected}
                  totalLessonsSelected={totalLessonsSelected}
                  handleFilterClick={handleFilterClick}
                />
              </section>

              <section id="exams" className="flex flex-col gap-5">
                {examFiltered.map((exam) => {
                  return (
                    <ExamCard
                      key={exam.id}
                      examId={exam.id}
                      name={exam.name}
                      questionsCount={exam.questionsCount}
                      handleCardClick={handleCardClick}
                    />
                  );
                })}
              </section>
            </main>
          )}
        </>
      )}
    </>
  );
}
