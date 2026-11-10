# API reference WEdu

## Refund APIs — 2026-11-09

All routes are authenticated; backend enforces ROLE_TEACHER ownership or ROLE_ADMIN. BFF path is `/api` + backend path. No bearer/signature/provider payload is exposed. Amounts are VND integers; timestamps ISO instants. Read-only ADMIN transaction lookup remains unchanged.

| Backend (same BFF method) | Authorization / request | Response |
| --- | --- | --- |
| GET /payment/orders | Teacher, page default0 | Orders{items:[Order{id,amount,status,createdAt,paidAt}],page,hasNext}; 10 rows/page. |
| POST /payment/orders/{id}/refunds | Teacher owner; Idempotency-Key alphanumeric/hyphen16–80; {reason,policyVersion:"plus-refund-v1"} | Item; reason trimmed3–1000. Same key returns same request; another active request/already refunded order409. |
| GET /payment/refunds | Teacher; page | Page restricted to authenticated user. |
| GET /payment/refunds/{id} | Teacher owner | Item, absent/not owned404. |
| POST /payment/refunds/{id}/cancel | Teacher owner; {revision} | Item; only pending/verification states. |
| GET /admin/refunds | ADMIN; keyword,status,from,to,page | Page; escaped literal email substring OR exact request/order ID; status/date constraints AND. |
| GET /admin/refunds/{id} | ADMIN | Item. |
| POST /admin/refunds/{id}/approve | ADMIN; {revision,legacyException?:boolean,reason?:string} | Item; legacy exception requires reason, cannot override hard ineligibility or other unknowns. |
| POST /admin/refunds/{id}/reject | ADMIN; {revision,reason} | Item; mandatory reason3–1000. |
| POST /admin/refunds/{id}/reconcile | ADMIN; no body required | Item; query only, 300-second throttle. |

Item = {id,orderId,email,amount,status,reason,policyVersion,eligibility,decisionReason,createdAt,updatedAt,paidAt,lastAiUsedAt,activeAi,projectedPlusExpiresAt,entitlementVerifiable,providerCode,providerStatus,refundTransactionNo,revision,history:[{at,status,actor,reason}],checks:[{key,result,reason}]}. Nullable timestamps/provider fields and email are allowed; projected expiry is unavailable for completed/unverifiable grants. Check keys PAYMENT/WINDOW/NOT_REFUNDED/AI_UNUSED/ENTITLEMENT; results PASSED/FAILED/UNKNOWN. Eligibility/checks are saved at submission/decision, not recomputed by GET.

Page = {items,page,pageSize:10,totalElements,totalPages,enabled,policyVersion}; page zero-based0–1000000. Dates yyyy-MM-dd in Asia/Ho_Chi_Minh with inclusive UI end date converted to exclusive next midnight; newest creation then ID descending. Keyword max254. IDs: order64 lowercase hex, refund32 lowercase hex. Mutations require current revision; stale/incompatible state409; invalid input400; anonymous/role failures401/403. Safe application error code REFUND_ERROR; uncertain provider errors retain PROCESSING instead of returning a false terminal result. Feature-disabled mutations503. BFF bounds JSON body to8192 characters and masks server failures. [Controller](../backend/src/main/java/com/wedu/exam_creation/payment/controller/RefundController.java), [provider reference](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).

> Cập nhật mới nhất 2026-11-10: phần hoàn tiền ở đầu tài liệu đối chiếu code đã merge vào main `cbcb49a`. Các snapshot theo ngày bên dưới giữ giá trị lịch sử, không chứng nhận deploy hoặc validation mới.

## Snapshot hiện tại — 2026-11-06

Đối chiếu source/config tại commit `abcde6e`. Inventory declaration hiện tại: **54 Spring endpoints** trong 11 controllers; **51 Next HTTP handlers** trong 49 route files; **1 Express endpoint**. Đây là declaration, không phải kết quả gọi server; POST [action] của review là một handler chọn approve/reject.

Các mục có ngày cũ là bằng chứng lịch sử của từng lần triển khai; khi mâu thuẫn, dùng snapshot hiện tại và source được dẫn. Không suy ra production hoặc kiểm thử end-to-end đã đạt từ việc có code.

## Khu vực ADMIN hai tính năng — 2026-11-01

Không thêm/đổi backend API hoặc HTTP method/path. UI mới dùng GET `/api/admin/users`, GET `/api/admin/users/search?keyword=...`, PUT `/api/admin/{id}/role?role=ROLE_TEACHER|ROLE_MODERATOR`, PUT lock/unlock và DELETE `/api/admin/{id}/delete`; adapters tiếp tục map tới routes backend hiện có bên dưới. Hai GET response thành công thêm `Cache-Control: private, no-store`.

Frontend `AdminUserEntity {id,email,username,role,avatarUrl:string|null,isActive:boolean|null,accountType,createdAt:string|null,updatedAt:string|null}` phản ánh CommonUserResponseDTO; các method tài khoản trong IAdminRepository/AdminRepositoryImpl/AdminService và 3 usecase có return annotation dùng type này. `UserEntity.isActived` của profile không đổi. Delete vẫn boolean, false không được báo là xóa thành công. Server tiếp tục chặn tự sửa/xóa/khóa và ADMIN khác; BFF role vẫn chỉ chấp nhận TEACHER/MODERATOR, không mở rộng cấp ADMIN.

Routes UI mới `/admin/users` và `/admin` redirect tới users; `/admin/ai` giữ nguyên. [Type](../frontend/domain/entities/adminUser.entity.ts), [service](../frontend/presentation/services/AdminService.ts), [validation](current-status.md#khu-vực-admin-hai-tính-năng--2026-11-01).


## Quản lý usage/budget AI — 2026-11-01

Backend [AiManagementController](../backend/src/main/java/com/wedu/exam_creation/ai/controller/AiManagementController.java), `@PreAuthorize(hasRole('ADMIN'))`, mọi response thành công no-store; Next BFF thêm `Cache-Control: private, no-store`, đọc cookie accessToken rồi gửi Bearer như cũ. Không token/plaintext key trong response.

| Backend | Next BFF | Input → output |
| --- | --- | --- |
| GET `/admin/ai/usage` | GET `/api/admin/ai/usage` | from/to YYYY-MM-DD bắt buộc, model tùy chọn → UsageReportDTO |
| GET `/admin/ai/status` | GET `/api/admin/ai/status` | → StatusDTO của tháng Việt Nam hiện tại, mọi model |
| GET `/admin/ai/budget` | GET `/api/admin/ai/budget` | → BudgetDTO |
| PUT `/admin/ai/budget` | PUT `/api/admin/ai/budget` | BudgetDTO → BudgetDTO revision mới |

[AiManagementDTO](../backend/src/main/java/com/wedu/exam_creation/ai/dto/AiManagementDTO.java) là các class Lombok nested, frontend mirror tại [aiManagement.entity.ts](../frontend/domain/entities/aiManagement.entity.ts):

- BudgetDTO `{revision:long, monthlyBudgetUsd:string|null, monthlyTokenQuota:long|null}`. PUT thay toàn bộ limits, null tắt từng giới hạn. Revision bắt buộc >=0, lần đầu 0; revision/quota phải là JSON integer (không làm tròn số thập phân hoặc nhận chuỗi). Tiền dạng decimal dương, tối đa 12 chữ số nguyên/8 thập phân; token nguyên dương <=9007199254740991. Không có cấu hình trả revision 0 và hai limit null. Stale revision trả 409, không overwrite.
- DailyUsageDTO `{day:string|null, requests, unknownUsageRequests, inputTokens, outputTokens, totalTokens:long, estimatedCostUsd:string}`. Tổng có day null, daily có YYYY-MM-DD.
- UsageReportDTO `{from,to,model:string|null,timezone,currency,models:string[],totals:DailyUsageDTO,days:DailyUsageDTO[]}`. from/to bao gồm đầu/cuối, 1–366 ngày, timezone Asia/Ho_Chi_Minh, currency USD; bù ngày trống. Model blank/>100 ký tự không hợp lệ; BFF bỏ empty model để lấy tất cả. models gồm model từng dùng và model cấu hình hiện tại.
- StatusDTO `{month,budget,usage,remainingBudgetUsd:string|null,remainingTokens:long|null,budgetUsedPercent:number|null,tokensUsedPercent:number|null,level:NORMAL|WARNING|BLOCKED,reasons:string[],balance:BalanceDTO}`. Remaining không âm; null là chưa giới hạn. Level warning >=80%, blocked >=100% hoặc số dư mới unavailable; bộ lọc report không tác động status.
- BalanceDTO `{available:boolean|null,updatedAt:Instant|null,stale:boolean,balances:BalanceInfoDTO[]}`; BalanceInfoDTO `{currency:USD|CNY,totalBalance,grantedBalance,toppedUpBalance:string}`. Chưa lấy được số dư trả null/empty/stale, không giả lập số dư 0. Không quy đổi tiền.

Gọi AI của tạo đề/sinh lại thêm lỗi `{message,code}`: 429 AI_BUDGET_EXCEEDED / AI_TOKEN_QUOTA_EXCEEDED / AI_BALANCE_UNAVAILABLE; 503 AI_TRACKING_UNAVAILABLE / AI_PRICING_UNAVAILABLE. GlobalExceptionHandler và BFF giữ status/code, frontend hiển thị message. Request bị chặn không gọi provider và không mất lượt review. API quản trị validation 400, sai role 403; BFF thiếu cookie 401, backend JwtAuthenticationFilter vẫn giữ contract có sẵn trả 400 khi thiếu Authorization header. Chưa đổi các route/DTO câu hỏi/exam hoặc exporter payload.

## Kiểm duyệt câu hỏi AI — 2026-10-31

- `POST /moderator/questions/{id}/approve` ↔ `POST /api/moderator/questions/{id}/approve`: JSON `{updatedAt: ISO instant|null, reviewRevision: nonnegative integer}`, thành công 204. Duyệt nội dung gốc hoặc promote bản sinh lại mới nhất, status APPROVED; xóa QuestionsReview.
- `POST /moderator/questions/{id}/reject` ↔ `POST /api/moderator/questions/{id}/reject`: cùng request, 200 `{status: "PENDING_REVIEW"|"NEEDS_CONSIDERATION", regenerationCount: number}`. Kết quả 1–4 là snapshot mới; kết quả 5 không gọi AI và xóa review.
- GET pending giữ paging/filter trên Questions gốc, thêm `regenerationCount`, `reviewRevision`, `latestRegeneratedQuestion: QuestionDTO|null`. Chỉ gửi snapshot mới nhất; image key được presign trên DTO, mô tả vẫn là text. Root question/options vẫn là bản gốc; UI ưu tiên nội dung snapshot nếu có.
- PATCH multipart hiện có thêm `payload.reviewRevision`; thiếu tương đương 0 để tương thích editor cũ chưa có snapshot. Khi revision hiện tại khác 0, client thiếu revision bị 409. Sửa latest snapshot nếu có, không tăng lượt, không thay nội dung Questions; updatedAt câu gốc vẫn tăng để guard mọi thao tác.

Các API giữ ROLE_MODERATOR và `private, no-store`. Next kiểm ID 24 ký tự hex; ID/payload sai ở Next trả 400. Backend approve/reject kiểm input/reviewRevision nhưng chưa kiểm định dạng ID; lỗi này đang được test `invalidInputDoesNotReadOrWrite` phát hiện, không chứng nhận direct backend malformed ID trả 400. Thiếu chương/bài/yêu cầu sinh lại trả 400; không có câu 404; status/version/review revision cũ trả 409; auth theo JWT/cookie hiện tại. AI lỗi hoặc output sai trả 500 với thông báo chung, không tính lượt. Next thiếu cookie trả 401, upstream mất kết nối trả 502; không chuyển nguyên thông báo provider. Next dùng chung dynamic route `[id]/[action]`, chỉ chấp nhận approve/reject.

[Controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [input](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ReviewPendingQuestionDTO.java), [result](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectPendingQuestionResponseDTO.java), [Next route](../frontend/app/api/moderator/questions/[id]/[action]/route.ts).


## Chỉnh sửa câu hỏi chờ duyệt — 2026-10-29

`PATCH /moderator/questions/{id}` ↔ `PATCH /api/moderator/questions/{id}`; ROLE_MODERATOR, multipart/form-data, thành công 204/no-store. ID là 24 ký tự hex. Part `payload` là JSON application/json: `{updatedAt: ISO instant|null, question: ContentDTO, options: ContentDTO[]}`. updatedAt lấy từ response danh sách, kiểm tra phiên bản trước upload và trong conditional update. Giữ số phương án, không nhận thay đổi metadata/status. ContentDTO giữ `{template, variables:{math:{math_N:latex}, image:{img_N:description}}}`; image trong request chỉ là mô tả, không được tin làm URL/key. Part ảnh tên `question.img_N` hoặc `options.<index>.img_N`; mỗi tên một file, JPEG/PNG tối đa 5 MiB, tối đa 10 ảnh thay thế/request. Next giới hạn JSON part 1 MiB; backend giữ multipart limits hiện có.

Backend chỉ giữ variables đang được template tham chiếu; key lặp dùng chung một giá trị trong cùng content. Thiếu LaTeX, placeholder sai dạng, thiếu ảnh cho mô tả AI, thừa/duplicate file, số phương án sai hoặc giới hạn không hợp lệ: 400. UI kiểm tra cú pháp LaTeX bằng KaTeX; backend kiểm tra binding/nonblank/độ dài, không thêm LaTeX parser Java. Template tối đa 50000 ký tự/content; công thức tối đa 10000 ký tự; options tối đa 50 và số lượng phải bằng bản gốc.

Next proxy đặt request body limit 65 MiB khớp giới hạn multipart Spring để không truncate nhiều ảnh; cấu hình này không bỏ kiểm tra 5 MiB/file hoặc 10 ảnh/request ở route/usecase. [Config](../frontend/next.config.ts).

Không upload mới thì backend lấy key ảnh cũ từ database cho đúng section/key; không nhận key từ client. Giá trị theo convention `media/` là key ảnh, còn lại là mô tả AI; API đọc chỉ presign key, giữ mô tả dạng text. Upload vào `media/questions/{id}/<uuid>.<extension>` bằng S3Service hiện có, sau đó atomic update question/options/updatedAt khi status vẫn PENDING_REVIEW và version còn khớp. Thiếu câu trả 404; trạng thái/version đã đổi trả 409; sai role 403; token theo JWT. Next thiếu cookie 401, mất kết nối backend 502, message chung không lộ URL/token. GET pending bổ sung `updatedAt`; `variables.image` có thể là mô tả AI hoặc URL ký của ảnh đã có. JSON persisted image là S3 key, không presigned URL.

Upload lỗi hoặc conditional conflict: cố gắng xóa ảnh mới đã upload, giữ dữ liệu cũ và ảnh cũ. Không xóa media cũ vì có thể được đề khác tham chiếu. Nếu DB mất acknowledgement, chưa xác định đã commit hay chưa: giữ ảnh mới để tránh xóa nhầm media đã được lưu; UI yêu cầu tải lại trước thử lại. Cleanup lỗi có thể để lại orphan, chỉ log thông báo chung. Không transaction phân tán hoặc retry tự động.

[Controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [usecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/EditPendingQuestionUsecase.java), [request DTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/EditPendingQuestionDTO.java), [Next route](../frontend/app/api/moderator/questions/[id]/route.ts), [tests](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorEditPendingQuestionTest.java). Section này thay thế nhận định chưa có edit API trong snapshot 2026-10-27 bên dưới; approve/regenerate vẫn chưa có.

## Danh sách câu hỏi chờ duyệt — 2026-10-27

`GET /moderator/questions/pending` ↔ `GET /api/moderator/questions/pending`; ROLE_MODERATOR. Query `page` mặc định 0 khi không truyền (zero-based, tối đa Java int), `search` tùy chọn tối đa 200 ký tự, `questionType` tùy chọn tối đa 100 ký tự. Filter trim; search literal không phân biệt hoa thường trên nội dung/chương/bài, loại câu khớp chính xác. Page rỗng không hợp lệ. Size cố định 10, không nhận tùy chỉnh từ client.

Response 200 `{items: PendingQuestionDTO[], page, size:10, totalElements, totalPages}`. Item có fields QuestionDTO (id/chapterId/lessonId/exerciseType/difficultyLevel/learningOutcomes/questionType/question/options) cộng `chapter`, `lesson`, `status`, `createdAt`. Nội dung giữ template/math/image placeholders; image values trong response là URL ký từ S3 key, không persist. Fallback khi chương/bài mất. Lọc Questions.status=PENDING_REVIEW trước count/skip/limit; sort createdAt DESC, _id DESC. Không có kết quả hoặc page vượt phạm vi trả items=[], total theo filter, không 404. Legacy status=null và APPROVED không được lấy. QuestionsReview không là nguồn nội dung danh sách.

Page âm/không phải integer/overflow hoặc filter quá dài: 400. Sai role: 403; token sai: theo JWT hiện có. Next thiếu cookie: 401, backend không kết nối: 502, chuyển status với message chung; Bearer chỉ phía server. Cache-Control private, no-store. Không log presigned URL/token, không thêm write API duyệt/sửa/regenerate.

Bằng chứng: [controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [usecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [repository](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java), [Next route](../frontend/app/api/moderator/questions/pending/route.ts), [types](../frontend/domain/entities/pendingQuestion.entity.ts).

## Collection câu hỏi cần duyệt — 2026-10-26

Module questionReview dùng QuestionReviewDocument và RegeneratedQuestionDocument cùng ContentDocument/VariablesDocument nội bộ cho collection QuestionsReview theo source hiện tại. Không dùng entity/document/mapper từ question. Snapshot giữ cùng cấu trúc dữ liệu và placeholders; không thêm/đổi endpoint hoặc HTTP DTO, chưa có API regenerate hoặc review.

Bằng chứng: [QuestionReviewDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/QuestionReviewDocument.java), [RegeneratedQuestionEntity](../backend/src/main/java/com/wedu/exam_creation/questionReview/domain/entity/RegeneratedQuestionEntity.java), [RegeneratedQuestionDocument](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/document/RegeneratedQuestionDocument.java), [QuestionReviewMapper](../backend/src/main/java/com/wedu/exam_creation/questionReview/infrastructure/mapper/QuestionReviewMapper.java).

## Trạng thái câu hỏi — 2026-10-26

Không thêm/đổi endpoint hoặc HTTP request/response DTO. QuestionService.insertFromDocx mặc định APPROVED cho câu parse; nhánh AI truyền PENDING_REVIEW qua method private. status chỉ lưu trong QuestionEntity/QuestionDocument, không đưa vào QuestionDTO hay payload frontend/exporter. Parse/approve file vẫn trả IDs theo contract cũ.

Bằng chứng: [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java).

## Duyệt/từ chối tệp — 2026-10-25

Backend `ROLE_MODERATOR`, Next BFF lấy cookie accessToken và chuyển Bearer chỉ phía server; cache private/no-store. Giữ list/content/delete/upload/parse cũ.

| Backend → Next | Request | Response |
| --- | --- | --- |
| POST /moderator/question-files/approve → POST /api/moderator/question-files/approve | multipart key:string, file:DOCX tùy chọn; không nhận subject từ client | 200 ApproveQuestionFileResponseDTO {questionIds:string[], fileDeleted:boolean, message:string} |
| POST /moderator/question-files/reject → POST /api/moderator/question-files/reject | JSON RejectQuestionFileDTO {key:string, reason:string} | 204, kể cả email gửi thất bại sau xóa |

Key phải đúng `question-files/<subject>/<filename>.docx`, không segment ./.. hoặc subject rỗng. Approve xác nhận tệp gốc tồn tại; bytes bản gốc hoặc bản chỉnh sửa được validate không rỗng, ≤50 MiB, extension DOCX và ZIP entries, rồi gọi parseDocument với subject từ key. Parse lỗi 500 giữ gốc; parse thành công/xóa lỗi vẫn 200 với fileDeleted=false để client chỉ retry DELETE, không approve lại trong phiên hiện tại.

Reject lý do trim 1–2000 ký tự; email từ basename trước dấu '-' cuối, validate địa chỉ trước khi xóa. Tên chuẩn hóa không chứa '-', email có '-' vẫn giữ. Key/email/reason không hợp lệ 400, sai role 403, token không hợp lệ 401, tệp mất 404, lỗi storage 500. Email lỗi chỉ WARN loại exception, không retry/Telegram và không đổi 204. Next thiếu cookie 401, mất kết nối 502, giữ status backend với message chung, không lộ chi tiết provider. Không có idempotency bền vững, lịch sử hoặc status database.

[ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [approve DTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/ApproveQuestionFileResponseDTO.java), [reject DTO](../backend/src/main/java/com/wedu/exam_creation/moderator/dto/RejectQuestionFileDTO.java), [Next approve](../frontend/app/api/moderator/question-files/approve/route.ts), [Next reject](../frontend/app/api/moderator/question-files/reject/route.ts).


Inventory đã cập nhật 2026-11-06:54 endpoint Spring,51 handler Next và1 endpoint Express; không phải kết quả gọi server. Backend/frontend kiểm duyệt tệp đã được merge vào main. [Module map](module-map.md), [business flows](business-flows.md), [current status](current-status.md).

## Contract cần lưu ý

### Xóa tệp tin câu hỏi — 2026-10-25

`DELETE /moderator/question-files?key=<S3 key>` yêu cầu ROLE_MODERATOR; Next proxy cùng query tại `DELETE /api/moderator/question-files`. Cookie access token chỉ chuyển thành Bearer tại adapter phía server. Key lấy nguyên từ danh sách, không lấy presigned URL hoặc suy từ fileName; Axios params encode key đúng một lần. Thành công `204 No Content`, kể cả object đã không còn; không gọi HEAD trước DELETE. `400` cho key rỗng/ngoài `question-files/`, key thư mục hoặc segment `.`/`..`; AWS lỗi trả `500` với message chung, không trả chi tiết AWS. Quyền JWT giữ nguyên (`403` sai role, `401` token rỗng/không hợp lệ; thiếu/sai định dạng header vẫn theo filter hiện có). Next thiếu cookie trả `401`, mất kết nối backend trả `502`.

Nguồn: [ModeratorController.deleteQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [ModeratorUsecase.deleteQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [Next handler](../frontend/app/api/moderator/question-files/route.ts), [DeleteQuestionFileUsecase](../frontend/usecases/moderator/DeleteQuestionFileUsecase.ts). Không thay DTO danh sách, database hoặc API upload.

- Moderator parse: multipart file/subject, HTTP 200 JSON string[] ID câu hỏi, đồng bộ List<String> Java với Promise<string[]> xuyên frontend. Không có parse-by-S3-key hoặc approval endpoint.
- Question files: GET /moderator/question-files yêu cầu MODERATOR, trả S3ObjectInfo[]; Next GET /api/moderator/question-files dùng private/no-store, 401 thiếu access cookie, chuyển Bearer tới backend. Frontend QuestionFileEntity chứa key/fileName/size/lastModified/presignedUrl. URL tải ký hạn 60 phút với attachment.
- DOCX: POST /storage/question-file và Next /api/storage/question-file trả string S3 key thực tế, multipart file/subject; backend lấy email từ principal. Key mặc định `question-files/<subject>/<email>-<NormalizedName>.docx`; từ 2026-10-25, tên trùng thêm `(1)`, `(2)`… trước `.docx`, chọn số nhỏ nhất còn trống bằng conditional PUT. Không thay method/path/payload/response type; caller phải dùng key trả về, không tự dựng từ tên upload. StorageService.uploadFileToQuestionBank hiện typed string. Backend kiểm DOCX ≤50 MiB, UI text 50 MB. [S3Service.updateDocumentFile/uploadUniqueDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [tests](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java).
- Không còn declaration POST /storage/question-bank, Next /api/storage/question-bank, GET /storage/documents. Test preservesLegacyDocumentsEndpoint còn mong route cuối trả 200; chưa chạy test trong lượt tài liệu này.
- Avatar backend PATCH /user/update-avatar; frontend adapter hiện gọi /user/avatar/${s3Key}, đây là mismatch còn lại. Signature/type frontend không chứng minh remote JSON đúng schema.

Nguồn: [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [Next files GET](../frontend/app/api/moderator/question-files/route.ts), [QuestionFileEntity](../frontend/domain/entities/questionFile.entity.ts), [StorageService](../frontend/presentation/services/StorageService.ts), `ModeratorQuestionFilesTest` (snapshot cũ; file không còn trong source hiện tại).

## Spring MVC endpoints

Public paths theo SecurityConstants; quyền class/method theo @PreAuthorize và owner theo principal/usecase/repository. ADMIN, MODERATOR và ROLE_TEACHER payment không tự suy ra role hierarchy. Cột signature lấy trực tiếp từ source, không chứng nhận HTTP runtime.

| Method + path | Symbol / source | Input → output (Java signature) |
| --- | --- | --- |
| `GET /admin/users` | [AdminController.getAllUsers](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<CommonUserResponseDTO>>` |
| `GET /admin/users/search` | [AdminController.findUsers](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam(name = "keyword") String keyword → ResponseEntity<List<CommonUserResponseDTO>>` |
| `PUT /admin/{userId}/role` | [AdminController.updateUserRole](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId, @RequestParam("role") String role → ResponseEntity<CommonUserResponseDTO>` |
| `PUT /admin/{userId}/lock` | [AdminController.lockUser](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId → ResponseEntity<CommonUserResponseDTO>` |
| `PUT /admin/{userId}/unlock` | [AdminController.unLockUser](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("userId") String userId → ResponseEntity<CommonUserResponseDTO>` |
| `DELETE /admin/{userId}` | [AdminController.deleteUser](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String userId → ResponseEntity<Boolean>` |
| `GET /admin/ai/usage` | [AiManagementController.usage](../backend/src/main/java/com/wedu/exam_creation/ai/controller/AiManagementController.java) | `@RequestParam String from, @RequestParam String to, @RequestParam(required = false) String model → ResponseEntity<UsageReportDTO>` |
| `GET /admin/ai/status` | [AiManagementController.status](../backend/src/main/java/com/wedu/exam_creation/ai/controller/AiManagementController.java) | `Không body/parameter → ResponseEntity<StatusDTO>` |
| `GET /admin/ai/budget` | [AiManagementController.budget](../backend/src/main/java/com/wedu/exam_creation/ai/controller/AiManagementController.java) | `Không body/parameter → ResponseEntity<BudgetDTO>` |
| `PUT /admin/ai/budget` | [AiManagementController.updateBudget](../backend/src/main/java/com/wedu/exam_creation/ai/controller/AiManagementController.java) | `@RequestBody BudgetDTO input → ResponseEntity<BudgetDTO>` |
| `POST /auth/register` | [AuthController.register](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@Valid @RequestBody NewUserRequestDTO newUser, HttpServletResponse response → ResponseEntity<UserResponseDTO>` |
| `POST /auth/login` | [AuthController.login](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@Valid @RequestBody LoginUserRequestDTO payload, HttpServletResponse response → ResponseEntity<UserResponseDTO>` |
| `POST /auth/logout` | [AuthController.logout](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@RequestHeader("Authorization") String authorization → ResponseEntity<Boolean>` |
| `PATCH /auth/change-password` | [AuthController.changePassword](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody ChangePasswordRequestDTO reqPayload, @RequestHeader("Authorization") String authorization → ResponseEntity<Boolean>` |
| `POST /auth/regenerate-access-token` | [AuthController.regenerateAccessToken](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@CookieValue(value = "refreshToken") String refreshToken → ResponseEntity<NewAccessTokenResponseDTO>` |
| `POST /auth/forgot-password` | [AuthController.forgotPassword](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@RequestParam("email") String email → ResponseEntity<String>` |
| `POST /auth/reset-password` | [AuthController.resetPassword](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) | `@Valid @RequestBody ResetPasswordRequestDTO dto → ResponseEntity<String>` |
| `GET /chapter/chapters` | [ChapterController.getAll](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java) | `Không body/parameter → ResponseEntity<List<ChapterResponseDTO>>` |
| `POST /draft/create` | [DraftController.createDraft](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody CreateDraftDTO payload → ResponseEntity<String>` |
| `GET /draft/{draftId}` | [DraftController.getDraft](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<DraftDTO>` |
| `PUT /draft/{draftId}/chapter` | [DraftController.updateChapters](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId, @RequestBody UpdateDraftDTO payload → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/{chapterId}/lesson` | [DraftController.updateLessons](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId, @PathVariable("chapterId") String chapterId, @RequestBody UpdateDraftDTO payload → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/generate-matrix` | [DraftController.generateMatrix](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId → ResponseEntity<Boolean>` |
| `PUT /draft/{draftId}/generate-matrix-details` | [DraftController.generateMatrixDetails](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable("draftId") String draftId → ResponseEntity<Boolean>` |
| `GET /draft/recent` | [DraftController.getRecentDraft](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<DraftDTO>>` |
| `GET /draft/user-drafts` | [DraftController.getAllUserDrafts](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<DraftDTO>>` |
| `DELETE /draft/{draftId}` | [DraftController.deleteDraft](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<Boolean>` |
| `POST /exam/generate/{draftId}` | [ExamController.generateExam](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String draftId → ResponseEntity<ExamGeneratedResponseDTO>` |
| `GET /exam/{examId}` | [ExamController.getExamById](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String examId → ResponseEntity<ExamDetailDTO>` |
| `GET /exam/user-exams` | [ExamController.getAllExams](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<ExamDTO>>` |
| `GET /exam/recent` | [ExamController.getRecentExam](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<List<ExamDTO>>` |
| `DELETE /exam/{examId}` | [ExamController.deleteExam](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String examId → ResponseEntity<Boolean>` |
| `POST /exporter/exam` | [ExporterController.exportExam](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestBody ExportRequestDTO payload → ResponseEntity<Resource>` |
| `POST /moderator/questions/{id}/approve` | [ModeratorController.approvePendingQuestion](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@PathVariable("id") String id, @RequestBody ReviewPendingQuestionDTO input → ResponseEntity<Void>` |
| `POST /moderator/questions/{id}/reject` | [ModeratorController.rejectPendingQuestion](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@PathVariable("id") String id, @RequestBody ReviewPendingQuestionDTO input → ResponseEntity<RejectPendingQuestionResponseDTO>` |
| `PATCH /moderator/questions/{id}` | [ModeratorController.editPendingQuestion](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@PathVariable("id") String id, @RequestPart("payload") EditPendingQuestionDTO payload, @RequestParam MultiValueMap<String, MultipartFile> files → ResponseEntity<Void>` |
| `GET /moderator/question-files` | [ModeratorController.listQuestionFiles](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `Không body/parameter → ResponseEntity<List<S3ObjectInfo>>` |
| `GET /moderator/questions/pending` | [ModeratorController.listPendingQuestions](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@RequestParam(name = "page", required = false) String page, @RequestParam(name = "search", defaultValue = "") String search, @RequestParam(name = "questionType", defaultValue = "") String questionType → ResponseEntity<PendingQuestionPageDTO>` |
| `DELETE /moderator/question-files` | [ModeratorController.deleteQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@RequestParam("key") String key → ResponseEntity<Void>` |
| `GET /moderator/question-files/content` | [ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@RequestParam("key") String key → ResponseEntity<byte[]>` |
| `POST /moderator/parse` | [ModeratorController.parseDocument](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("file") MultipartFile file, @RequestParam("subject") String subject → ResponseEntity<List<String>>` |
| `POST /moderator/question-files/approve` | [ModeratorController.approveQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("key") String key, @RequestParam(value = "file", required = false) MultipartFile file → ResponseEntity<ApproveQuestionFileResponseDTO>` |
| `POST /moderator/question-files/reject` | [ModeratorController.rejectQuestionFile](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) | `@RequestBody RejectQuestionFileDTO request → ResponseEntity<Void>` |
| `GET /payment/plan` | [PaymentController.plan](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `Không body/parameter → ResponseEntity<PaymentDTO.Plan>` |
| `POST /payment/orders` | [PaymentController.create](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestHeader("Idempotency-Key") String key, HttpServletRequest request → ResponseEntity<PaymentDTO.Order>` |
| `GET /payment/orders/{id}` | [PaymentController.get](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id → ResponseEntity<PaymentDTO.Order>` |
| `POST /payment/orders/{id}/verify` | [PaymentController.verify](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id → ResponseEntity<PaymentDTO.Order>` |
| `GET /payment/vnpay/ipn` | [PaymentController.ipn](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `@RequestParam MultiValueMap<String, String> parameters → ResponseEntity<Map<String, String>>` |
| `POST /payment/orders/{id}/return` | [PaymentController.confirmReturn](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @PathVariable String id, @RequestBody PaymentDTO.ReturnConfirmation request → ResponseEntity<PaymentDTO.Order>` |
| `POST /storage/avatar` | [StorageController.uploadAvatar](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) | `@RequestParam("file") MultipartFile file → ResponseEntity<String>` |
| `POST /storage/question-file` | [StorageController.uploadDocumentToQuestionBank](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("file") MultipartFile file, @RequestParam("subject") String subject → ResponseEntity<String>` |
| `GET /user/me` | [UserController.getMe](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) | `@AuthenticationPrincipal CustomUserDetails principal → ResponseEntity<CommonUserResponseDTO>` |
| `PATCH /user/update-avatar` | [UserController.updateAvatar](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("s3Key") String s3Key → ResponseEntity<Boolean>` |
| `PATCH /user/update-username` | [UserController.updateUser](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) | `@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("username") String username → ResponseEntity<Boolean>` |

## Next.js route handlers

Methods/paths có thể khác Spring, ví dụ Next PUT profile chuyển sang Spring PATCH. Cột wiring là imports của route hiện tại; helper handler.ts không được tính thành endpoint.

| Method + path | Source | Wiring imports |
| --- | --- | --- |
| `GET /api/admin/ai/budget` | [route.ts](<../frontend/app/api/admin/ai/budget/route.ts>) | `handleAiManagement` |
| `PUT /api/admin/ai/budget` | [route.ts](<../frontend/app/api/admin/ai/budget/route.ts>) | `handleAiManagement` |
| `GET /api/admin/ai/status` | [route.ts](<../frontend/app/api/admin/ai/status/route.ts>) | `handleAiManagement` |
| `GET /api/admin/ai/usage` | [route.ts](<../frontend/app/api/admin/ai/usage/route.ts>) | `handleAiManagement` |
| `GET /api/admin/users` | [route.ts](<../frontend/app/api/admin/users/route.ts>) | `GetAllUsersUsecase, AdminRepositoryImpl` |
| `GET /api/admin/users/search` | [route.ts](<../frontend/app/api/admin/users/search/route.ts>) | `FindUsersUsecase, AdminRepositoryImpl` |
| `DELETE /api/admin/[userId]/delete` | [route.ts](<../frontend/app/api/admin/[userId]/delete/route.ts>) | `AdminRepositoryImpl, DeleteUserUsecase` |
| `PUT /api/admin/[userId]/lock` | [route.ts](<../frontend/app/api/admin/[userId]/lock/route.ts>) | `AdminRepositoryImpl, LockUserUsecase` |
| `PUT /api/admin/[userId]/role` | [route.ts](<../frontend/app/api/admin/[userId]/role/route.ts>) | `UpdateUserRoleUsecase, AdminRepositoryImpl` |
| `PUT /api/admin/[userId]/unlock` | [route.ts](<../frontend/app/api/admin/[userId]/unlock/route.ts>) | `UnlockUserUsecase, AdminRepositoryImpl` |
| `PUT /api/auth/change-password` | [route.ts](<../frontend/app/api/auth/change-password/route.ts>) | `ChangePasswordUsecase, ChangePasswordPayload, ChangePasswordEntity, AuthRepositoryImpl` |
| `POST /api/auth/forgot-password` | [route.ts](<../frontend/app/api/auth/forgot-password/route.ts>) | `ForgotPasswordUsecase, AuthRepositoryImpl` |
| `POST /api/auth/login` | [route.ts](<../frontend/app/api/auth/login/route.ts>) | `LoginPayload, LoginPayloadEntity, AuthRepositoryImpl, LoginUsecase` |
| `POST /api/auth/logout` | [route.ts](<../frontend/app/api/auth/logout/route.ts>) | `AuthRepositoryImpl, LogoutUsecase` |
| `POST /api/auth/register` | [route.ts](<../frontend/app/api/auth/register/route.ts>) | `NewUserRequestEntity, RegisterUsecase, RegisterPayload, AuthRepositoryImpl` |
| `POST /api/auth/reset-password` | [route.ts](<../frontend/app/api/auth/reset-password/route.ts>) | `AuthRepositoryImpl, ResetPasswordPayload, ResetPasswordUsecase` |
| `GET /api/chapter` | [route.ts](<../frontend/app/api/chapter/route.ts>) | `GetAllChaptersUsecase, ChapterRepositoryImpl` |
| `POST /api/draft/create` | [route.ts](<../frontend/app/api/draft/create/route.ts>) | `CreateDraftUsecase, CreateDraftEntity, DraftRepositoryImpl, CreateDraftPayload` |
| `GET /api/draft/recent` | [route.ts](<../frontend/app/api/draft/recent/route.ts>) | `GetRecentDraftUsecase, DraftRepositoryImpl` |
| `GET /api/draft/user-drafts` | [route.ts](<../frontend/app/api/draft/user-drafts/route.ts>) | `GetAllUserDraftsUsecase, DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/chapter` | [route.ts](<../frontend/app/api/draft/[draftId]/chapter/route.ts>) | `UpdateChaptersDraftUsecase, UpdateDraftEntity, DraftRepositoryImpl, UpdateDraftPayload` |
| `DELETE /api/draft/[draftId]/delete` | [route.ts](<../frontend/app/api/draft/[draftId]/delete/route.ts>) | `DeleteDraftUsecase, DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/generate-matrix` | [route.ts](<../frontend/app/api/draft/[draftId]/generate-matrix/route.ts>) | `GenerateMatrixUsecase, DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/generate-matrix-details` | [route.ts](<../frontend/app/api/draft/[draftId]/generate-matrix-details/route.ts>) | `GenerateMatrixDetailsUsecase, DraftRepositoryImpl` |
| `GET /api/draft/[draftId]` | [route.ts](<../frontend/app/api/draft/[draftId]/route.ts>) | `GetDraftUsecase, DraftRepositoryImpl` |
| `PUT /api/draft/[draftId]/[chapterId]/lesson` | [route.ts](<../frontend/app/api/draft/[draftId]/[chapterId]/lesson/route.ts>) | `UpdateDraftEntity, DraftRepositoryImpl, UpdateDraftPayload, UpdateLessonsDraftUsecase` |
| `POST /api/exam/generate/[draftId]` | [route.ts](<../frontend/app/api/exam/generate/[draftId]/route.ts>) | `GenerateExamUsecase, ExamsRepositoryImpl` |
| `GET /api/exam/recent` | [route.ts](<../frontend/app/api/exam/recent/route.ts>) | `GetRecentExamUsecase, ExamsRepositoryImpl` |
| `GET /api/exam/user-exams` | [route.ts](<../frontend/app/api/exam/user-exams/route.ts>) | `GetAllExamsUsecase, ExamsRepositoryImpl` |
| `DELETE /api/exam/[examId]/delete` | [route.ts](<../frontend/app/api/exam/[examId]/delete/route.ts>) | `DeleteExamUsecase, ExamsRepositoryImpl` |
| `GET /api/exam/[examId]` | [route.ts](<../frontend/app/api/exam/[examId]/route.ts>) | `GetExamByIdUsecase, ExamsRepositoryImpl` |
| `POST /api/exporter/exam` | [route.ts](<../frontend/app/api/exporter/exam/route.ts>) | `ExportExamUsecase, ExporterRepositoryImpl, ExportPayload` |
| `POST /api/moderator/parse` | [route.ts](<../frontend/app/api/moderator/parse/route.ts>) | `ModeratorRepositoryImpl, ParseUsecase` |
| `POST /api/moderator/question-files/approve` | [route.ts](<../frontend/app/api/moderator/question-files/approve/route.ts>) | `ModeratorRepositoryImpl, ApproveQuestionFileUsecase, reviewError, reviewHeaders` |
| `GET /api/moderator/question-files/content` | [route.ts](<../frontend/app/api/moderator/question-files/content/route.ts>) | `ModeratorRepositoryImpl, GetQuestionFileContentUsecase` |
| `POST /api/moderator/question-files/reject` | [route.ts](<../frontend/app/api/moderator/question-files/reject/route.ts>) | `ModeratorRepositoryImpl, RejectQuestionFileUsecase, reviewError, reviewHeaders` |
| `DELETE /api/moderator/question-files` | [route.ts](<../frontend/app/api/moderator/question-files/route.ts>) | `ModeratorRepositoryImpl, GetQuestionFilesUsecase, DeleteQuestionFileUsecase` |
| `GET /api/moderator/question-files` | [route.ts](<../frontend/app/api/moderator/question-files/route.ts>) | `ModeratorRepositoryImpl, GetQuestionFilesUsecase, DeleteQuestionFileUsecase` |
| `GET /api/moderator/questions/pending` | [route.ts](<../frontend/app/api/moderator/questions/pending/route.ts>) | `ModeratorRepositoryImpl, GetPendingQuestionsUsecase` |
| `PATCH /api/moderator/questions/[id]` | [route.ts](<../frontend/app/api/moderator/questions/[id]/route.ts>) | `ModeratorRepositoryImpl, EditPendingQuestionUsecase` |
| `POST /api/moderator/questions/[id]/[action]` | [route.ts](<../frontend/app/api/moderator/questions/[id]/[action]/route.ts>) | `ModeratorRepositoryImpl, ReviewPendingQuestionUsecase` |
| `POST /api/payment/orders` | [route.ts](<../frontend/app/api/payment/orders/route.ts>) | `handlePayment` |
| `POST /api/payment/orders/[id]/return` | [route.ts](<../frontend/app/api/payment/orders/[id]/return/route.ts>) | `handlePayment` |
| `GET /api/payment/orders/[id]` | [route.ts](<../frontend/app/api/payment/orders/[id]/route.ts>) | `handlePayment` |
| `POST /api/payment/orders/[id]/verify` | [route.ts](<../frontend/app/api/payment/orders/[id]/verify/route.ts>) | `handlePayment` |
| `GET /api/payment/plan` | [route.ts](<../frontend/app/api/payment/plan/route.ts>) | `handlePayment` |
| `POST /api/storage/avatar` | [route.ts](<../frontend/app/api/storage/avatar/route.ts>) | `UploadAvatarUsecase, StorageRepositoryImpl` |
| `POST /api/storage/question-file` | [route.ts](<../frontend/app/api/storage/question-file/route.ts>) | `UploadFileToQuestionBankUsecase, StorageRepositoryImpl` |
| `GET /api/user/me` | [route.ts](<../frontend/app/api/user/me/route.ts>) | `GetMeUsecase, UserRepositoryImpl` |
| `PUT /api/user/update/avatar` | [route.ts](<../frontend/app/api/user/update/avatar/route.ts>) | `UpdateAvatarUsecase, UserRepositoryImpl` |
| `PUT /api/user/update/username` | [route.ts](<../frontend/app/api/user/update/username/route.ts>) | `UpdateUsernameUsecase, UserRepositoryImpl` |

## Express endpoint

POST /exporter/exam → exportAsWord → GenerateWordFileUsecase.execute; [app mount](../exporter-service/src/app.ts), [route](../exporter-service/src/routes/exporter/exporter.route.ts), [controller](../exporter-service/src/routes/exporter/controller/exporterController.ts). Input WordPayloadRequestDTO; output DOCX attachment; JSON limit 50 MB. Entry point không khai báo authentication middleware.

## DOCX preview chỉ đọc — 2026-10-25

- Backend `GET /moderator/question-files/content?key=<S3 key>`: ROLE_MODERATOR, `ModeratorController.getQuestionFileContent → ModeratorUsecase.getQuestionFileContent → S3Service.readDocumentFile`. Trả 200 binary MIME DOCX, `Cache-Control: private, no-store`, `X-Content-Type-Options: nosniff`.
- Key nguyên từ listing, thuộc `question-files/`, không thư mục/segment . hoặc .., đuôi .docx không phân biệt hoa/thường. Key sai/thiếu 400, object không còn 404, S3 lỗi khác 500 với message chung. Quyền JWT giữ nguyên.
- Next `GET /api/moderator/question-files/content?key=...` yêu cầu access cookie; chuyển Bearer chỉ phía server qua [GetQuestionFileContentUsecase](../frontend/usecases/moderator/GetQuestionFileContentUsecase.ts) và adapter. Thiếu cookie 401, backend không kết nối 502; bảo toàn status backend, trả message chung và private/no-store cả lỗi. Không nhận URL do client cung cấp.
- [Next route](../frontend/app/api/moderator/question-files/content/route.ts), [backend controller](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [tests](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java). Không đổi DTO listing hoặc schema.

## Backend question-file content — 2026-10-25

GET /moderator/question-files/content?key=<S3 key> yêu cầu ROLE_MODERATOR, trả 200 binary DOCX (application/vnd.openxmlformats-officedocument.wordprocessingml.document), Cache-Control private, no-store và X-Content-Type-Options nosniff. Key thuộc question-files/, không thư mục hoặc segment . / .., đuôi .docx không phân biệt hoa/thường. Thiếu/key sai trả 400; S3 404 trả 404; lỗi S3 khác trả 500 với message chung. Không đổi DTO danh sách hoặc schema. [ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java).

## Backend VNPay contract — 2026-11-05

JWT/ROLE_TEACHER required except GET IPN; owner mismatch returns 404; responses are private/no-store.

| Spring route | Contract |
| --- | --- |
| GET /payment/plan | {amount:20000,currency:VND,durationDays:30,available:boolean} |
| POST /payment/orders | Idempotency-Key header: 16–80 alphanumeric/hyphen characters, no body; returns own PaymentDTO.Order. Same user/key returns existing order; a deleted record is recreated with a different provider reference. |
| GET /payment/orders/{id} | Owned order, expires pending records when necessary. |
| POST /payment/orders/{id}/verify | No body. Querydr throttled per order/30s; unavailable or unverified provider response returns 503 {message,code:PAYMENT_VERIFICATION_UNAVAILABLE}, Retry-After:30. |
| POST /payment/orders/{id}/return | PaymentDTO.ReturnConfirmation {parameters:[[name,value],...]}; max32 unique vnp_* pairs, name suffix1–64 alphanumeric/underscore, value max1024 characters. Backend validates receipt then applies transaction; invalid400, missing/other owner404. |
| GET /payment/vnpay/ipn | Public server callback, rejects duplicate query keys. {RspCode,Message}:00 accepted,01 missing,02 already confirmed,04 amount mismatch,97 invalid signature/parameters,99 retry. |

PaymentDTO.Order: {orderId,amount,status,expiresAt,plusExpiresAt,paymentUrl,manualReview}; statuses PENDING/SUCCEEDED/FAILED/CANCELLED/EXPIRED. New expiry is createdAt+300s; checkout URL exists only while payable. Common user DTOs add nullable plusExpiresAt; accountType is effective FREE after expiry. JWT claims/roles stay unchanged. [Controller](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java), [DTO](../backend/src/main/java/com/wedu/exam_creation/payment/dto/PaymentDTO.java).

## Frontend payment contracts — 2026-11-06

Matching backend payment endpoints are already available in main through PR #131; this frontend commit adds the UI and BFF on that updated base.

Next BFF requires accessToken cookie, forwards server-side bearer authentication and returns private/no-store responses. Backend still enforces teacher/ownership.

| Next route | Expected backend contract |
| --- | --- |
| GET /api/payment/plan | GET /payment/plan → {amount,currency,durationDays,available} |
| POST /api/payment/orders | POST /payment/orders with validated Idempotency-Key(16–80 alphanumeric/hyphen), no body → PaymentOrder |
| GET /api/payment/orders/{id} | GET /payment/orders/{id}; id64 lowercase hex; own order only |
| POST /api/payment/orders/{id}/verify | POST /payment/orders/{id}/verify;503 preserves safe PAYMENT_VERIFICATION_UNAVAILABLE and Retry-After30 |
| POST /api/payment/orders/{id}/return | POST /payment/orders/{id}/return; {parameters:[[name,value],...]},1–32 unique vnp_* pairs, name suffix1–64 alphanumeric/underscore, value max1024 chars |
| GET /payment/vnpay/ipn | Exact public GET rewrite to BACKEND_URL/payment/vnpay/ipn, preserving callback query. No new Next route for IPN. |

PaymentOrder={orderId,amount,status,expiresAt,plusExpiresAt,paymentUrl,manualReview}, status PENDING/SUCCEEDED/FAILED/CANCELLED/EXPIRED. UserEntity adds optional nullable plusExpiresAt. Return data are untrusted until a backend response;400/403/404 do not trigger client-side grant. Missing owned order404 is distinct from network/500/HTML404 and can offer an explicit new purchase. [Handler](../frontend/app/api/payment/handler.ts), [types](../frontend/domain/entities/payment.entity.ts).

## ADMIN transaction lookup — 2026-11-08

Both backend endpoints require ROLE_ADMIN. Existing ROLE_TEACHER payment endpoints retain their owner/role rules. BFF reads accessToken cookie and forwards bearer server-side; all BFF responses use private,no-store.

| Backend | BFF | Contract |
| --- | --- | --- |
| GET /admin/transactions | GET /api/admin/transactions | keyword (trimmed, max254), status, from/to (yyyy-MM-dd), page (zero-based, default0). Returns {items,page,pageSize:10,totalElements,totalPages}. |
| POST /admin/transactions/{id}/lookup | POST /api/admin/transactions/{id}/lookup | No body; id64 lowercase hex. Returns {order,provider,amountMismatch,queriedAt}. No persistence mutations. |

Item/order = {orderId,email,amount,status,createdAt,transactionNo}; nullable email/createdAt/transactionNo. Amount is VND. Provider = {transactionNo,amount,bankCode,paidAt,responseCode,transactionStatus}; optional information is nullable. Amount converts VNPay minor units by dividing100; dates are ISO instants. queriedAt is backend response time, not provider update time.

Keyword OR-matches exact order ID, exact stored VNPay transactionNo, or escaped case-insensitive email substring. Combine keyword/status/date constraints with AND. Dates use Asia/Ho_Chi_Minh; inclusive UI end date maps to exclusive next midnight. Sort createdAt DESC,_id DESC. Status filters use stored PENDING/SUCCEEDED/FAILED/CANCELLED/EXPIRED. Empty list is200, not an upstream call.

Lookup requires signature verification by VnpayGateway, matching merchant/reference/command, and valid amount/status/date on successful query. Query response00 is distinct from transaction status00. A verified91 maps to404 VNPAY_TRANSACTION_NOT_FOUND; missing local order maps to404 ORDER_NOT_FOUND. Unavailable configuration, network errors, unverified/malformed responses or other provider errors map to503 TRANSACTION_LOOKUP_UNAVAILABLE. Invalid input400, unauthenticated/unauthorized401/403 through existing security. No raw provider payload, checkout URL, clientIp or signatures are exposed.

Evidence: [controller](../backend/src/main/java/com/wedu/exam_creation/payment/controller/AdminTransactionController.java), [DTO](../backend/src/main/java/com/wedu/exam_creation/payment/dto/AdminTransactionDTO.java), [BFF](../frontend/app/api/admin/transactions/handler.ts). Provider reference: [VNPay querydr](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html).
