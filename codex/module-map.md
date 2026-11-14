# Bản đồ module, symbols và API

## Migration auth bằng Java — 2026-11-14

AuthMigrationRunner (ConditionalOnProperty auth.migration.enabled=true, Order.HIGHEST_PRECEDENCE) → AuthMigrationUsecase.migrate → IAuthMigrationRepository.findUsers/dropLegacyIndexes/apply → AuthMigrationRepositoryImpl/MongoTemplate. AuthMigrationUserEntity giữ legacy scalar fields/opaque ID; AuthMigrationChangeEntity chỉ chứa ID/email/optional loginMethod, không phải HTTP DTO hay collection mới. AuthIndexConfig có order tiếp sau để kiểm email và tạo indexes. AuthMigrationUsecaseTest/AuthMigrationRepositoryTest/AuthMigrationRunnerTest thay test Node; hai file backend/scripts/prepare-auth-indexes.* đã xóa. [Inventory/validation](auth-java-migration-implementation-report.md).

## Callback Google dưới /auth — 2026-11-14

SecurityConfig.googleSecurityFilterChain: matcher callback `/auth/login/oauth2/code/google`, oauth2Login.redirectionEndpoint.baseUri `/auth/login/oauth2/code/*`. SecurityConstants.PUBLIC_PATHS và application.yml redirect-uri đồng bộ; xử lý qua Spring filter, không thêm controller. GoogleSecurityConfigTest dùng redirect-uri từ YAML; JwtOAuthCallbackFilterTest kiểm public path mới/khóa path cũ và liền kề. [API](api-reference.md), [validation](google-auth-implementation-report.md#callback-google-dưới-auth--2026-11-14).

## Auth OTP/Google — 2026-11-13

- VerificationController.register/metadata/resend/verify/exchange → VerificationUsecase.register/metadata/resend/verify/googleHandoff/exchange → IVerificationRepository.insert/find/reserveEmail/replaceOtp/markReady/failAttempt/consumeOtp/consumeHandoff. VerificationMapper tách domain/document; VerificationResponseDTO, VerifyOtpRequestDTO, GoogleExchangeRequestDTO là HTTP types; VerificationException mang code/status/retryAfter.
- UserService/UserUsecase.hasGoogleLink/linkGoogle → UserRepositoryImpl; UserDocument.email/googleSubject, UserEntity.googleSubject. AuthUsecase.register bị thay bằng VerificationUsecase.register; AuthUsecase.generateToken vẫn phát JWT/lưu refresh. AuthMigrationRunner/Usecase/Repository + AuthIndexConfig quản lý migration Java và indexes.
- EmailNormalizer.normalize/isCanonical dùng chung cho verification, lookup/save user và startup guard. Bỏ findByGoogleSubject; hasGoogleLink(email,subject) kiểm liên kết sau lookup email; UserRepositoryImpl.linkGoogle(id,subject,loginMethod) ghi sub/phương thức trong cùng partial conditional update.
- SecurityConfig.googleSecurityFilterChain riêng OAuth; CustomOAuth2UserService giờ là OIDC loader; OAuth2AuthenticationSuccessHandler chỉ tạo handoff. AuthController không còn register declaration.
- Frontend AuthService.register trả VerificationEntity; IAuthRepository/AuthRepositoryImpl/RegisterUsecase trả AuthFlowResult phía server. IVerificationRepository/VerificationRepositoryImpl/VerificationUsecase thêm metadata/verify/resend/exchange. useRegister chuyển /auth/verify-email; useVerifyEmail giữ countdown/server offset, mutation guard, error/cache/redirect. GoogleAuthButton dùng chung; proxy public đúng routes mới. [API](api-reference.md), [file inventory/validation](google-auth-implementation-report.md).

## Xuất ma trận đề — 2026-11-12

Exam page → `useExam.handleExportMatrix` → `ExportService.exportMatrix` → Next `/api/exporter/matrix` → `ExportMatrixUsecase` → `IExporterRepository.exportMatrix/ExporterRepositoryImpl` → Java `ExporterController.exportMatrix/ExporterUsecase.exportMatrix` → `ExamService.getExamMatrixById/ExamUsecase.getExamMatrixById` → owner-filtered `IExamRepository.getExamById` → `ExamMatrixDTO`. Java POST `export.url + "matrix"` → Express `exportMatrixAsWord` → `GenerateMatrixWordFileUsecase` → `buildMatrixTable/buildMatrixDetailsTable` → DOCX binary.

`ExportMatrixRequestDTO` và frontend `ExportMatrixPayload` kiểm tra đầu vào; `UnprocessableEntityException`/GlobalExceptionHandler trả 422 khi thiếu snapshot. `isMatrixWordPayload` kiểm tra payload Express. Frontend `downloadBlob` dùng chung cho xuất đề và ma trận; hook quản lý khóa thao tác/lỗi Blob JSON. [Danh sách file/symbol](matrix-export-implementation-report.md), [contract](api-reference.md#xuất-ma-trận-đề--2026-11-12).

Style theo mẫu: `GenerateMatrixWordFileUsecase` → [prepareMatrixExport](../exporter-service/src/routes/exporter/usecase/util/MatrixExportData.ts) chuẩn bị `MatrixExportData/MatrixExportRow`, nhóm snapshot và ánh xạ mức độ → builders render header/body/summary dùng chung. Utility có `sumCounts`, `levelTotals`, `pointsForCount`, `percentageForCount`, `formatMatrixNumber`; N từ matrix dùng chung cho hai bảng, không tính điểm trong Java/FE. `buildMatrixTable/buildMatrixDetailsTable` nay nhận MatrixExportData thay raw payload; wire DTO không đổi.

## Refund module — 2026-11-09

- Teacher `/me/bill/refunds` and ADMIN `/admin/transactions/refunds` → reusable RefundManager/RefundDetail → useRefunds → RefundService → Next handleRefund → frontend RefundUsecase → IRefundRepository/RefundRepositoryImpl → backend RefundController/RefundUsecase. Parent transaction/billing pages link the new entrypoints. All BFF responses are private,no-store.
- RefundUsecase.orders/search/get/create/decide/reconcile/reconcileDue coordinates IRefundRepository.find/findAttempt/activeForOrder/insert/update/search/count/due/claimPoll/account/saveAccount/recordAi/lastAiUse and IPaymentRepository.ordersForUser. RefundTransactions.run owns short transaction boundaries. RefundMapper maps RefundEntity/RefundAccountEntity and their documents, including separate EventDocument/CheckDocument snapshots; RefundDTO defines Create/Decision/Item/Page/Order/Orders/Event/Check. No document or HTTP model imports domain snapshot types.
- ExamUsecase.generateExam → QuestionService.generateExamQuestions overload with userId → RefundAiService.begin → teacher gate; persisted questions insert RefundAiReceipts. Moderator AI remains separate. PaymentUsecase.process initializes the same account and records plusStartsAt/refundTracked/plusLegacyProtected for new grants.
- RefundEntitlementService.preview/revoke uses ordered payment grants and UserService.paidPlusExpiry/hasLegacyPlus/revokePaidTime → UserUsecase → UserRepositoryImpl.revokePaidTime. VnpayGateway.refund/validRefund/queryRefund extend provider transport without changing ordinary lookup's read-only contract.
- RefundJobs.reconcile schedules reconciliation; RefundIndexConfig.indexes initializes indexes. Refund events enqueue PaymentEmailEntity.refundId/message; PaymentJobs chooses MailService.sendRefundResultEmail. [API](api-reference.md#refund-apis--2026-11-09), [schema](database-schema.md#refund-persistence--2026-11-09), [complete file inventory](refund-implementation-report.md).

> Cập nhật mới nhất 2026-11-10: phần hoàn tiền ở đầu tài liệu đối chiếu code đã merge vào main `cbcb49a`. Các snapshot theo ngày bên dưới giữ giá trị lịch sử, không chứng nhận deploy hoặc validation mới.

## Snapshot hiện tại — 2026-11-06

Đối chiếu source/config tại commit `abcde6e`. Entry points hiện có:11 Spring controllers,25 App Router pages,49 Next route files/51 HTTP handlers và1 Express route. AdminUsersPage/AiManagementPage, moderator review/editor và Bill/usePayment đều có source; không dùng các mô tả placeholder của snapshot cũ để kết luận hiện trạng.

Các mục có ngày cũ là bằng chứng lịch sử của từng lần triển khai; khi mâu thuẫn, dùng snapshot hiện tại và source được dẫn. Không suy ra production hoặc kiểm thử end-to-end đã đạt từ việc có code.

## Cấu hình giới hạn AI — 2026-11-01

- `AiManagementPage` nối `openBudget/cancelBudget`, trạng thái editing/conflict và budget đã lưu từ `useAiManagement` vào `AiBudgetForm`. Component render summary và chỉ mở form khi chọn Cấu hình giới hạn.
- Hook quản lý draft, validation riêng, mutation guard và conflict đến khi reload thành công. Save cập nhật budget cache, đóng form và invalidate status; lỗi status sau lưu được page phân biệt với lỗi lưu.
- `AiStatusPanel/LimitProgress` hiển thị thêm giới hạn cấu hình bên cạnh usage/remaining. Giữ GET/PUT budget, GET status, DTO/schema và backend enforcement hiện có.

[Inventory](source-catalog.md#cấu-hình-giới-hạn-ai--2026-11-01), [luồng](business-flows.md#cấu-hình-giới-hạn-ai--2026-11-01).

## Khu vực ADMIN hai tính năng — 2026-11-01

`getRoleHome` là mapping dùng chung cho `useLogin` và `Home`: ADMIN → /admin/users, MODERATOR → /moderator/question-files, các role khác → /. AdminPage (/admin) redirect tới /admin/users, AdminLayout vẫn kiểm ROLE_ADMIN.

AdminUsersPage → useAdminUsers → AdminService → GET users/search, PUT role/lock/unlock, DELETE user BFF → usecases admin hiện có → IAdminRepository/AdminRepositoryImpl → AdminController/AdminUsecase. AdminUserEntity tách khỏi UserEntity, khớp isActive của backend. AdminUserRow render dữ liệu/thao tác, AdminUserActionDialog giữ native dialog/focus/Escape; hook quản lý query/search/client pagination/mutation/confirmation/ref chống submit lặp. Navbar phân nhánh ADMIN với đúng hai links, useNavBar ẩn billing theo role.

[Types](../frontend/domain/entities/adminUser.entity.ts), [hook](../frontend/presentation/hooks/Admin/useAdminUsers.ts), [page](<../frontend/app/(site)/admin/users/page.tsx>), [API](api-reference.md#khu-vực-admin-hai-tính-năng--2026-11-01).


## Quản lý usage/budget AI — 2026-11-01

- UI: `AdminLayout` kiểm role → `AiManagementPage` → `useAiManagement` (filters/query/form/revision/ref chống submit lặp) → `AiManagementService` → Next handlers usage/status/budget → `handleAiManagement` → frontend `AiManagementUsecase` → `IAiManagementRepository/AiManagementRepositoryImpl`.
- Backend: `AiManagementController` `/admin/ai` ROLE_ADMIN → `AiManagementUsecase.report/status/getBudget/updateBudget` → `IAiUsageRepository/AiUsageRepositoryImpl` → AiUsage/AiBudget. Mapper `AiUsageMapper` tách document/entity; `AiManagementDTO` chứa BudgetDTO, DailyUsageDTO, UsageReportDTO, BalanceInfoDTO, BalanceDTO, StatusDTO.
- AI chung: `AIQuestionGenerationService.generateQuestions` → `AiUsageService.begin/complete`; `AiManagementUsecase.status` → `DeepSeekBalanceService.getBalance`. `AiPricingProperties` map model → giá cao điểm USD. `AiAccessException` → `GlobalExceptionHandler.handleAiAccess`, hai BFF tạo đề/review giữ code/status.
- Reusable UI: `AiMetricCard`, `AiUsageChart`, `AiStatusPanel` và `AiBudgetForm`; formatting tại utils/aiManagement. Backend repository aggregation theo ngày Việt Nam, không để query hoặc nghiệp vụ trong component/controller.

[API/DTO](api-reference.md#quản-lý-usagebudget-ai--2026-11-01), [inventory từng file](source-catalog.md#quản-lý-usagebudget-ai--2026-11-01), [schema](database-schema.md#quản-lý-usagebudget-ai--2026-11-01).

## Kiểm duyệt câu hỏi AI — 2026-10-31

- Approve/reject: ReviewPage → PendingQuestionCard → usePendingQuestions.handleAction → ModeratorService.reviewPendingQuestion → Next POST /api/moderator/questions/[id]/[action] → frontend ReviewPendingQuestionUsecase.execute → IModeratorRepository/ModeratorRepositoryImpl.reviewPendingQuestion → ModeratorController.approvePendingQuestion/rejectPendingQuestion → backend ReviewPendingQuestionUsecase.approve/reject.
- Điều phối: QuestionService.getPendingForEdit/commitPendingReview → IQuestionRepository.updatePendingState → MongoTemplate; QuestionReviewService.state/saveCandidate/delete → QuestionReviewUsecase.states/saveCandidate/delete → IQuestionReviewRepository.findByIds/ensureExists/update/delete → QuestionsReview. Review coordinator dùng ChapterService.getAll/AIQuestionGenerationService.generateQuestions và TransactionTemplate.
- Read: QuestionService.findPending lấy Questions rồi batch review states, attachReview thêm count/revision/latestRegeneratedQuestion; ModeratorUsecase ký ảnh của cả bản gốc và snapshot.
- Edit: usePendingQuestionEdit gửi reviewRevision; EditPendingQuestionUsecase dùng snapshot cuối làm baseline, gọi QuestionService.updatePendingContent overload 5 tham số có transaction. Có candidate thì chỉ touch version gốc và lưu snapshot; không có thì giữ update content cũ.
- DTO mới: ReviewPendingQuestionDTO, RejectPendingQuestionResponseDTO, common QuestionReviewStateDTO; PendingQuestionDTO thêm count/revision/latest; QuestionReviewMapper thêm toDTO/toSnapshot. Models review thêm regenerationCount/revision. Không đổi exam/exporter.
- AIQuestionGenerationService.buildPrompt → private getStringBuilder dựng đặc tả các GenerateQuestionRequestDTO; phần gọi AI và định dạng kết quả giữ nguyên. Commit backend/test/frontend đã merge vào main tại d3ad7a3; validation mới nhất ở current-status, gồm một test failure còn tồn tại.

[Inventory và mô tả từng file](source-catalog.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31), [API](api-reference.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31).


## Chỉnh sửa câu hỏi — 2026-10-29

ReviewPage → PendingQuestionCard → PendingQuestionEditForm → QuestionContentEditor/usePendingQuestionEdit → ModeratorService.editPendingQuestion → Next PATCH `/api/moderator/questions/[id]` → frontend EditPendingQuestionUsecase → IModeratorRepository/ModeratorRepositoryImpl.editPendingQuestion → ModeratorController.editPendingQuestion → backend EditPendingQuestionUsecase.execute → QuestionService.getPendingForEdit/updatePendingContent → IQuestionRepository.findById/updatePendingContent → QuestionRepositoryImpl. DTO request EditPendingQuestionDTO; common PendingQuestionDTO bổ sung updatedAt; QuestionDTOMapper.contentDTOToEntity map vào domain trước adapter.

Edit usecase phối hợp S3Service.uploadImageFile/deleteFile. QuestionImageReference.isStoredKey dùng chung với ModeratorUsecase.signContent để phân biệt mô tả và key. ReferencedContent render math/image/description dùng lại từ QuestionContent, MultipleChoiceContent và preview editor; utils questionContentEditor tách parser/serialization/validation khỏi component. ConflictException → GlobalExceptionHandler trả 409. Các endpoint cũ và phân trang 10 câu giữ nguyên. [API](api-reference.md), [inventory](source-catalog.md).

## Giao diện review — 2026-10-27

`ReviewPage` → `usePendingQuestions` → `ModeratorService.getPendingQuestions` → Next GET `/api/moderator/questions/pending` → `GetPendingQuestionsUsecase` → `IModeratorRepository/ModeratorRepositoryImpl` → `ModeratorController.listPendingQuestions` → `ModeratorUsecase.listPendingQuestions` → `QuestionService.findPending` → `IQuestionRepository.findPending/countPending` → `QuestionRepositoryImpl` → Questions. Repository trả `List<QuestionEntity>` và long riêng, hai query dùng chung `pendingQuery`; QuestionService gọi mapper chuyển `PendingQuestionDTO`, tạo `PendingQuestionPageDTO` và tính totalPages. Không QuestionPageEntity hoặc DTO trong kết quả infrastructure. ModeratorUsecase dùng `ChapterService.getAll` map tên/tìm chương-bài và S3Service ký ảnh trên DTO. Hook quản lý filters/debounce/page/query/cache theo user và thông báo; card giữ form sửa thử. Approve/reject đã nối POST [action], edit nối PATCH multipart; xem section kiểm duyệt câu hỏi AI phía trên.

[Page](../frontend/app/(site)/moderator/review/page.tsx), [card](../frontend/presentation/components/Moderator/PendingQuestionCard.tsx), [hook](../frontend/presentation/hooks/Moderator/usePendingQuestions.ts), [Next route](../frontend/app/api/moderator/questions/pending/route.ts), [API](api-reference.md).

## Collection câu hỏi cần duyệt — 2026-10-26

QuestionService.generateExamQuestions → QuestionReviewService.createForQuestions → QuestionReviewUsecase.createForQuestions → QuestionReviewEntity → IQuestionReviewRepository.saveReviews → QuestionReviewRepositoryImpl → QuestionReviewMapper → QuestionReviewDocument → QuestionsReview. regeneratedQuestions dùng RegeneratedQuestionEntity/RegeneratedQuestionDocument; ContentRegeneratedEntity/VariablesRegeneratedEntity và ContentDocument/VariablesDocument nằm trong questionReview. Mapper không uses QuestionMapper, không có import từ package question trong module review.

Bằng chứng: [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java), [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java).

## Trạng thái câu hỏi — 2026-10-26

QuestionService.insertFromDocx(List<NewQuestionDTO>) → private insert(List<NewQuestionDTO>, String status) → IQuestionRepository.saveQuestions → QuestionMapper.toDocument → Questions.status. Parse dùng APPROVED, generateExamQuestions truyền PENDING_REVIEW cho AI. toEntity giữ status; legacy null. Không enum/route/HTTP DTO mới.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

## Kiểm duyệt S3 — bổ sung 2026-10-25

- QuestionFilesPage → QuestionFilePreviewDialog/QuestionFileReviewForm → useQuestionFileReview → ModeratorService → Next approve/reject → ApproveQuestionFileUsecase/RejectQuestionFileUsecase → IModeratorRepository/ModeratorRepositoryImpl → ModeratorController → [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java).
- Approve: S3Service.readDocumentFile → StoredDocumentFile hoặc multipart chỉnh sửa → S3Service.validateDocumentFile → ModeratorUsecase.parseDocument → importer frozen → deleteFile. DTO {questionIds,fileDeleted,message}; fileDeleted=false chỉ retry xóa trong phiên UI.
- Reject: RejectQuestionFileDTO {key,reason} → validate email/lý do → kiểm tra tồn tại → deleteFile → MailService.sendQuestionFileRejectionEmail. Email lỗi chỉ log, không Telegram, vẫn 204.
- useQuestionFilePreview dùng chung renderer/sandbox cho key S3 hoặc File cục bộ; useQuestionFiles quản lý refresh/cache và cleanupKeys trong bộ nhớ. Không đổi schema hoặc thêm receipt/audit.

[API](api-reference.md#duyệttừ-chối-tệp--2026-10-25), [kiểm tra](current-status.md#kiểm-duyệt-tệp-s3--2026-10-25). Các dòng snapshot bên dưới mô tả trước khi bổ sung workflow này.


Inventory đối chiếu working tree 2026-11-06. Danh mục declaration/API lấy từ source, không từ endpoint server đang chạy. Endpoint tồn tại không chứng minh end-to-end pass; [current-status.md](current-status.md) ghi trạng thái và hạn chế. Links source dùng đường dẫn tương đối từ codex/.

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

Danh sách method/path/symbol hiện tại được gom tại [API reference](api-reference.md):54 Spring endpoints và 51 Next handlers trong 49 route files. Payment BFF gồm plan,orders,orders/[id],verify,return (5 route files); handler.ts là helper, IPN là rewrite và không tính như Next handler.

Frontend App Router pages:

- [(site)/admin/ai/page.tsx](<../frontend/app/(site)/admin/ai/page.tsx>)
- [(site)/admin/page.tsx](<../frontend/app/(site)/admin/page.tsx>)
- [(site)/admin/users/page.tsx](<../frontend/app/(site)/admin/users/page.tsx>)
- [(site)/auth/forgot-password/page.tsx](<../frontend/app/(site)/auth/forgot-password/page.tsx>)
- [(site)/auth/login/page.tsx](<../frontend/app/(site)/auth/login/page.tsx>)
- [(site)/auth/register/page.tsx](<../frontend/app/(site)/auth/register/page.tsx>)
- [(site)/auth/reset-password/page.tsx](<../frontend/app/(site)/auth/reset-password/page.tsx>)
- [(site)/exam/all/page.tsx](<../frontend/app/(site)/exam/all/page.tsx>)
- [(site)/exam/[examId]/page.tsx](<../frontend/app/(site)/exam/[examId]/page.tsx>)
- [(site)/generate/new/page.tsx](<../frontend/app/(site)/generate/new/page.tsx>)
- [(site)/generate/page.tsx](<../frontend/app/(site)/generate/page.tsx>)
- [(site)/generate/[draftId]/chapter/page.tsx](<../frontend/app/(site)/generate/[draftId]/chapter/page.tsx>)
- [(site)/generate/[draftId]/details/page.tsx](<../frontend/app/(site)/generate/[draftId]/details/page.tsx>)
- [(site)/generate/[draftId]/matrix/page.tsx](<../frontend/app/(site)/generate/[draftId]/matrix/page.tsx>)
- [(site)/generate/[draftId]/page.tsx](<../frontend/app/(site)/generate/[draftId]/page.tsx>)
- [(site)/generate/[draftId]/[chapterId]/page.tsx](<../frontend/app/(site)/generate/[draftId]/[chapterId]/page.tsx>)
- [(site)/landing-page/page.tsx](<../frontend/app/(site)/landing-page/page.tsx>)
- [(site)/me/bill/page.tsx](<../frontend/app/(site)/me/bill/page.tsx>)
- [(site)/me/page.tsx](<../frontend/app/(site)/me/page.tsx>)
- [(site)/moderator/page.tsx](<../frontend/app/(site)/moderator/page.tsx>)
- [(site)/moderator/question-bank/page.tsx](<../frontend/app/(site)/moderator/question-bank/page.tsx>)
- [(site)/moderator/question-files/page.tsx](<../frontend/app/(site)/moderator/question-files/page.tsx>)
- [(site)/moderator/review/page.tsx](<../frontend/app/(site)/moderator/review/page.tsx>)
- [(site)/upload/page.tsx](<../frontend/app/(site)/upload/page.tsx>)
- [page.tsx](<../frontend/app/page.tsx>)

## Luồng xem trước DOCX — 2026-10-25

QuestionFileRow.onPreview → useQuestionFiles.previewFile → [QuestionFilePreviewDialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx) → [useQuestionFilePreview](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts) → ModeratorService.getQuestionFileContent → [Next GET /api/moderator/question-files/content](../frontend/app/api/moderator/question-files/content/route.ts) → GetQuestionFileContentUsecase → IModeratorRepository/ModeratorRepositoryImpl → GET /moderator/question-files/content → ModeratorController/ModeratorUsecase.getQuestionFileContent → S3Service.readDocumentFile. Contract binary DOCX, key query, ROLE_MODERATOR; [API](api-reference.md). Không đổi model/database hoặc parse flow.

## Backend question-file content — 2026-10-25

GET /moderator/question-files/content?key=... → [ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) → [ModeratorUsecase.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) → [S3Service.readDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) → S3Client.getObjectAsBytes. ROLE_MODERATOR, binary DOCX no-store; không đổi DTO/schema. [Contract](api-reference.md).

## Backend payment module — 2026-11-05

- PaymentController: plan/create/get/verify/confirmReturn/ipn, routes at [API reference](api-reference.md#backend-vnpay-contract--2026-11-05).
- PaymentUsecase: idempotencyKeyHash/findAttempt/newOrderId, owned orders, Return validation, shared transaction/process, expiry and outbox.
- VnpayGateway/VnpayProperties/VnpayQueryException: checkout/signatures/querydr/configuration/safe diagnostic stages.
- PaymentOrderEntity/Document, PaymentEmailEntity/Document, PaymentMapper, IPaymentRepository/PaymentRepositoryImpl: persistence and conditional writes.
- PaymentSchedulingConfig/PaymentJobs: indexes, expiry, leased email delivery.
- PlusEntitlement, UserService/UserUsecase.grantPaidPlus and UserRepositoryImpl.grantPlus/updateRole/updatePassword: effective entitlement and field-specific writes. Common user DTOs add plusExpiresAt.
- ServiceUnavailableException/GlobalExceptionHandler.handleServiceUnavailable:503 mapping; MailService.sendPaymentResultEmail:result delivery.

[Payment source](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java).

## Frontend payment module — 2026-11-06

- app/(site)/me/bill/page.tsx: plan, account, missing order, result and retry rendering. BillingLayout:shared Navbar.
- usePayment: restore key/reference, openPaymentTab, beginPayment/startPayment/startNewPayment/continuePayment/verifyPayment, pendingReturn/reconcileOrder, updateOrder, polling/focus/overlap cleanup.
- PaymentResult:status, verify and resume interactions.
- PaymentService, PaymentUsecase, IPaymentRepository, PaymentRepositoryImpl: layer-specific plan/create/order/verify/confirmReturn calls.
- app/api/payment/handler.ts and plan/orders/{id}/verify/return routes: authenticated BFF validation and safe503 handling.
- PaymentPlan/PaymentOrder/PaymentReturnParameters:HTTP types; UserEntity.plusExpiresAt:nullable expiry.
- useAuth:effective expiry/cache refresh; useNavBar:teacher upgrade navigation; next.config/proxy:exact public IPN forward.

Matching backend payment endpoints are already available in main through PR #131; this frontend commit adds the UI and BFF on that updated base. [Routes](api-reference.md#frontend-payment-contracts--2026-11-06), [hook](../frontend/presentation/hooks/Payment/usePayment.ts).

## ADMIN transaction lookup — 2026-11-08

- Backend payment: AdminTransactionController → AdminTransactionUsecase → IPaymentRepository.searchTransactions/countTransactions/find and VnpayGateway.query. AdminTransactionDTO separates list/lookup output; AdminTransactionException provides safe lookup codes. AdminTransactionIndexConfig creates read indexes independently of payment enablement.
- Frontend: Navbar → /admin/transactions → /admin/transactions/search; TransactionFilters/Table/Detail render useAdminTransactions state. AdminTransactionService → BFF handleAdminTransactions → AdminTransactionUsecase → IAdminTransactionRepository/AdminTransactionRepositoryImpl → backend.
- New contracts/types: TransactionFilters, AdminTransaction, TransactionPage, TransactionLookup; GET /admin/transactions and POST /admin/transactions/{id}/lookup, with matching /api BFF routes. See [API reference](api-reference.md#admin-transaction-lookup--2026-11-08) and [source inventory](source-catalog.md#admin-transaction-lookup--2026-11-08).
