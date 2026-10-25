"use client";

import { Download, FileText, Trash2 } from "lucide-react";
import type { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

interface Props {
  file: QuestionFileEntity & {
    sizeLabel: string;
    lastModifiedLabel: string;
  };
  isDeleting: boolean;
  onDelete: (file: QuestionFileEntity) => void;
}

export default function QuestionFileRow({ file, isDeleting, onDelete }: Props) {
  return (
    <tr className="hover:bg-blue-50/40">
      <td className="px-6 py-5">
        <div className="flex items-center gap-3 min-w-64">
          <FileText size={22} className="shrink-0 text-blue-500" />
          <span className="font-medium text-gray-900 break-all">
            {file.fileName}
          </span>
        </div>
      </td>
      <td className="px-4 py-5 text-gray-600 whitespace-nowrap">
        {file.sizeLabel}
      </td>
      <td className="px-4 py-5 text-gray-600 whitespace-nowrap">
        {file.lastModifiedLabel}
      </td>
      <td className="px-6 py-5 text-right whitespace-nowrap">
        <a
          href={file.presignedUrl}
          target="_blank"
          rel="noopener noreferrer"
          aria-label={`Tải xuống ${file.fileName}`}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-50 px-3 py-2 font-medium text-blue-600 hover:bg-blue-100"
        >
          <Download size={16} />
          Tải xuống
        </a>
        <button
          type="button"
          disabled={isDeleting}
          aria-label={`Xóa ${file.fileName}`}
          onClick={() => onDelete(file)}
          className="ml-2 inline-flex items-center gap-2 rounded-lg bg-red-50 px-3 py-2 font-medium text-red-600 hover:bg-red-100 disabled:opacity-50"
        >
          <Trash2 size={16} />
          Xóa
        </button>
      </td>
    </tr>
  );
}
