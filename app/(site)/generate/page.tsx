"use client";

import Link from "next/link";

import NavBar from "@/presentation/components/layout/Navbar";
import useCategory from "@/presentation/hooks/Generate/useCategory";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useAllDraft from "@/presentation/hooks/Draft/useAllDraft";
import DraftCard from "@/presentation/components/Draft/DraftCard";

export default function AllExam() {
  const { user, isLoadingUser } = useAuth();

  const { drafts, isLoading, handleDraftCardClick } = useAllDraft();
  const { categories } = useCategory();

  return (
    <>
      <NavBar avatarUrl={user.avatarUrl} username={user.username} />
      {isLoading ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <main className="mt-10 w-6xl mx-auto px-4">
          <h3 className="text-4xl font-bold text-center text-blue-700 mb-5">
            Tạo đề kiểm tra
          </h3>

          <section id="exams" className="flex flex-col gap-5">
            {drafts.length > 0 ? (
              <>
                <div className="flex justify-between items-center mt-10">
                  <h4 className="text-xl font-bold">Danh sách bản nháp</h4>
                  <Link
                    href={"/generate/new"}
                    className="bg-blue-500 text-white px-5 py-3 rounded-xl"
                  >
                    Tạo đề kiểm tra mới
                  </Link>
                </div>
                {drafts.map((draft) => {
                  return (
                    <DraftCard
                      key={draft.id}
                      draftId={draft.id}
                      name={draft.examName}
                      questionsCount={draft.questionsCount}
                      handleCardClick={handleDraftCardClick}
                    />
                  );
                })}
              </>
            ) : (
              <div className="mt-35 flex items-center justify-center h-full">
                <div className="flex flex-col items-center">
                  <h4 className="font-bold text-2xl mb-5">
                    Bạn chưa tạo bản nháp nào!
                  </h4>
                  <div className="w-fit">
                    <Link
                      href={"/generate/new"}
                      className="bg-blue-500 text-white px-5 py-3 rounded-xl w-fit"
                    >
                      Tạo đề kiểm tra mới
                    </Link>
                  </div>
                </div>
              </div>
            )}
          </section>
        </main>
      )}
    </>
  );
}
