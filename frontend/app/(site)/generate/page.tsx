"use client";

import Link from "next/link";

import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useAllDraft from "@/presentation/hooks/Draft/useAllDraft";
import DraftCard from "@/presentation/components/Draft/DraftCard";
import Loader from "@/presentation/components/layout/Loader";
import { icons } from "@/presentation/common/icons";

export default function AllExam() {
  const { user, isLoadingUser } = useAuth();

  const { drafts, isLoading, handleDraftCardClick } = useAllDraft();

  const isLoadingGlobal = isLoading || isLoadingUser;

  return (
    <>
      {isLoadingGlobal ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-blue-50/50">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          <main className="lg:ml-60 px-5 sm:px-10 md:px-15 lg:px-20 py-15 mx-auto">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">
                  Quản lý bản nháp đề thi
                </h1>
                <p className="text-sm text-gray-500 mt-1">
                  Xem lại và tiếp tục hoàn thiện các đề kiểm tra đang soạn dở
                </p>
              </div>
            </div>

            {drafts.length > 0 && (
              <Link
                href="/generate/new"
                className="inline-flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold px-4 py-2.5 rounded-lg shadow-sm transition active:scale-95 w-fit"
              >
                {icons.plus}
                Tạo đề mới
              </Link>
            )}

            {/* Nội dung danh sách */}
            <section id="drafts-section">
              {drafts.length > 0 ? (
                <div>
                  <div className="flex items-center justify-between text-sm text-gray-500 mb-4 px-1">
                    <span>
                      Tổng cộng:{" "}
                      <strong className="text-gray-900 font-semibold">
                        {drafts.length}
                      </strong>{" "}
                      bản nháp
                    </span>
                  </div>

                  <div className="flex flex-col gap-4">
                    {drafts.map((draft) => (
                      <DraftCard
                        key={draft.id}
                        draftId={draft.id}
                        name={draft.examName}
                        questionsCount={draft.questionsCount}
                        handleCardClick={handleDraftCardClick}
                      />
                    ))}
                  </div>
                </div>
              ) : (
                /* Empty state */
                <div className="bg-white border border-gray-200 rounded-2xl p-12 text-center flex flex-col items-center justify-center shadow-sm max-w-lg mx-auto mt-12">
                  <div className="w-16 h-16 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4">
                    <svg
                      className="w-8 h-8"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                      />
                    </svg>
                  </div>

                  <h2 className="text-xl font-bold text-gray-900">
                    Bạn chưa có bản nháp nào
                  </h2>
                  <p className="text-sm text-gray-500 mt-2 mb-6 text-center leading-relaxed">
                    Các đề thi đang được biên soạn dở dang sẽ tự động lưu lại ở
                    đây để bạn có thể tiếp tục chỉnh sửa bất kỳ lúc nào.
                  </p>

                  <Link
                    href="/generate/new"
                    className="inline-flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm px-6 py-2.5 rounded-lg shadow-sm transition active:scale-95"
                  >
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 4v16m8-8H4"
                      />
                    </svg>
                    Tạo đề kiểm tra mới
                  </Link>
                </div>
              )}
            </section>
          </main>
        </div>
      )}
    </>
  );
}
