# API reference WEdu

## Duyệt/từ chối tệp — 2026-10-25

Backend `ROLE_MODERATOR`, Next BFF lấy cookie accessToken và chuyển Bearer chỉ phía server; cache private/no-store. Giữ list/content/delete/upload/parse cũ.

| Backend → Next | Request | Response |
| --- | --- | --- |
| POST /moderator/question-files/approve → POST /api/moderator/question-files/approve | multipart key:string, file:DOCX tùy chọn; không nhận subject từ client | 200 ApproveQuestionFileResponseDTO {questionIds:string[], fileDeleted:boolean, message:string} |
| POST /moderator/question-files/reject → POST /api/moderator/question-files/reject | JSON RejectQuestionFileDTO {key:string, reason:string} | 204, kể cả email gửi thất bại sau xóa |

Key phải đúng `question-files/<subject>/<filename>.docx`, không segment ./.. hoặc subject rỗng. Approve xác nhận tệp gốc tồn tại; bytes bản gốc hoặc bản chỉnh sửa được validate không rỗng, ≤50 MiB, extension DOCX và ZIP entries, rồi gọi parseDocument với subject từ key. Parse lỗi 500 giữ gốc; parse thành công/xóa lỗi vẫn 200 với fileDeleted=false để client chỉ retry DELETE, không approve lại trong phiên hiện tại.

Reject lý do trim 1–2000 ký tự; email từ basename trước dấu '-' cuối, validate địa chỉ trước khi xóa. Tên chuẩn hóa không chứa '-', email có '-' vẫn giữ. Key/email/reason không hợp lệ 400, sai role 403, token không hợp lệ 401, tệp mất 404, lỗi storage 500. Email lỗi chỉ WARN loại exception, không retry/Telegram và không đổi 204. Next thiếu cookie 401, mất kết nối 502, giữ status backend với message chung, không lộ chi tiết provider. Không có idempotency bền vững, lịch sử hoặc status database.

[ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [approve DTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ApproveQuestionFileResponseDTO.java), [reject DTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectQuestionFileDTO.java), [Next approve](../frontend/app/api/moderator/question-files/approve/route.ts), [Next reject](../frontend/app/api/moderator/question-files/reject/route.ts).


Snapshot source cập nhật 2026-10-25. Có 40 endpoint Spring, 39 handler Next và 1 endpoint Express theo declaration hiện tại; không phải kết quả gọi server. Backend/frontend kiểm duyệt tệp đã được merge vào main. [Module map](module-map.md), [business flows](business-flows.md), [current status](current-status.md).

## Contract cần lưu ý

### Xóa tệp tin câu hỏi — 2026-10-25

`DELETE /moderator/question-files?key=<S3 key>` yêu cầu ROLE_MODERATOR; Next proxy cùng query tại `DELETE /api/moderator/question-files`. Cookie access token chỉ chuyển thành Bearer tại adapter phía server. Key lấy nguyên từ danh sách, không lấy presigned URL hoặc suy từ fileName; Axios params encode key đúng một lần. Thành công `204 No Content`, kể cả object đã không còn; không gọi HEAD trước DELETE. `400` cho key rỗng/ngoài `question-files/`, key thư mục hoặc segment `.`/`..`; AWS lỗi trả `500` với message chung, không trả chi tiết AWS. Quyền JWT giữ nguyên (`403` sai role, `401` token rỗng/không hợp lệ; thiếu/sai định dạng header vẫn theo filter hiện có). Next thiếu cookie trả `401`, mất kết nối backend trả `502`.

Nguồn: [ModeratorController.deleteQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [ModeratorUsecase.deleteQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [Next handler](../frontend/app/api/moderator/question-files/route.ts), [DeleteQuestionFileUsecase](../frontend/usecases/moderator/DeleteQuestionFileUsecase.ts). Không thay DTO danh sách, database hoặc API upload.

- Moderator parse: multipart file/subject, HTTP 200 JSON string[] ID câu hỏi, đồng bộ List<String> Java với Promise<string[]> xuyên frontend. Không có parse-by-S3-key hoặc approval endpoint.
- Question files: GET /moderator/question-files yêu cầu MODERATOR, trả S3ObjectInfo[]; Next GET /api/moderator/question-files dùng private/no-store, 401 thiếu access cookie, chuyển Bearer tới backend. Frontend QuestionFileEntity chứa key/fileName/size/lastModified/presignedUrl. URL tải ký hạn 60 phút với attachment.
- DOCX: POST /storage/question-file và Next /api/storage/question-file trả string S3 key thực tế, multipart file/subject; backend lấy email từ principal. Key mặc định `question-files/<subject>/<email>-<NormalizedName>.docx`; từ 2026-10-25, tên trùng thêm `(1)`, `(2)`… trước `.docx`, chọn số nhỏ nhất còn trống bằng conditional PUT. Không thay method/path/payload/response type; caller phải dùng key trả về, không tự dựng từ tên upload. StorageService.uploadFileToQuestionBank hiện typed string. Backend kiểm DOCX ≤50 MiB, UI text 50 MB. [S3Service.updateDocumentFile/uploadUniqueDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [tests](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java).
- Không còn declaration POST /storage/question-bank, Next /api/storage/question-bank, GET /storage/documents. Test preservesLegacyDocumentsEndpoint còn mong route cuối trả 200; chưa chạy test trong lượt tài liệu này.
- Avatar backend PATCH /user/update-avatar; frontend adapter hiện gọi /user/avatar/${s3Key}, đây là mismatch còn lại. Signature/type frontend không chứng minh remote JSON đúng schema.

Nguồn: [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [Next files GET](../frontend/app/api/moderator/question-files/route.ts), [QuestionFileEntity](../frontend/domain/entities/questionFile.entity.ts), [StorageService](../frontend/presentation/services/StorageService.ts), [ModeratorQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java).

## Spring MVC endpoints

Mặc định authenticated; public paths theo SecurityConstants, quyền class/method theo @PreAuthorize, owner query theo principal/usecase/repository. Authority MODERATOR không tự bao gồm ADMIN nếu không có RoleHierarchy.

| Method + path | Symbol / source | Input → output (Java signature) |
| --- | --- | --- |
| `GET /admin/users` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `getAllUsers` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<CommonUserResponseDTO>>` |
| `GET /admin/users/search` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `findUsers` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(name = "keyword") String keyword → ResponseEntity<List<CommonUserResponseDTO>>` |
| `PUT /admin/{userId}/role` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `updateUserRole` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId, @RequestParam("role") String role → ResponseEntity<CommonUserResponseDTO>` |
| `PUT /admin/{userId}/lock` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `lockUser` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId → ResponseEntity<CommonUserResponseDTO>` |
| `PUT /admin/{userId}/unlock` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `unLockUser` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId → ResponseEntity<CommonUserResponseDTO>` |
| `DELETE /admin/{userId}` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) · `deleteUser` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String userId → ResponseEntity<Boolean>` |
| `POST /auth/register` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `register` | `@Valid @RequestBody NewUserRequestDTO newUser, HttpServletResponse response → ResponseEntity<UserResponseDTO>` |
| `POST /auth/login` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `login` | `@Valid @RequestBody LoginUserRequestDTO payload, HttpServletResponse response → ResponseEntity<UserResponseDTO>` |
| `POST /auth/logout` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `logout` | `@RequestHeader("Authorization") String authorization → ResponseEntity<Boolean>` |
| `PATCH /auth/change-password` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `changePassword` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody ChangePasswordRequestDTO reqPayload, @RequestHeader("Authorization") String authorization → ResponseEntity<Boolean>` |
| `POST /auth/regenerate-access-token` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `regenerateAccessToken` | `@CookieValue(value = "refreshToken") String refreshToken → ResponseEntity<NewAccessTokenResponseDTO>` |
| `POST /auth/forgot-password` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `forgotPassword` | `@RequestParam("email") String email → ResponseEntity<String>` |
| `POST /auth/reset-password` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) · `resetPassword` | `@Valid @RequestBody ResetPasswordRequestDTO dto → ResponseEntity<String>` |
| `GET /chapter/chapters` | [ChapterController](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java) · `getAll` | ` → ResponseEntity<List<ChapterResponseDTO>>` |
| `POST /draft/create` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `createDraft` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody CreateDraftDTO payload → ResponseEntity<String>` |
| `GET /draft/{draftId}` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `getDraft` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<DraftDTO>` |
| `PUT /draft/{draftId}/chapter` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `updateChapters` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId, @RequestBody UpdateDraftDTO payload → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/{chapterId}/lesson` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `updateLessons` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId, @PathVariable("chapterId") String chapterId, @RequestBody UpdateDraftDTO payload → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/generate-matrix` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `generateMatrix` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/generate-matrix-details` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `generateMatrixDetails` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId → ResponseEntity<Boolean>` |
| `GET /draft/recent` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `getRecentDraft` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<DraftDTO>>` |
| `GET /draft/user-drafts` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `getAllUserDrafts` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<DraftDTO>>` |
| `DELETE /draft/{draftId}` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) · `deleteDraft` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<Boolean>` |
| `POST /exam/generate/{draftId}` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) · `generateExam` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<ExamGeneratedResponseDTO>` |
| `GET /exam/{examId}` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) · `getExamById` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String examId → ResponseEntity<ExamDetailDTO>` |
| `GET /exam/user-exams` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) · `getAllExams` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<ExamDTO>>` |
| `GET /exam/recent` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) · `getRecentExam` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<ExamDTO>>` |
| `DELETE /exam/{examId}` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) · `deleteExam` | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String examId → ResponseEntity<Boolean>` |
| `POST /exporter/exam` | [ExporterController](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java) · `exportExam` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody ExportRequestDTO payload → ResponseEntity<Resource>` |
| `GET /moderator/question-files` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) · `listQuestionFiles` | ` → ResponseEntity<List<S3ObjectInfo>>` |
| `GET /moderator/question-files/content` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) · `getQuestionFileContent` | `@RequestParam("key") String key → ResponseEntity<byte[]>`, DOCX binary |
| `DELETE /moderator/question-files` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) · `deleteQuestionFile` | `@RequestParam("key") String key → ResponseEntity<Void>`, 204 |
| `POST /moderator/parse` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) · `parseDocument` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("file") MultipartFile file, @RequestParam("subject") String subject → ResponseEntity<List<String>>` |
| `POST /storage/avatar` | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) · `uploadAvatar` | `@RequestParam("file") MultipartFile file → ResponseEntity<String>` |
| `POST /storage/question-file` | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) · `uploadDocumentToQuestionBank` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("file") MultipartFile file, @RequestParam("subject") String subject → ResponseEntity<String>` |
| `GET /user/me` | [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) · `getMe` | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<CommonUserResponseDTO>` |
| `PATCH /user/update-avatar` | [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) · `updateAvatar` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("s3Key") String s3Key → ResponseEntity<Boolean>` |
| `PATCH /user/update-username` | [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) · `updateUser` | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("username") String username → ResponseEntity<Boolean>` |

## Next.js route handlers

Methods/paths có thể khác backend, ví dụ Next PUT profile đổi thành Spring PATCH. Cột wiring là imports được xác minh, không phải runtime trace.

| Method + path | Source | Wiring imports |
| --- | --- | --- |
| `DELETE /api/admin/[userId]/delete` | [app/api/admin/[userId]/delete/route.ts](../frontend/app/api/admin/[userId]/delete/route.ts) | `@/infrastructure/repositories/AdminRepositoryImpl`, `@/usecases/admin/DeleteUserUsecase` |
| `PUT /api/admin/[userId]/lock` | [app/api/admin/[userId]/lock/route.ts](../frontend/app/api/admin/[userId]/lock/route.ts) | `@/infrastructure/repositories/AdminRepositoryImpl`, `@/usecases/admin/LockUserUsecase` |
| `PUT /api/admin/[userId]/role` | [app/api/admin/[userId]/role/route.ts](../frontend/app/api/admin/[userId]/role/route.ts) | `@/usecases/admin/UpdateUserRoleUsecase`, `@/infrastructure/repositories/AdminRepositoryImpl` |
| `PUT /api/admin/[userId]/unlock` | [app/api/admin/[userId]/unlock/route.ts](../frontend/app/api/admin/[userId]/unlock/route.ts) | `@/usecases/admin/UnlockUserUsecase`, `@/infrastructure/repositories/AdminRepositoryImpl` |
| `GET /api/admin/users` | [app/api/admin/users/route.ts](../frontend/app/api/admin/users/route.ts) | `@/usecases/admin/GetAllUsersUsecase`, `@/infrastructure/repositories/AdminRepositoryImpl` |
| `GET /api/admin/users/search` | [app/api/admin/users/search/route.ts](../frontend/app/api/admin/users/search/route.ts) | `@/usecases/admin/FindUsersUsecase`, `@/infrastructure/repositories/AdminRepositoryImpl` |
| `PUT /api/auth/change-password` | [app/api/auth/change-password/route.ts](../frontend/app/api/auth/change-password/route.ts) | `@/usecases/auth/ChangePasswordUsecase`, `@/infrastructure/repositories/AuthRepositoryImpl` |
| `POST /api/auth/forgot-password` | [app/api/auth/forgot-password/route.ts](../frontend/app/api/auth/forgot-password/route.ts) | `@/usecases/auth/ForgotPasswordUsecase`, `@/infrastructure/repositories/AuthRepositoryImpl` |
| `POST /api/auth/login` | [app/api/auth/login/route.ts](../frontend/app/api/auth/login/route.ts) | `@/infrastructure/repositories/AuthRepositoryImpl`, `@/usecases/auth/LoginUsecase` |
| `POST /api/auth/logout` | [app/api/auth/logout/route.ts](../frontend/app/api/auth/logout/route.ts) | `@/infrastructure/repositories/AuthRepositoryImpl`, `@/usecases/auth/LogoutUsecase` |
| `POST /api/auth/register` | [app/api/auth/register/route.ts](../frontend/app/api/auth/register/route.ts) | `@/usecases/auth/RegisterUsecase`, `@/infrastructure/repositories/AuthRepositoryImpl` |
| `POST /api/auth/reset-password` | [app/api/auth/reset-password/route.ts](../frontend/app/api/auth/reset-password/route.ts) | `@/infrastructure/repositories/AuthRepositoryImpl`, `@/usecases/auth/ResetPasswordUsecase` |
| `GET /api/chapter` | [app/api/chapter/route.ts](../frontend/app/api/chapter/route.ts) | `@/usecases/chapter/GetAllChaptersUsecase`, `@/infrastructure/repositories/ChapterRepositoryImpl` |
| `PUT /api/draft/[draftId]/[chapterId]/lesson` | [app/api/draft/[draftId]/[chapterId]/lesson/route.ts](../frontend/app/api/draft/[draftId]/[chapterId]/lesson/route.ts) | `@/infrastructure/repositories/DraftRepositoryImpl`, `@/usecases/draft/UpdateLessonsDraftUsecase` |
| `PUT /api/draft/[draftId]/chapter` | [app/api/draft/[draftId]/chapter/route.ts](../frontend/app/api/draft/[draftId]/chapter/route.ts) | `@/usecases/draft/UpdateChaptersDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `DELETE /api/draft/[draftId]/delete` | [app/api/draft/[draftId]/delete/route.ts](../frontend/app/api/draft/[draftId]/delete/route.ts) | `@/usecases/draft/DeleteDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/generate-matrix` | [app/api/draft/[draftId]/generate-matrix/route.ts](../frontend/app/api/draft/[draftId]/generate-matrix/route.ts) | `@/usecases/draft/GenerateMatrixUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/generate-matrix-details` | [app/api/draft/[draftId]/generate-matrix-details/route.ts](../frontend/app/api/draft/[draftId]/generate-matrix-details/route.ts) | `@/usecases/draft/GenerateMatrixDetailsUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `GET /api/draft/[draftId]` | [app/api/draft/[draftId]/route.ts](../frontend/app/api/draft/[draftId]/route.ts) | `@/usecases/draft/GetDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `POST /api/draft/create` | [app/api/draft/create/route.ts](../frontend/app/api/draft/create/route.ts) | `@/usecases/draft/CreateDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `GET /api/draft/recent` | [app/api/draft/recent/route.ts](../frontend/app/api/draft/recent/route.ts) | `@/usecases/draft/GetRecentDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `GET /api/draft/user-drafts` | [app/api/draft/user-drafts/route.ts](../frontend/app/api/draft/user-drafts/route.ts) | `@/usecases/draft/GetAllUserDraftUsecase`, `@/infrastructure/repositories/DraftRepositoryImpl` |
| `DELETE /api/exam/[examId]/delete` | [app/api/exam/[examId]/delete/route.ts](../frontend/app/api/exam/[examId]/delete/route.ts) | `@/usecases/exam/DeleteExamUsecase`, `@/infrastructure/repositories/ExamsRepositoryImpl` |
| `GET /api/exam/[examId]` | [app/api/exam/[examId]/route.ts](../frontend/app/api/exam/[examId]/route.ts) | `@/usecases/exam/GetExamByIdUsecase`, `@/infrastructure/repositories/ExamsRepositoryImpl` |
| `POST /api/exam/generate/[draftId]` | [app/api/exam/generate/[draftId]/route.ts](../frontend/app/api/exam/generate/[draftId]/route.ts) | `@/usecases/exam/GenerateExamUsecase`, `@/infrastructure/repositories/ExamsRepositoryImpl` |
| `GET /api/exam/recent` | [app/api/exam/recent/route.ts](../frontend/app/api/exam/recent/route.ts) | `@/usecases/exam/GetRecentExamUsecase`, `@/infrastructure/repositories/ExamsRepositoryImpl` |
| `GET /api/exam/user-exams` | [app/api/exam/user-exams/route.ts](../frontend/app/api/exam/user-exams/route.ts) | `@/usecases/exam/GetAllExamsUsecase`, `@/infrastructure/repositories/ExamsRepositoryImpl` |
| `POST /api/exporter/exam` | [app/api/exporter/exam/route.ts](../frontend/app/api/exporter/exam/route.ts) | `@/usecases/exporter/ExportExamUsecase`, `@/infrastructure/repositories/ExporterRepositoryImpl` |
| `POST /api/moderator/parse` | [app/api/moderator/parse/route.ts](../frontend/app/api/moderator/parse/route.ts) | `@/infrastructure/repositories/ModeratorRepositoryImpl`, `@/usecases/moderator/ParseUsecase` |
| `GET /api/moderator/question-files` | [app/api/moderator/question-files/route.ts](../frontend/app/api/moderator/question-files/route.ts) | `@/infrastructure/repositories/ModeratorRepositoryImpl`, `@/usecases/moderator/GetQuestionFilesUsecase` |
| `GET /api/moderator/question-files/content` | [app/api/moderator/question-files/content/route.ts](../frontend/app/api/moderator/question-files/content/route.ts) | `@/infrastructure/repositories/ModeratorRepositoryImpl`, `@/usecases/moderator/GetQuestionFileContentUsecase` |
| `DELETE /api/moderator/question-files` | [app/api/moderator/question-files/route.ts](../frontend/app/api/moderator/question-files/route.ts) | `@/infrastructure/repositories/ModeratorRepositoryImpl`, `@/usecases/moderator/DeleteQuestionFileUsecase` |
| `POST /api/storage/avatar` | [app/api/storage/avatar/route.ts](../frontend/app/api/storage/avatar/route.ts) | `@/usecases/storage/UploadAvatarUsecase`, `@/infrastructure/repositories/StorageRepositoryImpl` |
| `POST /api/storage/question-file` | [app/api/storage/question-file/route.ts](../frontend/app/api/storage/question-file/route.ts) | `@/usecases/storage/UploadFileToQuestionBankUsecase`, `@/infrastructure/repositories/StorageRepositoryImpl` |
| `GET /api/user/me` | [app/api/user/me/route.ts](../frontend/app/api/user/me/route.ts) | `@/usecases/user/GetMeUsecase`, `@/infrastructure/repositories/UserRepositoryImpl` |
| `PUT /api/user/update/avatar` | [app/api/user/update/avatar/route.ts](../frontend/app/api/user/update/avatar/route.ts) | `@/usecases/user/UpdateAvatarUsecase`, `@/infrastructure/repositories/UserRepositoryImpl` |
| `PUT /api/user/update/username` | [app/api/user/update/username/route.ts](../frontend/app/api/user/update/username/route.ts) | `@/usecases/user/UpdateUsernameUsecase`, `@/infrastructure/repositories/UserRepositoryImpl` |

## Express endpoint

POST /exporter/exam → exportAsWord → GenerateWordFileUsecase.execute; [app mount](../exporter-service/src/app.ts), [route](../exporter-service/src/routes/exporter/exporter.route.ts), [controller](../exporter-service/src/routes/exporter/controller/exporterController.ts). Input WordPayloadRequestDTO; output DOCX attachment; JSON limit 50 MB. Entry point không khai báo authentication middleware.

## DOCX preview chỉ đọc — 2026-10-25

- Backend `GET /moderator/question-files/content?key=<S3 key>`: ROLE_MODERATOR, `ModeratorController.getQuestionFileContent → ModeratorUsecase.getQuestionFileContent → S3Service.readDocumentFile`. Trả 200 binary MIME DOCX, `Cache-Control: private, no-store`, `X-Content-Type-Options: nosniff`.
- Key nguyên từ listing, thuộc `question-files/`, không thư mục/segment . hoặc .., đuôi .docx không phân biệt hoa/thường. Key sai/thiếu 400, object không còn 404, S3 lỗi khác 500 với message chung. Quyền JWT giữ nguyên.
- Next `GET /api/moderator/question-files/content?key=...` yêu cầu access cookie; chuyển Bearer chỉ phía server qua [GetQuestionFileContentUsecase](../frontend/usecases/moderator/GetQuestionFileContentUsecase.ts) và adapter. Thiếu cookie 401, backend không kết nối 502; bảo toàn status backend, trả message chung và private/no-store cả lỗi. Không nhận URL do client cung cấp.
- [Next route](../frontend/app/api/moderator/question-files/content/route.ts), [backend controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [tests](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java). Không đổi DTO listing hoặc schema.

## Backend question-file content — 2026-10-25

GET /moderator/question-files/content?key=<S3 key> yêu cầu ROLE_MODERATOR, trả 200 binary DOCX (application/vnd.openxmlformats-officedocument.wordprocessingml.document), Cache-Control private, no-store và X-Content-Type-Options nosniff. Key thuộc question-files/, không thư mục hoặc segment . / .., đuôi .docx không phân biệt hoa/thường. Thiếu/key sai trả 400; S3 404 trả 404; lỗi S3 khác trả 500 với message chung. Không đổi DTO danh sách hoặc schema. [ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java).
