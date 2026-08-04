import { useRef, useEffect } from "react";

interface ExamChapterSelectProps {
  filteredChapters: { id: string; name: string }[];
  search: string;
  isOpen: boolean;
  onSearchChange: (value: string) => void;
  onSelect: (id: string, name: string) => void;
  onOpen: () => void;
  onClose: () => void;
}

export default function ExamChapterSelect({
  filteredChapters,
  search,
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
    <div className="w-full flex items-center justify-center gap-4">
      <div className="relative w-full" ref={dropdownRef}>
        <input
          type="text"
          className="w-full px-4 py-2.5 border border-gray-300 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600 transition-shadow"
          placeholder="Nhập tên chương để tìm kiếm..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          onClick={onOpen}
        />

        {isOpen && (
          <ul className="absolute z-10 w-full mt-1 border border-gray-200 rounded-xl shadow-xl bg-white max-h-60 overflow-y-auto">
            {filteredChapters.length > 0 ? (
              filteredChapters.map(({ id, name }) => (
                <li
                  key={id}
                  onClick={() => onSelect(id, name)}
                  className="p-3 bg-white text-gray-700 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors border-b last:border-b-0 border-gray-100"
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
