"use client";
import usePractice from "@/presentation/hooks/Practice/usePractice";
import QuestionContent from "@/presentation/components/Exam/QuestionContent";
import MultipleChoiceContent from "@/presentation/components/Exam/MultipleChoiceContent";
import TrueFalseContent from "@/presentation/components/Exam/TrueFalseContent";
import NavBar from "@/presentation/components/layout/NavBar";

export default function Practice() {
  const { practice, questionsSorted, handleExportDocx } = usePractice();
  let questionOrder = 0;

  return (
    <>
      <NavBar />
      {practice && questionsSorted ? (
        <>
          <div className="relative px-4 mt-26 mx-auto w-6xl bg-white ">
            <div className="absolute right-4 flex justify-end print:hidden">
              <button
                onClick={handleExportDocx}
                className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded cursor-pointer print:hidden shadow-md"
              >
                Xuất file Word
              </button>
            </div>

            <div className="w-4xl">
              <h1 className="font-bold text-3xl text-center my-15">
                {practice.title}
              </h1>

              {Object.entries(questionsSorted).map(
                ([lesson, questions], index) => {
                  return (
                    <main key={lesson} className="mb-10">
                      <h2 className="font-bold text-xl mb-4">
                        Phần {index + 1}. {lesson}
                      </h2>
                      <div>
                        {questions.map((question) => {
                          questionOrder += 1;
                          const template = question.question.template;
                          const variables = question.question.variables;
                          const type = question.questionType;
                          return (
                            <div key={question.id} className="mb-4">
                              {/* Câu hỏi */}
                              <QuestionContent
                                order={questionOrder}
                                template={template}
                                variables={variables}
                              />

                              {/* Phương án trả lời */}
                              {type === "Trắc nghiệm" && (
                                <div className="ml-5">
                                  {question.options.map((option, index) => {
                                    return (
                                      <div key={index} className="mb-2">
                                        <span>
                                          {String.fromCharCode(65 + index)}
                                          .{" "}
                                        </span>
                                        <MultipleChoiceContent
                                          template={option.template}
                                          variables={option.variables}
                                        />
                                      </div>
                                    );
                                  })}
                                </div>
                              )}
                              {type === "Đúng sai" && (
                                <div className="ml-5">
                                  {question.options.map((option, index) => {
                                    return (
                                      <div
                                        key={index}
                                        className="flex items-baseline justify-between mb-2"
                                      >
                                        <div>
                                          <span>
                                            {String.fromCharCode(97 + index)}
                                            .{" "}
                                          </span>
                                          <TrueFalseContent
                                            template={option.template}
                                            variables={option.variables}
                                          />
                                        </div>
                                        <div className="flex border rounded-md">
                                          <p className="font-semibold px-2 text-center border-r">
                                            Đ
                                          </p>
                                          <div className="w-10"></div>
                                          <p className="font-semibold px-2 text-center border-l">
                                            S
                                          </p>
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </main>
                  );
                },
              )}
            </div>
          </div>
        </>
      ) : (
        <></>
      )}
    </>
  );
}
