# WEdu Development Decisions

Tài liệu này ghi lại các quyết định phát triển hiện hành của dự án WEdu.

Mục đích của file là phân biệt giữa:

- vấn đề kỹ thuật đã được phát hiện;
- vấn đề được phép sửa ở thời điểm hiện tại;
- công việc đã lên kế hoạch nhưng chưa triển khai;
- công việc tạm hoãn để chờ thêm dữ liệu hoặc tính năng phụ thuộc;
- phần nằm ngoài phạm vi phát triển hiện tại.

Tài liệu này **không mô tả implementation hiện tại**. Để xem trạng thái source, đọc `current-status.md`, `architecture.md`, `business-flows.md` và các tài liệu kỹ thuật liên quan.

Nếu một vấn đề được ghi nhận trong các tài liệu kỹ thuật nhưng tài liệu này đánh dấu `DEFERRED`, `FROZEN`, `OUT_OF_SCOPE` hoặc `INVESTIGATE_ONLY`, không tự sửa vấn đề đó nếu task hiện tại không yêu cầu rõ ràng.

---

## Snapshot implementation đối chiếu 2026-10-24

Bổ sung phạm vi đã được duyệt 2026-10-25: chỉ triển khai chống ghi đè khi upload DOCX. Giữ tên chuẩn hóa nếu còn trống; nếu trùng trong cùng thư mục thì chọn hậu tố `(1)`, `(2)`… nhỏ nhất còn trống trước `.docx`, kể cả upload đồng thời. `S3Service.updateDocumentFile` dùng conditional PUT và trả key thực tế; không thay schema hoặc triển khai Approve/Reject trong thay đổi này. [Luồng](business-flows.md#5-upload-docx-danh-sách-file-và-kiểm-duyệt), [validation](current-status.md).

Bổ sung phạm vi theo yêu cầu 2026-10-25: moderator được xóa từng tệp trong `question-files/` sau xác nhận ở UI; API dùng S3 DeleteObject và danh sách refetch tại chỗ. Không mở rộng thành approve/reject, audit/submission hoặc purge version history; trạng thái PLANNED của workflow kiểm duyệt vẫn giữ nguyên. [Contract triển khai](api-reference.md), [validation](current-status.md).

Đây là ghi nhận **implementation** từ working tree (gồm staged/unstaged/untracked), không tự thay đổi các quyết định PLANNED/FROZEN/DEFERRED bên dưới hoặc xác nhận nghiệm thu.

- DOCX upload backend đã có `updateDocumentFile` 50 MiB, kiểm extension/ZIP entries, key dưới question-files; image uploader tách riêng. Frontend còn nhãn 25 MB và response type boolean lệch string key. [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [StorageService](../frontend/presentation/services/StorageService.ts).
- Moderator list/download đã có API, S3 pagination, Next route và files UI. Review/question-bank là placeholders; chưa submission/approve/reject state. [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java), [files page](../frontend/app/(site)/moderator/question-files/page.tsx).
- Parse contract đã đồng bộ string[] xuyên frontend/backend. Avatar adapter/cache vẫn còn lỗi đã ghi ở current-status; không tự sửa trong task tài liệu.
- Chỉ áp dụng môn Vật lý, importer/matrix/export architecture frozen, AI retry/cost và session policy deferred như quyết định hiện có. Không dùng số lượng source mới để tự đóng feature hoặc mở scope.
## 1. Trạng thái quyết định

Các trạng thái được sử dụng:

| Trạng thái               | Ý nghĩa                                                                                              |
| ------------------------ | ---------------------------------------------------------------------------------------------------- |
| `FIX_ALLOWED`            | Đã xác định là vấn đề có thể sửa khi task liên quan được giao.                                       |
| `PLANNED`                | Tính năng/thay đổi đã nằm trong kế hoạch nhưng chưa triển khai.                                      |
| `DEFERRED`               | Đã biết vấn đề nhưng chủ động hoãn xử lý.                                                            |
| `FROZEN`                 | Luồng hiện tại được giữ nguyên; không refactor hoặc thay đổi nếu không có yêu cầu trực tiếp.         |
| `OUT_OF_SCOPE`           | Không thuộc phạm vi phát triển hiện tại.                                                             |
| `INVESTIGATE_ONLY`       | Được phép kiểm tra, test và thu thập bằng chứng nhưng chưa được thay đổi implementation/schema/data. |
| `ACCEPTED_CURRENT_SCOPE` | Hạn chế đã biết nhưng được chấp nhận trong phạm vi hiện tại.                                         |

Một technical issue xuất hiện trong `current-status.md` **không đồng nghĩa với việc phải sửa ngay**.

---

# 2. Google OAuth2

**Trạng thái: `OUT_OF_SCOPE`**

Google OAuth2 chưa nằm trong phạm vi phát triển hiện tại.

Code OAuth2 hiện có chỉ được giữ lại để project có thể build, khởi động và chạy mà không phát sinh lỗi do configuration hoặc dependency.

Các inconsistency hiện đã biết gồm:

- `ROLE_USER` khác với local `ROLE_TEACHER`;
- account type chưa đồng nhất;
- cookie/token flow khác local authentication;
- refresh-token metadata chưa đồng nhất;
- redirect/configuration còn hard-coded hoặc chưa hoàn thiện.

Các vấn đề trên không phải task cần xử lý ở thời điểm hiện tại.

### Quy tắc

Không tự:

- refactor OAuth2;
- đồng bộ OAuth2 với local JWT;
- đổi role OAuth2;
- triển khai account linking;
- bổ sung refresh-token lifecycle cho OAuth2;
- thay đổi OAuth2 success handler;
- thay đổi OAuth2 cookie contract;

trừ khi task hiện tại yêu cầu rõ ràng.

Local email/password authentication là authentication flow chính trong phạm vi hiện tại.

---

# 3. DOCX Import và Pandoc

**Trạng thái: `FROZEN`**

Luồng import DOCX/Pandoc hiện tại đã đáp ứng phạm vi phát triển hiện tại và không được ưu tiên refactor.

Các hạn chế đã biết như:

- chapter merge theo chapter name;
- Mongo transaction không rollback media đã upload lên S3;
- parser/bankStats có các điểm cần kiểm chứng;
- runtime Pandoc/workspace có thể cần cấu hình thêm khi deploy;

vẫn được giữ trong technical documentation.

### Quyết định về chapter

WEdu hiện tại chỉ áp dụng cho **môn Vật lý**.

Do đó nguy cơ hai môn khác nhau có chapter cùng tên chưa phải vấn đề cần xử lý trong phạm vi hiện tại.

**Trạng thái vấn đề chapter merge: `ACCEPTED_CURRENT_SCOPE`.**

Khi WEdu mở rộng sang nhiều môn học, cần thiết kế lại identity/query của chapter để tránh merge chapter giữa các môn.

### Quy tắc

Không tự refactor:

- `PandocConverter`;
- `ImporterService`;
- chapter merge strategy;
- parser heading convention;
- question placeholder convention;
- S3 media extraction flow;

nếu task hiện tại không trực tiếp yêu cầu.

---

# 4. Upload DOCX vào Question Bank

**Trạng thái: `PLANNED`**

Ghi nhận lịch sử trước snapshot 24/10: luồng upload question-bank có mismatch:

- UI cho phép DOCX;
- UI thể hiện giới hạn khoảng 25 MB;
- tại snapshot cũ, backend upload validation thiên về JPEG/PNG và giới hạn nhỏ hơn.

Backend hiện đã tách validation DOCX 50 MiB theo snapshot trên; frontend còn mismatch response type/nhãn dung lượng. Quyết định PLANNED được giữ nguyên cho phạm vi chưa hoàn tất, không tự xác nhận feature đã nghiệm thu.

### Phạm vi khi sửa

Task upload DOCX được phép thay đổi:

- MIME validation;
- file extension validation;
- file-size validation;
- multipart configuration nếu cần;
- frontend/backend contract liên quan trực tiếp đến upload.

### Không mở rộng task

Việc sửa upload DOCX không đồng nghĩa được phép refactor:

- Pandoc parser;
- moderator workflow;
- chapter merge;
- question persistence;
- AI generation.

---

# 5. File Submission và Moderator Review

**Trạng thái: `PLANNED`**

Luồng hiện tại:

```text
Teacher upload
    ↓
S3
```

và:

```text
Moderator upload multipart
    ↓
Pandoc
    ↓
Question Bank
```

là hai flow độc lập. Hiện đã có moderator list/download S3 và UI riêng, nhưng chưa nối sang parse bằng submission ID; listing không đồng nghĩa approval workflow implemented.

UI có thể hiển thị trạng thái như “chờ duyệt”, nhưng source hiện tại chưa có persisted moderation workflow hoàn chỉnh.

Các thành phần sau chưa được coi là implemented:

- submission record;
- `PENDING`;
- `APPROVED`;
- `REJECTED`;
- reviewer;
- reviewedAt;
- rejection reason;
- review audit;
- approve/reject API;
- liên kết persisted giữa teacher upload và moderator review.

Moderator workflow sẽ được thiết kế và triển khai như một feature riêng.

Không tự suy ra workflow từ text trên UI hoặc Telegram notification.

---

# 6. AI Question Moderation

**Trạng thái: `PLANNED`**

AI-generated question hiện chưa có moderation state hoàn chỉnh.

Các question do AI sinh hiện được persistence theo model question hiện có và Telegram notification không được coi là persisted review state.

Moderation sẽ được triển khai cùng tính năng kiểm duyệt câu hỏi dành cho `MODERATOR`.

State machine dự kiến cần được thiết kế trước khi implementation, ví dụ:

```text
PENDING_REVIEW
      ↓
Moderator review
   ↙       ↘
APPROVED   REJECTED
```

Đây mới là hướng thiết kế, chưa phải persisted schema hiện tại.

### Quyết định còn phải chốt trước implementation

Cần xác định rõ:

- câu AI `PENDING_REVIEW` có được sử dụng ngay trong đề hay không;
- câu `REJECTED` có bị xóa hay giữ audit;
- AI có tự regenerate khi bị reject hay không;
- số lần regenerate tối đa;
- ai có quyền approve/reject;
- metadata/audit nào phải lưu.

Không tự tạo schema hoặc workflow trước khi các quyết định trên được chốt.

---

# 7. AI Response Validation và Retry

**Trạng thái: `DEFERRED`**

Source hiện tại có các assumption về:

- tổng số câu AI trả về;
- thứ tự câu trả về;
- chia kết quả bằng cursor/subList;
- metadata của từng nhóm;
- errors ban đầu sau khi AI đã bù câu.

Không triển khai validation/retry architecture ở thời điểm hiện tại.

Phần này sẽ được xử lý sau khi AI moderation workflow được thiết kế vì validation, rejection và regeneration có liên quan trực tiếp với nhau.

Không tự:

- thêm retry loop;
- regenerate question;
- thay đổi error reconciliation;
- thay đổi grouping algorithm;

nếu task hiện tại không yêu cầu.

---

# 8. AI Timeout và Cost

**Trạng thái: `DEFERRED`**

Chưa quyết định timeout cuối cùng hoặc cost/quota mechanism cho DeepSeek.

Configuration có thể chứa timeout nhưng việc configuration đó có thực sự được HTTP client sử dụng phải được xem là technical finding riêng.

### Kế hoạch

Trước khi quyết định:

- chạy các test case phức tạp;
- thu thập latency;
- thu thập token usage nếu API cung cấp;
- quan sát failure/timeout;
- quan sát retry requirement;
- đánh giá chi phí thực tế.

Sau khi có dữ liệu mới quyết định:

- timeout;
- retry policy;
- request limit;
- quota;
- cost limit;
- monitoring/dashboard.

Không tự implement dashboard hoặc cost-control mechanism trước bước đánh giá này.

---

# 9. Question MongoDB Unique Index

**Trạng thái: `INVESTIGATE_ONLY`**

Source hiện tại khai báo compound unique index liên quan:

```text
(chapterId, lessonId)
```

Nếu index này thực sự tồn tại trong MongoDB, nó có thể ngăn nhiều question thuộc cùng một lesson.

Business requirement của WEdu cho phép một lesson chứa nhiều question.

Tuy nhiên trạng thái index thực tế trong database và Mongo auto-index creation chưa được xác minh.

### Được phép

- kiểm tra annotation;
- kiểm tra Mongo configuration;
- kiểm tra index thực tế trong database;
- kiểm tra query pattern;
- đánh giá index phù hợp;
- viết báo cáo/recommendation.

### Chưa được phép

Không tự:

- drop index;
- migrate index;
- thay `unique=true`;
- tạo replacement index;
- chạy migration dữ liệu;

nếu task hiện tại chưa yêu cầu rõ ràng.

Nếu sau khi kiểm tra xác nhận unique index tồn tại và gây conflict với business requirement, đề xuất migration trước khi thực hiện.

---

# 10. Matrix Generation

**Trạng thái hiện tại: `FROZEN`**

Matrix hiện được generate tự động theo algorithm hiện có.

Tỷ lệ hiện tại được xem là authoritative behavior của hệ thống trong phạm vi hiện tại.

Người dùng chưa được phép chỉnh thủ công số lượng trong matrix.

Việc frontend có state/input liên quan đến chỉnh count không có nghĩa manual matrix editing đã được hỗ trợ đầy đủ.

### Manual Matrix Editing

**Trạng thái: `DEFERRED`**

Tính năng cho phép teacher chỉnh số lượng câu sau khi generate matrix sẽ được phát triển sau.

Khi triển khai cần thiết kế:

- validation tổng số câu;
- validation tỷ lệ;
- persistence;
- regeneration matrix details;
- inventory availability;
- behavior khi quay lại step trước;
- behavior khi reload.

Không tự thêm persistence cho manual matrix editing trong các task khác.

---

# 11. Exam Generation khi thiếu câu

**Trạng thái: `FROZEN` cho đến khi AI moderation được thiết kế**

Current implementation có thể:

1. tìm question phù hợp từ bank;
2. ghi nhận phần thiếu;
3. với PLUS account, gọi AI để bổ sung;
4. persistence AI questions;
5. sử dụng các question ID được sinh trong exam.

Một số edge case đã được ghi nhận:

- bank hoàn toàn rỗng có thể fail trước AI branch;
- errors ban đầu có thể vẫn còn sau khi AI đã bù;
- AI response count/order chưa được validate đầy đủ.

Các vấn đề này chưa được tự động sửa vì behavior cuối cùng phụ thuộc AI moderation workflow.

---

# 12. DOCX Export

**Trạng thái: `FROZEN` mặc định**

Luồng export hiện tại:

```text
Frontend
    ↓
Spring Boot
    ↓
Express exporter
    ↓
Spring Boot
    ↓
Client
```

được giữ nguyên.

Không refactor kiến trúc export nếu task không yêu cầu.

Các bug cụ thể đã được ghi nhận như:

- `<image_` và `<img_>` mismatch;
- missing image cache có thể gây lỗi;
- build/start output của exporter cần kiểm chứng.

Các bug này có thể được xử lý khi có task bug-fix riêng.

Không dùng bug-fix exporter làm lý do thay đổi toàn bộ exporter architecture hoặc chuyển DOCX generation sang Spring Boot.

---

# 13. Authentication và Session

Local JWT authentication là authentication flow chính.

Access token target TTL hiện tại:

```text
5 minutes
```

Refresh token:

```text
7 days
```

Access token và refresh token được quản lý thông qua HttpOnly cookie ở frontend/Next flow.

### Known inconsistency

Backend cookie configuration có thể còn giá trị access cookie 15 phút trong khi JWT/Next flow sử dụng 5 phút.

**Trạng thái: `FIX_ALLOWED`.**

Khi task auth bug-fix được giao, được phép đồng bộ cookie lifetime với JWT lifetime.

### Logout

Access JWT hiện không có blacklist và có thể tiếp tục hợp lệ cho đến khi hết TTL.

Với access-token TTL ngắn, đây chưa được xem là blocker trong phạm vi hiện tại.

Không tự triển khai access-token blacklist.

---

# 14. Password Reset và Session Revocation

Reset password hiện chưa chắc chắn thu hồi toàn bộ refresh sessions và token consume chưa được chứng minh atomic trong concurrent requests.

**Trạng thái: `DEFERRED` cho thay đổi nghiệp vụ session revocation.**

Trước khi thay đổi cần quyết định:

> Reset password có bắt buộc logout tất cả thiết bị/session đang đăng nhập hay không?

Không tự thay đổi session revocation policy nếu chưa có quyết định nghiệp vụ.

Security bug độc lập có thể được sửa nếu task yêu cầu và không thay đổi policy trên.

---

# 15. Frontend/Backend Contract Bugs

Các mismatch đã được technical documentation ghi nhận có thể được xử lý trong bug-fixing phase.

**Trạng thái: `FIX_ALLOWED` sau khi xác minh runtime/source.**

Bao gồm:

- moderator parse frontend `boolean` vs backend `List<String>`;
- frontend `isActived` vs backend `isActive`;
- React Query profile cache key mismatch;
- legacy `/api/auth/me` vs `/api/user/me`;
- chapter frontend type vs backend `bankStats`.

### Quy tắc

Trước khi sửa:

1. đọc cả frontend và backend contract;
2. kiểm tra call site;
3. xác minh runtime behavior nếu có thể;
4. chọn một canonical contract;
5. sửa tất cả affected types/adapters/call sites;
6. không thay backend API chỉ để khớp một TypeScript type sai.

---

# 16. Logging dữ liệu nhạy cảm

**Trạng thái: `FIX_ALLOWED`**

Không log:

- password;
- reset-password payload;
- JWT;
- refresh token;
- API key;
- credentials;
- presigned URL đầy đủ nếu chứa credential/query signature.

Các logging statement vi phạm nguyên tắc trên có thể được loại bỏ trong bug-fixing task mà không cần thay đổi business behavior.

---

# 17. Payment và PLUS

**Trạng thái: `PLANNED`**

Payment hiện chưa được coi là implemented.

UI liên quan bill/payment không chứng minh hệ thống đã có:

- payment gateway;
- payment transaction;
- webhook;
- invoice;
- payment status lifecycle;
- idempotency;
- automatic account upgrade.

`accountType == PLUS` hiện chỉ là trạng thái được sử dụng bởi business logic.

Không suy ra PLUS provisioning từ field này.

Payment/PLUS upgrade sẽ được thiết kế thành feature riêng.

Trước implementation cần xác định:

- payment provider;
- transaction states;
- webhook verification;
- idempotency;
- upgrade condition;
- PLUS duration;
- renewal;
- expiration;
- quota nếu có;
- refund/cancellation behavior.

Không tự implement payment trong task không liên quan.

---

# 18. Phân biệt technical finding và development task

Các tài liệu:

```text
architecture.md
business-flows.md
current-status.md
database-schema.md
module-map.md
api-reference.md
source-catalog.md
tech-stack.md
```

có thể ghi nhận vấn đề kỹ thuật.

Việc một vấn đề xuất hiện trong các tài liệu trên **không có nghĩa vấn đề đó được phép sửa ngay**.

Ví dụ:

```text
current-status:
"OAuth contract mismatch"

development-decisions:
OUT_OF_SCOPE
```

Kết luận:

```text
Không sửa OAuth.
```

Tương tự:

```text
current-status:
"Question unique chapter+lesson"

development-decisions:
INVESTIGATE_ONLY
```

Kết luận:

```text
Có thể kiểm tra index.
Không được thay index.
```

---

# 19. Quy tắc khi thực hiện task

Trước khi implement hoặc bug-fix:

1. Đọc `development-decisions.md`.
2. Đọc `current-status.md`.
3. Đọc `module-map.md`.
4. Đọc tài liệu chuyên biệt theo hướng dẫn trong `AGENTS.md`.
5. Đọc source hiện tại liên quan trực tiếp đến task.
6. Xác định phạm vi thay đổi.
7. Kiểm tra task có đụng `FROZEN`, `DEFERRED`, `OUT_OF_SCOPE` hoặc `INVESTIGATE_ONLY` hay không.
8. Nếu có, không mở rộng phạm vi nếu người dùng chưa yêu cầu rõ ràng.

Không dùng một bug nhỏ làm lý do thực hiện refactor diện rộng.

---

# 20. Thứ tự ưu tiên khi thông tin xung đột

Khi có xung đột giữa source, docs và task, áp dụng thứ tự:

1. Yêu cầu trực tiếp mới nhất của người dùng.
2. `development-decisions.md`.
3. `AGENTS.md`.
4. `business-flows.md`.
5. `architecture.md`.
6. `current-status.md`.
7. Các tài liệu inventory/reference.
8. Source code hiện tại.

Source code là nguồn sự thật về **implementation hiện tại**, nhưng không tự quyết định **business intent hoặc development roadmap**.

Nếu source khác docs:

- xác minh source;
- báo mismatch;
- cập nhật docs phù hợp nếu task thay đổi implementation;
- không tự thay business decision chỉ vì implementation hiện tại khác decision.

---

# 21. Snapshot quyết định hiện tại

| Area                                 | Decision                                  |
| ------------------------------------ | ----------------------------------------- |
| Local authentication                 | Current primary flow                      |
| Google OAuth2                        | `OUT_OF_SCOPE`                            |
| DOCX/Pandoc importer                 | `FROZEN`                                  |
| Multi-subject chapter isolation      | `ACCEPTED_CURRENT_SCOPE` / future scaling |
| DOCX upload validation               | `PLANNED`                                 |
| File moderation                      | `PLANNED`                                 |
| AI question moderation               | `PLANNED`                                 |
| AI validation/retry                  | `DEFERRED`                                |
| AI timeout/cost policy               | `DEFERRED`                                |
| Question unique Mongo index          | `INVESTIGATE_ONLY`                        |
| Automatic matrix generation          | `FROZEN`                                  |
| Manual matrix editing                | `DEFERRED`                                |
| Exam/AI edge-case changes            | `DEFERRED` until moderation design        |
| DOCX exporter architecture           | `FROZEN`                                  |
| Specific exporter bugs               | `FIX_ALLOWED` when assigned               |
| JWT/cookie TTL mismatch              | `FIX_ALLOWED`                             |
| Password-reset session policy        | `DEFERRED`                                |
| Frontend/backend contract mismatches | `FIX_ALLOWED` after verification          |
| Sensitive logging                    | `FIX_ALLOWED`                             |
| Payment/PLUS provisioning            | `PLANNED`                                 |

---

## 22. Cập nhật tài liệu này

Cập nhật `development-decisions.md` khi:

- chủ dự án chốt một business rule;
- một deferred feature được đưa vào development;
- phạm vi dự án thay đổi;
- một phần frozen được mở lại;
- một investigation được chấp thuận triển khai;
- một feature chuyển từ planned sang implementation.

Không tự chuyển trạng thái chỉ vì code đã được thay đổi ngoài tài liệu.

Khi một decision thay đổi implementation, cập nhật đồng thời các tài liệu liên quan theo `AGENTS.md`.
