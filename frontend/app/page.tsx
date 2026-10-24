"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { icons } from "@/presentation/common/icons";
import NavBar from "@/presentation/components/layout/Navbar";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import useDashboard from "@/presentation/hooks/Dashboard/useDashboard";
import ExamCard from "@/presentation/components/Exam/ExamCard";
import DraftCard from "@/presentation/components/Draft/DraftCard";
import Loader from "@/presentation/components/layout/Loader";

export default function Home() {
  const { user, isLoadingUser } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (user.role === "ROLE_MODERATOR") router.replace("/moderator/question-files");
  }, [user.role, router]);
  if (isLoadingUser || user.role === "ROLE_MODERATOR") return <Loader />;
  return <TeacherHome />;
}

function TeacherHome() {
  const { user, isLoadingUser } = useAuth();

  const {
    recentDrafts = [],
    recentExams = [],
    handleDraftCardClick,
    handleExamCardClick,
  } = useDashboard() || {};

  const finalDrafts = isLoadingUser ? [] : recentDrafts;
  const finalExams = isLoadingUser ? [] : recentExams;

  const hasNoData = finalDrafts.length === 0 && finalExams.length === 0;

  return (
    <>
      {isLoadingUser ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-blue-50/60">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          <main className="lg:ml-60 py-15 px-5 sm:px-10 md:px-15 lg:px-20">
            {/* Shortcut */}
            <section
              id="shortcut"
              className="grid grid-cols-1 md:grid-cols-3 gap-5"
            >
              <Link
                href={"/generate"}
                className="group flex flex-col justify-between bg-blue-600 p-6 rounded-2xl shadow-sm transition-all hover:bg-blue-700 hover:shadow-lg hover:-translate-y-1"
              >
                <div>
                  <div className="bg-white/20 text-white w-12 h-12 rounded-xl flex items-center justify-center mb-4">
                    {icons.generate}
                  </div>
                  <h3 className="text-white text-lg font-bold">
                    Tạo đề kiểm tra
                  </h3>
                  <p className="text-blue-100 text-sm mt-1">
                    Tự động tạo ma trận và đề thi chuẩn Công văn 7991
                  </p>
                </div>
                <div className="flex items-center gap-2 text-white font-medium text-sm mt-6">
                  <span>Bắt đầu ngay</span>
                  <span className="transition-transform group-hover:translate-x-1">
                    {icons.forward16}
                  </span>
                </div>
              </Link>

              <Link
                href="/exam/all"
                className="group flex flex-col justify-between bg-white border border-gray-200 p-6 rounded-2xl shadow-xs transition-all hover:border-blue-300 hover:shadow-lg hover:-translate-y-1"
              >
                <div>
                  <div className="bg-purple-50 text-purple-600 w-12 h-12 rounded-xl flex items-center justify-center mb-4">
                    {icons.book32}
                  </div>
                  <h3 className="text-gray-900 text-lg font-bold">
                    Danh sách đề
                  </h3>
                  <p className="text-gray-500 text-sm mt-1">
                    Xem và xuất bản các đề kiểm tra đã hoàn thành
                  </p>
                </div>
                <div className="flex items-center gap-2 text-blue-600 font-medium text-sm mt-6">
                  <span>Xem tất cả</span>
                  <span className="transition-transform group-hover:translate-x-1">
                    {icons.forward16}
                  </span>
                </div>
              </Link>

              <Link
                href="/upload"
                className="group flex flex-col justify-between bg-white border border-gray-200 p-6 rounded-2xl shadow-xs transition-all hover:border-blue-300 hover:shadow-lg hover:-translate-y-1"
              >
                <div>
                  <div className="bg-amber-50 text-amber-600 w-12 h-12 rounded-xl flex items-center justify-center mb-4">
                    {icons.database32}
                  </div>
                  <h3 className="text-gray-900 text-lg font-bold">
                    Ngân hàng câu hỏi
                  </h3>
                  <p className="text-gray-500 text-sm mt-1">
                    Tải lên tài liệu Word để hệ thống phân loại tự động
                  </p>
                </div>
                <div className="flex items-center gap-2 text-blue-600 font-medium text-sm mt-6">
                  <span>Tải lên ngay</span>
                  <span className="transition-transform group-hover:translate-x-1">
                    {icons.forward16}
                  </span>
                </div>
              </Link>
            </section>

            {/* Recent */}
            <section id="recent" className="mt-12">
              {finalExams && finalExams.length > 0 && (
                <div className="mb-10">
                  <div className="flex justify-between items-center mb-4">
                    <h2 className="text-lg font-bold text-gray-900">
                      Đề hoàn chỉnh gần đây
                    </h2>
                    <Link
                      href="/exam/all"
                      className="text-sm font-semibold text-blue-600 hover:underline"
                    >
                      Xem tất cả
                    </Link>
                  </div>
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
                  <h2 className="text-lg font-bold text-gray-900 mb-4">
                    Bản nháp gần đây
                  </h2>
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

              {/* Empty state khi chưa có đề nào */}
              {hasNoData && (
                <div className="bg-white border border-dashed border-gray-300 rounded-2xl p-10 text-center flex flex-col items-center">
                  <div className="w-16 h-16 bg-blue-50 text-blue-500 rounded-full flex items-center justify-center mb-4">
                    {icons.book}
                  </div>
                  <h3 className="text-base font-semibold text-gray-800">
                    Chưa có đề kiểm tra nào
                  </h3>
                  <p className="text-gray-500 text-sm mt-1 max-w-sm">
                    Hãy bấm vào nút Tạo đề kiểm tra ở trên để xây dựng đề thi
                    chuẩn đầu tiên của bạn.
                  </p>
                </div>
              )}
            </section>
          </main>
        </div>
      )}
    </>
  );
}
