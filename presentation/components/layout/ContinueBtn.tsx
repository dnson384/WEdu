interface Props {
  handleContinueClick: () => Promise<void>;
  content?: string;
}
export default function ContinueBtn({ handleContinueClick, content }: Props) {
  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 z-50">
      <div className="max-w-6xl mx-auto flex justify-end">
        <button
          type="button"
          onClick={handleContinueClick}
          className="bg-blue-600 hover:bg-blue-700 text-white px-8 py-2 rounded-lg font-medium shadow-md transition-colors cursor-pointer"
        >
          {content ? content : "Tiếp tục"}
        </button>
      </div>
    </div>
  );
}
