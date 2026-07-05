import { LessonData } from "@/domain/entities/category.entity";
import { ChangeEvent } from "react";

interface LessonOptionProps {
  currentLesson: LessonData;
  title: string;
  index: number;
  handleLessonOptionSelect(
    event: ChangeEvent<HTMLInputElement>,
    field: string,
    index: number,
  ): void;
}
export default function LessonOptionSelect({
  currentLesson,
  title,
  index,
  handleLessonOptionSelect,
}: LessonOptionProps) {
  let arr: string[] = [];
  if (title === "Dạng bài") arr = currentLesson.exerciseTypes;
  else if (title === "Độ khó") arr = currentLesson.difficultyLevels;
  else if (title === "Yêu cầu cần đạt") arr = currentLesson.learningOutcomes;
  else if (title === "Dạng câu hỏi") arr = currentLesson.questionTypes;

  return (
    <div className="flex gap-4">
      <p className="w-40 font-semibold text-blue-500">{title}</p>
      <div>
        {arr.map((content) => {
          return (
            <label
              key={content}
              className="flex items-start gap-2 w-xl wrap-normal text-justify"
            >
              <input
                type="checkbox"
                className="mt-1 accent-blue-500"
                value={content}
                onChange={(e) => handleLessonOptionSelect(e, title, index)}
              />
              {content}
            </label>
          );
        })}
      </div>
    </div>
  );
}
