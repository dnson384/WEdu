interface Props {
  lessonId: string;
  level: string;
  levelData: Record<string, Record<string, number>>;
}

export default function MatrixDetailsBlock({
  lessonId,
  level,
  levelData,
}: Props) {
  return (
    <div>
      <h3 className="text-2xl font-semibold text-blue-600">{level}</h3>

      <div className="mt-4">
        {Object.entries(levelData).map(([outcome, questionType]) => {
          return (
            <div key={outcome} className="mt-3">
              <h2 className="text-lg font-semibold text-blue-600">{outcome}</h2>

              <div
                className="grid space-x-5"
                style={{
                  gridTemplateColumns: `repeat(${Object.keys(questionType).length}, minmax(0, 1fr))`,
                }}
              >
                {Object.entries(questionType).map(([type, count]) => {
                  return (
                    <div key={type} className="mt-2 flex flex-col">
                      <label
                        htmlFor={`${lessonId}-${level}-${outcome}-${type}`}
                        className="text-blue-500 font-semibold"
                      >
                        {type}
                      </label>
                      <input
                        type="number"
                        id={`${lessonId}-${questionType}-${level}`}
                        value={count === 0 ? "" : count}
                        placeholder="0"
                        min="0"
                        disabled
                        className="border border-gray-300 px-3 py-1 rounded-md"
                      />
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
