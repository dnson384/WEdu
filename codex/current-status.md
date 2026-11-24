# Trạng thái triển khai và điểm cần xác nhận

## Validation on frontend/feature/generate-exam — 2026-11-24

Scoped ESLint, `npm.cmd run build` and `npx.cmd --no-install tsc --noEmit --incremental false` passed on the target branch. The first TypeScript run after switching branches failed because `.next/types` still referenced routes from the previous branch; rebuilding regenerated those types and the repeated TypeScript check passed. These two documentation files contain only this fix's context because the target branch previously had no `codex/` directory. No browser interaction test was run.

## Lesson navigation loading fix — 2026-11-24

Implemented: [useSelectLessonPage](../frontend/presentation/hooks/Generate/Child/useSelectLessonPage.ts) dùng `useTransition` cho điều hướng Tiếp tục, cả khi không đổi nội dung và sau khi lưu/refetch thành công. Loading và khóa submit bao gồm `isNavigating`, tránh hiện lại form nội dung sau khi finally kết thúc mutation nhưng trang đích chưa commit. Handler bỏ qua submit khi đang lưu/điều hướng. Lỗi lưu/refetch vẫn giải phóng submitting để thử lại; giữ nguyên đồng bộ cache và contract. Transition kết thúc theo điều hướng, không giữ cờ loading cố định khi component được tái sử dụng cho chương tiếp theo.

Validation: Prettier --write/--check hook đạt; `npx.cmd --no-install eslint presentation/hooks/Generate/Child/useSelectLessonPage.ts --fix` đạt; `npx.cmd --no-install tsc --noEmit --incremental false` đạt; `npm.cmd run build` đạt ngoài sandbox (lần đầu trong sandbox lỗi EPERM ghi `.next/trace-build`); `git diff --check` đạt. Chưa kiểm thử tương tác browser: chuyển chương, sang matrix, nhánh không thay đổi và lỗi lưu/refetch mới được rà soát theo code; chưa chứng nhận end-to-end. Không thêm framework test/dependency.

## Draft chapter and lesson cache fix — 2026-11-24

Implemented: [useSelectChapterPage](../frontend/presentation/hooks/Generate/Child/useSelectChapterPage.ts) chờ `refetchDraft({ throwOnError: true })` sau khi lưu chương trước khi chuyển bước; nhánh không thay đổi return ngay, tránh PUT thừa. [useSelectLessonPage](../frontend/presentation/hooks/Generate/Child/useSelectLessonPage.ts) refetch sau khi lưu nội dung và sinh matrix thành công trước khi chuyển bước. Cache chung `["draft", draftId]` được cập nhật, tránh dùng snapshot chapters rỗng còn fresh trong 5 phút. Hai hook nhận snapshot mới cùng ID, giữ lựa chọn chưa lưu và khởi tạo lại khi đổi draft/chapter. Submitting được giải phóng trong finally. Không đổi API/schema/backend hay thuật toán matrix.

Validation: Prettier --write/--check hai hook đạt; `npx.cmd --no-install eslint presentation/hooks/Generate/Child/useSelectChapterPage.ts presentation/hooks/Generate/Child/useSelectLessonPage.ts --fix` đạt; `npx.cmd --no-install tsc --noEmit --incremental false` đạt; `npm.cmd run build` đạt ngoài sandbox (lần đầu lỗi EPERM ghi `.next/trace`). `git diff --check` đạt. Chưa kiểm thử tương tác browser/backend thật; không thêm test framework/dependency. Không chứng nhận end-to-end hay production.
