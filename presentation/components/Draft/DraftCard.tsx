import { icons } from "@/presentation/common/icons";

interface Data {
  draftId: string;
  name: string;
  questionsCount: number;
  handleCardClick: (draftId: string) => void;
}

export default function DraftCard({
  draftId,
  name,
  questionsCount,
  handleCardClick,
}: Data) {

  return (
    <div
      className="border border-gray-100 shadow-sm rounded-xl py-5 px-8 select-none hover:bg-blue-100/50 hover:scale-101 hover:shadow-lg transition-all duration-300 ease-out"
      onClick={() => handleCardClick(draftId)}
    >
      <div className="flex items-center justify-between">
        {/* Left */}
        <div className="flex items-center gap-3">
          <div className="bg-orange-500/10 w-15 h-15 flex items-center justify-center rounded-xl">
            <div className="text-orange-500">{icons.docEdit}</div>
          </div>
          <div className="flex items-center gap-5">
            <p className="text-xl">{name}</p>
            <div className="border border-gray-200 lg:h-10"></div>
            <div className="flex items-center gap-2">
              <div className="p-0.5 border border-gray-500 w-fit rounded-full">
                {icons.question10}
              </div>
              <p className="text-gray-500">{questionsCount} câu hỏi</p>
            </div>
          </div>
        </div>

        {/* Right */}
        <div className="w-fit bg-orange-500/15 px-4 py-2 rounded-lg">
          <p className="text-orange-500 font-bold text-sm">Nháp</p>
        </div>
      </div>
    </div>
  );
}
