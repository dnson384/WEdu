import { useRef, useEffect } from "react";

interface ExamChapterSelectProps {
  filteredChapters: { id: string; name: string }[];
  search: string;
  curValue: string;
  index: number;
  isOpen: boolean;
  onSearchChange: (value: string) => void;
  onSelect: (id: string, name: string, index: number) => void;
  onOpen: () => void;
  onClose: () => void;
}

export default function ExamChapterSelect({
  filteredChapters,
  search,
  curValue,
  index,
  isOpen,
  onSearchChange,
  onSelect,
  onOpen,
  onClose,
}: ExamChapterSelectProps) {
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        onClose();
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [onClose]);

  return (
    <div className="flex items-center justify-center gap-4">
      <div className="relative" ref={dropdownRef}>
        <input
          type="text"
          className="w-xl px-3 py-2 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
          placeholder="Tên chương / chủ đề"
          value={curValue || search}
          onChange={(e) => onSearchChange(e.target.value)}
          onClick={onOpen}
        />

        {isOpen && (
          <ul className="absolute z-10 w-full mt-1 border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
            {filteredChapters.length > 0 ? (
              filteredChapters.map(({ id, name }) => (
                <li
                  key={id}
                  onClick={() => onSelect(id, name, index)}
                  className="p-3 bg-white text-gray-700 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors"
                >
                  {name}
                </li>
              ))
            ) : (
              <li className="p-3 text-gray-500 italic text-center">
                Không tìm thấy chương
              </li>
            )}
          </ul>
        )}
      </div>
    </div>
  );
}
