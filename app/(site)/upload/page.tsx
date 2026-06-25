"use client";
import NavBar from "@/presentation/components/layout/Navbar";
import { useAuth } from "@/presentation/hooks/Auth/useAuth";
import useDocxUpload from "@/presentation/hooks/UploadFile/useUploadFile";
import { icons } from "@/presentation/common/icons";

export default function Upload() {
  const {
    hiddenFileInput,
    subject,
    isSuccess,
    error,
    setSubject,
    handleInputClick,
    handleSelectedFile,
  } = useDocxUpload();
  const { user, isLoadingUser } = useAuth();

  return (
    <>
      {isLoadingUser ? (
        <div className="h-screen mx-auto px-4 flex justify-center items-center">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />
          <main className="h-screen flex justify-center items-center">
            <section className="relative mt-26">
              {/* Thông báo thành công */}
              {isSuccess && (
                <div className="fixed inset-0 z-50 h-fit top-20 flex justify-center">
                  <div className="bg-green-100 px-3 py-2 rounded-md flex items-center gap-2">
                    {icons.success}
                    <p className="text-green-500">Tải lên câu hỏi thành công</p>
                  </div>
                </div>
              )}
              {isSuccess === false && (
                <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
                  <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
                    {icons.error}
                    <p className="text-red-500">
                      Có lỗi trong quá trình tải lên câu hỏi
                    </p>
                  </div>
                </div>
              )}

              {/* Thông báo lỗi */}
              {error && (
                <div className="fixed inset-0 z-10 h-fit top-20 flex justify-center">
                  <div className="bg-red-100 px-3 py-1 rounded-md flex items-center gap-2">
                    {icons.error}
                    <p className="text-red-500">{error}</p>
                  </div>
                </div>
              )}
            </section>

            <section>
              <div className="flex items-center gap-3 mb-3">
                <p className="text-lg text-blue-600 font-bold">Tên môn học</p>
                <input
                  type="text"
                  placeholder="Nhập tên môn học"
                  className="border-b border-b-blue-600 focus:outline-0 px-2 py-1"
                  value={subject}
                  onChange={(e) => {
                    setSubject(e.target.value);
                  }}
                />
              </div>
              <div className="flex justify-center gap-2 bg-blue-600 w-full px-4 py-2 rounded-full cursor-pointer">
                <input
                  ref={hiddenFileInput}
                  type="file"
                  id="docx-input"
                  accept=".docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  className="hidden"
                  onClick={handleInputClick}
                  onChange={handleSelectedFile}
                />

                <label
                  htmlFor="docx-input"
                  className="text-white font-bold cursor-pointer"
                >
                  Chọn tài liệu Word (.docx)
                </label>
              </div>
            </section>
          </main>
        </>
      )}
    </>
  );
}
