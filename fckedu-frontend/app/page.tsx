"use client";

import Link from "next/link";

import { icons } from "@/presentation/common/icons";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useDashboard from "@/presentation/hooks/Dashboard/useDashboard";
import ExamCard from "@/presentation/components/Exam/ExamCard";
import useAllExam from "@/presentation/hooks/Exam/useAllExam";
import useAllDraft from "@/presentation/hooks/Draft/useAllDraft";
import DraftCard from "@/presentation/components/Draft/DraftCard";

export default function Home() {
  const { user, isLoadingUser } = useAuth();

  const {
    recentDrafts = [],
    recentExams = [],
    handleDraftCardClick,
    handleExamCardClick,
  } = useDashboard() || {};

  const finalDrafts = isLoadingUser ? [] : recentDrafts;
  const finalExams = isLoadingUser ? [] : recentExams;

  return (
    <>
      {isLoadingUser ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          <main className="lg:ml-60 my-15 px-20 bg-blue-500/5 min-h-screen">
            {/* Shortcut */}
            <section id="shortcut" className="grid grid-cols-3 gap-5">
              <Link
                href={"/generate"}
                className="flex flex-col gap-5 bg-blue-500 p-5 rounded-xl transition-all hover:bg-blue-500/90 hover:shadow-xl hover:-translate-y-0.5"
              >
                <div className="bg-white/20 text-white w-15 h-15 rounded-xl flex items-center justify-center">
                  {icons.generate}
                </div>
                <div className="flex flex-col gap-2">
                  <h3 className="text-white text-xl font-bold">
                    Tạo đề kiểm tra
                  </h3>
                  <p className="text-white/90">
                    Tự động tạo đề theo Công văn 7991
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <p className="text-white">Bắt đầu ngay</p>
                  <div className="text-white">{icons.forward16}</div>
                </div>
              </Link>

              <Link
                href={"/exam/all"}
                className="flex flex-col gap-5 bg-white border border-gray-200 p-5 rounded-xl transition-all hover:border-blue-500/40 hover:shadow-xl hover:-translate-y-0.5"
              >
                <div className="bg-purple-500/10 text-purple-500 w-15 h-15 rounded-xl flex items-center justify-center">
                  {icons.book32}
                </div>

                <div className="flex flex-col gap-2">
                  <h3 className="text-xl font-bold">Danh sách đề</h3>
                  <p className="text-black/70">Xem các đề đã tạo trước đó </p>
                </div>

                <div className="flex items-center gap-2">
                  <p className="text-blue-500">Xem tất cả</p>
                  <div className="text-blue-500">{icons.forward16}</div>
                </div>
              </Link>

              <Link
                href={"/upload"}
                className="flex flex-col gap-5 bg-white border border-gray-200 p-5 rounded-xl transition-all hover:border-blue-500/40 hover:shadow-xl hover:-translate-y-0.5"
              >
                <div className="bg-orange-500/10 text-orange-500 w-15 h-15 rounded-lg flex items-center justify-center">
                  {icons.database32}
                </div>

                <div className="flex flex-col gap-2">
                  <h3 className="text-xl font-bold">Ngân hàng câu hỏi</h3>
                  <p className="text-black/70">Tải lên tài liệu Word</p>
                </div>

                <div className="flex items-center gap-2">
                  <p className="text-blue-500">Tải lên ngay</p>
                  <div className="text-blue-500">{icons.forward16}</div>
                </div>
              </Link>
            </section>

            {/* Recent */}
            <section id="recent" className="mt-10">
              {finalExams && finalExams.length > 0 && (
                <div className="mb-10">
                  <h2 className="text-xl font-medium mb-5">
                    Đề tạo hoàn chỉnh gần đây
                  </h2>
                  <div className="flex flex-col gap-3">
                    {finalExams.map((exam) => {
                      return (
                        <ExamCard
                          key={exam.id}
                          examId={exam.id}
                          name={exam.name}
                          questionsCount={exam.questionsCount}
                          handleCardClick={handleExamCardClick}
                        />
                      );
                    })}
                  </div>
                </div>
              )}

              {finalDrafts && finalDrafts.length > 0 && (
                <div>
                  <h2 className="text-xl font-medium mb-5">Đề nháp gần đây</h2>
                  <div className="flex flex-col gap-3">
                    {finalDrafts.map((draft) => {
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
                  </div>
                </div>
              )}
            </section>
          </main>
        </>
      )}
    </>
  );
}
