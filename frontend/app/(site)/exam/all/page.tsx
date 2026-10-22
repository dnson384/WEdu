"use client";
import NavBar from "@/presentation/components/layout/Navbar";
import ExamCard from "@/presentation/components/Exam/ExamCard";
import FilterBar from "@/presentation/components/Exam/Filter/FilterBar";

import useAllExam from "@/presentation/hooks/Exam/useAllExam";
import useChapter from "@/presentation/hooks/Generate/useChapter";
import useExamFilter from "@/presentation/hooks/Exam/useExamFilter";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import Loader from "@/presentation/components/layout/Loader";
import Link from "next/link";
import { icons } from "@/presentation/common/icons";

export default function AllExam() {
  const { user, isLoadingUser } = useAuth();
  const { exams, isLoadingExams, handleCardClick } = useAllExam();
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
    examFiltered,
    handleFilterClick,
  } = useExamFilter({ chapters, exams });

  const isGlobalLoading = isLoadingUser || isLoadingExams || isLoadingChapters;

  return (
    <>
      {isGlobalLoading ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-blue-50/50">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="lg:ml-60 px-5 sm:px-10 md:px-15 lg:px-20 py-15">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">
                  Danh sách đề thi
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                  Quản lý và ôn luyện các bộ đề thi đã tạo
                </p>
              </div>

              {exams.length > 0 && (
                <Link
                  href="/generate"
                  className="inline-flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold px-4 py-2.5 rounded-lg shadow-sm transition active:scale-95 w-fit"
                >
                  {icons.plus}
                  Tạo đề mới
                </Link>
              )}
            </div>

            {exams.length > 0 ? (
              <div className="space-y-6">
                <section id="filter">
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

                <div className="flex items-center justify-between text-sm text-gray-500 px-1">
                  <span>
                    Hiển thị{" "}
                    <strong className="text-gray-900 font-semibold">
                      {examFiltered.length}
                    </strong>{" "}
                    trên tổng số {exams.length} đề thi
                  </span>
                </div>

                <section id="exams" className="flex flex-col gap-5">
                  {examFiltered.length > 0 ? (
                    <div className="flex flex-col gap-4">
                      {examFiltered.map((exam) => (
                        <ExamCard
                          key={exam.id}
                          examId={exam.id}
                          name={exam.name}
                          questionsCount={exam.questionsCount}
                          handleCardClick={handleCardClick}
                        />
                      ))}
                    </div>
                  ) : (
                    /* Trạng thái lọc không có kết quả */
                    <div className="bg-white border border-dashed border-gray-300 rounded-xl p-10 text-center flex flex-col items-center justify-center">
                      <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center text-gray-400 mb-3">
                        <svg
                          className="w-6 h-6"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                          />
                        </svg>
                      </div>
                      <h3 className="text-base font-semibold text-gray-900">
                        Không tìm thấy đề thi phù hợp
                      </h3>
                      <p className="text-sm text-gray-500 mt-1 max-w-sm">
                        Không có đề nào khớp với các chương hoặc bài học bạn đã
                        chọn. Hãy thử bỏ bớt bộ lọc.
                      </p>
                    </div>
                  )}
                </section>
              </div>
            ) : (
              <div className="bg-white border border-gray-200 rounded-2xl p-12 text-center flex flex-col items-center justify-center shadow-sm">
                <div className="w-16 h-16 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4">
                  {icons.doc}
                </div>
                <h2 className="text-xl font-bold text-gray-900">
                  Chưa có đề kiểm tra nào
                </h2>
                <p className="text-sm text-gray-500 mt-1 mb-6 max-w-md">
                  Bạn hiện chưa tạo bất kỳ đề thi nào. Bắt đầu tạo ngay bộ đề
                  đầu tiên để bắt đầu quá trình luyện thi.
                </p>
                <Link
                  href="/generate"
                  className="bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm px-6 py-2.5 rounded-lg shadow-sm transition active:scale-95"
                >
                  Tạo đề kiểm tra ngay
                </Link>
              </div>
            )}
          </main>
        </div>
      )}
    </>
  );
}
