"use client";

import NavBar from "@/presentation/components/layout/Navbar";
import useStructure from "@/presentation/hooks/Generate/Exam/Child/useStructure";

export default function GenerateExam() {
  const {
    examName,
    questionsCount,
    questionTypes,
    error,
    handleExamNameChange,
    handleQuestionsCountChange,
    handleQuestionTypesChange,
    handleContinueClick,
  } = useStructure();

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
      <NavBar />

      {/* Thông báo lỗi */}
      {error && (
        <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
          <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
            {icons.error}
            <p className="text-red-500">{error}</p>
          </div>
        </div>
      )}

      <main className="mt-32 w-6xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-center">Cấu trúc đề kiểm tra</h1>

        <section className="mt-10 flex flex-col items-center">
          {/* Tên bài kiểm tra */}
          <label htmlFor="questions_name" className="flex items-center gap-4">
            <p className="w-40 text-lg font-semibold text-blue-600">
              Tên bài kiểm tra
            </p>
            <input
              type="text"
              id="questions_name"
              placeholder="Nhập tên bài kiểm tra"
              className="w-xl px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
              value={examName.trim().length > 0 ? examName : ""}
              onChange={handleExamNameChange}
            />
          </label>

          {/* Số lượng câu hỏi */}
          <label
            htmlFor="questions_count"
            className="mt-4 flex items-center gap-4"
          >
            <p className="w-40 text-lg font-semibold text-blue-600">
              Số lượng câu hỏi
            </p>
            <input
              type="text"
              pattern="[0-9]*"
              id="questions_count"
              placeholder="Nhập số lượng câu hỏi"
              className="w-xl px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
              value={questionsCount ? questionsCount : ""}
              onChange={handleQuestionsCountChange}
            />
          </label>

          {/* Loại câu hỏi */}
          <div className="mt-4 flex gap-4">
            <p className="w-40 text-lg font-semibold text-blue-600">
              Loại câu hỏi
            </p>
            <div className="w-xl">
              {Object.entries(questionTypes).map(([type, checked]) => (
                <div key={type} className="w-full flex gap-2">
                  <input
                    type="checkbox"
                    id={type}
                    checked={checked}
                    className="accent-blue-600"
                    onChange={handleQuestionTypesChange}
                  />
                  <label className="w-full" htmlFor={type}>
                    {type}
                  </label>
                </div>
              ))}
            </div>
          </div>
        </section>

        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-50">
          <div className="max-w-6xl mx-auto flex justify-end">
            <button
              type="button"
              onClick={handleContinueClick}
              className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
            >
              Tiếp tục
            </button>
          </div>
        </div>
      </main>
    </>
  );
}
