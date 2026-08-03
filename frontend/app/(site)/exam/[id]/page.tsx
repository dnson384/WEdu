"use client";
import { motion, AnimatePresence } from "framer-motion";

import QuestionBlock from "@/presentation/components/Exam/QuestionBlock";
import NavBar from "@/presentation/components/layout/Navbar";
import Error from "@/presentation/components/layout/Error";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useExam from "@/presentation/hooks/Exam/useExam";
import { transformExamResToUI } from "@/presentation/utils/transformExamResToUI";
import Loader from "@/presentation/components/layout/Loader";

export default function Exam() {
  const {
    details,
    isLoading,
    errorsList,
    handleExportDocx,
    handleDeleteClick,
  } = useExam();
  const { user, isLoadingUser } = useAuth();

  const groupedQuestions = transformExamResToUI(details);

  return (
    <>
      {isLoadingUser || isLoading ? (
        <Loader />
      ) : (
        <div className="bg-blue-50/10">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="ml-60 flex justify-center">
            <div className="w-6xl mt-15 relative ">
              <AnimatePresence>
                {errorsList.length > 0 && (
                  <motion.div
                    initial={{ opacity: 0, y: -20, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: -20, scale: 0.95 }}
                    transition={{ duration: 0.5, ease: "anticipate" }}
                    className="absolute w-6xl z-20"
                  >
                    <div className="flex flex-col items-center justify-center gap-2">
                      {errorsList.map((error, index) => (
                        <motion.div
                          key={index}
                          initial={{ opacity: 0, x: 0 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: index * 0.05 }}
                          className="w-xl"
                        >
                          <Error message={error} />
                        </motion.div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

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

              <div className="absolute top-0 right-0 flex justify-end print:hidden">
                <div className="flex flex-col gap-2">
                  <button
                    onClick={() => handleExportDocx()}
                    className="border border-blue-500 bg-blue-500 hover:border-blue-700 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded-lg cursor-pointer print:hidden shadow-md"
                  >
                    Xuất file Word
                  </button>
                  <button
                    onClick={handleDeleteClick}
                    className="border border-red-500 hover:bg-red-100 text-red-500 py-2 px-4 rounded-lg cursor-pointer shadow-md"
                  >
                    Xóa đề kiểm tra
                  </button>
                </div>
              </div>
            </div>
          </main>
        </div>
      )}
    </>
  );
}
