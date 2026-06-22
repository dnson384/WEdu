"use client";
import { icons } from "@/presentation/common/icons";
import NavBar from "@/presentation/components/layout/Navbar";
import useNavBar from "@/presentation/hooks/layout/useNavBar";

export default function Home() {
  const { user, isLoading } = useNavBar();
  return (
    <>
      {isLoading ? (
        <></>
      ) : (
        <>
          <NavBar avatarUrl={user.avatarUrl} />
          <main className="mt-32">
            dday la trang chu
            {/* Footer */}
            <section className="py-8 border border-gray-300">
              <p className="text-center text-gray-500">
                © 2026 Hệ Thống Tạo Đề Tự Động - FckEdu. Tuân thủ chuẩn 7991 của
                Bộ Giáo dục.
              </p>
            </section>
          </main>
        </>
      )}
    </>
  );
}
