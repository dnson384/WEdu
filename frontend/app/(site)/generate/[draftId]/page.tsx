"use client";

import Error from "@/presentation/components/layout/Error";
import Loader from "@/presentation/components/layout/Loader";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useAlreadyStructure from "@/presentation/hooks/Generate/Exam/Child/useAlreadyStructure";

export default function GenerateExam() {
  const {
    isLoading,
    examName,
    questionsCount,
    questionTypes,
    errorMessage,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleDeleteClick,
    handleContinueClick,
  } = useAlreadyStructure();

  const { user, isLoadingUser } = useAuth();

  return (
    <>
      {/* Thông báo lỗi */}
      {errorMessage && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <Error message={errorMessage} />
        </div>
      )}

      {isLoadingUser || isLoading ? (
        <Loader />
      ) : (
        <div className="bg-blue-50/10 h-screen pt-20">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="ml-60">
            <section className="bg-white rounded-2xl shadow-lg w-4xl mx-auto py-10 px-15">
              <h1 className="text-4xl font-bold">Cấu trúc đề kiểm tra</h1>

              <article className="mt-10 flex flex-col items-center">
                {/* Tên bài kiểm tra */}
                <label htmlFor="questions_name" className="w-full">
                  <p className="font-semibold text-black/60 mb-1">
                    TÊN BÀI KIỂM TRA
                  </p>
                  <input
                    type="text"
                    id="questions_name"
                    placeholder="Nhập tên bài kiểm tra"
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl shadow-md focus:outline-none focus:ring-2 focus:ring-blue-600"
                    value={examName.trim().length > 0 ? examName : ""}
                    onChange={handleExamNameChange}
                  />
                </label>

                <div className="w-full grid grid-cols-2 gap-x-5 mt-4">
                  {/* Số lượng câu hỏi */}
                  <label htmlFor="questions_count" className="w-full">
                    <p className="font-semibold text-black/60 mb-1">
                      SỐ LƯỢNG CÂU HỎI
                    </p>
                    <input
                      type="text"
                      pattern="[0-9]*"
                      id="questions_count"
                      placeholder="0"
                      className="w-full px-4 py-3 border border-gray-200 rounded-xl shadow-md focus:outline-none focus:ring-2 focus:ring-blue-600"
                      value={questionsCount ? questionsCount : ""}
                      onChange={handleQuestionsCountChange}
                    />
                  </label>

                  {/* Loại câu hỏi */}
                  <div className="">
                    <p className="font-semibold text-black/60 mb-1">
                      LOẠI CÂU HỎI
                    </p>
                    <div className="w-xl">
                      {Object.entries(questionTypes).map(([type, checked]) => (
                        <div
                          key={type}
                          className="w-full flex items-center gap-2"
                        >
                          <input
                            type="checkbox"
                            id={type}
                            checked={checked}
                            className="accent-blue-600 w-4 h-4"
                            onChange={handleQuestionTypesChange}
                          />
                          <label className="w-full" htmlFor={type}>
                            {type}
                          </label>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </article>

              <div className="mt-10 flex justify-end">
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={handleDeleteClick}
                    className="border border-red-500 hover:bg-red-100 text-red-500 px-8 py-2 rounded-lg shadow-md transition-colors cursor-pointer"
                  >
                    Xóa bản nháp
                  </button>

                  <button
                    type="button"
                    onClick={handleContinueClick}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-semibold shadow-md transition-colors cursor-pointer"
                  >
                    Tiếp tục
                  </button>
                </div>{" "}
              </div>
            </section>
          </main>
        </div>
      )}
    </>
  );
}
