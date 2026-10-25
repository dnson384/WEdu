# Bàn giao kiểm duyệt tệp câu hỏi — 2026-10-25

Trước thay đổi: có danh sách/download/preview, parse multipart độc lập. Sau thay đổi: modal hỗ trợ duyệt bản gốc hoặc bản chỉnh sửa qua ModeratorUsecase.parseDocument rồi xóa gốc; từ chối nhập lý do, xóa S3 rồi gửi email. Email lỗi chỉ ghi log, vẫn thành công; không Telegram/retry. Backend `f2ea136` và frontend `d3a6473` đã commit/push, được merge vào main qua PR #113/#114; tài liệu được chuẩn bị thành commit riêng trên main. Test frontend đã được xóa theo yêu cầu trước commit frontend.

## Contract và giới hạn

- Thêm POST /moderator/question-files/approve (multipart key, file tùy chọn) → {questionIds,fileDeleted,message}; Next BFF tương ứng dưới /api.
- Thêm POST /moderator/question-files/reject (JSON key/reason) → 204, kể cả email lỗi sau xóa. Key lấy nguyên từ listing; subject từ key, email từ basename trước dấu '-' cuối. DOCX ≤50 MiB, reason trim 1–2000 ký tự.
- Không đổi database/schema/index hoặc thêm receipt S3; importer/Pandoc giữ nguyên. Không chống import trùng xuyên moderator/restart/mất phản hồi. Import thành công/xóa lỗi chỉ retry xóa trong phiên hiện tại; reload mất trạng thái đó.

## Validation

- Backend: Maven test chạy toàn bộ đạt **244 tests**, 0 failures/errors; 28 cases mới gồm 27 review và 1 email. Không cộng XML report cũ còn trong target vào tổng test chạy hiện tại.
- Frontend: 9 kiểm tra BFF/hook/form/preview đã đạt trước khi bộ test được xóa theo yêu cầu. Hiện không còn file test frontend; phần application code giữ nguyên so với lần TypeScript/ESLint/build đạt.
- TypeScript, ESLint các file frontend liên quan và production build đã đạt cho application code. Commit tài liệu được kiểm tra whitespace và liên kết trong phần thay đổi; không chạy lại application tests/build khi chỉ thay đổi tài liệu.
- Vướng ban đầu: cache .next/dev/types bị lỗi cú pháp, đã chuyển cache sinh tự động ra thư mục tạm và generate lại; Google Fonts bị chặn trong sandbox, build lại có mạng đạt. Test assertion log Unicode gặp encoding Windows, đã đổi kiểm tra mã loại lỗi ASCII và xác nhận không lộ chi tiết SMTP.
- Chưa kiểm tra AWS/SMTP/Pandoc thật hoặc trình duyệt thật; native dialog trong JSDOM dùng polyfill. Không coi mocked tests là xác nhận triển khai production.

## Ngoài phạm vi

Không sửa persisted audit, idempotency, AI moderation hoặc importer/Pandoc. Các snapshot tài liệu còn có nhận định/link legacy; các section mới nêu rõ trạng thái hiện tại cho feature này.

## Toàn bộ 36 file thay đổi

| File | Thay đổi |
| --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java) | Thêm email từ chối UTF-8 text/plain, tái sử dụng JavaMailSender. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | Thêm POST approve multipart và POST reject JSON, giữ quyền MODERATOR. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/dto/ApproveQuestionFileResponseDTO.java](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ApproveQuestionFileResponseDTO.java) | DTO trả questionIds, fileDeleted và message, thể hiện lỗi xóa sau import. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectQuestionFileDTO.java](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectQuestionFileDTO.java) | DTO JSON key/reason. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/infrastructure/StoredDocumentFile.java](../backend/src/main/java/com/wedu/exam_creation/moderator/infrastructure/StoredDocumentFile.java) | Adapter MultipartFile cho bytes DOCX gốc từ S3. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) | Điều phối duyệt/import/xóa; từ chối/xóa/email; email lỗi chỉ log; validate key/email/lý do. |
| [backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) | Tách validateDocumentFile dùng chung upload và approve; giữ quy tắc upload hiện có. |
| [backend/src/test/java/com/wedu/exam_creation/mail/service/MailRejectionTest.java](../backend/src/test/java/com/wedu/exam_creation/mail/service/MailRejectionTest.java) | Kiểm tra email Unicode/text/plain, địa chỉ nhận và nội dung lý do không được render HTML. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java) | Bổ sung mock MailService theo constructor usecase mới; giữ regression xóa. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) | Bổ sung mock MailService theo constructor usecase mới; giữ regression content. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java) | 27 cases MVC/security/usecase/storage về approve/reject, lỗi một phần, validation, email lỗi/log. |
| [codex/api-reference.md](../codex/api-reference.md) | Contract approve/reject, request/response/status và giới hạn xử lý lỗi. |
| [codex/architecture.md](../codex/architecture.md) | Ranh giới UI/BFF/backend/import/storage/mail và giới hạn không có state bền vững. |
| [codex/business-flows.md](../codex/business-flows.md) | Cập nhật luồng xem trước/duyệt bản gốc hoặc sửa/từ chối và thứ tự side effects. |
| [codex/current-status.md](../codex/current-status.md) | Trạng thái implementation, validation, giới hạn và lỗi cache/build ban đầu. |
| [codex/database-schema.md](../codex/database-schema.md) | Xác nhận không đổi schema/index; DTO HTTP và cleanupKeys không phải persistence. |
| [codex/development-decisions.md](../codex/development-decisions.md) | Ghi phạm vi kiểm duyệt S3 được duyệt, thay quyết định PLANNED cho thao tác file. |
| [codex/module-map.md](../codex/module-map.md) | Bổ sung luồng và symbols review liên tầng. |
| [codex/question-file-review-change-report.md](../codex/question-file-review-change-report.md) | Báo cáo bàn giao và danh sách đầy đủ file thay đổi trong task. |
| [codex/source-catalog.md](../codex/source-catalog.md) | Inventory DTO/adapter/routes/hooks/form/tests mới; sửa link ModeratorService và bỏ link user.service đã không tồn tại. |
| [codex/tech-stack.md](../codex/tech-stack.md) | Ghi validation đã chạy, việc xóa test frontend; không dependency hoặc cấu hình mới. |
| [frontend/app/(site)/moderator/question-files/page.tsx](../frontend/app/(site)/moderator/question-files/page.tsx) | Nối props kết quả review/cleanup vào modal, cập nhật nhãn danh sách chờ duyệt. |
| [frontend/app/api/moderator/question-files/approve/route.ts](../frontend/app/api/moderator/question-files/approve/route.ts) | BFF POST multipart approve: cookie, validation field, chỉ forward key/file, giữ kết quả import/xóa. |
| [frontend/app/api/moderator/question-files/reject/route.ts](../frontend/app/api/moderator/question-files/reject/route.ts) | BFF POST reject: cookie, JSON/reason validation, trim, trả 204. |
| [frontend/app/api/moderator/question-files/reviewError.ts](../frontend/app/api/moderator/question-files/reviewError.ts) | Dùng chung no-store và ánh xạ status/message lỗi an toàn cho hai BFF mới. |
| [frontend/domain/entities/questionFileReview.entity.ts](../frontend/domain/entities/questionFileReview.entity.ts) | Types ApproveQuestionFileResult và RejectQuestionFileInput. |
| [frontend/domain/repositories/IModeratorRepository.ts](../frontend/domain/repositories/IModeratorRepository.ts) | Thêm port approve/reject và kiểu trả về. |
| [frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts) | Gửi multipart/JSON tới backend, Bearer chỉ phía server. |
| [frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx) | Nối form kiểm duyệt, preview bản chỉnh sửa, khóa đóng/Escape khi pending. |
| [frontend/presentation/components/Moderator/QuestionFileReviewForm.tsx](../frontend/presentation/components/Moderator/QuestionFileReviewForm.tsx) | Component thao tác duyệt/tải bản sửa/form lý do/tái xóa sau import. |
| [frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts) | Tái sử dụng renderer sandbox cho S3 và File cục bộ; không hiển thị kết quả nguồn cũ. |
| [frontend/presentation/hooks/Moderator/useQuestionFileReview.ts](../frontend/presentation/hooks/Moderator/useQuestionFileReview.ts) | Quản lý file/reason/state/mutation/error; chặn gửi lặp và không tự retry. |
| [frontend/presentation/hooks/Moderator/useQuestionFiles.ts](../frontend/presentation/hooks/Moderator/useQuestionFiles.ts) | Cache/refetch sau review, ghi nhớ key chờ xóa trong bộ nhớ, xóa trạng thái khi cleanup xong. |
| [frontend/presentation/services/ModeratorService.ts](../frontend/presentation/services/ModeratorService.ts) | Thêm lời gọi BFF approve/reject từ giao diện. |
| [frontend/usecases/moderator/ApproveQuestionFileUsecase.ts](../frontend/usecases/moderator/ApproveQuestionFileUsecase.ts) | Usecase frontend chuyển yêu cầu approve qua repository. |
| [frontend/usecases/moderator/RejectQuestionFileUsecase.ts](../frontend/usecases/moderator/RejectQuestionFileUsecase.ts) | Usecase frontend chuyển yêu cầu reject qua repository. |
