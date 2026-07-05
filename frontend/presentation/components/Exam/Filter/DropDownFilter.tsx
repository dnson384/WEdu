// src/presentation/components/DropdownFilter.tsx
import React, { useState, useRef, useEffect } from 'react';

interface DropdownFilterProps {
  label: string;
  placeholder: string;
  searchPlaceholder: string;
  selectedCount: number;
  searchValue: string;
  onSearchChange: (value: string) => void;
  items: { id: string; name: string }[];
  selectedIds: string[];
  onToggle: (id: string) => void;
}

export const DropdownFilter: React.FC<DropdownFilterProps> = ({
  label,
  placeholder,
  searchPlaceholder,
  selectedCount,
  searchValue,
  onSearchChange,
  items,
  selectedIds,
  onToggle,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Đóng dropdown khi click ra ngoài vùng hiển thị
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Tiêu đề hiển thị số lượng phần tử đã chọn
  const displayLabel = selectedCount > 0 ? `Đã chọn (${selectedCount})` : placeholder;

  return (
    <div className="flex flex-col gap-1.5 flex-1 relative" ref={dropdownRef}>
      <label className="text-sm font-medium text-gray-500">{label}</label>
      
      {/* Nút bấm hiển thị Dropdown */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center justify-between w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-lg text-left text-sm text-gray-700 hover:bg-gray-100 transition-colors"
      >
        <span className={selectedCount > 0 ? "font-semibold text-blue-600" : ""}>
          {displayLabel}
        </span>
        <svg className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Menu xổ xuống */}
      {isOpen && (
        <div className="absolute top-full left-0 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-64 flex flex-col">
          {/* Ô tìm kiếm nội bộ */}
          <div className="p-2 border-b border-gray-100 sticky top-0 bg-white z-10">
            <div className="relative">
              <input
                type="text"
                value={searchValue}
                onChange={(e) => onSearchChange(e.target.value)}
                placeholder={searchPlaceholder}
                className="w-full pl-8 pr-3 py-1.5 text-sm bg-gray-50 border border-gray-200 rounded-md focus:outline-none focus:border-blue-500"
              />
              <svg className="w-4 h-4 text-gray-400 absolute left-2.5 top-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
          </div>

          {/* Danh sách các dòng kèm Checkbox */}
          <div className="overflow-y-auto flex-1 p-1">
            {items.length === 0 ? (
              <div className="text-xs text-gray-400 text-center py-4">Không tìm thấy kết quả</div>
            ) : (
              items.map((item) => {
                const isChecked = selectedIds.includes(item.id);
                return (
                  <label
                    key={item.id}
                    className="flex items-center gap-3 px-3 py-2 text-sm rounded-md hover:bg-gray-50 cursor-pointer select-none"
                  >
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => onToggle(item.id)}
                      className="w-4 h-4 rounded text-blue-600 border-gray-300 focus:ring-blue-500"
                    />
                    <span className={`truncate ${isChecked ? 'font-medium text-gray-900' : 'text-gray-600'}`}>
                      {item.name}
                    </span>
                  </label>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
};