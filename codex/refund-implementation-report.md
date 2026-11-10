# Báo cáo triển khai hoàn tiền PLUS — 2026-11-09

Đối chiếu lại ngày 2026-11-10: code đã merge vào main `cbcb49a`, gồm backend `bf42f2b` qua PR #137, tests `0c619db` qua PR #138 và frontend `f1064c4` qua PR #139. Lượt chuẩn bị commit tài liệu chỉ kiểm tra source, cấu hình, inventory và liên kết; không chạy lại application build/tests hoặc xác minh dịch vụ thật. Kết quả bên dưới là các lần validation đã chạy trong quá trình triển khai.

Validation tài liệu 2026-11-10: **1.576 liên kết file nội bộ và 64 liên kết heading Markdown** đều có target; `git diff --check` đạt. Chỉ 10 file codex/ được chuẩn bị cho commit tài liệu trên main; không sửa application code/config hoặc đưa secrets vào tài liệu.

Đã triển khai sau khi người dùng duyệt phương án. Trước đây chỉ có tra cứu giao dịch; nay teacher gửi/hủy/theo dõi yêu cầu và ADMIN đánh giá/duyệt/từ chối/đối soát. Hoàn toàn bộ trong 48 giờ khi chưa nhận kết quả AI; có ngoại lệ bằng chứng AI cũ với giải trình. AI bị giữ trong lúc chờ, hạn PLUS tiếp tục chạy. Hoàn thành thu hồi đúng phần thời gian còn lại của đơn và gửi email qua outbox.

Contract/schema: thêm 10 endpoint Spring và BFF tương ứng (GET /payment/orders bổ sung vào route POST hiện có), RefundDTO/TypeScript types; ba collection RefundRequests/RefundAccounts/RefundAiReceipts; metadata khoảng quyền trên PaymentOrders và payload refund trên PaymentEmails. Chi tiết ở [API](api-reference.md#refund-apis--2026-11-09) và [schema](database-schema.md#refund-persistence--2026-11-09).

## Validation và giới hạn

- Nhóm backend payment/refund: **133 tests, 125 passed, 0 failures/errors, 8 skipped**. Trong đó 49 cases mới cho refund: 46 passed, 3 Mongo concurrency cases skipped. 5 cases Mongo payment có sẵn cũng skipped vì chưa có dedicated replica set URI.
- Frontend production build đạt; TypeScript `--noEmit --incremental false` đạt; targeted ESLint toàn bộ frontend trong scope đạt. Harness tạm ngoài repository với React SSR/JSDOM và NextRequest/BFF đạt **24 checks** (disabled actions, ngoại lệ, XSS escaping, ownership input, revision, authentication, no-store và che lỗi server).
- Ở lần validation triển khai ban đầu, `git diff --check` đạt và **1.576 liên kết file nội bộ** trong 10 tài liệu thay đổi không có target bị thiếu. Inventory dưới đây ghi đủ **73 file** của thay đổi triển khai; trạng thái chưa stage/commit/push tại lần đó là lịch sử, các commit code/test hiện đã merge như ghi phía trên.
- Full backend suite đã chạy trong phiên, trước khi hoàn tất các test refund mới: **460 tests, 2 failures, 1 error, 5 skipped**. Hai assertion failures đã được ghi nhận trước task trong current-status: `AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit` và `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite`. Error `BackendApplicationTests.contextLoads` do kết nối MongoDB TLS không khả dụng trong môi trường hiện tại. Không sửa các lỗi ngoài scope và không coi full suite là pass.

Source hiện tại yêu cầu khai báo REFUND_ENABLED trong môi trường chạy, không có default trong application.yml: false tắt mutation/polling, true bật. Spring không tự đọc backend/.env. Trạng thái bật trên môi trường thực tế vẫn unknown; chưa kiểm chứng VNPay sandbox thật, SMTP thật hoặc replica set test. Unknown provider/commit không tự gửi lại lệnh hoàn tiền; sau 24 giờ dừng polling tự động, ADMIN tiếp tục xác minh. Không có nút force-success hoặc force-clear tác vụ AI không rõ. Nếu DB không đủ bằng chứng quyền PLUS, không tự hạ toàn bộ tài khoản về FREE. Các tình huống cần điều tra vận hành được giữ nguyên dữ liệu; không mở rộng sang hoàn một phần hay bù thời gian chờ.

Nguồn protocol: [VNPay refund/querydr](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html); quy tắc replacement giữ identity: [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-crud-operations.html). Không chứng nhận production/E2E từ unit tests.

## Toàn bộ file thay đổi

| File | Thay đổi |
| --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java>) | Truyền userId vào luồng sinh câu hỏi để gắn bằng chứng cho đúng người dùng. |
| [backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java](<../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java>) | Email kết quả hoàn tiền dạng plaintext và dùng chung hàm gửi kết quả. |
| [backend/src/main/java/com/wedu/exam_creation/payment/controller/RefundController.java](<../backend/src/main/java/com/wedu/exam_creation/payment/controller/RefundController.java>) | API teacher/admin; ownership từ principal, quyền ROLE_*, response no-store và lỗi an toàn. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentEmailEntity.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentEmailEntity.java>) | Thêm refundId/message cho email sự kiện hoàn tiền. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java>) | Thêm khoảng cấp PLUS, refundedAt và flags bảo vệ dữ liệu mới/legacy. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundAccountEntity.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundAccountEntity.java>) | Model khóa AI theo người dùng, bằng chứng và các yêu cầu đang giữ quyền. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundEntity.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundEntity.java>) | Model yêu cầu, điều kiện, lịch sử, revision và lần gửi VNPay. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentRepository.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentRepository.java>) | Thêm truy vấn các đơn theo chủ sở hữu để tính quyền và hiển thị lịch sử. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IRefundRepository.java](<../backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IRefundRepository.java>) | Port persistence cho yêu cầu, account gate, bằng chứng AI và đối soát. |
| [backend/src/main/java/com/wedu/exam_creation/payment/dto/RefundDTO.java](<../backend/src/main/java/com/wedu/exam_creation/payment/dto/RefundDTO.java>) | Contract tạo/duyệt, danh sách/chi tiết yêu cầu và lịch sử giao dịch. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentEmailDocument.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentEmailDocument.java>) | Persist thông báo hoàn tiền trong outbox hiện có. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java>) | Persist khoảng quyền và metadata hoàn tiền của từng đơn. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAccountDocument.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAccountDocument.java>) | Mongo RefundAccounts, identity string và revision cho gate. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAiReceiptDocument.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAiReceiptDocument.java>) | Mongo bằng chứng AI được ghi cùng transaction lưu đề. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundDocument.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundDocument.java>) | Mongo yêu cầu hoàn tiền, snapshots điều kiện và lịch sử xử lý. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/mapper/RefundMapper.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/mapper/RefundMapper.java>) | MapStruct giữa domain entities và Mongo documents hoàn tiền. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/RefundIndexConfig.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/RefundIndexConfig.java>) | Khởi tạo collection/index, unique idempotency và index phục vụ đối soát. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java>) | Truy vấn đơn theo user; CAS lưu bổ sung metadata quyền/hoàn tiền. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/RefundRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/RefundRepositoryImpl.java>) | Mongo truy vấn, CAS không thay identity, khóa polling và bằng chứng AI. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java](<../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java>) | Ký/xác minh refund toàn phần và truy vấn mã hoàn tiền; giữ lookup hiện có. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentJobs.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentJobs.java>) | Gửi sự kiện refund qua cùng lease/retry outbox thanh toán. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java>) | Ghi khoảng PLUS, độ phủ theo dõi AI và nguồn legacy khi xác nhận thanh toán. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundAiService.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundAiService.java>) | Tuần tự hóa AI/refund; kiểm tra lại PLUS, giữ attempt và ghi kết quả transaction. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundEntitlementService.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundEntitlementService.java>) | Tính/thu hồi thời hạn còn lại theo đơn, dời các quyền mua sau và bảo vệ legacy. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundJobs.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundJobs.java>) | Lập lịch đối soát hoàn tiền mỗi phút. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundSettings.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundSettings.java>) | Bind feature flag và phiên bản điều khoản hoàn tiền; cấu hình YAML yêu cầu biến môi trường riêng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundTransactions.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundTransactions.java>) | Transaction ngắn, retry abort xác định; không replay commit không rõ. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundUsecase.java>) | Điều phối gửi/hủy/duyệt/từ chối, điều kiện, VNPay, thu hồi quyền và email outbox. |
| [backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java](<../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java>) | Gate trước gọi AI; đánh dấu kết quả AI lưu cùng transaction đề. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java>) | Chỉ cập nhật accountType/plusExpiresAt khi thu hồi thời gian đã mua. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java>) | Expose facade quyền PLUS cho payment, không truy cập user adapter trực tiếp. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java>) | Đọc hạn PLUS, nhận diện legacy và facade thu hồi quyền. |
| [backend/src/main/resources/application.yml](<../backend/src/main/resources/application.yml>) | Bổ sung payment.refund.enabled từ REFUND_ENABLED bắt buộc khai báo; giữ cấu hình VNPay hiện có. |
| [backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayRefundGatewayTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayRefundGatewayTest.java>) | 3 cases checksum/request refund, response bị sửa và response rỗng. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundAiServiceTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundAiServiceTest.java>) | 6 cases giữ AI, quyền PLUS cũ, commit/rollback và kết quả không rõ. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundControllerTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundControllerTest.java>) | 6 cases phân quyền, principal ownership, input và no-store. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundEntitlementTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundEntitlementTest.java>) | 7 cases nhiều lần mua, thời gian đã trôi, legacy và thu hồi một lần. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundMongoIntegrationTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundMongoIntegrationTest.java>) | 3 cases đồng thời trên replica set riêng; tự tạo/xóa DB test, skip nếu thiếu URI. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundPersistenceTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundPersistenceTest.java>) | 3 cases roundtrip snapshots và CAS giữ đúng identity/revision. |
| [backend/src/test/java/com/wedu/exam_creation/payment/RefundUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/payment/RefundUsecaseTest.java>) | 21 cases điều kiện, ownership, ngoại lệ, replay, provider và quyết định cuối. |
| [codex/api-reference.md](<api-reference.md>) | Toàn bộ route, role, request/response và quy tắc lỗi/phân trang. |
| [codex/architecture.md](<architecture.md>) | Ranh giới service, transaction, gate AI và provider HTTP/outbox. |
| [codex/business-flows.md](<business-flows.md>) | Luồng teacher/admin, chuyển trạng thái, điều kiện và thông báo. |
| [codex/current-status.md](<current-status.md>) | Trạng thái source, feature flag, unknown và giới hạn vận hành. |
| [codex/database-schema.md](<database-schema.md>) | Collections, fields, states, indexes và tương thích legacy. |
| [codex/development-decisions.md](<development-decisions.md>) | Quyết định người dùng đã duyệt, điều khoản và phạm vi hoàn tiền mới. |
| [codex/module-map.md](<module-map.md>) | Symbols, entrypoints và liên kết contracts/schemas mới. |
| [codex/refund-implementation-report.md](<refund-implementation-report.md>) | Báo cáo này: behavior, kết quả kiểm tra và inventory đầy đủ từng file. |
| [codex/source-catalog.md](<source-catalog.md>) | Liên kết inventory hoàn tiền, API, schema và module map. |
| [codex/tech-stack.md](<tech-stack.md>) | Cấu hình enablement, lịch jobs, commands và môi trường test riêng. |
| [frontend/app/(site)/admin/transactions/page.tsx](<../frontend/app/(site)/admin/transactions/page.tsx>) | Thêm lối vào Xử lý yêu cầu hoàn tiền từ Quản lý giao dịch. |
| [frontend/app/(site)/admin/transactions/refunds/page.tsx](<../frontend/app/(site)/admin/transactions/refunds/page.tsx>) | Compose RefundManager cho trang xử lý yêu cầu ADMIN. |
| [frontend/app/(site)/me/bill/page.tsx](<../frontend/app/(site)/me/bill/page.tsx>) | Thêm liên kết Giao dịch và hoàn tiền trong trang thanh toán. |
| [frontend/app/(site)/me/bill/refunds/page.tsx](<../frontend/app/(site)/me/bill/refunds/page.tsx>) | Compose RefundManager cho trang giao dịch và hoàn tiền teacher. |
| [frontend/app/api/admin/refunds/[id]/approve/route.ts](<../frontend/app/api/admin/refunds/[id]/approve/route.ts>) | Expose BFF /api/admin/refunds/[id]/approve qua handler xác thực dùng chung. |
| [frontend/app/api/admin/refunds/[id]/reconcile/route.ts](<../frontend/app/api/admin/refunds/[id]/reconcile/route.ts>) | Expose BFF /api/admin/refunds/[id]/reconcile qua handler xác thực dùng chung. |
| [frontend/app/api/admin/refunds/[id]/reject/route.ts](<../frontend/app/api/admin/refunds/[id]/reject/route.ts>) | Expose BFF /api/admin/refunds/[id]/reject qua handler xác thực dùng chung. |
| [frontend/app/api/admin/refunds/[id]/route.ts](<../frontend/app/api/admin/refunds/[id]/route.ts>) | Expose BFF /api/admin/refunds/[id] qua handler xác thực dùng chung. |
| [frontend/app/api/admin/refunds/route.ts](<../frontend/app/api/admin/refunds/route.ts>) | Expose BFF /api/admin/refunds qua handler xác thực dùng chung. |
| [frontend/app/api/payment/orders/[id]/refunds/route.ts](<../frontend/app/api/payment/orders/[id]/refunds/route.ts>) | Expose BFF /api/payment/orders/[id]/refunds qua handler xác thực dùng chung. |
| [frontend/app/api/payment/orders/route.ts](<../frontend/app/api/payment/orders/route.ts>) | Expose BFF /api/payment/orders qua handler xác thực dùng chung. |
| [frontend/app/api/payment/refunds/[id]/cancel/route.ts](<../frontend/app/api/payment/refunds/[id]/cancel/route.ts>) | Expose BFF /api/payment/refunds/[id]/cancel qua handler xác thực dùng chung. |
| [frontend/app/api/payment/refunds/[id]/route.ts](<../frontend/app/api/payment/refunds/[id]/route.ts>) | Expose BFF /api/payment/refunds/[id] qua handler xác thực dùng chung. |
| [frontend/app/api/payment/refunds/route.ts](<../frontend/app/api/payment/refunds/route.ts>) | Expose BFF /api/payment/refunds qua handler xác thực dùng chung. |
| [frontend/app/api/refund-handler.ts](<../frontend/app/api/refund-handler.ts>) | BFF dùng chung: xác thực cookie, giới hạn/kiểm tra payload và che lỗi server. |
| [frontend/domain/entities/refund.entity.ts](<../frontend/domain/entities/refund.entity.ts>) | Types yêu cầu, điều kiện, phân trang và nhãn trạng thái tiếng Việt. |
| [frontend/domain/repositories/IRefundRepository.ts](<../frontend/domain/repositories/IRefundRepository.ts>) | Port frontend cho danh sách, chi tiết, lịch sử và các mutation hoàn tiền. |
| [frontend/infrastructure/repositories/RefundRepositoryImpl.ts](<../frontend/infrastructure/repositories/RefundRepositoryImpl.ts>) | HTTP adapter server-side đến backend, giữ bearer trên server. |
| [frontend/presentation/components/Payment/RefundDetail.tsx](<../frontend/presentation/components/Payment/RefundDetail.tsx>) | Chi tiết, từng điều kiện, lịch sử, xác nhận/từ chối/ngoại lệ/hủy/đối soát. |
| [frontend/presentation/components/Payment/RefundManager.tsx](<../frontend/presentation/components/Payment/RefundManager.tsx>) | UI dùng chung: danh sách, bộ lọc ADMIN, giao dịch teacher và form gửi yêu cầu. |
| [frontend/presentation/hooks/Payment/useRefunds.ts](<../frontend/presentation/hooks/Payment/useRefunds.ts>) | State/query theo user, chống submit đồng thời, giữ idempotency khi lỗi và refresh quyền. |
| [frontend/presentation/services/RefundService.ts](<../frontend/presentation/services/RefundService.ts>) | Presentation service gọi BFF, hỗ trợ hủy request đọc bằng AbortSignal. |
| [frontend/usecases/payment/RefundUsecase.ts](<../frontend/usecases/payment/RefundUsecase.ts>) | Frontend usecase điều phối qua repository port. |
