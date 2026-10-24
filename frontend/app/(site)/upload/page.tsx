"use client";

import NavBar from "@/presentation/components/layout/Navbar";
import useAuth from "@/presentation/hooks/Auth/useAuth";
import useUploadToQuestionBank from "@/presentation/hooks/QuestionBank/useUploadToQuestionBank";
import { icons } from "@/presentation/common/icons";
import Loader from "@/presentation/components/layout/Loader";
import Error from "@/presentation/components/layout/Error";

export default function UploadToQuestionBank() {
  const { user, isLoadingUser } = useAuth();
  const {
    hiddenFileInput,
    subject,
    selectedFileName,
    isUploading,
    isSuccess,
    error,
    isDragging,
    setSubject,
    handleInputClick,
    onFileChange,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    handleRemoveFile,
    handleSubmitUpload,
  } = useUploadToQuestionBank();

  return (
    <>
      {isLoadingUser ? (
        <Loader />
      ) : (
        <div className="min-h-screen bg-gray-50/50">
          <NavBar avatarUrl={user.avatarUrl} username={user.username} />

          {/* Thông báo trạng thái */}
          <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-full max-w-md px-4 pointer-events-none">
            {isSuccess && (
              <div className="pointer-events-auto bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-xl shadow-lg flex items-center gap-3">
                <span className="text-green-600 text-lg">{icons.success}</span>
                <p className="text-sm font-medium">
                  Tải lên câu hỏi thành công. Đang chờ kiểm duyệt.
                </p>
              </div>
            )}

            {(isSuccess === false || error) && (
              <div className="pointer-events-auto shadow-lg flex items-center gap-3">
                <Error message={error} />
              </div>
            )}
          </div>

          <main className="lg:ml-60 px-5 sm:px-10 md:px-15 lg:px-20 py-15">
            {/* Header trang */}
            <div className="mb-8">
              <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">
                Thêm vào ngân hàng câu hỏi
              </h1>
              <p className="text-sm text-gray-500 mt-1">
                Tải lên tệp tài liệu Word chứa câu hỏi theo định dạng chuẩn để
                hệ thống xử lý
              </p>
            </div>

            {/* Khung Form Tải lên */}
            <div className="bg-white border border-gray-200/80 rounded-2xl p-6 sm:p-8 shadow-sm space-y-6">
              {/* Nhập môn học */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Tên môn học <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: Lịch sử Đảng, Toán cao cấp..."
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-600 text-sm transition"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  disabled={isUploading}
                />
              </div>

              {/* Vùng Drag & Drop */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Tài liệu câu hỏi <span className="text-red-500">*</span>
                </label>

                <div
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={handleInputClick}
                  className={`relative flex flex-col items-center justify-center p-8 border-2 border-dashed rounded-xl cursor-pointer transition-all duration-200 ${
                    isDragging
                      ? "border-blue-500 bg-blue-50/60 scale-[1.01]"
                      : "border-gray-300 hover:border-blue-400 hover:bg-gray-50/50 bg-gray-50/20"
                  }`}
                >
                  <input
                    ref={hiddenFileInput}
                    type="file"
                    id="docx-input"
                    accept=".docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    className="hidden"
                    onChange={onFileChange}
                    disabled={isUploading}
                  />

                  {/* Icon minh họa Word */}
                  <div
                    className={`w-14 h-14 rounded-2xl flex items-center justify-center mb-3 transition-colors ${
                      isDragging
                        ? "bg-blue-100 text-blue-600"
                        : "bg-blue-50 text-blue-500"
                    }`}
                  >
                    <svg
                      className="w-7 h-7"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                      />
                    </svg>
                  </div>

                  <p className="text-sm font-semibold text-gray-700 text-center">
                    Kéo và thả tệp Word (.docx) vào đây, hoặc{" "}
                    <span className="text-blue-600 hover:underline">
                      chọn tệp
                    </span>
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    Hỗ trợ tệp định dạng .docx dung lượng tối đa 50MB
                  </p>
                </div>
              </div>

              {/* Hiển thị File đã chọn */}
              {selectedFileName && (
                <div className="flex items-center justify-between p-3.5 bg-blue-50/60 border border-blue-100 rounded-xl">
                  <div className="flex items-center gap-3 truncate">
                    <div className="p-2 bg-blue-100 text-blue-600 rounded-lg shrink-0">
                      <svg
                        className="w-5 h-5"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"
                          clipRule="evenodd"
                        />
                      </svg>
                    </div>
                    <span className="text-sm font-medium text-gray-800 truncate">
                      {selectedFileName}
                    </span>
                  </div>

                  {!isUploading && (
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemoveFile();
                      }}
                      className="text-gray-400 hover:text-red-500 p-1 rounded-md transition"
                      title="Xóa tệp đã chọn"
                    >
                      <svg
                        className="w-5 h-5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M6 18L18 6M6 6l12 12"
                        />
                      </svg>
                    </button>
                  )}
                </div>
              )}

              {/* Nút Submit */}
              <button
                type="button"
                disabled={isUploading}
                onClick={handleSubmitUpload}
                className="w-full flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 px-4 rounded-lg shadow-sm transition active:scale-98 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
              >
                {isUploading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang tải tệp lên...</span>
                  </>
                ) : (
                  <span>Tải lên ngân hàng đề</span>
                )}
              </button>
            </div>
          </main>
        </div>
      )}
    </>
  );
}
