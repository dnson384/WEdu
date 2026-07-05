"use client";

import { LessonData } from "@/domain/entities/category.entity";
import { useEffect, useRef } from "react";

interface ChapterSelectProps {
  lessons: LessonData[];
  searchLesson: string;
  isOpen: boolean;
  onSearchChange: (value: string) => void;
  onSelect: (lesson: LessonData, index: number) => void;
  onOpen: () => void;
  onClose: () => void;
}

export default function LessonSelect({
  lessons,
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
    <div className="flex items-center gap-4">
      <h3 className="w-40 text-lg font-semibold text-blue-600">Bài</h3>

      <div className="relative" ref={dropdownRef}>
        <input
          type="text"
          className="w-xl px-3 py-1 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-600"
          placeholder="Tên bài"
          value={searchLesson}
          onChange={(e) => onSearchChange(e.target.value)}
          onClick={onOpen}
        />

        {isOpen && (
          <ul className="absolute z-5 w-full mt-1 border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
            {lessons.length > 0 ? (
              lessons.map((lesson, index) => (
                <li
                  key={index}
                  onClick={() => onSelect(lesson, index)}
                  className="p-3 bg-white text-gray-700 cursor-pointer hover:bg-blue-50 hover:text-blue-600 transition-colors"
                >
                  {lesson.name}
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
