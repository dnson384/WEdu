import { QuestionDetail } from "@/domain/entities/exam.entity";
import QuestionContent from "./QuestionContent";
import MultipleChoiceContent from "./MultipleChoiceContent";
import TrueFalseContent from "./TrueFalseContent";
import EssayContent from "./EssayContent";

interface Props {
  questionType: string;
  questions: QuestionDetail[];
  index: number;
}
export default function QuestionBlock({
  questionType,
  questions,
  index,
}: Props) {
  return (
    <section>
      <h3 className="font-semibold text-lg mb-4">
        Phần {index + 1}. {questionType}
      </h3>

      <article>
        {questions.map((question, index) => {
          const template = question.question.template;
          const variables = question.question.variables;
          return (
            <div key={question.id} className="mb-4">
              <QuestionContent
                order={index + 1}
                template={template}
                variables={variables}
              />
              {questionType === "Nhiều lựa chọn" && (
                <div className="ml-5">
                  {question.options.map((option, index) => {
                    return (
                      <div key={index} className="mb-2">
                        <span>{String.fromCharCode(65 + index)}. </span>
                        <MultipleChoiceContent
                          template={option.template}
                          variables={option.variables}
                        />
                      </div>
                    );
                  })}
                </div>
              )}
              {questionType === "Đúng sai" && (
                <div className="ml-5">
                  {question.options.map((option, index) => {
                    return (
                      <div
                        key={index}
                        className="flex items-baseline justify-between gap-1 mb-2"
                      >
                        <div>
                          <span>{String.fromCharCode(97 + index)}. </span>
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
              {questionType === "Tự luận" && (
                <div className="ml-5 flex flex-col gap-3">
                  {question.options.map((option, index) => {
                    return (
                      <div className="flex gap-1" key={index}>
                        <span>{String.fromCharCode(97 + index)}. </span>
                        <EssayContent
                          template={option.template}
                          variables={option.variables}
                        />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </article>
    </section>
  );
}
