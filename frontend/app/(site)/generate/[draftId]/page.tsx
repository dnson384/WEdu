"use client";

import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useAlreadyStructure from "@/presentation/hooks/Generate/Exam/Child/useAlreadyStructure";

export default function GenerateExam() {
  const {
    isLoading,
    examName,
    questionsCount,
    questionTypes,
    error,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleContinueClick,
  } = useAlreadyStructure();

  const { user, isLoadingUser } = useAuth();

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
    <>
      {/* Thông báo lỗi */}
      {error && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
            {icons.error}
            <p className="text-red-500">{error}</p>
          </div>
        </div>
      )}

      {isLoadingUser && isLoading ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <div className="bg-blue-50/50 h-screen pt-20">
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
                <button
                  type="button"
                  onClick={handleContinueClick}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-semibold shadow-md transition-colors cursor-pointer"
                >
                  Tiếp tục
                </button>
              </div>
            </section>
          </main>
        </div>
      )}
    </>
  );
}
