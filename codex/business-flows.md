# Draft flow

## 7. Draft → matrix → matrixDetails

Cập nhật loading 2026-11-24: `useSelectLessonPage` theo dõi điều hướng Tiếp tục bằng `useTransition`; Loader và khóa submit duy trì trong lúc lưu hoặc chuyển trang, bao gồm nhánh không thay đổi nội dung. Kết thúc mutation không làm form nội dung hiện lại khi trang đích còn chờ. Lỗi lưu/refetch giải phóng trạng thái submitting để thử lại. [Validation và giới hạn](current-status.md#lesson-navigation-loading-fix--2026-11-24).

Cập nhật 2026-11-24: [useSelectChapterPage](../frontend/presentation/hooks/Generate/Child/useSelectChapterPage.ts) chờ refetch bản nháp sau khi lưu chương trước khi chuyển sang nội dung. [useSelectLessonPage](../frontend/presentation/hooks/Generate/Child/useSelectLessonPage.ts) refetch sau khi lưu nội dung và sinh matrix thành công trước khi chuyển bước. Cache chung `["draft", draftId]` nhận snapshot mới; lỗi refetch giữ trang và hiển thị lỗi dù mutation trước đó có thể đã lưu thành công. Hai hook giữ lựa chọn chưa lưu khi nhận snapshot cùng draft/chapter, khởi tạo lại khi đổi draft/chapter. Chương không thay đổi thì chuyển bước mà không PUT. Không đổi contract hay chính sách matrix. [Validation](current-status.md#draft-chapter-and-lesson-cache-fix--2026-11-24).
