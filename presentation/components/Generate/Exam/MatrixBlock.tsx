interface Props {
  lessonId: string;
  questionType: string;
  questionTypeData: Record<string, number>;
  handleMatrixInputChange: (
    lessonId: string,
    questionType: string,
    difficultyLevel: string,
    newCount: number,
  ) => void;
}

export default function MatrixBlock({
  lessonId,
  questionType,
  questionTypeData,
  handleMatrixInputChange,
}: Props) {
  return (
    <div>
      <h3 className="text-xl font-semibold text-blue-600">{questionType}</h3>

      <div
        className="grid space-x-5"
        style={{
          gridTemplateColumns: `repeat(${Object.keys(questionTypeData).length}, minmax(0, 1fr))`,
        }}
      >
        {Object.entries(questionTypeData).map(([level, count]) => {
          return (
            <div key={level} className="mt-2 flex flex-col">
              <label
                htmlFor={`${lessonId}-${questionType}-${level}`}
                className="text-blue-500 font-semibold"
              >
                {level}
              </label>
              <input
                type="number"
                id={`${lessonId}-${questionType}-${level}`}
                value={count === 0 ? "" : count}
                placeholder="0"
                min="0"
                disabled
                onFocus={(e) => e.target.select()}
                onChange={(e) => {
                  const val = e.target.value;
                  const newValue = val === "" ? 0 : parseInt(val, 10);
                  handleMatrixInputChange(
                    lessonId,
                    questionType,
                    level,
                    newValue,
                  );
                }}
                className="border border-gray-300 px-3 py-1 rounded-md"
              />
            </div>
          );
        })}
      </div>
    </div>
  );
}
