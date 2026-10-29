# Trạng thái triển khai và điểm cần xác nhận

## Giao diện câu hỏi chờ duyệt — 2026-10-27

Snapshot sau merge vào main `e149d9f`: backend `87cf507`, tests `9b8595b`, frontend `031c64b`. Các mức implemented dưới đây đối chiếu source đã merge, không xác nhận production hoặc MongoDB/S3 thật. Kiểm tra lần chuẩn bị commit tài liệu: chỉ 7 file codex thay đổi, diff whitespace và links bổ sung được kiểm tra; không chạy lại application tests cho thay đổi chỉ tài liệu.

Refactor ranh giới repository/usecase: đã xóa `question/domain/entity/QuestionPageEntity.java`. IQuestionRepository và QuestionRepositoryImpl trả List<QuestionEntity> qua findPending, long qua countPending; adapter dùng pendingQuery chung cho bộ lọc, không trả DTO hoặc wrapper phân trang. QuestionService điều phối hai method, map entity và tạo PendingQuestionPageDTO (class Lombok, không record). PendingQuestionsRepositoryTest cập nhật contract; QuestionServicePendingQuestionsTest thêm kiểm tra mapping/totalPages/trang rỗng. JSON API, schema, thứ tự count trước fetch và phân trang 10 câu giữ nguyên; không đổi frontend/application.yml. Validation: `mvn -q test` đạt **257 tests, 0 failures/errors**; `git diff --check` và 51 links local bổ sung trong docs đạt. Lần test đầu phát hiện assertion so sánh identity regex giữa hai query mới; đã đổi sang so sánh BSON JSON (gồm regex/flags), chạy lại đạt. Không kiểm chứng MongoDB thật trong bước refactor.

Validation lần nối API: `mvn -q test` đạt **255 tests, 0 failures/errors**, gồm 9 MVC/security/usecase cases mới và 2 repository cases mới. `npx.cmd tsc --noEmit --incremental false`, ESLint 9 file frontend liên quan, `npm.cmd run build` và `git diff --check` đạt. Một lần chạy Maven bị sandbox chặn cache ngoài workspace; chạy lại với quyền phù hợp đạt. Không truy cập MongoDB/S3 thật hoặc kiểm chứng browser thật; MVC test dùng service mocks, repository test kiểm chứng query MongoTemplate mock. Không thay đổi application.yml có sẵn. Danh sách file thay đổi/xóa và mô tả tại [source catalogue](source-catalog.md#giao-diện-review--2026-10-27).

Implemented danh sách thật tại `/moderator/review` qua GET moderator pending: chỉ Questions.status=PENDING_REVIEW, cố định 10 câu/trang, mới nhất trước và tie-break theo _id giảm dần. Tìm nội dung/chương/bài và lọc loại câu phía server trước count/pagination; UI debounce 300 ms, reset trang khi lọc, loading/error/retry/empty và Trang trước/sau. Có tổng số câu, tên chương/bài (fallback nếu thiếu), ngày tạo, math và ảnh presigned trên DTO. Đã bỏ fixtures/hook mẫu. Mỗi thẻ vẫn có Duyệt thẳng, Chỉnh sửa, Từ chối – yêu cầu regenerate; các thao tác này chỉ giao diện, chưa có write API. Quyền UI theo ModeratorLayout; API theo ROLE_MODERATOR. Không thay schema, QuestionsReview hoặc việc dùng câu AI trong đề. Phần này thay thế nhận định review page là placeholder/dữ liệu minh họa trong snapshot cũ.

Bằng chứng: [page](../frontend/app/(site)/moderator/review/page.tsx), [card](../frontend/presentation/components/Moderator/PendingQuestionCard.tsx), [hook](../frontend/presentation/hooks/Moderator/usePendingQuestions.ts), [contract](api-reference.md). Query count và fetch là hai thao tác, không đảm bảo snapshot khi có ghi đồng thời; chưa thêm index riêng. Mỗi request đọc danh mục chương/bài qua ChapterService.getAll để tìm tên và map response; chưa benchmark khi dữ liệu lớn.

## Collection câu hỏi cần duyệt — 2026-10-26

Implemented snapshot riêng trong questionReview: RegeneratedQuestion, Content, Variables ở cả domain/entity và infrastructure/document. Root regeneratedQuestions dùng các kiểu nội bộ; QuestionReviewMapper tự map, không import từ question. Giữ source hiện tại: collection QuestionsReview, service → QuestionReviewUsecase → repository, transaction tại QuestionService. Chưa regenerate/append/API/approve/reject hoặc backfill. Validation: mvn -q clean test đạt 244 tests, 0 failures/errors; git diff --check đạt. Java harness tạm ngoài repo với Spring context, MongoTemplate mock và transaction manager giả lập đạt wiring có QuestionReviewUsecase, DOCX không tạo review, AI ID, danh sách rỗng, nested model/entity/BSON roundtrip đầy đủ metadata/status/content/options/math/image/timestamps, collection QuestionsReview, snapshot không @Document, commit và rollback interception khi review lỗi. Source và MapStruct generated code của questionReview không import từ question. Không tạo file test trong repo. Chưa kiểm chứng MongoDB thật.

Bằng chứng: [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java), [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java).

## Trạng thái câu hỏi — 2026-10-26

Implemented trạng thái ban đầu: String status, parse APPROVED, AI PENDING_REVIEW. Legacy thiếu field đọc null, không backfill; không filter hoặc workflow duyệt câu hỏi. Public method đổi thành insertFromDocx, ImporterService đã cập nhật nơi gọi. QuestionStatusTest.java đã xóa theo yêu cầu. Validation sau rename/xóa test: mvn -q clean test đạt 244 tests, 0 failures/errors; git diff --check đạt; chưa chạy MongoDB/Pandoc/AI thật. Không đổi HTTP API. Các snapshot cũ ghi chưa có status được thay thế bởi cập nhật này.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

## Kiểm duyệt tệp S3 — 2026-10-25

Implemented theo source: modal preview hỗ trợ duyệt ngay, chọn/xem trước DOCX chỉnh sửa rồi duyệt, hoặc nhập lý do từ chối. [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java).approveQuestionFile nối key S3 tới `parseDocument`; import thành công xóa tệp gốc. Xóa sau import lỗi trả `fileDeleted=false`, UI ghi nhớ key và chỉ cho xóa lại trong phiên hiện tại. Không database/receipt/audit; không bảo đảm idempotency xuyên moderator/restart/mất phản hồi.

Từ chối xác thực email/lý do trước khi xóa; xóa thành công rồi gửi email một lần. Email lỗi chỉ WARN loại exception, không log thông tin SMTP/credentials, không retry/Telegram; API vẫn 204. Không bảo đảm người dùng nhận email. [Contract](api-reference.md#duyệttừ-chối-tệp--2026-10-25), [hook](../frontend/presentation/hooks/Moderator/useQuestionFileReview.ts), [form](../frontend/presentation/components/Moderator/QuestionFileReviewForm.tsx).

Validation: `mvn -q test`: **244 tests**, 0 failures/errors, gồm **27** cases [ModeratorQuestionFileReviewTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java) và **1** [MailRejectionTest](../backend/src/test/java/com/wedu/exam_creation/mail/service/MailRejectionTest.java) mới. Frontend từng đạt 9 kiểm tra BFF/hook/form/preview DOCX trong JSDOM trước khi bộ test được xóa theo yêu cầu; hiện không còn file test frontend trong dự án. TypeScript, ESLint các file liên quan và production build đã đạt cho phần application code không đổi. AWS/SMTP/Pandoc thật và trình duyệt thật chưa kiểm tra; JSDOM dùng polyfill dialog.

Lần đầu TypeScript bị chặn bởi cache `.next/dev/types` có lỗi cú pháp: đã chuyển cache sinh tự động sang thư mục tạm và generate lại. Build lần đầu không tải được Google Fonts trong sandbox, chạy lại có mạng đạt. Các snapshot cũ bên dưới là lịch sử, không thay thế cập nhật này về kiểm duyệt tệp.


## Cách đọc kết luận

Cập nhật chống trùng tên DOCX 2026-10-25: [S3Service.updateDocumentFile/uploadUniqueDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) giữ tên chuẩn hóa hoặc thêm hậu tố `(1)`, `(2)`… nhỏ nhất còn trống trong cùng thư mục. Conditional PUT `If-None-Match: *` bảo vệ file cũ khi upload đồng thời; 412 thử tên tiếp theo, 409 retry cùng key tối đa 3 lần, lỗi khác không bị coi là trùng tên. API trả key thực tế, không đổi schema/DTO hoặc triển khai kiểm duyệt.

Validation: `mvn -q test` đạt **216 tests**, 0 failures/errors/skipped; riêng [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java) đạt **17 tests**, gồm 9 cases mới cho collision/concurrency/retry/lỗi storage. Kiểm tra 3 instance upload đồng thời dùng S3Client mock với conditional write nguyên tử; xác nhận nội dung cũ, gap hậu tố, phân biệt thư mục/email và stream mở lại sau lỗi. `git diff --check` đạt. Chưa upload AWS thật hoặc chạy UI end-to-end; frontend không thay đổi. Lượt compile test đầu gặp lỗi kiểu trả về builder AWS trong test mới, đã sửa trước lượt chạy đạt này; không có test failure còn lại.

Tách component 2026-10-25: JSX của từng dòng tệp trong `QuestionFilesPage` chuyển sang [QuestionFileRow](../frontend/presentation/components/Moderator/QuestionFileRow.tsx), giữ nguyên giao diện, liên kết tải và callback/trạng thái xóa. Không đổi API/schema hoặc logic hook. TypeScript, ESLint cho page/component và kiểm tra luồng xóa bằng React DOM + React Query trong JSDOM đều đạt; chưa kiểm tra trên trình duyệt thật.

Refactor 2026-10-25: `QuestionFilesPage` chỉ render giao diện và truyền props; [useQuestionFiles](../frontend/presentation/hooks/Moderator/useQuestionFiles.ts) quản lý query/mutation, state, handler, cache/refetch và nhãn định dạng tệp. API và hành vi lấy/xóa tệp giữ nguyên. TypeScript, ESLint cho page/hook, production build và kiểm tra hành vi React Query + React DOM trong JSDOM đều đạt sau refactor; chưa kiểm tra trình duyệt hoặc AWS thật.

Cập nhật 2026-10-25: xóa tệp câu hỏi đã có code backend/frontend: DELETE moderator giới hạn prefix, native confirmation dialog, chặn gửi lặp, thông báo và refetch danh sách sau thành công. Dùng S3 DeleteObject hiện có, không thêm database/approval/version purge. [API](api-reference.md), [backend test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java), [UI](../frontend/app/(site)/moderator/question-files/page.tsx).

Validation tính năng xóa: `mvn -q test` đạt **192 tests**, gồm **17 test xóa** đi qua MVC/security/usecase/storage với S3Client mock. TypeScript, ESLint các file liên quan, `npm.cmd run build` và `git diff --check` đạt. 9 kiểm tra Next DELETE/adapter với HTTP backend giả lập đạt (key đặc biệt, Bearer, 204 lặp, 400/401/403/500/502). Kiểm tra React Query + React DOM trong JSDOM đạt các luồng Hủy/Escape, focus nút Hủy, chặn gửi lặp/đóng khi pending, thành công/refetch, lỗi DELETE và lỗi GET sau DELETE. JSDOM dùng polyfill showModal/close, chưa xác minh focus trap/backdrop trên trình duyệt thật và chưa xóa object AWS thật.

Khảo sát source trong working tree ngày 2026-10-24, gồm thay đổi chưa commit; không chỉ dựa vào Git HEAD. Không thay application code/config/database, không đọc `.env`, không gọi dịch vụ ngoài. Không chạy build/test trong tác vụ phân tích này; vì vậy không chứng nhận ứng dụng chạy thành công.

- **implemented:** có code xử lý cụ thể ở các lớp được nêu; không có nghĩa feature đã được nghiệm thu/runtime pass.
- **partial:** có một phần implementation nhưng thiếu mắt xích hoặc có contract/config mismatch được chỉ ra.
- **missing:** không tìm thấy implementation trong phạm vi source/manifest/route/document đã khảo sát; không khẳng định dịch vụ ngoài repo không tồn tại.
- **unknown:** source không đủ để kết luận (deployment, dữ liệu/index thật, quy trình con người, ý định sản phẩm).

“Đang triển khai” không thể suy ra thời gian/kế hoạch từ file tồn tại; dùng **partial** cho phần code dở dang, không tự gán roadmap. Bản đồ symbols/API ở [module-map.md](module-map.md); dữ liệu ở [database-schema.md](database-schema.md).

## Ma trận tính năng

| Tính năng | Trạng thái theo phạm vi | Bằng chứng và giới hạn |
| --- | --- | --- |
| Local register/login | implemented | [AuthUsecase.register/login](../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java), [Next routes](../frontend/app/api/auth); BCrypt, default teacher/FREE, JWT/cookie. Runtime chưa thử. |
| Refresh/logout/đổi password | implemented | `AuthUsecase.regenerateAccessToken/logout/changePassword`, [proxy](../frontend/proxy.ts); metadata refresh, revoke phiên khác khi đổi mật khẩu; access không blacklist. |
| Forgot/reset password + email | implemented | [PasswordResetTokenUsecase](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java), [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java), [reset page](../frontend/app/(site)/auth/reset-password/page.tsx); không revoke sessions khi reset. SMTP delivery unknown. |
| Google OAuth2 end-to-end | partial | [OAuth service](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java), [success handler](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java); role/cookie/path/redirect/persistence khác local flow. |
| Profile, username, avatar | profile/username implemented; avatar partial | [UserUsecase](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java), [avatar adapter](../frontend/infrastructure/repositories/UserRepositoryImpl.ts), [hook](../frontend/presentation/hooks/Me/useUpdateAvatar.ts); frontend avatar endpoint/cache còn lệch, không chứng nhận e2e. |
| Admin user APIs | implemented | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java), [AdminUsecase](../backend/src/main/java/com/wedu/exam_creation/admin/usecase/AdminUsecase.java), [Next admin routes](../frontend/app/api/admin); search/list/role/lock/unlock/delete. |
| Admin management screen | missing | App Router page inventory trong [module-map.md](module-map.md) chưa có admin page; có API/services/usecases không đồng nghĩa có screen. |
| Chapter listing | implemented | [ChapterController](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java), [ChapterRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java). |
| Draft create/read/update chapter/lesson/delete | implemented | [DraftUsecase](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java), [DraftRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java), [generate pages](../frontend/app/(site)/generate). |
| Matrix/matrixDetails auto-generation | implemented | [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java), generate endpoints; deterministic ratios/allocation, không stock-aware. |
| Persist matrix chỉnh tay | partial | [useMatrix.handleMatrixInputChange/handleContinueClick](../frontend/presentation/hooks/Generate/Child/useMatrix.ts): state có chỉnh nhưng tiếp tục chỉ gọi generate; controller chưa có API nhận counts sửa. |
| Sinh đề từ ngân hàng | implemented | [QuestionService.generateExamQuestions](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [ExamUsecase](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java); sampling, loại duplicate pool, trả thiếu và lưu đề. |
| AI bổ sung cho PLUS | partial | Cùng QuestionService + [AIQuestionGenerationService](../backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java); có gọi/parse/lưu/đưa vào đề, nhưng chưa validate count/order đầy đủ, empty bank lỗi trước AI, timeout chưa nối. |
| AI-only generation/job status/cancel | missing | Không thấy endpoint/job document; flow hiện là synchronous fallback trong `QuestionService`. |
| Xem/list/recent/xóa đề, render math/image | implemented | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java), [exam page](../frontend/app/(site)/exam/[examId]/page.tsx), [QuestionContent](../frontend/presentation/components/Exam/QuestionContent.tsx). |
| Word export pipeline | implemented (source), vận hành partial | [ExporterUsecase](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java), [GenerateWordFileUsecase](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts); exporter start/output mismatch, chưa smoke-test DOCX. |
| Upload avatar image | implemented | [S3Service.uploadImageFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), Tika JPEG/PNG ≤5 MiB; UUID filename. |
| Upload DOCX | implemented theo source; hàng chờ persisted missing | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) dùng updateDocumentFile, `.docx` + ZIP entries, ≤50 MiB; UI hiện 50 MB và StorageService typed string S3 key. Tên trùng thêm `(n)` qua conditional PUT. Không có submission state. |
| Moderator parse/import DOCX | implemented (source), vận hành unknown | [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [ImporterService](../backend/src/main/java/com/wedu/exam_creation/importer/service/ImporterService.java), [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java). Input multipart trực tiếp; phụ thuộc Pandoc. |
| Moderator list/download question files | implemented theo source | [ModeratorUsecase.listQuestionFiles](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [S3Service.listQuestionFiles](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [Next GET](../frontend/app/api/moderator/question-files/route.ts), [files page](../frontend/app/(site)/moderator/question-files/page.tsx); role MODERATOR, pagination S3, signed download URL, loading/empty/error/retry/refetch. |
| File approve/reject implemented; câu hỏi/AI và audit missing | partial | Có approve/reject API cho file; không submission/review fields hoặc audit; [review page](../frontend/app/(site)/moderator/review/page.tsx) và [bank page](../frontend/app/(site)/moderator/question-bank/page.tsx) mới placeholder. |
| Telegram review/cleanup notifications | implemented | [TelegramNotificationService](../backend/src/main/java/com/wedu/exam_creation/notification/telegram/TelegramNotificationService.java), call sites QuestionService/UserUsecase; chỉ gửi message, không review state machine. |
| PLUS entitlement/accountType read | implemented | User tạo FREE; QuestionService đọc PLUS. |
| Mua/nâng PLUS, payment/webhook/invoice | missing; UI partial | [Bill](../frontend/app/(site)/me/bill/page.tsx) chỉ placeholder; không payment module/collection/dependency trong inventory. Cách PLUS được cấp ngoài repo unknown. |
| MongoDB transactions | implemented cấu hình/code | [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java); runtime topology và kết quả rollback unknown. Không phải distributed transaction. |
| Automatic tests | partial | Backend có auth/user/storage, thêm [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java), [StorageQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageQuestionFilesTest.java), [ModeratorQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java). Có test legacy route đã bị xóa; chưa chạy tests trong lượt cập nhật tài liệu. Frontend/exporter chưa có test suite thực. |

## Vấn đề kỹ thuật được xác minh bằng đọc code

Đây là phát hiện tĩnh, chưa phải lỗi đã tái hiện; không sửa trong tác vụ này.

| Vấn đề | Bằng chứng | Hệ quả/điều cần kiểm chứng |
| --- | --- | --- |
| Exporter build output không khớp start | [tsconfig](../exporter-service/tsconfig.json), [manifest](../exporter-service/package.json) | outDir comment nhưng start trỏ dist/app.js; compile có thể emit cạnh source, không tự giả định start chạy. |
| Zod là dependency gián tiếp dù source import | [auth schema](../frontend/presentation/schemas/auth.schema.ts), [package.json](../frontend/package.json), lockfile | Cần dependency trực tiếp khi có tác vụ dependency; chưa sửa hiện tại. |
| OAuth lệch hợp đồng local | OAuth service/handler ở bảng trên, [SecurityConstants](../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java) | ROLE_USER, accountType chưa set, snake_case cookies, no refresh metadata, redirect cứng; JWT filter có thể chặn OAuth paths. |
| TTL/cookie HTTPS thiếu nhất quán | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java), [Next login](../frontend/app/api/auth/login/route.ts), [proxy](../frontend/proxy.ts), [application.yml](../backend/src/main/resources/application.yml) | Backend cookie access 15 phút vs JWT/Next 5; Next login secure false, refresh theo production. |
| Reset password chưa revoke RT/atomic consume | [PasswordResetTokenUsecase.resetPassword](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java) | Chưa transaction/atomic claim token; hành vi nhiều request đồng thời chưa thử. |
| Upload contract/size frontend còn lệch | [StorageService](../frontend/presentation/services/StorageService.ts), [upload page](../frontend/app/(site)/upload/page.tsx), [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) | Backend đã tách DOCX khỏi ảnh. Frontend còn boolean thay vì string key và nhãn 25 MB thay vì 50 MiB. |
| Thiếu Pandoc/runtime writable workspace | [Dockerfile](../backend/Dockerfile), `PandocConverter.parse` | Chưa cài executable, runtime user riêng; quyền ghi static/uploads chưa được chuẩn bị. |
| AI timeout và response assumptions | [DeepSeekProperties](../backend/src/main/java/com/wedu/exam_creation/ai/properties/DeepSeekProperties.java), `AIQuestionGenerationService`, `QuestionService` | timeoutSeconds không nối RestClient; subList phụ thuộc count/order; Telegram failure đồng bộ có thể làm flow lỗi. |
| Empty bank không đi tới AI | [QuestionRepositoryImpl.findByLessonIds](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java), `QuestionService.generateExamQuestions` | Repo ném NotFound nếu pool rỗng, trước nhánh PLUS. |
| Unique chapter+lesson ở Questions | [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java) | Nếu index tồn tại thì chặn nhiều câu một lesson; index thật/auto-create unknown. |
| Chapter merge thiếu subject | [ChapterRepositoryImpl.saveChapter](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java) | Query name-only có thể merge hai môn cùng tên chương. |
| BankStat parser so trùng difficulty | [PandocConverter.parse](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java) | Điều kiện merge lặp difficultyLevels, bỏ so learningOutcomes; cần fixture để tái hiện. |
| Profile field name lệch | [user.entity.ts](../frontend/domain/entities/user.entity.ts), [CommonUserResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/response/CommonUserResponseDTO.java) | Frontend `isActived`, Java `isActive`; cần kiểm tra JSON runtime, không tự sửa API. |
| SSR/cache profile lệch | [layout](../frontend/app/layout.tsx), [useAuth](../frontend/presentation/hooks/Auth/useAuth.ts), [UserService](../frontend/presentation/services/UserService.ts) | prefetch key user-profile vs user, relative Axios URL và chưa chuyển server cookie. |
| Helper route profile cũ | [services/userService.ts](../frontend/services/userService.ts) | Gọi /api/auth/me, inventory hiện có /api/user/me; kiểm tra call site trước tái sử dụng. |
| Trộn Jackson namespace | [JacksonConfig](../backend/src/main/java/com/wedu/exam_creation/config/JacksonConfig.java), [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java), [JWT filter](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/filter/JwtAuthenticationFilter.java) | com.fasterxml.jackson và tools.jackson là các types khác; compatibility cần compile xác minh. |
| Mongo transaction không bọc side effects | `ImporterService`, `ExamUsecase`, `ExporterUsecase`, `TelegramNotificationService` | Media/HTTP đã gửi không rollback; chưa thấy outbox/queue/idempotency workflow. |
| Reset token TTL/index | [PasswordResetTokenDocument](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/document/PasswordResetTokenDocument.java) | expireAfter 10m tính từ expiryDate (đã cộng 15m); không phải hạn sử dụng 10m. Logic expiry vẫn 15m. |
| Xóa user không cascade toàn bộ dữ liệu | [UserUsecase.deleteUser](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java) | Xóa user và RT, chưa xóa draft/exam/reset token/media. Retention policy unknown. |
| Logging có dữ liệu nhạy cảm trong vài nhánh | [AuthService.resetPassword](../frontend/presentation/services/AuthService.ts), `ExporterUsecase.fetchImagesAsBase64` | Source log payload reset và URL ảnh lỗi; chỉ ghi nhận, không sao chép giá trị thực vào docs. |
| Ảnh option exporter dùng sai prefix | [OptionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/OptionBuilder.ts), [GenerateWordFileUsecase](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts) | Regex split nhận `<img_N>` nhưng OptionBuilder kiểm tra `<image_`; nhánh ảnh option có thể không chạy. Chưa xuất file thử. |
| Cache ảnh thiếu có thể làm export lỗi | [QuestionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/QuestionBuilder.ts), `OptionBuilder`, `ExporterUsecase.fetchImagesAsBase64` | Backend bỏ qua ảnh tải lỗi, builder gọi Buffer.from trên cache entry chưa kiểm tra; `if(buffer)` nằm sau conversion. |
| Chapter frontend type lệch DTO | [chapter.entity.ts](../frontend/domain/entities/chapter.entity.ts), [ChapterRepositoryImpl](../frontend/infrastructure/repositories/ChapterRepositoryImpl.ts), [LessonDataResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/LessonDataResponseDTO.java) | Type frontend khai báo flat exerciseTypes/difficultyLevels/...; backend trả bankStats. Adapter trả raw response, không transform. |
| Loại câu ngoài danh sách có thể bị UI bỏ | [transformExamResToUI](../frontend/presentation/utils/transformExamResToUI.ts), [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java) | UI chỉ giữ Nhiều lựa chọn/Đúng sai/Trả lời ngắn; có nhánh Tự luận trong matrix nhưng chưa xác nhận hỗ trợ xuyên suốt. |

### Findings bổ sung tại snapshot 24/10

| Vấn đề | Bằng chứng | Giới hạn |
| --- | --- | --- |
| Avatar endpoint/cache mismatch | [UserRepositoryImpl](../frontend/infrastructure/repositories/UserRepositoryImpl.ts), [useUpdateAvatar](../frontend/presentation/hooks/Me/useUpdateAvatar.ts), [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) | Adapter gọi `/user/avatar/${s3Key}` thay `/user/update-avatar`; hook new QueryClient và key user-profile lệch useAuth user. |
| Test còn mong route legacy | [ModeratorQuestionFilesTest.preservesLegacyDocumentsEndpoint](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java), [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) | Test đòi GET /storage/documents trả 200 nhưng controller hiện không khai báo route này. Quan sát tĩnh, chưa chạy test. |
| DOCX ghi đè khi trùng tên — đã xử lý 2026-10-25 | [S3Service.updateDocumentFile/uploadUniqueDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) | Cùng email/subject/normalized filename nay thử hậu tố `(1)`, `(2)`… bằng conditional PUT, không ghi đè object hiện có. Key vẫn chứa email; chưa có owner metadata/approval record. |

Bulk matrix update repository lọc draft ID nhưng không lặp owner filter; luồng gọi hiện đã nạp draft theo owner trong use case. Đừng mô tả mọi persistence write đều tự kiểm quyền. Bằng chứng `DraftRepositoryImpl.updateMatrix/updateMatrixDetails`, `DraftUsecase.generateMatrix/generateMatrixDetails`.

## Contract đã đồng bộ và implementation mới

Moderator parse hiện trả `Promise<string[]>` xuyên [IModeratorRepository](../frontend/domain/repositories/IModeratorRepository.ts), [ModeratorRepositoryImpl](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts), [ParseUsecase](../frontend/usecases/moderator/ParseUsecase.ts), [ModeratorService](../frontend/presentation/services/MorderatorService.ts), khớp Java List<String>. TypeScript generic không kiểm định response JSON runtime.

Backend DOCX không còn dùng image-only uploader: `uploadImageFile` cho avatar, `updateDocumentFile` cho DOCX. List files đã chuyển từ StorageController sang ModeratorController/Usecase, có UI files riêng. Những thay đổi này bao gồm staged/unstaged/untracked source, không đồng nghĩa đã commit/deploy. Không tự chuyển roadmap PLANNED thành quyết định đã đóng.
## Nghiệp vụ cần chủ dự án xác nhận

1. **Review:** file/câu AI có được dùng trước khi duyệt? Review diễn ra trong app hay ngoài Telegram? Ai approve/reject, cần lưu audit gì?
2. **Submission:** teacher upload S3 rồi moderator tải lại và gửi parse thủ công, hay cần một submission ID nối hai bước? UI còn 25 MB, backend 50 MiB: giới hạn sản phẩm cần thống nhất là gì?
3. **Roles:** ADMIN có nên có MODERATOR permissions? ROLE_USER của Google có cần hợp nhất với TEACHER? Ai tạo admin đầu tiên?
4. **PLUS/payment:** PLUS được cấp thủ công hay thanh toán? Có hạn dùng/quota không? “Giao dịch” là payment hay DB transaction?
5. **Exam generation:** thiếu câu vẫn lưu đề có đúng không? Nếu AI bù đủ, errors thiếu ban đầu có cần giữ? Tỉ lệ ma trận và Tự luận có yêu cầu chính thức nào?
6. **Dữ liệu:** chapter name duy nhất theo môn hay toàn cục? Câu AI đã lưu có cần cập nhật bankStats? Xóa user cần giữ/xóa những dữ liệu nào?
7. **Security:** reset password có thu hồi tất cả phiên? Có cho liên kết Google với local account cùng email? Cách xử lý avatar Google URL khác S3 key?

Các câu hỏi này không ngăn hoàn tất tài liệu và không được biến thành tính năng/giả định trong code.

## Unknown về vận hành

Database/index/version MongoDB thực, nội dung bucket, Pandoc/Node version đã cài, production topology, CORS/proxy hosting, SMTP deliverability, bot/review bên ngoài, tình trạng test/build và mức độ sử dụng thực đều **unknown**. File config và dependency không đủ chứng minh triển khai thành công.

## Preview DOCX chỉ đọc — 2026-10-25

Implemented theo source: click dòng/tên file mở modal preview DOCX chỉ đọc, loading/error/retry, Đóng/Escape/Tải xuống; iframe sandbox/CSP cách ly. API content giới hạn moderator/key/prefix/.docx, trả binary từ S3; không đổi database hoặc triển khai approval. [Dialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx), [contract](api-reference.md), [backend test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java).

Validation: mvn -q test đạt 207 tests (15 content tests mới); TypeScript, ESLint file liên quan và npm.cmd run build đạt. Kiểm tra tạm bằng React DOM/JSDOM và DOCX fixture thực render tiếng Việt/bảng/ảnh/MathML đạt; mở/đóng, Escape trong iframe, khôi phục focus, abort khi unmount, click dòng độc lập Tải xuống/Xóa, lỗi/retry và Next proxy binary/auth/key/error đạt. JSDOM dùng polyfill dialog và MathML style; chưa xác minh layout/focus trap/CSP thực thi trên browser thật hoặc đọc AWS thật. DOCX có thể khác Word về bố cục.

npm install báo 7 vulnerabilities (1 low, 4 high, 2 critical) trong dependency tree; chưa audit nguồn hoặc tự nâng dependency ngoài scope.

## Backend API nội dung DOCX — 2026-10-25

Implemented theo source: GET /moderator/question-files/content giới hạn ROLE_MODERATOR và key question-files/ có đuôi .docx, trả binary DOCX no-store, 404 khi object không tồn tại và lỗi storage chung. Không đổi schema hoặc triển khai approval/UI. [Controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java).

Validation backend: mvn -q test đạt 207 tests, 0 failures/errors, gồm 15 tests content mới (binary/MIME/cache, key, quyền, 404 và lỗi S3) và regression xóa. S3Client được mock; chưa đọc AWS thật. git diff --cached --check đạt cho snapshot backend.
