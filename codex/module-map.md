# Bản đồ module, symbols và API

## Trạng thái câu hỏi — 2026-10-26

QuestionService.insertFromDocx(List<NewQuestionDTO>) → private insert(List<NewQuestionDTO>, String status) → IQuestionRepository.saveQuestions → QuestionMapper.toDocument → Questions.status. Parse dùng APPROVED, generateExamQuestions truyền PENDING_REVIEW cho AI. toEntity giữ status; legacy null. Không enum/route/HTTP DTO mới.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

## Kiểm duyệt S3 — bổ sung 2026-10-25

- QuestionFilesPage → QuestionFilePreviewDialog/QuestionFileReviewForm → useQuestionFileReview → ModeratorService → Next approve/reject → ApproveQuestionFileUsecase/RejectQuestionFileUsecase → IModeratorRepository/ModeratorRepositoryImpl → ModeratorController → [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java).
- Approve: S3Service.readDocumentFile → StoredDocumentFile hoặc multipart chỉnh sửa → S3Service.validateDocumentFile → ModeratorUsecase.parseDocument → importer frozen → deleteFile. DTO {questionIds,fileDeleted,message}; fileDeleted=false chỉ retry xóa trong phiên UI.
- Reject: RejectQuestionFileDTO {key,reason} → validate email/lý do → kiểm tra tồn tại → deleteFile → MailService.sendQuestionFileRejectionEmail. Email lỗi chỉ log, không Telegram, vẫn 204.
- useQuestionFilePreview dùng chung renderer/sandbox cho key S3 hoặc File cục bộ; useQuestionFiles quản lý refresh/cache và cleanupKeys trong bộ nhớ. Không đổi schema hoặc thêm receipt/audit.

[API](api-reference.md#duyệttừ-chối-tệp--2026-10-25), [kiểm tra](current-status.md#kiểm-duyệt-tệp-s3--2026-10-25). Các dòng snapshot bên dưới mô tả trước khi bổ sung workflow này.


Snapshot working tree 2026-10-24. Danh mục declaration/API lấy từ source, không từ endpoint server đang chạy. Endpoint tồn tại không chứng minh end-to-end pass; [current-status.md](current-status.md) ghi trạng thái và hạn chế. Links source dùng đường dẫn tương đối từ codex/.

## 1. Entry points và ranh giới

`QuestionFilesPage` render từng tệp qua [QuestionFileRow](../frontend/presentation/components/Moderator/QuestionFileRow.tsx). Component nhận dữ liệu đã định dạng, `isDeleting` và `onDelete`; chỉ hiển thị dòng bảng và chuyển sự kiện xóa về hook, không gọi API.

Xóa question file: `QuestionFilesPage → useQuestionFiles → ModeratorService.deleteQuestionFile → Next DELETE → DeleteQuestionFileUsecase → IModeratorRepository.deleteQuestionFile → ModeratorRepositoryImpl → ModeratorController.deleteQuestionFile → ModeratorUsecase.deleteQuestionFile → S3Service.deleteFile → S3Client.deleteObject`. [useQuestionFiles](../frontend/presentation/hooks/Moderator/useQuestionFiles.ts) quản lý query/mutation, state/ref hộp thoại, cache, thông báo và handler; trả files có sizeLabel/lastModifiedLabel để page chỉ render. Chỉ moderator, chỉ prefix `question-files/`; không đổi database hoặc API. [Contract](api-reference.md), [hộp thoại](../frontend/presentation/components/Moderator/DeleteQuestionFileDialog.tsx), [usecase frontend](../frontend/usecases/moderator/DeleteQuestionFileUsecase.ts).

| Module | Entry point | Cấu trúc/contract |
| --- | --- | --- |
| Frontend | [app/layout.tsx](../frontend/app/layout.tsx), [app/page.tsx](../frontend/app/page.tsx), [proxy.ts](../frontend/proxy.ts) | Pages/components → presentation hooks → presentation services → Next route handlers → usecases → domain repository → Axios adapters. Kiểm tra call site vì còn service/helper cũ. |
| Backend | [BackendApplication](../backend/src/main/java/com/wedu/exam_creation/BackendApplication.java) | Feature controllers/usecases, domain entities/repositories, DTOs, Mongo documents/adapters; facade services/common DTOs liên feature. |
| Exporter | [src/app.ts](../exporter-service/src/app.ts), [exporter.route.ts](../exporter-service/src/routes/exporter/exporter.route.ts) | Express JSON → controller → GenerateWordFileUsecase → builders → LaTeX converter; trả DOCX bytes. |

## 2. Quan hệ feature backend

| Feature | Symbols chính và dependency |
| --- | --- |
| auth | `AuthController → AuthUsecase → UserService, SecurityService, RefreshTokenService, PasswordResetTokenService, AuthDTOMapper` |
| security | `SecurityConfig → JwtAuthenticationFilter, CustomOAuth2UserService, OAuth2AuthenticationSuccessHandler`; JWT provider + user details/principal; SecurityService facade BCrypt/JWT |
| user | `UserController → UserUsecase → UserRepositoryImpl, UserDTOMapper, UserCommonDTOMapper, RefreshTokenService, S3Service, TelegramNotificationService`; UserService facade |
| admin | `AdminController → AdminUsecase → UserService, UserCommonDTOMapper` |
| chapter | `ChapterController → ChapterUsecase → IChapterRepository, ChapterDTOMapper`; ChapterService insert/getByIds cho importer/draft/exam |
| draft | `DraftController → DraftUsecase → IDraftRepository, ChapterService, DraftDTOMapper, DraftUtil`; DraftService get/delete cho feature khác |
| question | `QuestionService → IQuestionRepository, QuestionDTOMapper, AIQuestionGenerationService, TelegramNotificationService`; không có QuestionController CRUD riêng |
| exam | `ExamController → ExamUsecase/ExamService → IExamRepository, QuestionService, DraftService, ChapterService, response mappers, S3Service` |
| importer/moderator | `ModeratorController → ModeratorUsecase → ImporterService` cho parse; `ModeratorUsecase → S3Service.listQuestionFiles` cho list/download metadata. UI moderator và Next GET dùng GetQuestionFilesUsecase; parse và review chưa được nối bằng submission record. |
| exporter | `ExporterController → ExporterUsecase → ExamService, RestTemplate → Express`; tải ảnh bằng CompletableFuture |
| refreshToken | `RefreshTokenService → IRefreshTokenRepository/RefreshTokenRepositoryImpl, RefreshTokenDTOMapper` |
| passwordResetToken/mail | `PasswordResetTokenService → PasswordResetTokenUsecase → UserService, IPasswordResetTokenRepository, SecurityService, MailService → JavaMailSender` |
| storage/notification | `StorageController → S3Service → S3Client/S3Presigner/Tika`; TelegramNotificationService → RestTemplate |

Danh mục source từng feature trong source-catalog.md là bằng chứng cho symbols/dependencies; đối chiếu constructor nếu đổi wiring. Không giả định mọi class `Service` chỉ là facade: QuestionService chứa nghiệp vụ lớn; UserUsecase inject implementation cụ thể.

## 3. Contract liên tầng cần giữ

| Contract | Source cần đọc cùng nhau |
| --- | --- |
| Auth input/user/cookies | frontend `presentation/schemas/auth.schema.ts`, `domain/entities/auth.entity.ts`, `AuthRepositoryImpl`; backend `AuthController`, `AuthUsecase`, `auth/dto`, `common/dto/user`; không trả token plaintext ra UI theo type AuthorizedResponseEntity |
| Draft create/update | frontend `domain/entities/draft.entity.ts`, schema, DraftRepositoryImpl; backend `CreateDraftDTO`, `UpdateDraftDTO/UpdateParamDTO`, domain payloads, DraftDTO/common draft DTO |
| Generate exam | `ExamController.generateExam` → `ExamGeneratedResponseDTO {examId,errors}` ↔ frontend `ExamGeneratedResponseEntity`; backend HTTP 200 trong controller, Next route trả 201 |
| Exam content | backend common `ExamDetailDTO/ExamQuestionDTO/QuestionDetailResponseDTO`, frontend `exam.entity.ts`, response mapper, UI transform helpers |
| Question placeholders | common `ContentDTO/VariablesDTO`, question entity/document, importer DTO/helpers, frontend QuestionContent, exporter ContentDTO/builders; `<math_N>` và `<img_N>` |
| Parse | Java ModeratorController trả List<String>; frontend IModeratorRepository, ModeratorRepositoryImpl, ParseUsecase, ModeratorService trả Promise<string[]> ID câu hỏi. Next route chuyển nguyên JSON array. |
| User profile | Java `CommonUserResponseDTO.isActive` vs frontend `UserEntity.isActived`; đối chiếu JSON runtime trước sửa |
| Chapter list | Java LessonDataResponseDTO trả bankStats; frontend LessonData khai báo flat exerciseTypes/difficultyLevels/learningOutcomes/questionTypes; ChapterRepositoryImpl chưa transform |
| Export Java → Express | Hai `WordPayloadRequestDTO`: examName, questionsSorted[{questionType,questionsData[{id,question,options}]}], imageCache{questions,options}; base64 maps keyed theo image URL |

### Contract bổ sung 24/10

| Contract | Source/behavior cần đọc cùng nhau |
| --- | --- |
| Question files | S3ObjectInfo ↔ QuestionFileEntity; GET /moderator/question-files ↔ Next /api/moderator/question-files; response key/fileName/size/lastModified/presignedUrl. ModeratorLayout chặn UI theo role, backend @PreAuthorize chặn API. |
| DOCX upload | POST /storage/question-file ↔ Next /api/storage/question-file; multipart file/subject, response string key thực tế; StorageService.uploadFileToQuestionBank hiện khai báo string. Backend dùng updateDocumentFile → uploadUniqueDocumentFile, 50 MiB; tên trùng thêm `(n)` bằng S3 conditional PUT; UI text 50 MB. |
| Avatar | Upload image tách storage (5 MiB); user adapter hiện gọi sai /user/avatar/${s3Key} so với backend /user/update-avatar. Hook new QueryClient và key user-profile còn lệch useAuth user. |

## 4. API backend và Next.js

Quyền: mặc định backend authenticated; exceptions theo `SecurityConstants.PUBLIC_PATHS`; admin/moderator theo annotation controller/method, owner theo principal/usecase/repository. Next API thường đọc cookie rồi chuyển Bearer. HTTP method Next có thể khác backend: ví dụ change password/profile PUT → PATCH, delete thêm segment `/delete`. Đọc cả hai bảng và repository tương ứng.

API response lỗi đa số `{message}`, bean validation Java là field map; route Next có thể bọc/đổi status. Không xem TypeScript type là runtime validation response. [GlobalExceptionHandler](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java), [frontend repositories](../frontend/infrastructure/repositories).

Danh mục tách theo chủ đề để tránh nạp toàn bộ source index mỗi phiên:

- [api-reference.md](api-reference.md): 36 Spring endpoints, 35 Next handlers, Express route, signatures và wiring.
- [source-catalog.md](source-catalog.md): toàn bộ backend domain/DTO/repository/usecase/controller/helpers, frontend pages/ports/adapters/hooks/services/components và exporter files.
- [database-schema.md](database-schema.md): 7 collection declarations, 20 document classes (7 root, 13 embedded) và 99 field declarations.

## Luồng xem trước DOCX — 2026-10-25

QuestionFileRow.onPreview → useQuestionFiles.previewFile → [QuestionFilePreviewDialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx) → [useQuestionFilePreview](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts) → ModeratorService.getQuestionFileContent → [Next GET /api/moderator/question-files/content](../frontend/app/api/moderator/question-files/content/route.ts) → GetQuestionFileContentUsecase → IModeratorRepository/ModeratorRepositoryImpl → GET /moderator/question-files/content → ModeratorController/ModeratorUsecase.getQuestionFileContent → S3Service.readDocumentFile. Contract binary DOCX, key query, ROLE_MODERATOR; [API](api-reference.md). Không đổi model/database hoặc parse flow.

## Backend question-file content — 2026-10-25

GET /moderator/question-files/content?key=... → [ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) → [ModeratorUsecase.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) → [S3Service.readDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) → S3Client.getObjectAsBytes. ROLE_MODERATOR, binary DOCX no-store; không đổi DTO/schema. [Contract](api-reference.md).
