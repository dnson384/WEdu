"use client";
import { motion, AnimatePresence } from "framer-motion";

import QuestionBlock from "@/presentation/components/Exam/QuestionBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import Error from "@/presentation/components/layout/Error";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import useExam from "@/presentation/hooks/Exam/useExam";
import Loader from "@/presentation/components/layout/Loader";
import Noti from "@/presentation/components/layout/Noti";
import { transformExamResToUI } from "@/presentation/utils/transformExamResToUI";

export default function Exam() {
  const { user, isLoadingUser } = useAuth();
  const {
    examDetail,
    isLoadingExam,
    errorsList,
    noti,
    handleExportExam,
    handleExportMatrix,
    handleDeleteClick,
  } = useExam();

  const groupedQuestions = transformExamResToUI(examDetail);

  const isGlobalLoading = isLoadingUser || isLoadingExam;

  return (
    <>
      {isGlobalLoading ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-blue-50/50">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          {/* Toast Thông báo & Lỗi dạng nổi ở trên cùng */}
          <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 flex flex-col items-center gap-2 pointer-events-none w-full max-w-lg px-4">
            <AnimatePresence>
              {noti && (
                <motion.div
                  initial={{ opacity: 0, y: -15, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -15, scale: 0.95 }}
                  transition={{ duration: 0.25 }}
                  className="pointer-events-auto w-full shadow-lg"
                >
                  <Noti message={noti} />
                </motion.div>
              )}

              {errorsList.length > 0 &&
                errorsList.map((error, index) => (
                  <motion.div
                    key={index}
                    initial={{ opacity: 0, y: -15, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: -15, scale: 0.95 }}
                    transition={{ duration: 0.25, delay: index * 0.04 }}
                    className="pointer-events-auto w-full shadow-lg"
                  >
                    <Error message={error} />
                  </motion.div>
                ))}
            </AnimatePresence>
          </div>

          <main className="lg:ml-60 py-15 px-5 sm:px-10 md:px-15 lg:px-20">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
              {/* Cột trái: Nội dung Đề thi (Chiếm 8-9 phần) */}
              <div className="lg:col-span-8 xl:col-span-9 bg-white border border-gray-200/80 rounded-2xl p-6 sm:p-8 shadow-sm">
                <header className="border-b border-gray-100 pb-5 mb-6">
                  <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 leading-snug">
                    {examDetail?.name || "Chi tiết đề thi"}
                  </h1>
                </header>

                <div className="space-y-8">
                  {Object.entries(groupedQuestions).map(
                    ([questionType, questions], index) => (
                      <QuestionBlock
                        key={questionType}
                        questionType={questionType}
                        questions={questions}
                        index={index}
                      />
                    ),
                  )}
                </div>
              </div>

              {/* Cột phải: Thanh công cụ dính khi cuộn (Sticky Sidebar) */}
              <aside className="lg:col-span-4 xl:col-span-3 sticky top-20 print:hidden">
                <div className="bg-white border border-gray-200/80 rounded-2xl p-5 shadow-sm space-y-4">
                  <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">
                    Thao tác đề thi
                  </h3>

                  <div className="flex flex-col gap-2.5">
                    {/* Xuất đề thi */}
                    <button
                      type="button"
                      onClick={handleExportExam}
                      className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2.5 px-4 rounded-xl shadow-sm transition active:scale-98 cursor-pointer text-sm"
                    >
                      <svg
                        className="w-4 h-4 shrink-0"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                        />
                      </svg>
                      Xuất đề thi (.docx)
                    </button>

                    {/* Xuất ma trận */}
                    <button
                      type="button"
                      onClick={handleExportMatrix}
                      className="w-full flex items-center justify-center gap-2 bg-white border border-blue-600 text-blue-600 hover:bg-blue-50/70 font-medium py-2.5 px-4 rounded-xl shadow-sm transition active:scale-98 cursor-pointer text-sm"
                    >
                      <svg
                        className="w-4 h-4 shrink-0"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                        />
                      </svg>
                      Xuất ma trận đề
                    </button>

                    <hr className="border-gray-100 my-1" />

                    {/* Xóa đề thi */}
                    <button
                      type="button"
                      onClick={handleDeleteClick}
                      className="w-full flex items-center justify-center gap-2 border border-red-200 text-red-600 hover:bg-red-50/80 font-medium py-2.5 px-4 rounded-xl transition active:scale-98 cursor-pointer text-sm"
                    >
                      <svg
                        className="w-4 h-4 shrink-0"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                      Xóa đề kiểm tra
                    </button>
                  </div>
                </div>
              </aside>
            </div>
          </main>
        </div>
      )}
    </>
  );
}
