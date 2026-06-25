"use client";

import QuestionBlock from "@/presentation/components/Exam/QuestionBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useExam from "@/presentation/hooks/Exam/useExam";
import { transformExamResToUI } from "@/presentation/utils/transformExamResToUI";

export default function Exam() {
  const { details, isLoading, handleExportDocx } = useExam();
  const { user, isLoadingUser } = useAuth();

  const groupedQuestions = transformExamResToUI(details);

  return (
    <>
      {isLoadingUser ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          {" "}
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          {isLoading ? (
            <div className="h-screen mx-auto px-4 flex justify-center items-center">
              <div className="loader"></div>
            </div>
          ) : (
            <main className="w-6xl mx-auto">
              <div className="relative px-4 mt-26 mx-auto w-6xl bg-white ">
                <div className="absolute right-4 flex justify-end print:hidden">
                  <button
                    onClick={() => handleExportDocx(groupedQuestions)}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded cursor-pointer print:hidden shadow-md"
                  >
                    Xuất file Word
                  </button>
                </div>
              </div>

              <div className="w-4xl px-4 mb-20">
                <h1 className="text-3xl font-bold mb-5">{details.name}</h1>

                {Object.entries(groupedQuestions).map(
                  ([questionType, questions], index) => {
                    return (
                      <QuestionBlock
                        key={questionType}
                        questionType={questionType}
                        questions={questions}
                        index={index}
                      />
                    );
                  },
                )}
              </div>
            </main>
          )}
        </>
      )}
    </>
  );
}
