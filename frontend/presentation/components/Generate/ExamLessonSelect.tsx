"use client";

import { LessonData } from "@/domain/entities/chapter.entity";
import { useEffect, useRef } from "react";

interface ChapterSelectProps {
  filteredLessons: LessonData[];
  searchLesson: string;
  isOpen: boolean;
  onSearchChange: (value: string) => void;
  onSelect: (id: string, name: string) => void;
  onOpen: () => void;
  onClose: () => void;
}

export default function ExamLessonSelect({
  filteredLessons,
  searchLesson,
  isOpen,
  onSearchChange,
  onSelect,
  onOpen,
  onClose,
}: ChapterSelectProps) {
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
          className="w-full px-4 py-2.5 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600 transition-shadow"
          placeholder="Nhập tên nội dung / đơn vị kiến thức để tìm kiếm..."
          value={searchLesson}
          onChange={(e) => onSearchChange(e.target.value)}
          onClick={onOpen}
        />

        {isOpen && (
          <ul className="absolute z-10 w-full mt-1 border border-gray-200 rounded-lg shadow-xl bg-white max-h-60 overflow-y-auto">
            {filteredLessons.length > 0 ? (
              filteredLessons.map((lesson, index) => (
                <li
                  key={index}
                  onClick={() => onSelect(lesson.id, lesson.name)}
                  className="p-3 bg-white text-gray-700 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors border-b last:border-b-0 border-gray-100"
                >
                  {lesson.name}
                </li>
              ))
            ) : (
              <li className="p-3 text-gray-500 italic text-center">
                Không tìm thấy nội dung
              </li>
            )}
          </ul>
        )}
      </div>
    </div>
  );
}
