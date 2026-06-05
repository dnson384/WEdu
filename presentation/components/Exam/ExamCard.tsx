interface Data {
  examId: string;
  name: string;
  questionsCount: number;
  handleCardClick: (examId: string) => void;
}

export default function ExamCard({
  examId,
  name,
  questionsCount,
  handleCardClick,
}: Data) {
  const icons = {
    doc: (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="48"
        height="48"
        viewBox="-4 -2 24 24"
      >
        <path d="M-4 -2h24v24H-4z" fill="none" />
        <path
          fill="#1447e6"
          d="M3 0h10a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H3a3 3 0 0 1-3-3V3a3 3 0 0 1 3-3m0 2a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1zm2 1h6a1 1 0 0 1 0 2H5a1 1 0 1 1 0-2m0 12h2a1 1 0 0 1 0 2H5a1 1 0 0 1 0-2m0-4h6a1 1 0 0 1 0 2H5a1 1 0 0 1 0-2m0-4h6a1 1 0 0 1 0 2H5a1 1 0 1 1 0-2"
        />
      </svg>
    ),
    question: (
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="10"
        height="10"
        viewBox="0 0 24 24"
      >
        <path d="M0 0h24v24H0z" fill="none" />
        <path
          fill="none"
          stroke="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.5"
          d="M12 21v-.5m0-3c0-5.1 5-3.825 5-8.924c0-6.768-10-6.768-10 0"
        />
      </svg>
    ),
  };

  return (
    <div
      className="border border-gray-100 shadow-sm rounded-xl py-5 px-8 select-none hover:bg-blue-100 hover:scale-101 hover:shadow-lg transition-all duration-300 ease-out"
      onClick={() => handleCardClick(examId)}
    >
      <div className="flex items-center gap-3">
        <div>{icons.doc}</div>
        <div>
          <p className="text-xl mb-1">{name}</p>
          <div className="flex items-center gap-2">
            <div className="p-0.5 border border-gray-500 w-fit rounded-full">
              {icons.question}
            </div>
            <p className="text-gray-500">{questionsCount} câu hỏi</p>
          </div>
        </div>
      </div>
    </div>
  );
}
