# Source catalogue WEdu

## Kiểm duyệt câu hỏi AI — 2026-10-31

Inventory **52 file** application/tests/docs thay đổi hoặc thêm; không đổi config/dependency/exporter. Không xóa/rename file. Application/tests đã commit và push: backend 85269d8, 89553d9; tests e938083; frontend 835576c. Đã merge vào main tại d3ad7a3 qua PR #123–125. Tám file docs được chuẩn bị trong commit đồng bộ context này. Harness JSDOM và log validation chỉ nằm trong thư mục tạm, không thuộc source repository.

Symbols/contracts mới: ReviewPendingQuestionUsecase.approve/reject (backend), ReviewPendingQuestionDTO, RejectPendingQuestionResponseDTO, QuestionReviewStateDTO; QuestionService.commitPendingReview/updatePendingContent overload có revision; IQuestionRepository.updatePendingState; QuestionReviewService.state/states/saveCandidate/delete; QuestionReviewUsecase.states/saveCandidate/delete; IQuestionReviewRepository.findByIds/ensureExists/update/delete; QuestionReviewMapper.toDTO/toSnapshot. Frontend QuestionReviewAction/Input/Result, reviewPendingQuestion trên port/adapter/service, ReviewPendingQuestionUsecase.execute và POST /api/moderator/questions/[id]/[action]. [Contract](api-reference.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31), [validation](current-status.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31).

| File | Thay đổi |
| --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java](<../backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java>) | Ẩn provider exception khỏi log/response; tách helper getStringBuilder dựng đặc tả prompt. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/usecase/QuestionImageReference.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/QuestionImageReference.java>) | Bỏ comment, không đổi nhận diện S3 key. |
| [frontend/app/api/moderator/question-files/reject/route.ts](<../frontend/app/api/moderator/question-files/reject/route.ts>) | Chuẩn hóa định dạng và tách biến repository; không đổi contract từ chối tệp. |
| [frontend/app/api/moderator/questions/[id]/route.ts](<../frontend/app/api/moderator/questions/[id]/route.ts>) | Chuẩn hóa định dạng route PATCH, giữ contract multipart hiện có. |
| [backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/PendingQuestionDTO.java](<../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/PendingQuestionDTO.java>) | Thêm bộ đếm, reviewRevision và snapshot mới nhất cho API đọc. |
| [backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/QuestionReviewStateDTO.java](<../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/QuestionReviewStateDTO.java>) | Common DTO trao đổi count/revision/latest giữa question và questionReview. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java>) | Thêm POST approve/reject và inject coordinator kiểm duyệt. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/dto/EditPendingQuestionDTO.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/dto/EditPendingQuestionDTO.java>) | Nhận reviewRevision trong payload editor. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectPendingQuestionResponseDTO.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectPendingQuestionResponseDTO.java>) | Response trạng thái và bộ đếm sau từ chối. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/dto/ReviewPendingQuestionDTO.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ReviewPendingQuestionDTO.java>) | Request kiểm duyệt chứa updatedAt/reviewRevision. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecase.java>) | Chọn baseline snapshot mới nhất, kiểm revision, giữ validation/upload ảnh. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java>) | Ký ảnh trên DTO snapshot mới nhất, giữ mô tả AI dạng text. |
| [backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecase.java>) | Coordinator approve/reject, dựng yêu cầu/validate AI, transaction ngắn và xử lý conflict. |
| [backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java](<../backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java>) | Port conditional update trạng thái/nội dung câu chờ duyệt. |
| [backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java>) | Atomic update status/content/version với guard _id/status/updatedAt. |
| [backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java](<../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java>) | Batch attach review, commit trạng thái và editor transactional cho snapshot. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/QuestionReviewEntity.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/QuestionReviewEntity.java>) | Thêm regenerationCount/revision vào domain review. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/domain/repository/IQuestionReviewRepository.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/repository/IQuestionReviewRepository.java>) | Port đọc batch, khởi tạo thiếu, update/delete có revision. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java>) | Persist regenerationCount/revision trong QuestionsReview. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java>) | Map snapshot riêng sang/từ common QuestionDTO. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/repository/QuestionReviewRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/repository/QuestionReviewRepositoryImpl.java>) | Mongo read/upsert setOnInsert và update/delete theo revision, hỗ trợ legacy. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewService.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewService.java>) | Facade common DTO cho state/batch/saveCandidate/delete. |
| [backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewUsecase.java>) | Quản lý snapshot cuối, count/revision, append/edit/delete và default cũ. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java>) | Bổ sung mock coordinator cho controller có dependency mới. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorEditPendingQuestionTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorEditPendingQuestionTest.java>) | Bổ sung mock coordinator và xác thực overload editor có revision. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorPendingQuestionsTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorPendingQuestionsTest.java>) | Kiểm tra API approve/reject, quyền, no-store và ký ảnh snapshot. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java>) | Bổ sung mock coordinator, giữ hồi quy nội dung DOCX. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java>) | Bổ sung mock coordinator, giữ hồi quy kiểm duyệt tệp. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecaseTest.java>) | Cập nhật guard revision, thêm baseline ảnh/options của snapshot cuối. |
| [backend/src/test/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecaseTest.java>) | Kiểm tra lượt 1–5, duyệt, AI lỗi/sai, stale request, conflict và transaction interception. |
| [backend/src/test/java/com/wedu/exam_creation/question/infrastructure/repository/PendingQuestionUpdateRepositoryTest.java](<../backend/src/test/java/com/wedu/exam_creation/question/infrastructure/repository/PendingQuestionUpdateRepositoryTest.java>) | Bổ sung kiểm tra trạng thái cuối chỉ ghi status/version, timestamp tăng. |
| [backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServicePendingQuestionsTest.java](<../backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServicePendingQuestionsTest.java>) | Bổ sung editor chỉ ghi snapshot, touch version và chặn ghi khi guard thất bại. |
| [backend/src/test/java/com/wedu/exam_creation/questionReview/infrastructure/repository/QuestionReviewRepositoryTest.java](<../backend/src/test/java/com/wedu/exam_creation/questionReview/infrastructure/repository/QuestionReviewRepositoryTest.java>) | Kiểm tra query revision legacy, setOnInsert, conditional update/delete. |
| [backend/src/test/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewUsecaseTest.java>) | Kiểm tra mapping snapshot, giữ keys/metadata, edit không mất lượt, legacy và conflict. |
| [codex/api-reference.md](<../codex/api-reference.md>) | Đồng bộ POST approve/reject và GET/PATCH mở rộng. |
| [codex/architecture.md](<../codex/architecture.md>) | Ghi service boundaries, transaction, gọi AI và concurrency. |
| [codex/business-flows.md](<../codex/business-flows.md>) | Ghi đầy đủ luồng con người duyệt từng bản và nhánh hết lượt. |
| [codex/current-status.md](<../codex/current-status.md>) | Ghi hành vi, kiểm tra đã chạy, lỗi đã xử lý và giới hạn chưa xác minh. |
| [codex/database-schema.md](<../codex/database-schema.md>) | Ghi fields review mới, legacy defaults và chuyển trạng thái/xóa review. |
| [codex/development-decisions.md](<../codex/development-decisions.md>) | Ghi phạm vi đã duyệt, giới hạn lượt, lưu/xóa review và phạm vi exam giữ nguyên. |
| [codex/module-map.md](<../codex/module-map.md>) | Đồng bộ call graph, methods, DTO và model mới. |
| [codex/source-catalog.md](<../codex/source-catalog.md>) | Inventory toàn bộ file thay đổi và symbols mới trong feature. |
| [frontend/app/(site)/moderator/review/page.tsx](<../frontend/app/(site)/moderator/review/page.tsx>) | Nối trạng thái mutation vào card và mô tả giới hạn lượt. |
| [frontend/app/api/moderator/questions/[id]/[action]/route.ts](<../frontend/app/api/moderator/questions/[id]/[action]/route.ts>) | Dynamic POST approve/reject, validate cookie/action/payload và lỗi an toàn. |
| [frontend/domain/entities/pendingQuestion.entity.ts](<../frontend/domain/entities/pendingQuestion.entity.ts>) | Types count/revision/latest và request/action/result kiểm duyệt. |
| [frontend/domain/repositories/IModeratorRepository.ts](<../frontend/domain/repositories/IModeratorRepository.ts>) | Port reviewPendingQuestion cho approve/reject. |
| [frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts](<../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts>) | Adapter POST backend kiểm duyệt với access token. |
| [frontend/presentation/components/Moderator/PendingQuestionCard.tsx](<../frontend/presentation/components/Moderator/PendingQuestionCard.tsx>) | Hiển thị bản mới nhất, số lượt, nút trạng thái cuối và editor đúng bản. |
| [frontend/presentation/hooks/Moderator/usePendingQuestionEdit.ts](<../frontend/presentation/hooks/Moderator/usePendingQuestionEdit.ts>) | Giữ và gửi revision cùng phiên bản editor. |
| [frontend/presentation/hooks/Moderator/usePendingQuestions.ts](<../frontend/presentation/hooks/Moderator/usePendingQuestions.ts>) | Mutation thật, chặn gửi lặp, notice/refetch và sửa trang sau xóa. |
| [frontend/presentation/services/ModeratorService.ts](<../frontend/presentation/services/ModeratorService.ts>) | Presentation service POST các route kiểm duyệt Next. |
| [frontend/usecases/moderator/ReviewPendingQuestionUsecase.ts](<../frontend/usecases/moderator/ReviewPendingQuestionUsecase.ts>) | Frontend usecase chuyển thao tác qua moderator repository. |



## Editor câu hỏi, LaTeX và ảnh — 2026-10-29

Inventory toàn bộ file thay đổi trong tính năng editor (không gồm các file tạm validation ngoài repository):

| File backend | Thay đổi |
| --- | --- |
| [PendingQuestionDTO.java](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/PendingQuestionDTO.java) | Bổ sung updatedAt trong response để xác định phiên bản editor. |
| [ConflictException.java](../backend/src/main/java/com/wedu/exam_creation/common/exception/ConflictException.java) | Exception cho trạng thái/phiên bản đã đổi. |
| [GlobalExceptionHandler.java](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java) | Map ConflictException thành HTTP 409. |
| [ModeratorController.java](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | PATCH multipart editPendingQuestion, gọi edit usecase riêng. |
| [ModeratorUsecase.java](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) | GET pending giữ mô tả ảnh, chỉ ký URL cho media keys. |
| [EditPendingQuestionDTO.java](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/EditPendingQuestionDTO.java) | Class Lombok payload updatedAt/question/options. |
| [EditPendingQuestionUsecase.java](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecase.java) | Validate references, lấy key cũ từ DB, upload/cleanup, điều phối conditional update. |
| [QuestionImageReference.java](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/QuestionImageReference.java) | Convention phân biệt media/ key với mô tả AI. |
| [IQuestionRepository.java](../backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java) | Thêm findById và updatePendingContent nhận entity/scalar. |
| [QuestionDTOMapper.java](../backend/src/main/java/com/wedu/exam_creation/question/dto/mapper/QuestionDTOMapper.java) | Thêm contentDTOToEntity. |
| [QuestionRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java) | Find entity; atomic update question/options/updatedAt theo _id/status/version. |
| [QuestionService.java](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java) | getPendingForEdit và updatePendingContent: DTO tại usecase, chặn non-pending/conflict. |

| File frontend | Thay đổi |
| --- | --- |
| [ReviewPage](../frontend/app/(site)/moderator/review/page.tsx) | Nối onSaved, banner phản ánh edit đã hoạt động. |
| [Next PATCH route](../frontend/app/api/moderator/questions/[id]/route.ts) | Validate cookie/id/payload/file parts, proxy multipart, 204/no-store và lỗi chung. |
| [next.config.ts](../frontend/next.config.ts) | Giới hạn proxy body 65 MiB đồng bộ Spring, tránh cắt multipart nhiều ảnh. |
| [pendingQuestion.entity.ts](../frontend/domain/entities/pendingQuestion.entity.ts) | updatedAt trong entity HTTP. |
| [IModeratorRepository.ts](../frontend/domain/repositories/IModeratorRepository.ts) | Contract editPendingQuestion. |
| [ModeratorRepositoryImpl.ts](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts) | PATCH backend, chuyển Bearer phía server. |
| [ModeratorService.ts](../frontend/presentation/services/ModeratorService.ts) | PATCH BFF khi lưu form. |
| [EditPendingQuestionUsecase.ts](../frontend/usecases/moderator/EditPendingQuestionUsecase.ts) | Port call cho PATCH. |
| [usePendingQuestions.ts](../frontend/presentation/hooks/Moderator/usePendingQuestions.ts) | onSaved thông báo/invalidate cache; chỉ approve/regenerate còn preview. |
| [usePendingQuestionEdit.ts](../frontend/presentation/hooks/Moderator/usePendingQuestionEdit.ts) | Draft/ref state, LaTeX/file validation, multipart, mutation/error và blob URL lifecycle. |
| [questionContentEditor.ts](../frontend/presentation/utils/questionContentEditor.ts) | Unique refs, draft, parser, validation LaTeX, serialize và preview content. |
| [PendingQuestionCard.tsx](../frontend/presentation/components/Moderator/PendingQuestionCard.tsx) | Bỏ form sửa mẫu, dùng edit form thật. |
| [PendingQuestionEditForm.tsx](../frontend/presentation/components/Moderator/PendingQuestionEditForm.tsx) | Form dùng hook, Lưu/Hủy/loading/error. |
| [QuestionContentEditor.tsx](../frontend/presentation/components/Moderator/QuestionContentEditor.tsx) | Editor dùng lại: template, input math, mô tả/upload ảnh, chèn ref và preview. |
| [ReferencedContent.tsx](../frontend/presentation/components/Exam/ReferencedContent.tsx) | Renderer dùng chung math/image/description, trust=false. |
| [QuestionContent.tsx](../frontend/presentation/components/Exam/QuestionContent.tsx) | Dùng ReferencedContent, giữ numbering và wrapper. |
| [MultipleChoiceContent.tsx](../frontend/presentation/components/Exam/MultipleChoiceContent.tsx) | Dùng renderer chung, không lặp logic math/image. |

| File test backend | Thay đổi |
| --- | --- |
| [EditPendingQuestionUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecaseTest.java) | Upload/replace description, refs lặp, giữ key cũ, cleanup/conflict/unknown acknowledgement, namespaces. |
| [ModeratorEditPendingQuestionTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorEditPendingQuestionTest.java) | PATCH multipart thực qua MockMvc, quyền/token/JSON/ID/status. |
| [PendingQuestionUpdateRepositoryTest.java](../backend/src/test/java/com/wedu/exam_creation/question/infrastructure/repository/PendingQuestionUpdateRepositoryTest.java) | Query guard và giới hạn fields update, kết quả no-match. |
| [QuestionServicePendingQuestionsTest.java](../backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServicePendingQuestionsTest.java) | Chặn sửa câu approved và chuyển no-match thành conflict. |
| [ModeratorPendingQuestionsTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorPendingQuestionsTest.java) | Mô tả không bị ký URL, fixture media key và mock editor dependency. |
| [ModeratorDeleteQuestionFileTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java) | Mock editor dependency mới của controller. |
| [ModeratorQuestionFileContentTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) | Mock editor dependency mới của controller. |
| [ModeratorQuestionFileReviewTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java) | Mock editor dependency mới của controller. |

| Tài liệu thay đổi | Nội dung |
| --- | --- |
| [development-decisions.md](development-decisions.md) | Phạm vi edit được duyệt, thay mô tả bằng key, giới hạn approve/regenerate. |
| [api-reference.md](api-reference.md) | PATCH multipart, updatedAt, response/error/limits và image semantics. |
| [architecture.md](architecture.md) | Editor/hook/BFF/usecase/repository, DTO boundary và media cleanup. |
| [business-flows.md](business-flows.md) | Luồng sửa/Hủy/Lưu, ảnh cũ/mới, version/conflict. |
| [database-schema.md](database-schema.md) | Không field mới; semantics image description → key, conditional update. |
| [module-map.md](module-map.md) | Wiring, symbols/routes mới và renderer dùng lại. |
| [current-status.md](current-status.md) | Implementation, validation và giới hạn chưa kiểm chứng. |
| [tech-stack.md](tech-stack.md) | Cấu hình giới hạn multipart của Next proxy, không thêm dependency. |
| `codex/source-catalog.md` | Inventory đầy đủ file/symbol của thay đổi này. |

## Giao diện review — 2026-10-27

- [ReviewPage](../frontend/app/(site)/moderator/review/page.tsx): danh sách API thật, tìm kiếm, lọc loại câu, loading/error/retry/empty và phân trang 10 câu.
- [PendingQuestionCard](../frontend/presentation/components/Moderator/PendingQuestionCard.tsx): renderer dùng lại, form sửa thử và ba lựa chọn duyệt/chỉnh sửa/từ chối–regenerate.
- [usePendingQuestions](../frontend/presentation/hooks/Moderator/usePendingQuestions.ts): query, debounce/filter/page/state/thông báo; export `ReviewPreviewAction`. Hook mẫu và fixtures đã xóa.
- [pendingQuestion.entity.ts](../frontend/domain/entities/pendingQuestion.entity.ts): `PendingQuestionEntity`, `PendingQuestionPage`, `PendingQuestionFilters`.
- [Next pending route](../frontend/app/api/moderator/questions/pending/route.ts), [GetPendingQuestionsUsecase](../frontend/usecases/moderator/GetPendingQuestionsUsecase.ts): cookie validation, paging/filter input, proxy/error/no-store; port/adapter/service mở rộng `getPendingQuestions`.
- [PendingQuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/PendingQuestionDTO.java), [PendingQuestionPageDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/PendingQuestionPageDTO.java): response liên feature và HTTP.
- Đã xóa `question/domain/entity/QuestionPageEntity.java`: repository chỉ trả danh sách entity và count long; DTO phân trang chỉ tạo ở QuestionService. PendingQuestionPageDTO giữ class Lombok `@Data/@NoArgsConstructor/@AllArgsConstructor`. JSON/phân trang không đổi.
- [QuestionDTOMapper](../backend/src/main/java/com/wedu/exam_creation/question/dto/mapper/QuestionDTOMapper.java): `entityToPendingDTO`, disable builder cho DTO kế thừa. [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java)/[repository](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java): `findPending`.
- [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java)/[ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java): `listPendingQuestions`; [ChapterService](../backend/src/main/java/com/wedu/exam_creation/chapter/usecase/ChapterService.java): `getAll` qua repository/mapper hiện có.
- [ModeratorPendingQuestionsTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorPendingQuestionsTest.java), [PendingQuestionsRepositoryTest](../backend/src/test/java/com/wedu/exam_creation/question/infrastructure/repository/PendingQuestionsRepositoryTest.java): auth/input/response và query/count/sort/limit. Ba MVC tests tệp hiện có bổ sung mocks ChapterService/QuestionService theo constructor mới.
- [QuestionServicePendingQuestionsTest](../backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServicePendingQuestionsTest.java): mapping entity → DTO và metadata phân trang thuộc usecase; giữ tổng kết quả khi trang yêu cầu rỗng.

Các file còn lại trong thay đổi nối API:

- [IQuestionRepository.java](../backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java): findPending trả List<QuestionEntity>, countPending trả long. Adapter dùng private pendingQuery chung, không map DTO.
- [IModeratorRepository.ts](../frontend/domain/repositories/IModeratorRepository.ts): contract getPendingQuestions.
- [ModeratorRepositoryImpl.ts](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts): gọi backend với filters và Bearer phía server.
- [ModeratorService.ts](../frontend/presentation/services/ModeratorService.ts): gọi Next pending API, hỗ trợ AbortSignal.
- [ModeratorDeleteQuestionFileTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java): thêm mocks hai dependency đọc câu hỏi.
- [ModeratorQuestionFileContentTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java): thêm mocks hai dependency đọc câu hỏi.
- [ModeratorQuestionFileReviewTest.java](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java): thêm mocks hai dependency đọc câu hỏi.
- Xóa `frontend/presentation/hooks/Moderator/usePendingQuestionPreview.ts`: thay bằng hook query thật; xóa `frontend/presentation/data/pendingQuestionExamples.ts`: bỏ dữ liệu minh họa. Hai file này được tạo ở bước giao diện trước, chưa tracked trong Git.
- [api-reference.md](api-reference.md): endpoint/query/response/quyền và lỗi mới.
- [architecture.md](architecture.md): ranh giới đọc dữ liệu, map DTO và ký ảnh.
- [business-flows.md](business-flows.md): luồng danh sách thật và giới hạn ba thao tác write.
- [current-status.md](current-status.md): mức hoàn thiện, kiểm tra và giới hạn chưa xác minh.
- [development-decisions.md](development-decisions.md): bổ sung phạm vi API đọc đã duyệt.
- [module-map.md](module-map.md): wiring/symbols mới, bỏ hook/fixtures cũ.
- `codex/source-catalog.md` (file này): inventory file/symbol mới và thay đổi/xóa.

Không thay đổi config, dependency, database schema/index; `backend/src/main/resources/application.yml` là thay đổi có sẵn của người dùng.

## Collection câu hỏi cần duyệt — 2026-10-26

questionReview bổ sung 6 model snapshot riêng: domain/entity RegeneratedQuestionEntity, ContentRegeneratedEntity, VariablesRegeneratedEntity; infrastructure/document RegeneratedQuestionDocument, ContentDocument, VariablesDocument. QuestionReviewEntity và QuestionReviewDocument đổi kiểu regeneratedQuestions sang model nội bộ; QuestionReviewMapper bỏ uses QuestionMapper. QuestionReviewService gọi QuestionReviewUsecase; collection hiện tại QuestionsReview. Không import model/mapper từ question.

Bằng chứng: [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java), [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java).

## Trạng thái câu hỏi — 2026-10-26

Symbols cập nhật: QuestionDocument.status và QuestionEntity.status (String); QuestionService.insertFromDocx(List<NewQuestionDTO>) mặc định APPROVED, method private insert(List<NewQuestionDTO>, String) nhận PENDING_REVIEW từ generateExamQuestions. Không thêm QuestionStatus.java. QuestionStatusTest.java đã được xóa theo yêu cầu; không thêm test mới.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

## Symbols kiểm duyệt tệp — 2026-10-25

| Nhóm | Symbols/files bổ sung hoặc mở rộng |
| --- | --- |
| Backend moderator | ModeratorController.approveQuestionFile/rejectQuestionFile; ModeratorUsecase.approveQuestionFile/rejectQuestionFile/reviewKeyParts |
| Backend DTO/adapter | [ApproveQuestionFileResponseDTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ApproveQuestionFileResponseDTO.java), [RejectQuestionFileDTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectQuestionFileDTO.java), [StoredDocumentFile](../backend/src/main/java/com/wedu/exam_creation/moderator/infrastructure/StoredDocumentFile.java) implements MultipartFile |
| Storage/mail | S3Service.validateDocumentFile dùng chung updateDocumentFile/approve; MailService.sendQuestionFileRejectionEmail |
| Frontend types/ports | [ApproveQuestionFileResult/RejectQuestionFileInput](../frontend/domain/entities/questionFileReview.entity.ts); IModeratorRepository/ModeratorRepositoryImpl/ModeratorService.approveQuestionFile/rejectQuestionFile |
| Frontend usecases | [ApproveQuestionFileUsecase](../frontend/usecases/moderator/ApproveQuestionFileUsecase.ts), [RejectQuestionFileUsecase](../frontend/usecases/moderator/RejectQuestionFileUsecase.ts) |
| Next routes/helper | [approve POST](../frontend/app/api/moderator/question-files/approve/route.ts), [reject POST](../frontend/app/api/moderator/question-files/reject/route.ts), [reviewError/reviewHeaders](../frontend/app/api/moderator/question-files/reviewError.ts) |
| UI | [useQuestionFileReview](../frontend/presentation/hooks/Moderator/useQuestionFileReview.ts) (ReviewComplete export), [QuestionFileReviewForm](../frontend/presentation/components/Moderator/QuestionFileReviewForm.tsx); QuestionFilePreviewDialog nối review; useQuestionFilePreview thêm localFile; useQuestionFiles thêm handleReviewComplete/previewNeedsCleanup |
| Tests | [ModeratorQuestionFileReviewTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java), [MailRejectionTest](../backend/src/test/java/com/wedu/exam_creation/mail/service/MailRejectionTest.java); existing content/delete tests thêm mock MailService |

[Contracts](api-reference.md#duyệttừ-chối-tệp--2026-10-25), [validation](current-status.md#kiểm-duyệt-tệp-s3--2026-10-25). Không thêm model persistence hoặc đổi parser.


Snapshot working tree 2026-10-24, gồm staged/unstaged/untracked files. Chỉ mục này xác nhận file tồn tại, không chứng nhận feature hoàn tất hoặc đã deploy. [Module map](module-map.md), [API reference](api-reference.md), [database schema](database-schema.md), [current status](current-status.md). Chỉ đọc feature cần sửa để giảm context.

## Backend theo feature/layer

### Backend `admin`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) |
| `usecase` | [AdminUsecase](../backend/src/main/java/com/wedu/exam_creation/admin/usecase/AdminUsecase.java) |

### Backend `ai`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `properties` | [DeepSeekProperties](../backend/src/main/java/com/wedu/exam_creation/ai/properties/DeepSeekProperties.java) |
| `service` | [AIQuestionGenerationService](../backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java) |

### Backend `auth`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) |
| `dto/mapper` | [AuthDTOMapper](../backend/src/main/java/com/wedu/exam_creation/auth/dto/mapper/AuthDTOMapper.java) |
| `dto/request` | [ChangePasswordRequestDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/ChangePasswordRequestDTO.java), [ResetPasswordRequestDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/ResetPasswordRequestDTO.java) |
| `dto/response` | [AuthorizedResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/AuthorizedResponseDTO.java), [NewAccessTokenResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/NewAccessTokenResponseDTO.java), [UserResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/UserResponseDTO.java) |
| `usecase` | [AuthUsecase](../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java) |

### Backend `chapter`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ChapterController](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java) |
| `domain/entity` | [BankStatEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/BankStatEntity.java), [ChapterEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/ChapterEntity.java), [LessonDataEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/LessonDataEntity.java) |
| `domain/repository` | [IChapterRepository](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/repository/IChapterRepository.java) |
| `dto/mapper` | [ChapterDTOMapper](../backend/src/main/java/com/wedu/exam_creation/chapter/dto/mapper/ChapterDTOMapper.java) |
| `infrastructure/document` | [BankStatDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/BankStatDocument.java), [ChapterDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/ChapterDocument.java), [LessonDataDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/LessonDataDocument.java) |
| `infrastructure/mapper` | [ChapterMapper](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/mapper/ChapterMapper.java) |
| `infrastructure/repository` | [ChapterRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java) |
| `usecase` | [ChapterService](../backend/src/main/java/com/wedu/exam_creation/chapter/usecase/ChapterService.java), [ChapterUsecase](../backend/src/main/java/com/wedu/exam_creation/chapter/usecase/ChapterUsecase.java) |

### Backend `common`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `dto/ai/request` | [GenerateQuestionRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/request/GenerateQuestionRequestDTO.java) |
| `dto/ai/response` | [AIListQuestionsResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/response/AIListQuestionsResponseDTO.java), [AIQuestionResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/response/AIQuestionResponseDTO.java) |
| `dto/chapter` | [NewBankStatDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewBankStatDTO.java), [NewChapterDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewChapterDTO.java), [NewLessonDataDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewLessonDataDTO.java) |
| `dto/chapter/response` | [BankStatResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/BankStatResponseDTO.java), [ChapterResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/ChapterResponseDTO.java), [LessonDataResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/LessonDataResponseDTO.java), [SavedChapterResponse](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/SavedChapterResponse.java) |
| `dto/draft/response` | [ChapterDraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/ChapterDraftDTO.java), [DraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/DraftDTO.java), [LessonDraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/LessonDraftDTO.java), [MatrixDetailItemDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/MatrixDetailItemDTO.java), [MatrixItemDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/MatrixItemDTO.java) |
| `dto/exam/response` | [ExamDetailDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamDetailDTO.java), [ExamGeneratedDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamGeneratedDTO.java), [ExamQuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamQuestionDTO.java), [ExamQuestionGeneratedDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamQuestionGeneratedDTO.java), [QuestionDetailResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/QuestionDetailResponseDTO.java) |
| `dto/question` | [NewQuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/NewQuestionDTO.java) |
| `dto/question/response` | [ContentDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/ContentDTO.java), [QuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/QuestionDTO.java), [VariablesDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/VariablesDTO.java) |
| `dto/refreshToken/request` | [NewRTRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/refreshToken/request/NewRTRequestDTO.java) |
| `dto/refreshToken/response` | [RTResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/refreshToken/response/RTResponseDTO.java) |
| `dto/token` | [ATPayload](../backend/src/main/java/com/wedu/exam_creation/common/dto/token/ATPayload.java), [RTPayload](../backend/src/main/java/com/wedu/exam_creation/common/dto/token/RTPayload.java) |
| `dto/user/mapper` | [UserCommonDTOMapper](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/mapper/UserCommonDTOMapper.java) |
| `dto/user/request` | [NewUserRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/NewUserRequestDTO.java), [UserUpdateFields](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/UserUpdateFields.java) |
| `dto/user/response` | [CommonUserResponseAllDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/response/CommonUserResponseAllDTO.java), [CommonUserResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/response/CommonUserResponseDTO.java) |
| `exception` | [AIGenerationException](../backend/src/main/java/com/wedu/exam_creation/common/exception/AIGenerationException.java), [BadRequestException](../backend/src/main/java/com/wedu/exam_creation/common/exception/BadRequestException.java), [ForbiddenException](../backend/src/main/java/com/wedu/exam_creation/common/exception/ForbiddenException.java), [GlobalExceptionHandler](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java), [InternalServerException](../backend/src/main/java/com/wedu/exam_creation/common/exception/InternalServerException.java), [NotFoundException](../backend/src/main/java/com/wedu/exam_creation/common/exception/NotFoundException.java), [UnAuthorizedException](../backend/src/main/java/com/wedu/exam_creation/common/exception/UnAuthorizedException.java) |

### Backend `config`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `` | [AppConfig](../backend/src/main/java/com/wedu/exam_creation/config/AppConfig.java), [JacksonConfig](../backend/src/main/java/com/wedu/exam_creation/config/JacksonConfig.java), [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java), [S3Config](../backend/src/main/java/com/wedu/exam_creation/config/S3Config.java), [SecurityConfig](../backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java) |

### Backend `draft`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) |
| `domain/entity` | [ChapterDraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/ChapterDraftEntity.java), [DraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/DraftEntity.java), [LessonDraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/LessonDraftEntity.java), [MatrixDetailItemEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/MatrixDetailItemEntity.java), [MatrixItemEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/MatrixItemEntity.java) |
| `domain/payload` | [UpdateChaptersPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateChaptersPayload.java), [UpdateLessonsPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateLessonsPayload.java), [UpdateMatrixDetailsPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateMatrixDetailsPayload.java), [UpdateMatrixPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateMatrixPayload.java), [UpdateParam](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateParam.java) |
| `domain/repository` | [IDraftRepository](../backend/src/main/java/com/wedu/exam_creation/draft/domain/repository/IDraftRepository.java) |
| `dto/mapper` | [DraftDTOMapper](../backend/src/main/java/com/wedu/exam_creation/draft/dto/mapper/DraftDTOMapper.java) |
| `dto/request` | [CreateDraftDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/CreateDraftDTO.java), [UpdateDraftDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/UpdateDraftDTO.java), [UpdateParamDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/UpdateParamDTO.java) |
| `infrastructure/document` | [ChapterDraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/ChapterDraftDocument.java), [DraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/DraftDocument.java), [LessonDraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/LessonDraftDocument.java), [MatrixDetailItemDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/MatrixDetailItemDocument.java), [MatrixItemDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/MatrixItemDocument.java) |
| `infrastructure/mapper` | [DraftMapper](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/mapper/DraftMapper.java) |
| `infrastructure/repository` | [DraftRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java) |
| `usecase` | [DraftService](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftService.java), [DraftUsecase](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java) |
| `usecase/util` | [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java) |
| `usecase/util/dto` | [BankPool](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/BankPool.java), [Candidate](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/Candidate.java), [ComboKey](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/ComboKey.java), [RemainderItem](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/RemainderItem.java) |

### Backend `exam`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) |
| `domain/entity` | [ChapterExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ChapterExamEntity.java), [ExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamEntity.java), [ExamMatrixDetailItemEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamMatrixDetailItemEntity.java), [ExamMatrixItemEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamMatrixItemEntity.java), [LessonExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/LessonExamEntity.java), [QuestionExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/QuestionExamEntity.java) |
| `domain/repository` | [IExamRepository](../backend/src/main/java/com/wedu/exam_creation/exam/domain/repository/IExamRepository.java) |
| `dto/mapper` | [ExamCommonDraftDTOMapper](../backend/src/main/java/com/wedu/exam_creation/exam/dto/mapper/ExamCommonDraftDTOMapper.java), [ExamResponseDTOMapper](../backend/src/main/java/com/wedu/exam_creation/exam/dto/mapper/ExamResponseDTOMapper.java) |
| `dto/response` | [ExamChapterDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamChapterDTO.java), [ExamDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamDTO.java), [ExamGeneratedResponseDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamGeneratedResponseDTO.java) |
| `infrastructure/document` | [ChapterExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ChapterExamDocument.java), [ExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamDocument.java), [ExamMatrixDetailItemDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamMatrixDetailItemDocument.java), [ExamMatrixItemDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamMatrixItemDocument.java), [LessonExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/LessonExamDocument.java), [QuestionExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/QuestionExamDocument.java) |
| `infrastructure/mapper` | [ExamMapper](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/mapper/ExamMapper.java) |
| `infrastructure/repository` | [ExamRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/repository/ExamRepositoryImpl.java) |
| `usecase` | [ExamService](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamService.java), [ExamUsecase](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java) |

### Backend `exporter`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ExporterController](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java) |
| `dto/request` | [ExportRequestDTO](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/ExportRequestDTO.java), [ImageCacheData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/ImageCacheData.java), [QuestionData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/QuestionData.java), [QuestionsSortedData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/QuestionsSortedData.java), [WordPayloadRequestDTO](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/WordPayloadRequestDTO.java) |
| `usecase` | [ExporterUsecase](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java) |

### Backend `importer`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `dto/pandoc` | [ExtractedHeaderResult](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/ExtractedHeaderResult.java), [HeaderContent](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/HeaderContent.java), [PandocEleOutput](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/PandocEleOutput.java) |
| `dto/parsed` | [BankStatDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/BankStatDTO.java), [LessonDataImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/LessonDataImporterDTO.java), [NewChapterImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/NewChapterImporterDTO.java), [NewQuestionImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/NewQuestionImporterDTO.java), [OptionsDataImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/OptionsDataImporterDTO.java), [ParsedDataOutput](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/ParsedDataOutput.java), [QuestionContentImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/QuestionContentImporterDTO.java), [VariablesDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/VariablesDTO.java) |
| `infrastructure/pandoc` | [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java) |
| `infrastructure/pandoc/helper` | [BulletListExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/BulletListExtractor.java), [HeaderExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/HeaderExtractor.java), [IsArrayEqual](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/IsArrayEqual.java), [ParaExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/ParaExtractor.java), [TitleExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/TitleExtractor.java) |
| `service` | [ImporterService](../backend/src/main/java/com/wedu/exam_creation/importer/service/ImporterService.java) |

### Backend `mail`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `service` | [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java) |

### Backend `moderator`

Symbols mới cho xóa tệp: `ModeratorController.deleteQuestionFile(String key)` (DELETE), `ModeratorUsecase.deleteQuestionFile(String key)` (validate prefix/segment, gọi storage). [Test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java) đi qua JWT, method security, usecase và S3Service với S3Client mock.

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) |
| `usecase` | [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) |

### Backend `notification`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `telegram` | [TelegramNotificationService](../backend/src/main/java/com/wedu/exam_creation/notification/telegram/TelegramNotificationService.java) |

### Backend `passwordResetToken`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [PasswordResetTokenEntity](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/domain/entity/PasswordResetTokenEntity.java) |
| `domain/repository` | [IPasswordResetTokenRepository](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/domain/repository/IPasswordResetTokenRepository.java) |
| `infrastructure/document` | [PasswordResetTokenDocument](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/document/PasswordResetTokenDocument.java) |
| `infrastructure/mapper` | [PasswordResetTokenMapper](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/mapper/PasswordResetTokenMapper.java) |
| `infrastructure/repository` | [PasswordResetTokenRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/repository/PasswordResetTokenRepositoryImpl.java) |
| `usecase` | [PasswordResetTokenService](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenService.java), [PasswordResetTokenUsecase](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java) |

### Backend `question`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [ContentEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/ContentEntity.java), [QuestionEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/QuestionEntity.java), [VariablesEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/VariablesEntity.java) |
| `domain/repository` | [IQuestionRepository](../backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java) |
| `dto/mapper` | [QuestionDTOMapper](../backend/src/main/java/com/wedu/exam_creation/question/dto/mapper/QuestionDTOMapper.java) |
| `dto/request` | [ExamMatrixDetailDTO](../backend/src/main/java/com/wedu/exam_creation/question/dto/request/ExamMatrixDetailDTO.java) |
| `infrastructure/document` | [ContentDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/ContentDocument.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java), [VariablesDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/VariablesDocument.java) |
| `infrastructure/mapper` | [QuestionMapper](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/mapper/QuestionMapper.java) |
| `infrastructure/repository` | [QuestionRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java) |
| `usecase` | [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java) |

### Backend `questionReview`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [QuestionReviewEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/QuestionReviewEntity.java) |
| `domain/repository` | [IQuestionReviewRepository](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/repository/IQuestionReviewRepository.java) |
| `infrastructure/document` | [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java) |
| `infrastructure/mapper` | [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java) |
| `infrastructure/repository` | [QuestionReviewRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/repository/QuestionReviewRepositoryImpl.java) |
| `usecase` | [QuestionReviewService](../backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewService.java) |

Các model snapshot nội bộ:

| Layer | Source |
| --- | --- |
| domain/entity | [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [ContentRegeneratedEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/ContentRegeneratedEntity.java), [VariablesRegeneratedEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/VariablesRegeneratedEntity.java) |
| infrastructure/document | [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [ContentDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/ContentDocument.java), [VariablesDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/VariablesDocument.java) |
| usecase | [QuestionReviewUsecase](../backend/src/main/java/com/wedu/exam_creation/questionReview/usecase/QuestionReviewUsecase.java) |

### Backend `refreshToken`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [RefreshTokenEntity](../backend/src/main/java/com/wedu/exam_creation/refreshToken/domain/entity/RefreshTokenEntity.java) |
| `domain/repository` | [IRefreshTokenRepository](../backend/src/main/java/com/wedu/exam_creation/refreshToken/domain/repository/IRefreshTokenRepository.java) |
| `dto/mapper` | [RefreshTokenDTOMapper](../backend/src/main/java/com/wedu/exam_creation/refreshToken/dto/mapper/RefreshTokenDTOMapper.java) |
| `infrastructure/document` | [RefreshTokenDocument](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/document/RefreshTokenDocument.java) |
| `infrastructure/mapper` | [RefreshTokenMapper](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/mapper/RefreshTokenMapper.java) |
| `infrastructure/repository` | [RefreshTokenRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/repository/RefreshTokenRepositoryImpl.java) |
| `usecase` | [RefreshTokenService](../backend/src/main/java/com/wedu/exam_creation/refreshToken/usecase/RefreshTokenService.java) |

### Backend `security`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `constant` | [SecurityConstants](../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java) |
| `dto` | [CookieDataDTO](../backend/src/main/java/com/wedu/exam_creation/security/dto/CookieDataDTO.java) |
| `infrastructure/filter` | [JwtAuthenticationFilter](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/filter/JwtAuthenticationFilter.java) |
| `infrastructure/handler` | [OAuth2AuthenticationSuccessHandler](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java) |
| `infrastructure/principal` | [CustomUserDetails](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/principal/CustomUserDetails.java) |
| `infrastructure/provider` | [JwtTokenProvider](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/provider/JwtTokenProvider.java) |
| `infrastructure/service` | [CustomOAuth2UserService](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java), [CustomUserDetailsService](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomUserDetailsService.java) |
| `service` | [SecurityService](../backend/src/main/java/com/wedu/exam_creation/security/service/SecurityService.java) |

### Backend `storage`

Cập nhật 2026-10-25: [S3Service.updateDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) gọi helper private mới `uploadUniqueDocumentFile(MultipartFile, String folderName, String fileName)`. Helper thử tên chuẩn hóa rồi `(1)`, `(2)`… bằng `PutObjectRequest.ifNoneMatch("*")`; 412 chuyển tên, 409 retry có giới hạn, trả key đã ghi. `uploadMultipartFile` vẫn phục vụ ảnh. [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java) bổ sung kiểm tra giữ nội dung cũ, gap hậu tố, upload đồng thời nhiều instance, stream retry và phân biệt lỗi S3.

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) |
| `dto/response` | [S3ObjectInfo](../backend/src/main/java/com/wedu/exam_creation/storage/dto/response/S3ObjectInfo.java) |
| `service` | [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) |

### Backend `user`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) |
| `domain/entity` | [UserEntity](../backend/src/main/java/com/wedu/exam_creation/user/domain/entity/UserEntity.java) |
| `domain/repository` | [IUserRepository](../backend/src/main/java/com/wedu/exam_creation/user/domain/repository/IUserRepository.java) |
| `dto/mapper` | [UserDTOMapper](../backend/src/main/java/com/wedu/exam_creation/user/dto/mapper/UserDTOMapper.java) |
| `dto/request` | [LoginUserRequestDTO](../backend/src/main/java/com/wedu/exam_creation/user/dto/request/LoginUserRequestDTO.java), [RTPayloadRequestDTO](../backend/src/main/java/com/wedu/exam_creation/user/dto/request/RTPayloadRequestDTO.java) |
| `infrastructure/document` | [UserDocument](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java) |
| `infrastructure/mapper` | [UserMapper](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/mapper/UserMapper.java) |
| `infrastructure/repository` | [UserRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java) |
| `usecase` | [UserService](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java), [UserUsecase](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java) |

## Frontend App Router

Handler DELETE nằm cùng [question-files route](../frontend/app/api/moderator/question-files/route.ts). Symbols mới: [DeleteQuestionFileUsecase.execute](../frontend/usecases/moderator/DeleteQuestionFileUsecase.ts), `ModeratorService.deleteQuestionFile`, `IModeratorRepository.deleteQuestionFile`, `ModeratorRepositoryImpl.deleteQuestionFile`. [DeleteQuestionFileDialog](../frontend/presentation/components/Moderator/DeleteQuestionFileDialog.tsx) được dùng bởi trang quản lý files; native dialog quản lý focus bàn phím, Hủy/Escape chỉ đóng khi không đang gửi request.

| Route URL | Page source |
| --- | --- |
| `/auth/forgot-password` | [app/(site)/auth/forgot-password/page.tsx](../frontend/app/(site)/auth/forgot-password/page.tsx) |
| `/auth/login` | [app/(site)/auth/login/page.tsx](../frontend/app/(site)/auth/login/page.tsx) |
| `/auth/register` | [app/(site)/auth/register/page.tsx](../frontend/app/(site)/auth/register/page.tsx) |
| `/auth/reset-password` | [app/(site)/auth/reset-password/page.tsx](../frontend/app/(site)/auth/reset-password/page.tsx) |
| `/exam/[examId]` | [app/(site)/exam/[examId]/page.tsx](../frontend/app/(site)/exam/[examId]/page.tsx) |
| `/exam/all` | [app/(site)/exam/all/page.tsx](../frontend/app/(site)/exam/all/page.tsx) |
| `/generate/[draftId]/[chapterId]` | [app/(site)/generate/[draftId]/[chapterId]/page.tsx](../frontend/app/(site)/generate/[draftId]/[chapterId]/page.tsx) |
| `/generate/[draftId]/chapter` | [app/(site)/generate/[draftId]/chapter/page.tsx](../frontend/app/(site)/generate/[draftId]/chapter/page.tsx) |
| `/generate/[draftId]/details` | [app/(site)/generate/[draftId]/details/page.tsx](../frontend/app/(site)/generate/[draftId]/details/page.tsx) |
| `/generate/[draftId]/matrix` | [app/(site)/generate/[draftId]/matrix/page.tsx](../frontend/app/(site)/generate/[draftId]/matrix/page.tsx) |
| `/generate/[draftId]` | [app/(site)/generate/[draftId]/page.tsx](../frontend/app/(site)/generate/[draftId]/page.tsx) |
| `/generate/new` | [app/(site)/generate/new/page.tsx](../frontend/app/(site)/generate/new/page.tsx) |
| `/generate` | [app/(site)/generate/page.tsx](../frontend/app/(site)/generate/page.tsx) |
| `/landing-page` | [app/(site)/landing-page/page.tsx](../frontend/app/(site)/landing-page/page.tsx) |
| `/me/bill` | [app/(site)/me/bill/page.tsx](../frontend/app/(site)/me/bill/page.tsx) |
| `/me` | [app/(site)/me/page.tsx](../frontend/app/(site)/me/page.tsx) |
| `/moderator` | [app/(site)/moderator/page.tsx](../frontend/app/(site)/moderator/page.tsx) |
| `/moderator/question-bank` | [app/(site)/moderator/question-bank/page.tsx](../frontend/app/(site)/moderator/question-bank/page.tsx) |
| `/moderator/question-files` | [app/(site)/moderator/question-files/page.tsx](../frontend/app/(site)/moderator/question-files/page.tsx) |
| `/moderator/review` | [app/(site)/moderator/review/page.tsx](../frontend/app/(site)/moderator/review/page.tsx) |
| `/upload` | [app/(site)/upload/page.tsx](../frontend/app/(site)/upload/page.tsx) |
| `/` | [app/page.tsx](../frontend/app/page.tsx) |

Layouts/entry points: [root layout](../frontend/app/layout.tsx), [moderator layout](../frontend/app/(site)/moderator/layout.tsx), [proxy](../frontend/proxy.ts), [QueryProvider](../frontend/provider/QueryProvider.tsx). Review/question-bank moderator pages là placeholders; question-files có list/download. Handlers đầy đủ ở API reference.

### Frontend `domain`

| Thư mục | Files / symbols |
| --- | --- |
| `entities` | [auth.entity](../frontend/domain/entities/auth.entity.ts), [chapter.entity](../frontend/domain/entities/chapter.entity.ts), [draft.entity](../frontend/domain/entities/draft.entity.ts), [exam.entity](../frontend/domain/entities/exam.entity.ts), [generateExam.entity](../frontend/domain/entities/generateExam.entity.ts), [question.entity](../frontend/domain/entities/question.entity.ts), [questionFile.entity](../frontend/domain/entities/questionFile.entity.ts), [user.entity](../frontend/domain/entities/user.entity.ts) |
| `repositories` | [IAdminRepository](../frontend/domain/repositories/IAdminRepository.ts), [IAuthRepository](../frontend/domain/repositories/IAuthRepository.ts), [IChapterRepository](../frontend/domain/repositories/IChapterRepository.ts), [IDraftRepository](../frontend/domain/repositories/IDraftRepository.ts), [IExamRepository](../frontend/domain/repositories/IExamRepository.ts), [IExporterRepository](../frontend/domain/repositories/IExporterRepository.ts), [IModeratorRepository](../frontend/domain/repositories/IModeratorRepository.ts), [IStorageRepository](../frontend/domain/repositories/IStorageRepository.ts), [IUserRepository](../frontend/domain/repositories/IUserRepository.ts) |

### Frontend `usecases`

| Thư mục | Files / symbols |
| --- | --- |
| `admin` | [DeleteUserUsecase](../frontend/usecases/admin/DeleteUserUsecase.ts), [FindUsersUsecase](../frontend/usecases/admin/FindUsersUsecase.ts), [GetAllUsersUsecase](../frontend/usecases/admin/GetAllUsersUsecase.ts), [LockUserUsecase](../frontend/usecases/admin/LockUserUsecase.ts), [UnlockUserUsecase](../frontend/usecases/admin/UnlockUserUsecase.ts), [UpdateUserRoleUsecase](../frontend/usecases/admin/UpdateUserRoleUsecase.ts) |
| `auth` | [ChangePasswordUsecase](../frontend/usecases/auth/ChangePasswordUsecase.ts), [ForgotPasswordUsecase](../frontend/usecases/auth/ForgotPasswordUsecase.ts), [LoginUsecase](../frontend/usecases/auth/LoginUsecase.ts), [LogoutUsecase](../frontend/usecases/auth/LogoutUsecase.ts), [RegisterUsecase](../frontend/usecases/auth/RegisterUsecase.ts), [ResetPasswordUsecase](../frontend/usecases/auth/ResetPasswordUsecase.ts) |
| `chapter` | [GetAllChaptersUsecase](../frontend/usecases/chapter/GetAllChaptersUsecase.ts) |
| `draft` | [CreateDraftUsecase](../frontend/usecases/draft/CreateDraftUsecase.ts), [DeleteDraftUsecase](../frontend/usecases/draft/DeleteDraftUsecase.ts), [GenerateMatrixDetailsUsecase](../frontend/usecases/draft/GenerateMatrixDetailsUsecase.ts), [GenerateMatrixUsecase](../frontend/usecases/draft/GenerateMatrixUsecase.ts), [GetAllUserDraftUsecase](../frontend/usecases/draft/GetAllUserDraftUsecase.ts), [GetDraftUsecase](../frontend/usecases/draft/GetDraftUsecase.ts), [GetRecentDraftUsecase](../frontend/usecases/draft/GetRecentDraftUsecase.ts), [UpdateChaptersDraftUsecase](../frontend/usecases/draft/UpdateChaptersDraftUsecase.ts), [UpdateLessonsDraftUsecase](../frontend/usecases/draft/UpdateLessonsDraftUsecase.ts) |
| `exam` | [DeleteExamUsecase](../frontend/usecases/exam/DeleteExamUsecase.ts), [GenerateExamUsecase](../frontend/usecases/exam/GenerateExamUsecase.ts), [GetAllExamsUsecase](../frontend/usecases/exam/GetAllExamsUsecase.ts), [GetExamByIdUsecase](../frontend/usecases/exam/GetExamByIdUsecase.ts), [GetRecentExamUsecase](../frontend/usecases/exam/GetRecentExamUsecase.ts) |
| `exporter` | [ExportExamUsecase](../frontend/usecases/exporter/ExportExamUsecase.ts) |
| `moderator` | [GetQuestionFilesUsecase](../frontend/usecases/moderator/GetQuestionFilesUsecase.ts), [ParseUsecase](../frontend/usecases/moderator/ParseUsecase.ts) |
| `storage` | [UploadAvatarUsecase](../frontend/usecases/storage/UploadAvatarUsecase.ts), [UploadFileToQuestionBankUsecase](../frontend/usecases/storage/UploadFileToQuestionBankUsecase.ts) |
| `user` | [GetMeUsecase](../frontend/usecases/user/GetMeUsecase.ts), [UpdateAvatarUsecase](../frontend/usecases/user/UpdateAvatarUsecase.ts), [UpdateUsernameUsecase](../frontend/usecases/user/UpdateUsernameUsecase.ts) |

### Frontend `infrastructure`

| Thư mục | Files / symbols |
| --- | --- |
| `repositories` | [AdminRepositoryImpl](../frontend/infrastructure/repositories/AdminRepositoryImpl.ts), [AuthRepositoryImpl](../frontend/infrastructure/repositories/AuthRepositoryImpl.ts), [ChapterRepositoryImpl](../frontend/infrastructure/repositories/ChapterRepositoryImpl.ts), [DraftRepositoryImpl](../frontend/infrastructure/repositories/DraftRepositoryImpl.ts), [ExamsRepositoryImpl](../frontend/infrastructure/repositories/ExamsRepositoryImpl.ts), [ExporterRepositoryImpl](../frontend/infrastructure/repositories/ExporterRepositoryImpl.ts), [ModeratorRepositoryImpl](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts), [StorageRepositoryImpl](../frontend/infrastructure/repositories/StorageRepositoryImpl.ts), [UserRepositoryImpl](../frontend/infrastructure/repositories/UserRepositoryImpl.ts) |

### Frontend `presentation`

[QuestionFileRow](../frontend/presentation/components/Moderator/QuestionFileRow.tsx) tách JSX lặp trong danh sách tệp: props `file` (QuestionFileEntity kèm sizeLabel/lastModifiedLabel), `isDeleting`, `onDelete`. Hiển thị thông tin tệp, liên kết tải xuống và nút xóa; được dùng trong `QuestionFilesPage`.

Hook mới [useQuestionFiles](../frontend/presentation/hooks/Moderator/useQuestionFiles.ts), được dùng bởi [QuestionFilesPage](../frontend/app/(site)/moderator/question-files/page.tsx), chứa toàn bộ logic danh sách/xóa. Trả `files` kèm `sizeLabel`/`lastModifiedLabel`, trạng thái query/xóa, lỗi/thông báo/tệp được chọn và các handler `handleRefresh`, `handleDeleteClick`, `handleCancelDelete`, `handleConfirmDelete`. Đây là dữ liệu trình bày nội bộ; không thay QuestionFileEntity hoặc DTO HTTP.

| Thư mục | Files / symbols |
| --- | --- |
| `common` | [icons](../frontend/presentation/common/icons.tsx) |
| `components/Category` | [chapterSelect](../frontend/presentation/components/Category/chapterSelect.tsx), [lessonOptionSelect](../frontend/presentation/components/Category/lessonOptionSelect.tsx), [lessonSelect](../frontend/presentation/components/Category/lessonSelect.tsx) |
| `components/common` | [Logo](../frontend/presentation/components/common/Logo.tsx) |
| `components/Draft` | [DraftCard](../frontend/presentation/components/Draft/DraftCard.tsx) |
| `components/Exam` | [EssayContent](../frontend/presentation/components/Exam/EssayContent.tsx), [ExamCard](../frontend/presentation/components/Exam/ExamCard.tsx), [MultipleChoiceContent](../frontend/presentation/components/Exam/MultipleChoiceContent.tsx), [QuestionBlock](../frontend/presentation/components/Exam/QuestionBlock.tsx), [QuestionContent](../frontend/presentation/components/Exam/QuestionContent.tsx), [TrueFalseContent](../frontend/presentation/components/Exam/TrueFalseContent.tsx) |
| `components/Exam/Filter` | [DropDownFilter](../frontend/presentation/components/Exam/Filter/DropDownFilter.tsx), [FilterBar](../frontend/presentation/components/Exam/Filter/FilterBar.tsx) |
| `components/Generate` | [ChapterExamBlock](../frontend/presentation/components/Generate/ChapterExamBlock.tsx), [ExamChapterSelect](../frontend/presentation/components/Generate/ExamChapterSelect.tsx), [ExamLessonSelect](../frontend/presentation/components/Generate/ExamLessonSelect.tsx), [LessonExamBlock](../frontend/presentation/components/Generate/LessonExamBlock.tsx), [MatrixBlock](../frontend/presentation/components/Generate/MatrixBlock.tsx), [MatrixDetailsBlock](../frontend/presentation/components/Generate/MatrixDetailsBlock.tsx) |
| `components/layout` | [Error](../frontend/presentation/components/layout/Error.tsx), [Loader](../frontend/presentation/components/layout/Loader.tsx), [Navbar](../frontend/presentation/components/layout/Navbar.tsx), [NavBarLanding](../frontend/presentation/components/layout/NavBarLanding.tsx), [NavigationBTN](../frontend/presentation/components/layout/NavigationBTN.tsx), [Noti](../frontend/presentation/components/layout/Noti.tsx), [Success](../frontend/presentation/components/layout/Success.tsx) |
| `hooks/Auth` | [useAuth](../frontend/presentation/hooks/Auth/useAuth.ts), [useForgotPassword](../frontend/presentation/hooks/Auth/useForgotPassword.ts), [useLogin](../frontend/presentation/hooks/Auth/useLogin.ts), [useRegister](../frontend/presentation/hooks/Auth/useRegister.ts), [useResetPassword](../frontend/presentation/hooks/Auth/useResetPassword.ts) |
| `hooks/Dashboard` | [useDashboard](../frontend/presentation/hooks/Dashboard/useDashboard.ts) |
| `hooks/Draft` | [useAllDraft](../frontend/presentation/hooks/Draft/useAllDraft.ts) |
| `hooks/Exam` | [useAllExam](../frontend/presentation/hooks/Exam/useAllExam.ts), [useExam](../frontend/presentation/hooks/Exam/useExam.ts), [useExamFilter](../frontend/presentation/hooks/Exam/useExamFilter.ts) |
| `hooks/Generate` | [useChapter](../frontend/presentation/hooks/Generate/useChapter.ts) |
| `hooks/Generate/Child` | [useAlreadyStructure](../frontend/presentation/hooks/Generate/Child/useAlreadyStructure.ts), [useExamStructure](../frontend/presentation/hooks/Generate/Child/useExamStructure.ts), [useMatrix](../frontend/presentation/hooks/Generate/Child/useMatrix.ts), [useMatrixDetails](../frontend/presentation/hooks/Generate/Child/useMatrixDetails.ts), [useSelectChapterPage](../frontend/presentation/hooks/Generate/Child/useSelectChapterPage.ts), [useSelectLessonPage](../frontend/presentation/hooks/Generate/Child/useSelectLessonPage.ts) |
| `hooks/layout` | [useNavBar](../frontend/presentation/hooks/layout/useNavBar.ts) |
| `hooks/Me` | [useChangePassword](../frontend/presentation/hooks/Me/useChangePassword.ts), [useUpdateAvatar](../frontend/presentation/hooks/Me/useUpdateAvatar.ts), [useUpdateUsername](../frontend/presentation/hooks/Me/useUpdateUsername.ts) |
| `hooks/QuestionBank` | [useUploadToQuestionBank](../frontend/presentation/hooks/QuestionBank/useUploadToQuestionBank.ts) |
| `schemas` | [auth.schema](../frontend/presentation/schemas/auth.schema.ts), [draft.schema](../frontend/presentation/schemas/draft.schema.ts), [export.schema](../frontend/presentation/schemas/export.schema.ts), [generate.schema](../frontend/presentation/schemas/generate.schema.ts) |
| `services` | [AdminService](../frontend/presentation/services/AdminService.ts), [AuthService](../frontend/presentation/services/AuthService.ts), [ChapterService](../frontend/presentation/services/ChapterService.ts), [DraftService](../frontend/presentation/services/DraftService.ts), [ExamService](../frontend/presentation/services/ExamService.ts), [ExportService](../frontend/presentation/services/ExportService.ts), [ModeratorService](../frontend/presentation/services/ModeratorService.ts), [StorageService](../frontend/presentation/services/StorageService.ts), [UserService](../frontend/presentation/services/UserService.ts) |
| `utils` | [transformExamResToUI](../frontend/presentation/utils/transformExamResToUI.ts), [transformMatrixDetailsToUI](../frontend/presentation/utils/transformMatrixDetailsToUI.ts), [transformMatrixToUI](../frontend/presentation/utils/transformMatrixToUI.ts) |

### Frontend `services`

| Thư mục | Files / symbols |
| --- | --- |
| `.` | [userService](../frontend/services/userService.ts) |

### Frontend `provider`

| Thư mục | Files / symbols |
| --- | --- |
| `.` | [QueryProvider](../frontend/provider/QueryProvider.tsx) |

## Exporter

| Source | Đường dẫn trong src |
| --- | --- |
| [app](../exporter-service/src/app.ts) | `app.ts` |
| [exporterController](../exporter-service/src/routes/exporter/controller/exporterController.ts) | `routes/exporter/controller/exporterController.ts` |
| [MathContext](../exporter-service/src/routes/exporter/dto/request/MathContext.ts) | `routes/exporter/dto/request/MathContext.ts` |
| [WordPayloadRequestDTO](../exporter-service/src/routes/exporter/dto/request/WordPayloadRequestDTO.ts) | `routes/exporter/dto/request/WordPayloadRequestDTO.ts` |
| [exporter.route](../exporter-service/src/routes/exporter/exporter.route.ts) | `routes/exporter/exporter.route.ts` |
| [OptionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/OptionBuilder.ts) | `routes/exporter/infrastructure/builder/OptionBuilder.ts` |
| [QuestionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/QuestionBuilder.ts) | `routes/exporter/infrastructure/builder/QuestionBuilder.ts` |
| [latexToOmml](../exporter-service/src/routes/exporter/infrastructure/converter/latexToOmml.ts) | `routes/exporter/infrastructure/converter/latexToOmml.ts` |
| [generateWordFile](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts) | `routes/exporter/usecase/generateWordFile.ts` |

## Backend tests

Test tồn tại không đồng nghĩa pass; chưa chạy trong lượt tài liệu. ModeratorQuestionFilesTest còn kiểm route /storage/documents đã bị xóa khỏi controller hiện tại.

| Package | Test source |
| --- | --- |
| `com/wedu/exam_creation` | [BackendApplicationTests](../backend/src/test/java/com/wedu/exam_creation/BackendApplicationTests.java) |
| `com/wedu/exam_creation/auth/controller` | [AuthControllerLoginTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerLoginTest.java), [AuthControllerLogoutTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerLogoutTest.java), [AuthControllerRegisterTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerRegisterTest.java) |
| `com/wedu/exam_creation/auth/usecase` | [AuthUsecaseLoginTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLoginTest.java), [AuthUsecaseLogoutTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLogoutTest.java), [AuthUsecaseRegisterTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseRegisterTest.java) |
| `com/wedu/exam_creation/moderator/controller` | [ModeratorQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java) |
| `com/wedu/exam_creation/storage/controller` | [StorageAvatarControllerTest](../backend/src/test/java/com/wedu/exam_creation/storage/controller/StorageAvatarControllerTest.java) |
| `com/wedu/exam_creation/storage/service` | [StorageAvatarServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageAvatarServiceTest.java), [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java), [StorageQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageQuestionFilesTest.java) |
| `com/wedu/exam_creation/user/controller` | [UserControllerMeTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerMeTest.java), [UserControllerUpdateAvatarTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerUpdateAvatarTest.java), [UserControllerUpdateUsernameTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerUpdateUsernameTest.java) |
| `com/wedu/exam_creation/user/usecase` | [UserUsecaseMeTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseMeTest.java), [UserUsecaseUpdateAvatarTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseUpdateAvatarTest.java), [UserUsecaseUpdateUsernameTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseUpdateUsernameTest.java) |

## Symbols preview DOCX — 2026-10-25

- [QuestionFilePreviewDialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx): native modal, iframe sandbox, Đóng/Tải xuống, Escape và khôi phục focus.
- [useQuestionFilePreview](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts): tải binary, import động docx-preview, render, retry, AbortController, bỏ kết quả cũ; export previewDocument chứa CSP. useQuestionFiles thêm previewFile/handlePreviewClick/handleClosePreview; QuestionFileRow thêm onPreview.
- [Next GET content](../frontend/app/api/moderator/question-files/content/route.ts) → [GetQuestionFileContentUsecase.execute](../frontend/usecases/moderator/GetQuestionFileContentUsecase.ts) → IModeratorRepository.getQuestionFileContent / ModeratorRepositoryImpl.getQuestionFileContent. ModeratorService.getQuestionFileContent gọi BFF với signal.
- Backend ModeratorController.getQuestionFileContent → ModeratorUsecase.getQuestionFileContent (dùng chung validateQuestionFileKey với xóa) → S3Service.readDocumentFile → S3Client.getObjectAsBytes. [ModeratorQuestionFileContentTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) có 15 cases.

## Backend preview content symbols — 2026-10-25

[ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) là GET /moderator/question-files/content, trả ResponseEntity<byte[]>; [ModeratorUsecase.getQuestionFileContent / validateQuestionFileKey](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) kiểm key/extension và ánh xạ lỗi; [S3Service.readDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) đọc binary. [ModeratorQuestionFileContentTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) có 15 cases MVC/security/usecase/storage mock.
