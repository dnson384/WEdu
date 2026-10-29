# Luồng nghiệp vụ và trạng thái WEdu

## Moderator sửa câu hỏi với LaTeX và mô tả ảnh — 2026-10-29

1. Mở Chỉnh sửa: clone nội dung/phương án và updatedAt. Template hiển thị nguyên placeholders; mỗi key math/image duy nhất trong mỗi content có input riêng. `<img_0>` ở câu hỏi độc lập với `<img_0>` ở phương án.
2. Nhập LaTeX, xem trước KaTeX. Ảnh AI giữ trường mô tả; moderator chọn ảnh JPEG/PNG để thay thế. Ảnh đã có hiển thị preview và có thể giữ nguyên hoặc chọn thay. Thêm tham chiếu bằng nút chèn hoặc nhập template; xóa tham chiếu thì bỏ biến/file tương ứng khỏi payload. Hủy bỏ toàn bộ draft, không gọi API/upload.
3. Lưu: frontend kiểm tra nội dung/tham chiếu/LaTeX/ảnh, gửi payload JSON + files có namespace; backend xác thực moderator, câu còn pending, phiên bản khớp và binding đầy đủ. Mô tả chưa có ảnh không cho lưu; giữ ảnh cũ chỉ từ database.
4. Upload ảnh mới vào S3, thay giá trị mô tả trong map chuẩn bị lưu bằng object key. Conditional update MongoDB lưu câu/options và updatedAt, giữ PENDING_REVIEW; chỉ sau thành công dữ liệu persisted mới thay mô tả. Không lưu mô tả riêng, không lưu presigned URL, không sửa QuestionsReview.
5. Thành công 204: đóng form, refetch danh sách; GET trả URL ký cho key ảnh, công thức theo map. Conflict 409: giữ draft, yêu cầu tải lại; upload/validation lỗi không ghi DB. Cleanup media mới best-effort, không xóa media cũ; outcome DB không xác định giữ media mới và kiểm tra lại qua GET trước retry.

Luồng chỉ chỉnh sửa, không approve hoặc regenerate. [Editor](../frontend/presentation/components/Moderator/PendingQuestionEditForm.tsx), [backend usecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecase.java), [API](api-reference.md).

## Moderator xem câu hỏi chờ duyệt — 2026-10-27

Moderator mở `/moderator/review`, UI gọi GET pending qua Next proxy access cookie → Bearer. Backend kiểm tra ROLE_MODERATOR; chỉ đọc Questions.status=PENDING_REVIEW, filter nội dung/chương/bài/loại câu trước count/phân trang cố định 10. Trả mới nhất trước, tên chương/bài và ảnh presigned; không có chương/bài dùng nhãn dự phòng. UI có tổng số, Trang trước/sau, loading/error/retry/empty; đổi bộ lọc trở lại trang 0, tìm kiếm debounce 300 ms. Không lấy từ QuestionsReview.regeneratedQuestions; không backfill legacy null, không thay việc dùng câu AI trong đề.

Duyệt thẳng, Chỉnh sửa, Từ chối – yêu cầu regenerate vẫn chỉ giao diện; không ghi APPROVED/REJECTED, không gọi AI, không tạo snapshot/audit hoặc gửi thông báo. [Contract](api-reference.md), [page](../frontend/app/(site)/moderator/review/page.tsx).

## Collection câu hỏi cần duyệt — 2026-10-26

Câu AI PENDING_REVIEW được lưu trong Questions; IDs chuyển vào QuestionReviewService → QuestionReviewUsecase → entity → repository → QuestionsReview. regeneratedQuestions mặc định rỗng; kiểu phần tử mới RegeneratedQuestionEntity/RegeneratedQuestionDocument cùng cấu trúc Question nhưng hoàn toàn thuộc questionReview, kể cả Content/Variables. Giữ transaction, Telegram và việc dùng câu AI trong đề; không regenerate/append hoặc đổi approve/reject.

Bằng chứng: [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java), [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java).

## Trạng thái câu hỏi — 2026-10-26

ImporterService.parseDocument → QuestionService.insertFromDocx(List<NewQuestionDTO>) gán APPROVED trước khi lưu. Nhánh generateExamQuestions dùng method private insert(questions, "PENDING_REVIEW") cho AI; vẫn gửi Telegram và dùng IDs ngay trong đề. Không lọc status khi chọn lại câu, không thêm API/UI duyệt câu hỏi.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

Snapshot source 2026-10-24; xem [current-status.md](current-status.md) để hiểu nhãn trạng thái. “Có code” không đồng nghĩa đã xác nhận end-to-end. Đường dẫn backend dưới đây là source thật trong `backend/src/main/java/com/wedu/exam_creation`.

## 1. Đăng ký, đăng nhập và phiên

Luồng xóa tệp moderator (2026-10-25): nút Xóa cạnh Tải xuống trong bảng → hộp thoại có tên tệp → Hủy/Escape đóng mà không gọi API; xác nhận khóa thao tác gửi lặp và gọi DELETE → backend kiểm role/key → S3 DeleteObject → 204. Thành công đóng hộp thoại, thông báo, bỏ dòng đã xóa khỏi cache rồi gọi lại API danh sách tại chỗ. DELETE lỗi giữ hộp thoại để thử lại/hủy; lỗi GET sau DELETE hiển thị như lỗi tải danh sách, không coi là xóa thất bại. Không reload toàn trình duyệt hoặc thêm submission/review state. [Trang files](../frontend/app/(site)/moderator/question-files/page.tsx), [dialog](../frontend/presentation/components/Moderator/DeleteQuestionFileDialog.tsx), [contract](api-reference.md).

**implemented** ở mức local-auth handlers/services; tích hợp chạy thật **unknown**.

1. UI/hooks gọi `AuthService` → Next `/api/auth/login|register`, schema kiểm tra input.
2. Next tạo use case/repository; Axios gọi `/auth/login|register` Spring.
3. Backend register kiểm tra confirm/duplicate email, BCrypt password, tạo user mặc định `ROLE_TEACHER`, `FREE`, active, avatar key mặc định. Login kiểm tra user, phương thức GOOGLE, active và password.
4. Sinh cặp JWT; lưu metadata refresh token; Spring trả user và hai Set-Cookie. Next đọc cookies rồi đặt HttpOnly `accessToken`/`refreshToken`, trả `{user}`.
5. Proxy xác minh access token bằng JOSE; nếu thiếu/hết hạn dùng refresh cookie gọi Spring regenerate. Proxy thêm `x-account-id`; protected Next routes chuyển access token sang Bearer tới backend.

Bằng chứng: [AuthUsecase.register/login/generateToken](../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java), [UserUsecase.createNewUser](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java), [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java), [AuthRepositoryImpl](../frontend/infrastructure/repositories/AuthRepositoryImpl.ts), [login route](../frontend/app/api/auth/login/route.ts), [proxy](../frontend/proxy.ts).

Access JWT cấu hình 5 phút; refresh 7 ngày. Backend login/register cookie access maxAge lại là 15 phút; Next cookie là 5 phút. Refresh tra metadata theo JTI/user, không phải refresh-token rotation. Logout xóa refresh token theo access claim parentJti; Next xóa cookies; access JWT cũ không bị blacklist. Đổi mật khẩu kiểm tra mật khẩu cũ/xác nhận rồi thu hồi refresh token **các phiên khác**, giữ JTI hiện tại. Bằng chứng: `AuthUsecase.regenerateAccessToken/logout/changePassword/saveUserAndRevokeRT`, [logout route](../frontend/app/api/auth/logout/route.ts).

## 2. Authorization và role

| Role/điều kiện | Quyền được xác minh | Bằng chứng |
| --- | --- | --- |
| Public paths | Login/register/logout/regenerate/forgot/reset, static, Swagger; logout vẫn cần Authorization header trong controller | [SecurityConstants.PUBLIC_PATHS](../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java) |
| Mọi request khác | Authenticated; filter nạp user và kiểm tra enabled | [SecurityConfig](../backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java), [JwtAuthenticationFilter](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/filter/JwtAuthenticationFilter.java) |
| `ROLE_ADMIN` | `/admin/**`; cấm tự đổi role/khóa/mở/xóa và cấm tác động admin khác trong use case | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java), [AdminUsecase](../backend/src/main/java/com/wedu/exam_creation/admin/usecase/AdminUsecase.java) |
| `ROLE_MODERATOR` | `/moderator/parse`, `/moderator/question-files`; layout moderator kiểm tra role, backend annotation quyết định quyền | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [ModeratorLayout](../frontend/app/(site)/moderator/layout.tsx) |
| `ROLE_TEACHER` | Role mặc định local registration; draft/exam dùng owner ID, không annotation teacher riêng | [UserUsecase](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java), [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java), [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) |
| `ROLE_USER` | Có trong khởi tạo tài khoản Google; không nằm trong danh sách role admin use case cho phép | [CustomOAuth2UserService.loadUser](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java) |
| `accountType == PLUS` | Bù câu hỏi thiếu bằng AI; không phải Spring role | [QuestionService.generateExamQuestions](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java) |

Không thấy RoleHierarchy hoặc cơ chế admin tự có quyền moderator. Backend principal chứa authority lấy từ role hiện tại trong DB: [CustomUserDetails.getAuthorities](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/principal/CustomUserDetails.java). Không dùng UI hoặc `x-account-id` như bằng chứng backend đã phân quyền.

Google OAuth2 là **partial**: service có tạo/cập nhật user nhưng role `ROLE_USER`, không set accountType/timestamps tại nhánh tạo mới; avatar là Google URL. Success handler dùng cookie snake_case/path khác, redirect `/dashboard`, không lưu metadata refresh token. Public constants chưa liệt kê `/oauth2/**`, `/login/oauth2/**`; filter JWT có thể cản luồng trước OAuth. Bằng chứng: [CustomOAuth2UserService](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java), [OAuth2AuthenticationSuccessHandler](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java), `SecurityConstants`, `JwtAuthenticationFilter.shouldNotFilter`. Không khẳng định Google login hoạt động thực tế.

## 3. Quên/đặt lại mật khẩu và SMTP

`AuthUsecase.forgotPassword` validate email → `PasswordResetTokenUsecase.requestReset`: yêu cầu user tồn tại, xóa reset token cũ của user, sinh 32 byte random, lưu SHA-256 hash với hạn 15 phút và `used=false`, gửi link `${FRONTEND_URL}/auth/reset-password?token=...`. SMTP dùng HTML email, Gmail STARTTLS 587.

Reset: so khớp confirmation → hash token nhận vào → tìm unused token → kiểm tra expiry → hash/update password → `used=true`. Không thấy thu hồi refresh-token ở luồng reset; lưu password và token chưa được bao bằng transaction trong use case này. SMTP failure có thể để lại token đã lưu, chưa thấy compensation. Bằng chứng: [PasswordResetTokenUsecase](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java), [repository](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/repository/PasswordResetTokenRepositoryImpl.java), [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java), [application.yml](../backend/src/main/resources/application.yml). Trạng thái code: **implemented**; bảo đảm gửi thư/chống race end-to-end: **unknown**.

## 4. Profile và avatar

Username: Next PUT chuyển thành backend PATCH; use case validate nonblank và ≤254 ký tự. Avatar: UI upload → S3 trả key → gọi update profile với key → lưu key mới → xóa ảnh cũ trừ avatar mặc định; lưu thất bại cố xóa ảnh mới; xóa ảnh cũ lỗi gửi Telegram. `getMe` chuyển key thành presigned URL, không ghi URL tạm vào Mongo.

Bằng chứng: [useUpdateAvatar](../frontend/presentation/hooks/Me/useUpdateAvatar.ts), [UserRepositoryImpl frontend](../frontend/infrastructure/repositories/UserRepositoryImpl.ts), [UserUsecase.updateAvatar/updateUsername/getMe](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java). Xác minh ownership của S3 key đầu vào chưa thấy trong `updateAvatar`; không coi compensation là transaction phân tán.


Frontend avatar vẫn **partial**: [UserRepositoryImpl.updateAvatar](../frontend/infrastructure/repositories/UserRepositoryImpl.ts) gọi `/user/avatar/${s3Key}` nhưng [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) khai báo PATCH `/user/update-avatar` với query s3Key. Hook tạo `new QueryClient()` và invalidate `user-profile`, trong khi [useAuth](../frontend/presentation/hooks/Auth/useAuth.ts) dùng key `user`; chưa làm mới đúng cache provider. Không mô tả luồng này là end-to-end hoàn thiện.

## 5. Upload DOCX, danh sách file và kiểm duyệt

**Upload/list/preview và duyệt/từ chối tệp implemented theo source; không có submission ID hoặc persisted audit.**

1. Teacher UI `/upload` → `StorageService.uploadFileToQuestionBank` → `POST /api/storage/question-file` → `StorageRepositoryImpl` → `POST /storage/question-file` → `S3Service.updateDocumentFile(file, "question-files/" + subject, 50, principal.email)`.
2. Moderator mở `/moderator/question-files` → `ModeratorService.getQuestionFiles` → Next GET `/api/moderator/question-files` → `GetQuestionFilesUsecase` → repository → Spring GET `/moderator/question-files` → `ModeratorUsecase.listQuestionFiles` → `S3Service.listQuestionFiles("question-files")`. Kết quả là `{key,fileName,size,lastModified,presignedUrl}[]`.
3. Modal preview → POST `/moderator/question-files/approve` với key, file chỉnh sửa tùy chọn → [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java).approveQuestionFile → đọc bản gốc, validate DOCX → `parseDocument` với subject từ key. Không file chỉnh sửa thì dùng bytes S3 qua StoredDocumentFile; có file thì import bytes bản chỉnh sửa. Parse lỗi giữ tệp gốc; thành công xóa tệp gốc. Xóa lỗi trả IDs và fileDeleted=false; UI chỉ retry xóa trong phiên hiện tại. Không lưu bản chỉnh sửa. `/moderator/parse` cũ giữ multipart/subject và string[] IDs.
4. Từ chối → form lý do trim 1–2000 ký tự → POST `/moderator/question-files/reject` → xác thực key/email → kiểm tra tệp tồn tại → xóa S3 → MailService.sendQuestionFileRejectionEmail. Email lấy trước dấu '-' cuối trong basename vì tên chuẩn hóa không chứa '-'; giữ được email có dấu '-'. Email không xác định thì không xóa. Xóa lỗi không gửi email. Email lỗi chỉ WARN loại exception, không retry/Telegram, vẫn 204. Email text/plain không diễn giải lý do thành HTML.

Backend DOCX validation: file không rỗng, ≤50 MiB, đuôi `.docx` không phân biệt hoa/thường, ZIP có entry `[Content_Types].xml` và `word/document.xml`; đây chưa phải kiểm định toàn bộ OOXML hay antivirus. Tên file bỏ path gốc/dấu tiếng Việt/ký tự phân cách, ghép PascalCase, thêm `.docx`; key đầu tiên là `question-files/<subject>/<email>-<NormalizedName>.docx`. Cập nhật 2026-10-25: `S3Service.updateDocumentFile → uploadUniqueDocumentFile` ghi bằng `If-None-Match: *`; nếu S3 trả 412 thì thử `<NormalizedName>(1).docx`, `(2)`… theo thứ tự đến tên còn trống. S3 quyết định việc chiếm tên nguyên tử, không HEAD rồi PUT; upload đồng thời không ghi đè nhau. Lỗi 409 thử lại cùng key tối đa 3 lần sau lần đầu; lỗi khác hoặc hết lượt thử được ném lại, không coi là trùng tên. Mỗi lần PUT mở stream mới và API trả đúng key thành công. Subject chưa được chuẩn hóa trong controller. Avatar tách riêng `uploadImageFile(..., "avatars", 5)` dùng Tika JPEG/PNG và UUID.

List S3 duyệt tất cả trang bằng continuation token, bỏ object kết thúc `/`, tạo download URL hạn 60 phút với Content-Disposition attachment/UTF-8 filename. Next GET dùng `Cache-Control: private, no-store`, trả 401 thiếu token, giữ lỗi 401/403 từ upstream và thông báo chung cho lỗi khác. Backend dùng role MODERATOR; lỗi AWS SDK được đổi thành InternalServerException chung. Không log hoặc đưa giá trị URL đã ký vào tài liệu.

Quy tắc conditional write dựa trên [AWS S3 conditional writes](https://docs.aws.amazon.com/AmazonS3/latest/userguide/conditional-writes.html). Trùng tên xét object hiện tại; key đã xóa hoặc có current delete marker có thể được dùng lại, không bảo đảm tên duy nhất trong toàn bộ version history. Lỗi mạng sau khi S3 đã nhận file vẫn có thể khiến người dùng gửi lại thành một bản mới; thay đổi này chống ghi đè, không bổ sung idempotency theo nội dung.

UI list có loading/empty/error/retry/refetch và tải xuống. Query key `["moderator-question-files", user.id]`, enabled theo role MODERATOR, staleTime 0, refetchOnMount always, retry false. Login/root điều hướng moderator tới `/moderator/question-files`; navbar có menu theo role, vào mục files invalidate query prefix. Login/logout dùng QueryClient từ provider, cancel/clear cache để hạn chế dữ liệu người dùng cũ. `/moderator/review` và `/moderator/question-bank` chỉ là placeholders.

**Đối chiếu source 2026-10-25:** UI upload hiện ghi 50 MB, backend kiểm 50 MiB; service `uploadFileToQuestionBank` đã khai báo `Promise<string>`/Axios string, khớp S3 key trả về. `/storage/question-bank`, `/api/storage/question-bank` và GET `/storage/documents` đã không còn trong route source hiện tại. Không coi tên method cũ `uploadFileToQuestionBank` là endpoint còn tồn tại.

Bằng chứng: [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [Next files GET](../frontend/app/api/moderator/question-files/route.ts), [upload route](../frontend/app/api/storage/question-file/route.ts), [questionFile.entity.ts](../frontend/domain/entities/questionFile.entity.ts), [files page](../frontend/app/(site)/moderator/question-files/page.tsx), [useLogin](../frontend/presentation/hooks/Auth/useLogin.ts), [useNavBar](../frontend/presentation/hooks/layout/useNavBar.ts), [upload UI](../frontend/app/(site)/upload/page.tsx), [StorageService](../frontend/presentation/services/StorageService.ts).

**Không có trong phạm vi đã duyệt:** submission collection, pending/approved/rejected field, reviewer/audit hoặc receipt S3. Email trong filename không thay owner/audit record. Không bảo đảm chống import trùng xuyên moderator/restart/mất phản hồi; tải lại trang mất trạng thái đã import nhưng chưa xóa. AI moderation vẫn là thông báo Telegram và insert/use ngay như phần 8.

## 6. Import DOCX/Pandoc vào ngân hàng

`ImporterService.parseDocument` transaction: validate subject/file → Pandoc workspace `static/uploads/pandoc_<uuid>/input.docx` → process `pandoc -f docx -t json --mathjax --extract-media=...` → upload extracted images vào S3 `media/` → parse metadata title=chapter, subtitle=lesson; heading 1=dạng bài, 2=độ khó, 3=learning outcome, 4=loại câu; paragraphs/body và bullet lists/options → tạo chapter/lesson/bankStats → lưu questions → trả danh sách ID. Workspace local dọn trong finally.

Chapter repository merge theo **chapter name**, không kèm subject; lesson theo name trong chapter; bankStats so dạng bài, loại câu, difficulty/outcomes. Parser hiện tạo một lesson từ subtitle mỗi file. Question content dùng template/variables map math/image. Imported questions được insert thẳng, không có bước approval tiếp.

Bằng chứng: [ImporterService](../backend/src/main/java/com/wedu/exam_creation/importer/service/ImporterService.java), [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java), [helpers](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper), [ChapterRepositoryImpl.saveChapter](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java), [QuestionRepositoryImpl.saveQuestions](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java). Transaction không xóa media S3 đã upload nếu Mongo rollback. Parser merge stat lặp so difficultyLevels thay vì so learningOutcomes; đây là lỗi logic tĩnh cần kiểm chứng bằng fixture, chưa sửa.

## 7. Draft → matrix → matrixDetails

Tạo nháp với tên, tổng số câu, questionTypes; ban đầu chapters rỗng. UI chọn chương/bài, gửi `{add:[{id,name}],del:[id]}`. Owner filter đi từ authenticated principal xuống repository.

`DraftUtil.generateMatrix`: loại câu dùng trọng số Nhiều lựa chọn 30%, Đúng sai 40%, Trả lời ngắn 30%; phần dư dồn loại cuối trong input; difficulty 40/30/20/10 cho Nhận biết/Thông hiểu/Vận dụng/Vận dụng cao, chia dư theo phần thập phân; phân đều vòng tròn theo lessons. Không giả định đây là chính sách giáo dục đã được xác nhận. `generateMatrixDetails` lấy các outcome/dạng bài từ bankStats, tạo tích tổ hợp cho từng ô matrix rồi phân vòng; fallback “Kiến thức tổng hợp”/“Chưa phân loại”. Không giới hạn allocation theo tồn kho câu thực.

Frontend có state/input handler thay count, nhưng handler tiếp tục chỉ gọi generate endpoint, không gửi counts sửa để persist; chưa thấy API lưu matrix chỉnh tay. Bằng chứng: [DraftUsecase](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java), [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java), [DraftRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java), [useMatrix](../frontend/presentation/hooks/Generate/Child/useMatrix.ts), [useMatrixDetails](../frontend/presentation/hooks/Generate/Child/useMatrixDetails.ts). Draft không có status enum hoặc bước khóa/finalize.

## 8. Sinh đề và AI generation

`ExamUsecase.generateExam`: nạp nháp của user → lấy matrixDetails `selectedCount > 0` → chapter service lấy context → `QuestionService.generateExamQuestions`.

Question service nạp pool theo lesson IDs, match lesson/dạng bài/độ khó/loại câu/outcome; shuffle và lấy đủ trong khả năng, xóa câu đã chọn khỏi pool. Ghi errors cho ô thiếu. Nếu `PLUS` và có yêu cầu thiếu: gọi DeepSeek một batch JSON → parse → insert AI questions → Telegram notifyPendingReview → dùng ngay ID mới trong đề. Code chia kết quả theo số lượng/thứ tự request bằng cursor/subList; chưa validate đủ tổng và metadata từng nhóm. Errors thiếu ban đầu không được xóa sau AI bù. Nếu repository không tìm được **bất kỳ** câu nào ở lessons thì ném NotFound trước AI; không coi đây là AI-first generation.

Lưu `ExamEntity` chứa cấu trúc chapter/lesson snapshot và nhóm question IDs theo type/difficulty; không copy toàn bộ body câu hỏi. Generate trả `{examId, errors}`; không có job ID/status retry/cancel. Đọc đề hydrate nội dung từ Questions, generate presigned ảnh, UI render KaTeX. Xóa đề chỉ xóa document Exam, không xóa câu ngân hàng.

Bằng chứng: [ExamUsecase](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java), [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionRepositoryImpl.findByLessonIds](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java), [AIQuestionGenerationService](../backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java), [ExamResponseDTOMapper](../backend/src/main/java/com/wedu/exam_creation/exam/dto/mapper/ExamResponseDTOMapper.java), [TelegramNotificationService](../backend/src/main/java/com/wedu/exam_creation/notification/telegram/TelegramNotificationService.java).

## 9. Xuất Word

UI → Next `/api/exporter/exam` → backend `ExporterController` với `{examId,examName}` → ExamService kiểm tra owner → lấy groups, gom URLs ảnh → tải song song Base64 → tạo `{examName, questionsSorted, imageCache}` → POST `EXPRESS_EXPORTER_URL + "exam"` → Express `/exporter/exam` → builders/docx → MathJax MathML → OMML → JSZip patch XML → binary DOCX quay về browser. Tên file download có thể lấy từ request, tên trong document lấy từ exam backend; không nhầm hai nguồn.

Bằng chứng: [Next export route](../frontend/app/api/exporter/exam/route.ts), [ExporterController](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java), [ExporterUsecase](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java), [Express route](../exporter-service/src/routes/exporter/exporter.route.ts), [GenerateWordFileUsecase](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts), [latexToOmml](../exporter-service/src/routes/exporter/infrastructure/converter/latexToOmml.ts). Ảnh tải lỗi bị log rồi bỏ khỏi cache; builder chưa kiểm cache trước Buffer.from. [OptionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/OptionBuilder.ts) còn kiểm `<image_` trong khi regex split dùng `<img_`; hỗ trợ ảnh option vì vậy là partial. Chưa có transaction/job export; không gọi exporter khi tác vụ chỉ cập nhật tài liệu.

## 10. Giao dịch, PLUS và câu hỏi cần xác nhận

- **Giao dịch dữ liệu implemented:** MongoTransactionManager, annotations trong importer/exam/đổi mật khẩu/repository; không phải payment transaction hoặc distributed transaction. Bằng chứng [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java), `ExamUsecase.generateExam`, `ImporterService.parseDocument`, `AuthUsecase.changePassword`.
- **Thanh toán missing**, UI **partial**: [Bill](../frontend/app/(site)/me/bill/page.tsx) chỉ hiển thị text; chưa thấy gateway, webhook, invoice/payment collection, trạng thái pending/paid/failed/refund hoặc API upgrade account. `accountType` chỉ được tạo FREE/đọc PLUS trong các luồng đã khảo sát. Không suy ra người dùng mua PLUS được bằng app hiện tại.
- **unknown cần chủ dự án xác nhận:** file/câu AI phải được duyệt trước khi dùng hay sau khi dùng? Teacher và moderator liên kết submission ra sao? Admin có nên kiêm moderator? Ai cấp PLUS, thời hạn/quota thế nào? Sinh thiếu có được coi thành công? Chính sách matrix mặc định và hỗ trợ Tự luận có chủ đích gì? Reset password có cần thu hồi toàn bộ phiên? Chapter name có duy nhất xuyên môn không? “Giao dịch” mong muốn là thanh toán hay transaction dữ liệu?

## Xem trước tệp câu hỏi chỉ đọc — 2026-10-25

Moderator click dòng/tên tệp ở /moderator/question-files → modal chỉ đọc → tải DOCX qua [API content](api-reference.md) → render bằng docx-preview trong iframe cách ly. Tải khi mở, có loading/error/Thử lại; Đóng hoặc Escape (kể cả khi focus ở iframe) đóng và khôi phục focus. Nút Tải xuống/Xóa ở danh sách không mở preview. Chuyển/đóng file hủy request, không dùng kết quả render cũ.

Preview không có toolbar soạn thảo trực tiếp; moderator tải xuống sửa ngoài hệ thống rồi chọn DOCX chỉnh sửa để xem trước và duyệt theo phần 5. Bố cục/font/ngắt trang có thể khác Word; có Tải xuống file gốc. Trạng thái persisted submission/approval vẫn missing như trước. [Dialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx), [hook](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts).

## Backend cung cấp nội dung xem trước — 2026-10-25

Moderator gọi GET /moderator/question-files/content với key từ danh sách → backend kiểm role/key/prefix/.docx → đọc object S3 → trả binary DOCX no-store. Object không còn trả 404; lỗi storage khác trả 500 chung. Đây là đọc nội dung, không sửa/lưu/import tài liệu hoặc chuyển trạng thái duyệt. [Usecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [API](api-reference.md).
