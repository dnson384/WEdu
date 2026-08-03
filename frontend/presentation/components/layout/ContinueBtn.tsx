interface Props {
  handleContinueClick: () => Promise<void>;
  handleBackClick: () => void;
}
export default function ContinueBtn({ handleContinueClick, handleBackClick }: Props) {
  return (
    <div className="w-fit py-4 z-50 flex items-center gap-3">
        <button
          id="back"
          type="button"
          onClick={handleBackClick}
          className="border border-blue-500 hover:bg-blue-100 text-blue-500 px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
        >
          Quay lại
        </button>

        <button
          id="forward"
          type="button"
          onClick={handleContinueClick}
          className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
        >
          Tiếp tục
        </button>
      </div>
  );
}
