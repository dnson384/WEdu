import { icons } from "@/presentation/common/icons";

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
  return (
    <div
      role="button"
      onClick={() => handleCardClick(examId)}
      className="bg-white border border-gray-200/80 shadow-2xs rounded-xl p-4 md:px-6 select-none cursor-pointer hover:bg-green-100/50 hover:scale-101 hover:shadow-lg transition-all duration-300 ease-out"
    >
      <div className="flex items-center justify-between gap-4">
        {/* Left */}
        <div className="flex items-center gap-3.5 min-w-0">
          <div className="bg-green-50 text-green-500 w-11 h-11 shrink-0 flex items-center justify-center rounded-xl">
            {icons.docCheck}
          </div>

          <div className="flex flex-col sm:flex-row sm:items-center gap-1 sm:gap-4 min-w-0">
            <p className="text-base truncate">
              {name || "Đề kiểm tra không tên"}
            </p>
            <div className="hidden sm:block h-4 w-px bg-gray-200"></div>
            <div className="flex items-center gap-1.5 text-xs text-gray-500 shrink-0">
              <span className="p-0.5 border border-gray-400 rounded-full">
                {icons.question10}
              </span>
              <span>{questionsCount} câu hỏi</span>
            </div>
          </div>
        </div>

        {/* Right */}
        <div className="shrink-0 bg-green-50 text-green-500 font-semibold text-xs px-3 py-1.5 rounded-full border border-green-200/60">
          Hoàn thiện
        </div>
      </div>
    </div>
  );
}
