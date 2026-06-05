"use client";

import NavBar from "@/presentation/components/layout/NavBar";
import useAllPractices from "@/presentation/hooks/Practice/useAllPractices";
import Link from "next/link";

export default function AllPractices() {
  const { practices, isLoading, errorMessage } = useAllPractices();
  return (
    <>
      <NavBar />

      {/* Loading */}
      {isLoading && (
        <div className="flex justify-center items-center h-150 mt-26 mx-auto">
          <div className="loader"></div>
        </div>
      )}

      {/* Chưa có đề ôn tập */}
      {errorMessage && (
        <div className="mt-26 flex flex-col items-center justify-center rounded-lg h-150">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="64"
            height="64"
            viewBox="0 0 24 24"
          >
            <g
              fill="none"
              stroke="#155dfc"
              strokeLinejoin="round"
              strokeWidth="1"
            >
              <path
                strokeLinecap="round"
                d="M7 21a2 2 0 0 1-2-2V3h9l5 5v11a2 2 0 0 1-2 2zm5-8v4m-2-2h4"
              />
              <path d="M13 3v6h6" />
            </g>
          </svg>
          <p className="mt-2 text-lg text-gray-600 font-medium">
            {errorMessage}
          </p>
          <Link
            href={"/generate/practice"}
            className="mt-5 bg-blue-600 text-white px-5 py-3 rounded-lg hover:bg-blue-700 "
          >
            Tạo đề ôn tập
          </Link>
        </div>
      )}
    </>
  );
}
