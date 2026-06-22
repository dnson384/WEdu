"use client";

import Link from "next/link";

interface Data {
  avatarUrl: string;
}

export default function NavBar({ avatarUrl }: Data) {
  const navLinks = [
    {
      name: "Danh sách đề",
      href: "/exam/all",
    },
    {
      name: "Tạo đề",
      href: "/generate/exam",
    },
    {
      name: "Tải lên câu hỏi",
      href: "/upload",
    },
  ];

  return (
    <nav className="fixed z-10 bg-white top-0 w-full shadow-md">
      <div className="max-w-6xl mx-auto p-4">
        <div className="flex items-center justify-between">
          <div className="shrink-0 hover:text-blue-600 transition-colors">
            <Link href="/" className="font-bold text-xl">
              Generate Exam
            </Link>
          </div>

          <div className="flex items-center gap-4">
            {navLinks.map((link) => {
              if (link.href) {
                return (
                  <Link
                    key={link.name}
                    href={link.href}
                    className={`px-3 py-2 rounded-md text-sm font-semibold transition-colors hover:bg-blue-600 hover:text-white`}
                  >
                    {link.name}
                  </Link>
                );
              }
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}
