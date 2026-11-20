# Trạng thái triển khai và điểm cần xác nhận

## Bổ sung test chapter — 2026-11-20

Module chapter trước đây chưa có test class riêng. Đã thêm 6 class cùng package theo production class 1–1: controller, usecase, service, repository, DTO mapper và document mapper. Tổng 58 test methods/63 cases: controller8, usecase3, service10, repository22, DTO mapper12, document mapper8. Test kiểm contract GET có xác thực, mapping lồng nhau/null/rỗng, tạo/merge chương-bài-bankStats, count, truy vấn/not-found và lỗi dependency. Giữ merge theo chapter name đã được ACCEPTED_CURRENT_SCOPE; không đổi production/API/schema/dependency hoặc importer FROZEN. [Inventory từng file, testing rules và validation](chapter-test-rule-alignment-report.md).

Validation: Maven wrapper không khởi động được (`Cannot index into a null array / Cannot start maven from wrapper`); Maven đã cài chạy cùng cấu hình repository. Spotless apply/check scoped 6 test classes đạt; audit tên/DisplayName/sequence58/58 đạt. `mvn -q "-Dtest=Chapter*Test" test`:63/63 passed, không skipped. `mvn -q test`:802 cases,792 passed,2 failures,1 error,7 skipped; toàn bộ63 chapter cases đạt. Ba lỗi trùng baseline đã ghi nhận ở auth/draft: AiUsageRepositoryImplTest query assertion, ReviewPendingQuestionUsecaseTest invalid-input và BackendApplicationTests thiếu VNPAY_ENABLED. Không sửa/disable test ngoài scope. Chapter tests mock Mongo/dependencies; chưa xác minh persistence với Mongo thật hoặc end-to-end frontend.

## Snapshot main sau merge payment/Google — 2026-11-19

Đã đối chiếu main tại `be3c072`: PR #147 backend main `a17aca7`, #148 backend test `495c80c`, #149 frontend payment `f5f6e94` và #150 frontend Google `9f33837` đã merge. Source backend/src/main, backend/src/test và frontend không khác cây source tại `9f33837`. Không chứng nhận deploy, migration hoặc external service đã được kiểm thử thật.

Validation đã chạy trên staged snapshot trước commit: backend main Spotless/package đạt (bỏ qua test); backend test Spotless và184 tests đạt177 passed/7 Mongo integration skipped,0 failures/errors. Frontend payment Prettier/ESLint,54 checks và Webpack compile đạt; frontend Google Prettier/ESLint và46 checks origin/callback/cookie/binding đạt. **TypeScript/production build trên checkout sạch vẫn chưa đạt:** Git lưu draftRepositoryImpl.ts/userRepositoryImpl.ts, imports dùng DraftRepositoryImpl/UserRepositoryImpl, gây hai lỗi TS1261 có sẵn. Kết quả TypeScript/build đạt trên working tree ở các snapshot bên dưới không thay thế kết quả staged snapshot này. Lượt commit context không sửa application code hoặc chạy lại test/build; chỉ kiểm links/inventory và whitespace.

## Đồng bộ style quản lý giao dịch với ADMIN — 2026-11-19

Trang giao dịch dùng cùng khung max-w-7xl, header Quản trị hệ thống, nút Làm mới, card trắng/viền slate/shadow nhẹ và control rounded-xl với các trang tài khoản/AI. Bộ lọc giữ keyword/status/ngày; bảng có header slate-50, hàng hover, badge màu trạng thái, empty state và phân trang dùng chung. Loading ban đầu hiển thị card; khi refetch vẫn giữ dữ liệu đã tải, báo đang cập nhật và disable phân trang; lỗi có dữ liệu cũ được ghi chú. Không thay query hook, API/schema/backend hoặc quyền.

Files: [transactions/page.tsx](../frontend/app/(site)/admin/transactions/page.tsx) dựng header/loading/error; [TransactionFilters](../frontend/presentation/components/Admin/TransactionFilters.tsx) đồng bộ form/input/nút; [TransactionTable](../frontend/presentation/components/Admin/TransactionTable.tsx) đồng bộ bảng/badge/empty state và chuyển trang zero-based sang UI one-based; [transactionDisplay](../frontend/presentation/utils/transactionDisplay.ts) thêm transactionStatusClass; [AdminPageHeader](../frontend/presentation/components/Admin/AdminPageHeader.tsx) mới tái sử dụng header/refresh; [AdminPagination](../frontend/presentation/components/Admin/AdminPagination.tsx) mới dùng UI one-based và disable khi busy; [users/page.tsx](../frontend/app/(site)/admin/users/page.tsx) dùng header/pagination chung, giữ callbacks/khóa khi pending; [ai/page.tsx](../frontend/app/(site)/admin/ai/page.tsx) dùng header chung, giữ ai.refresh/disabled. [module-map](module-map.md) và [source-catalog](source-catalog.md) đồng bộ symbol/inventory.

Validation: Prettier/ESLint8 file, TypeScript noEmit, production build Webpack và scoped diff whitespace đạt. Harness tạm React SSR/mock hooks và callback thật của component:36 checks đạt cho header/refresh, first/last/busy/empty pagination, chuyển zero/one-based, filter submit/change, badge, loading/error/stale data và khóa pending users/refresh AI. Browser inventory không có surface khả dụng nên chưa kiểm tra pixel/browser end-to-end; không thay dependency hoặc test runner, không chạy backend vì chỉ đổi presentation.

## Mở trực tiếp tra cứu giao dịch — 2026-11-19

[Trang quản lý giao dịch](../frontend/app/(site)/admin/transactions/page.tsx) hiển thị ngay bộ lọc, danh sách, loading/error/retry và phân trang bằng hook hiện có. Bỏ thẻ trung gian và link quay lại; [URL search cũ](../frontend/app/(site)/admin/transactions/search/page.tsx) redirect một chiều về /admin/transactions. Không đổi API/schema/backend hay nghiệp vụ payment; vẫn chỉ tra cứu DB.

Validation: Prettier/ESLint hai page và TypeScript noEmit đạt. Harness tạm ngoài repo dùng React SSR, mock hook/components và Next redirect thật: render ngay filter/list, giữ callbacks tìm kiếm/phân trang, không có link trung gian và redirect307 từ URL search cũ về trang chính đạt. Scoped diff whitespace đạt. Không thêm test runner/dependency, không chạy lại backend/full build hoặc browser end-to-end trong task UI này.

## Gỡ hoàn tiền và tra cứu giao dịch từ DB — 2026-11-19

User đã duyệt xóa toàn bộ flow hoàn tiền và xác nhận ADMIN chỉ tra cứu DB, bỏ nút/API gọi VNPay. Source hiện không còn RefundController/RefundUsecase/RefundAiService, model/repository/index/job hoàn tiền, frontend refund pages/BFF/components/hooks/services. Xóa cấu hình REFUND_ENABLED. API GET /payment/orders của flow refund và POST /admin/transactions/{id}/lookup đã gỡ; POST /payment/orders (tạo checkout) vẫn giữ.

ADMIN GET /admin/transactions chỉ đọc PaymentOrders, gồm mã đơn, email, số tiền, ngày tạo, trạng thái và mã VNPay đã lưu. Thanh toán giữ chữ ký IPN/Return, owner/role guard, cấp PLUS và email kết quả. querydr vẫn phục vụ verify của checkout khi cần; lỗi sandbox99 chưa được xác nhận đã sửa, không còn là dependency của tra cứu ADMIN.

QuestionService đọc lại quyền PLUS/active qua UserService trước khi gọi AI, không còn refund hold/AI receipt. Không tự xóa RefundRequests/RefundAccounts/RefundAiReceipts hay sửa quyền PLUS trên DB thật. PaymentEmails claim loại các event có refundId cũ; không gửi nhầm thành email thanh toán. Migration payment chỉ còn xử lý storage, không đọc/ghi tài khoản refund. Các snapshot refund trước ngày này là lịch sử và đã bị quyết định hiện tại thay thế.

[Báo cáo gỡ hoàn tiền và tra cứu DB](refund-removal-report.md) có inventory từng file và validation. Validation mới: backend184 tests (177 passed,7 Mongo integration skipped,0 failures/errors); Spotless scoped, frontend Prettier/ESLint13 file, TypeScript/typegen, production Webpack build và harness18 checks đều đạt. Clean generated class/types sau khi xóa routes; build sandbox lỗi fonts/trace đã chạy lại ngoài sandbox đạt. Chưa gọi VNPay/Mongo/SMTP thật hoặc chạy migration trên database thật.

## Checksum querydr theo đặc tả VNPay — 2026-11-18

[VnpayGateway.query](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java) tạo biến local `checksumData` bằng đúng thứ tự `RequestId|Version|Command|TmnCode|TxnRef|TransactionDate|CreateDate|IpAddr|OrderInfo`, dùng raw values, không URL-encode hoặc thêm dấu phân cách cuối. `sign` giữ HMAC-SHA512, UTF-8 cho key/data và lowercase hex. `TransactionNo` tùy chọn vẫn gửi trong JSON nhưng không nằm trong checksum querydr theo đặc tả. Không đổi API/schema, ký checkout/refund, xác minh phản hồi hay trạng thái nghiệp vụ.

[VnpayQueryGatewayTest](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayQueryGatewayTest.java) thêm fixed-vector HMAC-SHA512 tính độc lập bằng .NET với dummy key: xác minh checksum trong JSON khi có/không có TransactionNo bằng UUID/thời gian cố định, và UTF-8 gồm tiếng Việt, khoảng trắng, ký tự `+ & %`. Source trước thay đổi vốn đã ghép cùng thứ tự; thay đổi làm rõ công thức và bổ sung bằng chứng kiểm thử độc lập, không chứng nhận nguyên nhân mã99 hoặc VNPay thực đã hoạt động.

Validation: Spotless apply/check đúng hai file Java đạt bằng Maven đã cài với cùng cấu hình repository; wrapper vẫn lỗi `Cannot index into a null array` trước khi Maven khởi động. Lệnh `mvn -q '-Dtest=VnpayQueryGatewayTest,VnpayGatewayTest,VnpayRefundGatewayTest,Payment*Test,Refund*Test,AdminTransaction*Test,PlusEntitlementTest' test` đạt: 163 tests, 153 passed, 10 Mongo integration skipped, 0 failures/errors; riêng VnpayQueryGatewayTest có11 tests đạt. Scoped diff whitespace đạt. Không chạy full suite hoặc gọi VNPay/Mongo/SMTP thật; không thay đổi credentials/config.

## Chẩn đoán phản hồi hoàn tiền VNPay — 2026-11-18

[VnpayGateway.refund](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java) giữ mã `vnp_ResponseCode` đúng hai chữ số trong exception/log khi phản hồi không xác minh được, thay vì luôn ghi `UNKNOWN`. Phản hồi chỉ có `99 / Unexpected error` vẫn là `REFUND_UNVERIFIED`; mã thiếu/sai định dạng và body rỗng vẫn là `UNKNOWN`. Không ghi provider message, payload hoặc chữ ký. Không đổi API/schema, trạng thái nghiệp vụ hay gửi lại lệnh: yêu cầu chưa xác minh vẫn giữ PROCESSING để đối soát.

VnpayRefundGatewayTest (đã xóa) bổ sung unsigned99/00, mã lỗi không hợp lệ, bảo vệ log và body rỗng. Chưa xác nhận nguyên nhân lỗi99 trên merchant thực; thay đổi chỉ sửa việc mất mã chẩn đoán.

Validation: Spotless apply hai file Java đạt bằng Maven đã cài, cùng cấu hình repository; wrapper `mvnw.cmd` lỗi `Cannot index into a null array` trước khi khởi động Maven. Lần gọi Maven trong sandbox lỗi không truy cập được local repository; lần đầu bộ lọc glob sai cú pháp regex, đã sửa thành `.*VnpayGateway.java,.*VnpayRefundGatewayTest.java` và chạy thành công. Nhóm `Refund*Test,VnpayRefundGatewayTest,VnpayGatewayTest,VnpayQueryGatewayTest`: 70 tests, 67 passed, 3 Mongo integration skipped, 0 failures/errors; không có dedicated Mongo URI. Scoped diff whitespace đạt. Không chạy full suite hoặc gọi VNPay/Mongo/SMTP thật; không thay đổi các sửa đổi payment/auth/AGENTS.md đã có trong working tree ngoài hai file thuộc task.

## Frontend origin cho Google/OTP qua proxy — 2026-11-18

Implemented `getFrontendOrigin` trong [flow.ts](../frontend/app/api/auth/flow.ts), dùng chung cho kiểm same-origin và redirect trong [Google callback](../frontend/app/api/auth/google/callback/route.ts)/[start](../frontend/app/api/auth/google/start/route.ts). Frontend `FRONTEND_URL` ưu tiên public origin, fallback request origin khi chưa đặt; chỉ HTTP(S) không credentials. Đã thêm biến vào local `frontend/.env` (Git ignored) theo domain ngrok hiện có. Không đổi API/schema/backend.

Validation: Prettier ba file đạt (dùng Prettier đã cache; lệnh npx ban đầu không truy cập được registry do EACCES); ESLint ba file và `tsc --noEmit --incremental false` đạt. Harness Node tạm ngoài repo dùng NextRequest/NextResponse thật, mock exchange/backend: đạt redirect200/202, callback thiếu code/verifier, exchange lỗi, start thiếu backend, giữ start backend/binding, cookie set/clear và no-store; origin cấu hình/trailing slash, fallback localhost, chặn origin lạ/thiếu/cross-site và URL cấu hình sai protocol/credentials đều đạt. Không thêm dependency/test runner. Chưa chạy production build hoặc đăng nhập Google thật; cần khởi động lại frontend rồi xác minh qua ngrok. Backend browser URL còn fallback localhost nếu chưa đặt `GOOGLE_OAUTH_BASE_URL`; tunnel3000 không tự expose8080, giữ ngoài scope sửa redirect này. Các thay đổi payment/AGENTS.md có sẵn được giữ nguyên.

## Payment embedded VNPay - 2026-11-18

Implemented in source: embedded PaymentOrders.vnpayTransaction with separate entity/document types, nested search/index/throttle, ObjectId userId mapping, request-scoped checkout IP, derived manualReview/AI coverage and retained plusLegacyProtected. Opt-in Java migration handles both old flat orders and intermediate split data; default startup checks storage readiness. API shape is unchanged.

Validation: 146 tests passed, 10 Mongo integration tests skipped, no failures/errors; scoped Spotless passed. [Report](payment-storage-refactor-report.md) contains exact validation and complete inventory. Real database migration, deployed indexes and VNPay sandbox remain unverified. WEDU_PAYMENT_TEST_MONGO_URI is absent; replica-set integration tests are skipped. Historical results below do not certify this change.

## Migration auth bằng Java — 2026-11-14

Implemented AuthMigrationRunner → AuthMigrationUsecase → IAuthMigrationRepository/AuthMigrationRepositoryImpl, thay hai script JS đã xóa. Mặc định tắt; khi bật, migration chạy trước AuthIndexConfig, guard lỗi hướng dẫn --auth.migration.enabled=true. Spotless11 file và147 tests migration/index/auth đạt. Chưa chạy migration/index thực trên MongoDB hoặc deploy. [Files và cách chạy](auth-java-migration-implementation-report.md).

## Callback Google dưới /auth — 2026-11-14

Implemented callback backend `/auth/login/oauth2/code/google`, thay path cũ. Application YAML, OAuth filter processing/matcher và JWT public path đồng bộ.8 tests OAuth/JWT filter đạt; YAML parse/property đạt. Chưa gọi Google thật hay sửa Google Console. [Files/validation](google-auth-implementation-report.md#callback-google-dưới-auth--2026-11-14).

## Chỉnh sửa email/MIXED — 2026-11-13

Bỏ emailKey trong model/query/save; chuẩn hóa email trực tiếp và unique index email. Không tìm user Google bằng sub. Liên kết LOCAL có mật khẩu chuyển MIXED trong cùng conditional write với googleSubject; LOCAL/MIXED dùng password, GOOGLE-only vẫn từ chối password. Migration Java hỗ trợ cleanup bản emailKey trước và đánh dấu các LOCAL đã có Google link hợp lệ thành MIXED. [Validation và files của chỉnh sửa này](email-login-method-implementation-report.md). Chưa áp migration vào DB thật.

## Đăng ký OTP và Google — 2026-11-13

Implemented trong source: OTP cho mọi đăng ký mới LOCAL/GOOGLE, hồ sơ tạm, resend/cooldown/rolling quota, transaction hoàn tất user+refresh, Google OIDC/binding/handoff, teacher/active guard, lookup bằng email, liên kết Google theo sub và chuyển LOCAL thành MIXED. Frontend có nút Google hoạt động, shared OTP page/hook và cookie BFF. Register chuyển202 chờ xác minh; mô tả register tạo user ngay/OAuth partial/cookie snake_case cũ bên dưới đã được thay thế bởi mục này.

Migration/index production, Google consent thực, SMTP delivery và Mongo transaction/concurrency thực vẫn unknown; chưa chạy against external services hoặc deploy. Cần OTP_SECRET, Google/SMTP config và chuẩn hóa email trước startup. [Validation chính xác, lỗi baseline và toàn bộ files](google-auth-implementation-report.md).

## Xuất ma trận đề — 2026-11-12

Source đối chiếu 2026-11-13 trên main `5baf5e1`: backend `23a4111` (PR #140), backend tests `ba3f912` (PR #141), exporter `ef66943` (PR #142) và frontend `97e1d67` (PR #143) đã merge. Ba file JUnit nằm trong source. Mẫu DOCX và harness `exporter-service/tests/matrixExport.test.ts` không được lưu trong snapshot repo hiện tại; kết quả exporter 8 tests bên dưới thuộc phiên triển khai, không phải test suite có thể chạy lại từ checkout này. Lượt đồng bộ context chỉ kiểm source/links/diff, không chạy lại application build/tests hoặc chứng nhận production.

Bổ sung theo confirm: đã bỏ tên bài kiểm tra khỏi cả hai phần trong nội dung DOCX; filename vẫn `<tên đề>_ma trận đề.docx`, wire contract giữ nguyên. Test kiểm tra cả document XML và binary qua HTTP để ngăn tên file bị chèn lại vào document. Kết quả kiểm tra lượt bổ sung được ghi trong [report](matrix-export-implementation-report.md#bỏ-tên-bài-kiểm-tra-khỏi-nội-dung-docx).

Implemented trong source theo solution đã duyệt: teacher-only, owner-filtered snapshot export, tự tải tên tiếng Việt, trạng thái chờ và lỗi. Style đã cập nhật theo docs/format_file_ma_tran_de.docx: hai bảng với header 4 hàng, gộp chương/bài, Arial 12, trang Letter ngang, lề 1 inch, viền đen và tổng câu/điểm/tỷ lệ. Vận dụng cao cộng vào Vận dụng khi xuất; mọi câu bằng điểm trên thang 10, N từ tổng matrix, N=0 trả 0. Không đổi schema/thuật toán sinh đề.

Validation lượt style: exporter `npx tsc --noEmit` đạt; 8/8 tests đạt, gồm aggregation không mutate source, merge grids, equal points/rounding/zero, 60 bài với nội dung dài và smoke HTTP matrix/exam. Chưa kiểm ảnh render: không có managed document runtime, LibreOffice/Word hoặc browser khả dụng trong phiên; chỉ chứng nhận cấu trúc OOXML, chưa chứng nhận phân trang/hiển thị Word. Không chạy lại frontend/backend vì lượt style chỉ đổi exporter. Chi tiết ở [report](matrix-export-implementation-report.md#cập-nhật-style-theo-mẫu).

Validation lượt ban đầu: 13 backend tests mới đạt; exporter 3 tests đạt (validation, ZIP/XML, HTTP matrix và smoke route exam); frontend build, TypeScript và ESLint các file thay đổi đạt; exporter TypeScript đạt. Hai harness tạm BFF/download và React hook đạt (status errors, tên file, cleanup, chống bấm lặp, reset sau lỗi). Build/test ban đầu gặp giới hạn sandbox Windows; chạy lại ngoài sandbox thành công. Chưa chứng nhận browser end-to-end với backend/Mongo thật, Word visual layout hay production; không chạy full backend suite. [Báo cáo và danh sách toàn bộ file](matrix-export-implementation-report.md).

## PLUS refunds — 2026-11-09

**Đã gỡ khỏi phạm vi KLTN ngày 2026-11-19.** Không còn route, service, job hoặc model runtime của flow này. Dữ liệu lịch sử không bị xóa trên DB thật. [Báo cáo gỡ hoàn tiền và tra cứu DB](refund-removal-report.md) ghi nhận phạm vi hiện hành; [báo cáo cũ](refund-implementation-report.md) chỉ là lịch sử triển khai.

## Snapshot hiện tại — 2026-11-06

Đối chiếu source/config tại commit `abcde6e`. Đã có cả backend và frontend payment/PLUS trong working tree; các nhãn missing/partial bên dưới đã được sửa theo phạm vi source. Lượt đồng bộ này chỉ đọc source và sửa Markdown, không chạy lại build/tests hoặc gọi VNPay/MongoDB/AWS/SMTP.

Kiểm tra tài liệu 2026-11-06: 1.478 liên kết nội bộ đều trỏ tới file/thư mục tồn tại; không phát hiện anchor trỏ tới heading không tồn tại trong các liên kết đã kiểm tra. Bảng declaration khớp 54 endpoint Spring, 51 handler Next và 12 root collections; code fences cân bằng và `git diff --check` đạt. Chỉ 10 file Markdown trong codex/ thay đổi; không sửa application/config/schema. Các kết quả Maven/build được giữ theo lần chạy trước, không coi đây là một lần chạy mới.

Các mục có ngày cũ là bằng chứng lịch sử của từng lần triển khai; khi mâu thuẫn, dùng snapshot hiện tại và source được dẫn. Không suy ra production hoặc kiểm thử end-to-end đã đạt từ việc có code.

## Cấu hình giới hạn AI — 2026-11-01

Snapshot implementation/validation bên dưới thuộc commit `476fb98` trên `frontend/feature/ai-management`. Khi chuẩn bị commit tài liệu, `main`/`origin/main` đang ở `11db659`, chưa chứa commit frontend này; đây không phải xác nhận tính năng đã merge hoặc deploy trên main.

Implemented: `/admin/ai` có summary cấu hình và nút mở form, điền sẵn/hủy draft, lưu/đóng form, hiển thị giới hạn mới và refetch status. Validation form tách khỏi filter; lỗi lưu giữ input; 409 khóa lưu đến khi reload thành công. Lỗi status sau lưu nêu rõ cấu hình đã lưu, dùng Làm mới để thử lại. Backend WEdu áp dụng cơ chế hiện có, không gửi hạn mức sang DeepSeek; API/schema không đổi.

Validation lượt này: ESLint bốn file frontend thay đổi, `tsc --noEmit --incremental false` và production `npm run build -- --webpack` đều đạt trên working tree hiện tại. Build sandbox lần đầu lỗi tải Google Fonts (EACCES); chạy ngoài sandbox đạt. Harness React/JSDOM tạm đạt mở/hủy/mở lại, validation, save/double-submit, conflict/reload (kể cả đóng/mở lại), lỗi lưu giữ draft, lỗi status sau lưu/retry, summary mới, tắt hai giới hạn và hiển thị NORMAL/WARNING/BLOCKED với mock status; giữ kiểm tra filters/charts/role/BFF. Không thêm dependency/test runner. Chưa kiểm tra pixel layout trên browser hoặc gọi MongoDB/DeepSeek thật; không chạy lại backend tests vì không sửa backend. Các snapshot validation cũ bên dưới giữ nguyên phạm vi lịch sử, không chứng nhận checkout Git sạch khác casing working tree.

[Inventory](source-catalog.md#cấu-hình-giới-hạn-ai--2026-11-01).

## Snapshot main sau merge AI và quản lý tài khoản — 2026-11-01

Đã đối chiếu `main`/`origin/main` tại `399b476` sau PR #126–129. Bốn commit đã merge: backend main `66c3c92`, backend tests `54462e0`, frontend AI `9c997a0`, frontend tài khoản `1e36f83`. Source backend/frontend trên main giống cây source đã validation ở `1e36f83`; không thay application code trong lượt cập nhật docs này. Các phần dưới ghi behavior/snapshot theo từng lượt, không phải chứng nhận deploy.

Validation mới nhất được thực hiện trên các staged snapshot trước merge:

- Backend main: `mvn clean package -Dmaven.test.skip=true` đạt. Full backend tests: **376 tests, 2 failures, 0 errors**. Hai failures là `AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit` (assertion query phẳng so với `$and`) và `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite` (mong BadRequestException, nhận NullPointerException). Context load đạt; không ghi nhận full suite pass.
- Frontend AI và tài khoản: ESLint các file staged, DOM harness và Webpack compile đạt. **TypeScript/production build chưa đạt trên staged snapshot**: Git lưu `draftRepositoryImpl.ts`/`userRepositoryImpl.ts`, còn imports dùng `DraftRepositoryImpl`/`UserRepositoryImpl`, gây TS1261 khi xuất checkout sạch. Hai lỗi casing có sẵn, ngoài các commit feature; kết quả TypeScript/build đạt trên working tree ở các snapshot cũ bên dưới không thay thế kết quả staged snapshot này.
- Chưa kiểm thử DeepSeek/MongoDB thật, tài khoản thật hoặc pixel layout trên browser. Harness dùng dữ liệu giả lập, không thêm dependency/test runner. Chi phí vẫn là ước tính cao điểm; unknown usage và concurrent requests có thể làm giới hạn không tuyệt đối.

Lượt docs-only này kiểm tra links, inventory và whitespace; không chạy lại application build/tests và không sửa các failure trên.

## Khu vực ADMIN hai tính năng — 2026-11-01

Implemented theo source: menu ADMIN chính xác hai mục `/admin/users` và `/admin/ai`, dùng chung getRoleHome cho login/home; `/admin` redirect tới users. ADMIN không mount TeacherHome/useDashboard khi vào `/`. Menu tài khoản giữ profile/logout, ẩn Thanh toán. Teacher/moderator navigation giữ luồng hiện có.

Trang tài khoản dùng AdminService → BFF/usecases/port/adapter hiện có, tìm theo tên/email, phân trang 10 dòng ở client trên danh sách backend trả về. AdminUserEntity riêng phản ánh CommonUserResponseDTO.isActive (nullable), tránh dùng nhầm UserEntity.isActived của profile. UI có xác nhận đổi role/khóa/mở khóa/xóa, ref chặn submit lặp, không retry mutation; refresh query sau thành công/lỗi và clamp page khi trang cuối hết dữ liệu. Bản thân/ADMIN khác được bảo vệ; trạng thái null hiển thị chưa xác định và không cho khóa/mở khóa. Chỉ cho role TEACHER/MODERATOR như BFF hiện có. Hai BFF GET tài khoản thêm private,no-store.

Validation: `npx.cmd tsc --noEmit --incremental false`, ESLint 19 file application trong lượt này và `npm.cmd run build` đạt. Build sandbox lỗi tải Google Fonts, chạy lại với quyền mạng đạt. Harness React DOM/JSDOM tạm đạt: menu hai mục, profile/logout/no billing, teacher menu, home/login theo role, list/search/pagination/isActive, bảo vệ admin, role/lock/unlock/delete confirmation, chống submit lặp, Hủy, lỗi/no retry, clamp page và BFF no-store. Không thêm dependency/test runner. Không chạy lại backend vì không đổi backend; lỗi assertion query đã ghi ở phần AI bên dưới vẫn ngoài scope. Chưa thao tác trên tài khoản thật hoặc kiểm tra trực quan bằng browser.

[Inventory từng file của lượt này](source-catalog.md#khu-vực-admin-hai-tính-năng--2026-11-01). Backend vẫn trả toàn bộ danh sách khớp tìm kiếm; pagination server là giới hạn chưa xử lý, không mở contract trong lượt này.


## Quản lý usage/budget AI — 2026-11-01

Đồng bộ package AI sau khi chuyển source sang `ai.usecase`: hai test AIQuestionGenerationUsageTest/DeepSeekBalanceServiceTest chuyển cùng package để truy cập constructor package-private, không đổi visibility hoặc nghiệp vụ. Lỗi khởi động ConflictingBeanDefinitionException do class cũ `ai.service` còn trong target/classes đã được loại bằng `mvn -q clean compile` (đạt). `BackendApplicationTests.contextLoads` đạt; hai class test đã chuyển đạt tổng 6 cases. Chạy nhóm AI + case budget review + contextLoads: **60 tests, 1 failure, 0 errors**. Failure hiện tại là `AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit`: assertion mong query phẳng `_id/revision`, source hiện dùng `$and` tương đương; không sửa ngoài phạm vi đồng bộ package. Kết quả 376 tests bên dưới là snapshot trước thay đổi package/query hiện tại, không chứng nhận working tree hiện tại đã pass full suite.

Implemented theo source: ROLE_ADMIN có `/admin/ai`, navigation, bộ lọc tối đa 366 ngày/model, tổng hợp theo ngày và ba biểu đồ SVG request/token/chi phí. Admin cấu hình budget USD/token tháng toàn hệ thống với revision; cảnh báo 80%, chặn >=100% tại AIQuestionGenerationService cho cả tạo đề và sinh lại. Chưa cấu hình không bật giới hạn nội bộ. Số dư DeepSeek cache 60 giây, timeout riêng 5 giây connect/read; lỗi hiển thị stale và vẫn dùng budget nội bộ. Không đổi timeout/retry của generation client. API/status/code 429/503 được truyền qua hai BFF hiện có; bị chặn không mất lượt review.

Usage STARTED lưu trước HTTP; usage thực từ response được hoàn tất trước parse, snapshot giá cao điểm USD theo model/cache. AiUsageRepositoryImpl dùng NOT_SUPPORTED để các thao tác usage/config không tham gia transaction câu hỏi. Unknown gồm request đang chạy, lỗi/thiếu usage hoặc không hoàn tất ghi nhận; tổng tiền/token chỉ tính phần đã biết, UI luôn nêu thiếu dữ liệu. Không lưu prompt, response, key hoặc presigned URL.

Validation: 59 tests tập trung đạt (58 tests trong 6 class AI mới và 1 case bổ sung vào ReviewPendingQuestionUsecaseTest). Bao gồm từ chối quota/revision JSON thập phân bằng deserializer Jackson 3 của MVC, kiểm tra pipeline filter/timezone và scale biểu đồ chi phí dưới 1 USD trong harness. Full `mvn -q test`: **376 tests, 1 failure, 0 errors**; failure có sẵn `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite` vẫn mong BadRequestException nhưng nhận NullPointerException, không sửa ngoài scope. Các test mới dùng mock HTTP/Mongo và transaction manager giả; xác minh suspension của transaction bằng Spring proxy, chưa chứng minh durability/rollback trên Mongo thật. Maven cần chạy ngoài sandbox vì cache `C:\.m2\repository` bị chặn.

Frontend TypeScript, ESLint các file thay đổi và production build đạt. Harness JSDOM tạm trong thư mục Temp đạt: biểu đồ/filter, warning/stale/unknown/empty/error, save/double-submit/conflict/reload budget, role guard và BFF auth/filter/validation/no-store/429 propagation. Không thêm test runner/dependency. Browser inventory trống, chưa kiểm tra trực quan desktop/mobile bằng browser; DOM harness không chứng minh pixel layout. Chưa gọi DeepSeek hoặc MongoDB thật, chưa xác nhận deploy/credentials/provider balance thực.

Giới hạn đã chấp nhận: chi phí cao điểm là ước tính có thể cao hơn hóa đơn; usage ngoài ứng dụng/lịch sử trước triển khai không có; in-flight/concurrent requests có thể vượt budget; unknown usage có thể làm tổng chưa đầy đủ. Model mới cần cấu hình giá backend trước khi gọi; Mongo account cần quyền tạo hai index usage (tạo lazy khi đọc/ghi usage). [Inventory đầy đủ](source-catalog.md#quản-lý-usagebudget-ai--2026-11-01), [API](api-reference.md#quản-lý-usagebudget-ai--2026-11-01).

## Kiểm duyệt câu hỏi AI — 2026-10-31

Implemented theo source: approve/reject API với ROLE_MODERATOR; UI hiển thị và chỉnh sửa bản sinh lại mới nhất, chống gửi lặp, refetch sau kết quả/lỗi và quay lại trang còn hợp lệ khi trang cuối hết câu. Duyệt chuyển APPROVED, giữ ID và promote nội dung mới nhất nếu có. Từ chối 1–4 sinh/lưu snapshot; lần 5 chuyển NEEDS_CONSIDERATION. AI lỗi/không hợp lệ không ghi hoặc mất lượt. Hai nhánh kết thúc xóa QuestionsReview. Chưa đổi cách dùng câu trong đề. Các nhận định approve/regenerate chỉ UI ở snapshot cũ bên dưới đã được thay thế.

Đã commit/push backend `85269d8` và `89553d9`, tests `e938083`, frontend `835576c`; các commit đã có trong `origin/main` tại `d3ad7a3` qua PR #123–125. Inventory 52 file cho cả application/tests/docs tại source-catalog; không chứng nhận production.

Validation đầy đủ gần nhất: `mvn -q clean test` chạy **317 tests, 1 failure, 0 errors** trên staged snapshot của nhánh test. `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite` mong BadRequestException nhưng nhận NullPointerException trong mock vì backend approve/reject không kiểm tra định dạng ID trước khi đọc câu. Chưa sửa lỗi này. Sau refactor `89553d9`, `mvn -q clean package -Dmaven.test.skip=true` đạt; không chứng nhận tests đã pass. Transaction tests dùng mocks/transaction manager giả, chưa chứng minh rollback MongoDB thật. TypeScript `npx.cmd tsc --noEmit --incremental false`, ESLint cả 12 file frontend, production build và JSDOM harness tạm đạt. Harness kiểm tra phân trang sau duyệt, chặn submit lặp, preview/editor bản mới, payload revision, refetch conflict, nhãn lần từ chối thứ 5 và disabled actions. Không thêm test runner/dependency frontend. Hai dòng trailing whitespace còn trong application code của `89553d9`; không thuộc diff tài liệu lần này.

Lần Maven đầu bị sandbox chặn cache; chạy lại với quyền phù hợp. Một assertion ban đầu phát hiện update nội dung thêm status thừa; đã giữ update editor chỉ ghi content/version. TypeScript lần đầu gặp cache route cũ; `next typegen` giải quyết. Build lần đầu ghi log trong .next bị EBUSY, đã chuyển log ra thư mục tạm; lần sau bị chặn Google Fonts, chạy với mạng đạt. Test failure và hai dòng whitespace ở trên vẫn còn. Chưa gọi AI/S3/MongoDB thật hoặc kiểm thử trình duyệt thật. Báo cáo UI duyệt nhầm ID chưa tái hiện được: JSDOM với 9 ID khác nhau, click thẻ thứ 7 gửi đúng ID; sau refetch thẻ thứ 8 được đánh lại số 7. Chưa có request/response runtime để kết luận nguyên nhân báo cáo. Request đồng thời có thể tốn nhiều lần gọi AI nhưng chỉ một kết quả được ghi. Duyệt thay câu theo ID cũng làm đề cũ đọc nội dung mới theo cơ chế tham chiếu hiện tại.

Bằng chứng: [usecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecase.java), [tests](../backend/src/test/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecaseTest.java), [inventory toàn bộ file thay đổi](source-catalog.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31), [contract](api-reference.md#kiểm-duyệt-câu-hỏi-ai--2026-10-31).


## Chỉnh sửa câu hỏi, công thức và upload ảnh — 2026-10-29

Implemented theo source đã merge tại main `7bdb65d`: backend `a55ce65`, tests `fa7ca22`, frontend `55c9193`. Form editor thật cho template/LaTeX/mô tả ảnh của câu và từng phương án; input theo unique reference, chèn tham chiếu, preview, Hủy không upload và Lưu multipart. Backend PATCH moderator chỉ sửa PENDING_REVIEW đúng phiên bản, giữ số phương án/status/metadata, map DTO tại usecase và update entity tại repository. Mô tả AI không bị presign như key nữa; mỗi ảnh thiếu phải upload, sau thành công database lưu S3 key thay mô tả. Không lưu mô tả riêng. updatedAt bổ sung response để chống ghi đè. Approve/regenerate vẫn chỉ UI; các nhận định edit chưa nối API ở snapshot cũ bên dưới đã được thay thế.

Validation: `mvn -q test` đạt **279 tests, 0 failures/errors** (22 cases mới); TypeScript `--noEmit --incremental false`, ESLint 17 file frontend liên quan và production build đạt. Harness tạm React DOM/JSDOM/Next proxy đạt 33 kiểm tra: unique refs, LaTeX, mô tả không thành ảnh, Hủy khôi phục draft, thiếu upload, namespaces multipart, conflict giữ draft, revoke blob URLs và proxy auth/validation. Không thêm frontend test script/dependency. Build sandbox đầu tiên lỗi tải Google Fonts; chạy lại có mạng đạt. Sau khi phát hiện Next proxy mặc định truncate body tại 10 MiB, đã đặt 65 MiB đồng bộ Spring, lint/build cấu hình mới đạt.

Chưa kiểm chứng layout trên browser thật, MongoDB/S3 thật hoặc production. Cleanup ảnh mới best-effort; DB acknowledgement không rõ thì giữ ảnh để tránh xóa media đã commit, ảnh cũ không bị xóa vì có thể dùng trong đề. Backend kiểm tra binding/nonblank/limits, cú pháp LaTeX do KaTeX phía frontend kiểm tra; không thêm Java parser. [Contract](api-reference.md), [file/symbol inventory](source-catalog.md).

Lượt đồng bộ context trên main chỉ thay đổi 9 file codex: kiểm tra diff whitespace và links local bổ sung; không chạy lại application tests vì source ứng dụng không đổi so với snapshot đã kiểm tra ở trên.

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

Các số test gắn với ngày cũ chỉ mô tả lần chạy đó, không phải kết quả của snapshot hôm nay.

“Đang triển khai” không thể suy ra thời gian/kế hoạch từ file tồn tại; dùng **partial** cho phần code dở dang, không tự gán roadmap. Bản đồ symbols/API ở [module-map.md](module-map.md); dữ liệu ở [database-schema.md](database-schema.md).

## Ma trận tính năng

| Tính năng | Trạng thái theo phạm vi | Bằng chứng và giới hạn |
| --- | --- | --- |
| Local register/login | implemented | [AuthUsecase.register/login](../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java), [Next routes](../frontend/app/api/auth); BCrypt, default teacher/FREE, JWT/cookie. Runtime chưa thử. |
| Refresh/logout/đổi password | implemented | `AuthUsecase.regenerateAccessToken/logout/changePassword`, [proxy](../frontend/proxy.ts); metadata refresh, revoke phiên khác khi đổi mật khẩu; access không blacklist. |
| Forgot/reset password + email | implemented | [PasswordResetTokenUsecase](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java), [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java), [reset page](../frontend/app/(site)/auth/reset-password/page.tsx); không revoke sessions khi reset. SMTP delivery unknown. |
| Google OAuth2 end-to-end | partial | [OAuth service](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java), [success handler](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java); role/cookie/path/redirect/persistence khác local flow. |
| Profile, username, avatar | profile/username implemented; avatar partial | [UserUsecase](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java), [avatar adapter](../frontend/infrastructure/repositories/userRepositoryImpl.ts), [hook](../frontend/presentation/hooks/Me/useUpdateAvatar.ts); frontend avatar endpoint/cache còn lệch, không chứng nhận e2e. |
| Admin user APIs | implemented | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java), [AdminUsecase](../backend/src/main/java/com/wedu/exam_creation/admin/usecase/AdminUsecase.java), [Next admin routes](../frontend/app/api/admin); search/list/role/lock/unlock/delete. |
| Admin management screen | implemented | [AdminUsersPage](<../frontend/app/(site)/admin/users/page.tsx>), [AiManagementPage](<../frontend/app/(site)/admin/ai/page.tsx>), layout/role navigation; ADMIN có hai tính năng chính quản lý tài khoản và AI. |
| Chapter listing | implemented | [ChapterController](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java), [ChapterRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java). |
| Draft create/read/update chapter/lesson/delete | implemented | [DraftUsecase](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java), [DraftRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java), [generate pages](../frontend/app/(site)/generate). |
| Matrix/matrixDetails auto-generation | implemented | [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java), generate endpoints; deterministic ratios/allocation, không stock-aware. |
| Persist matrix chỉnh tay | partial | [useMatrix.handleMatrixInputChange/handleContinueClick](../frontend/presentation/hooks/Generate/Child/useMatrix.ts): state có chỉnh nhưng tiếp tục chỉ gọi generate; controller chưa có API nhận counts sửa. |
| Sinh đề từ ngân hàng | implemented | [QuestionService.generateExamQuestions](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java), [ExamUsecase](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java); sampling, loại duplicate pool, trả thiếu và lưu đề. |
| AI bổ sung cho PLUS | partial | Cùng QuestionService + [AIQuestionGenerationService](../backend/src/main/java/com/wedu/exam_creation/ai/usecase/AIQuestionGenerationService.java); có gọi/parse/lưu/đưa vào đề, nhưng chưa validate count/order đầy đủ, empty bank lỗi trước AI, timeout chưa nối. |
| AI-only generation/job status/cancel | missing | Không thấy endpoint/job document; flow hiện là synchronous fallback trong `QuestionService`. |
| Xem/list/recent/xóa đề, render math/image | implemented | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java), [exam page](../frontend/app/(site)/exam/[examId]/page.tsx), [QuestionContent](../frontend/presentation/components/Exam/QuestionContent.tsx). |
| Word export pipeline | implemented (source), vận hành partial | [ExporterUsecase](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java), [GenerateWordFileUsecase](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts); exporter start/output mismatch, chưa smoke-test DOCX. |
| Upload avatar image | implemented | [S3Service.uploadImageFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), Tika JPEG/PNG ≤5 MiB; UUID filename. |
| Upload DOCX | implemented theo source; hàng chờ persisted missing | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) dùng updateDocumentFile, `.docx` + ZIP entries, ≤50 MiB; UI hiện 50 MB và StorageService typed string S3 key. Tên trùng thêm `(n)` qua conditional PUT. Không có submission state. |
| Moderator parse/import DOCX | implemented (source), vận hành unknown | [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [ImporterService](../backend/src/main/java/com/wedu/exam_creation/importer/service/ImporterService.java), [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java). Input multipart trực tiếp; phụ thuộc Pandoc. |
| Moderator list/download question files | implemented theo source | [ModeratorUsecase.listQuestionFiles](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java), [S3Service.listQuestionFiles](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [Next GET](../frontend/app/api/moderator/question-files/route.ts), [files page](../frontend/app/(site)/moderator/question-files/page.tsx); role MODERATOR, pagination S3, signed download URL, loading/empty/error/retry/refetch. |
| Kiểm duyệt tệp và câu hỏi AI | implemented trong phạm vi đã duyệt; audit bền vững missing | Duyệt/từ chối DOCX qua S3; [ReviewPendingQuestionUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ReviewPendingQuestionUsecase.java) và [review page](<../frontend/app/(site)/moderator/review/page.tsx>) đã nối approve/reject/regenerate/editor, guard revision và tối đa5 lượt. Question-bank page còn placeholder; không đồng nhất với review page. |
| Telegram review/cleanup notifications | implemented | [TelegramNotificationService](../backend/src/main/java/com/wedu/exam_creation/notification/telegram/TelegramNotificationService.java), call sites QuestionService/UserUsecase; chỉ gửi message, không review state machine. |
| PLUS entitlement/accountType read | implemented | [PlusEntitlement](../backend/src/main/java/com/wedu/exam_creation/user/domain/entity/PlusEntitlement.java): plusExpiresAt, FREE hiệu lực khi hết hạn, giữ PLUS legacy không có hạn; grant từ kết quả thanh toán đã xác thực. |
| Thanh toán VNPay / PLUS | implemented trong source; vận hành sandbox chưa chứng nhận | [PaymentUsecase](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java), [usePayment](../frontend/presentation/hooks/Payment/usePayment.ts): gói 20.000đ/30 ngày, đơn mới5 phút, checkout tab mới, IPN/querydr/Return backend xác thực, transaction cấp PLUS/outbox. Querydr lỗi trả503; signed Return cần receipt đầy đủ, IPN phải đăng ký riêng. |
| MongoDB transactions | implemented cấu hình/code | [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java); runtime topology và kết quả rollback unknown. Không phải distributed transaction. |
| Automatic tests | partial | Backend có JUnit/Mockito/MockMvc và Mongo integration tests tùy chọn. Lần chạy full backend trước lượt tài liệu: 445 tests,438 đạt, 2 lỗi AI/moderator có sẵn, 5 skipped; payment 64 đạt/5 skipped. Frontend có build/TypeScript/ESLint và 6 harness tạm đã đạt; không có test script trong package.json, exporter test là placeholder. Không chạy lại trong lượt này. |

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
| Import/exam/export transaction không bọc side effects ngoài | `ImporterService`, `ExamUsecase`, `ExporterUsecase`, `TelegramNotificationService` | Media/HTTP của import/exam/export đã gửi không rollback; payment riêng có Mongo outbox/idempotency, không suy rộng sang các feature khác. |
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
| Avatar endpoint/cache mismatch | [UserRepositoryImpl](../frontend/infrastructure/repositories/userRepositoryImpl.ts), [useUpdateAvatar](../frontend/presentation/hooks/Me/useUpdateAvatar.ts), [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) | Adapter gọi `/user/avatar/${s3Key}` thay `/user/update-avatar`; hook new QueryClient và key user-profile lệch useAuth user. |
| Test còn mong route legacy | `ModeratorQuestionFilesTest.preservesLegacyDocumentsEndpoint` (snapshot cũ; file không còn trong source hiện tại), [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) | Test đòi GET /storage/documents trả 200 nhưng controller hiện không khai báo route này. Quan sát tĩnh, chưa chạy test. |
| DOCX ghi đè khi trùng tên — đã xử lý 2026-10-25 | [S3Service.updateDocumentFile/uploadUniqueDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) | Cùng email/subject/normalized filename nay thử hậu tố `(1)`, `(2)`… bằng conditional PUT, không ghi đè object hiện có. Key vẫn chứa email; chưa có owner metadata/approval record. |

Bulk matrix update repository lọc draft ID nhưng không lặp owner filter; luồng gọi hiện đã nạp draft theo owner trong use case. Đừng mô tả mọi persistence write đều tự kiểm quyền. Bằng chứng `DraftRepositoryImpl.updateMatrix/updateMatrixDetails`, `DraftUsecase.generateMatrix/generateMatrixDetails`.

## Contract đã đồng bộ và implementation mới

Moderator parse hiện trả `Promise<string[]>` xuyên [IModeratorRepository](../frontend/domain/repositories/IModeratorRepository.ts), [ModeratorRepositoryImpl](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts), [ParseUsecase](../frontend/usecases/moderator/ParseUsecase.ts), [ModeratorService](../frontend/presentation/services/MorderatorService.ts), khớp Java List<String>. TypeScript generic không kiểm định response JSON runtime.

Backend DOCX không còn dùng image-only uploader: `uploadImageFile` cho avatar, `updateDocumentFile` cho DOCX. List files đã chuyển từ StorageController sang ModeratorController/Usecase, có UI files riêng. Những thay đổi này bao gồm staged/unstaged/untracked source, không đồng nghĩa đã commit/deploy. Không tự chuyển roadmap PLANNED thành quyết định đã đóng.
## Nghiệp vụ cần chủ dự án xác nhận

1. **Review:** file/câu AI có được dùng trước khi duyệt? Review diễn ra trong app hay ngoài Telegram? Ai approve/reject, cần lưu audit gì?
2. **Submission:** luồng S3 approve/reject đã được chốt và có code; không có submission ID/audit bền vững. Chỉ mở lại phần persistence khi có yêu cầu riêng; upload hiện dùng50 MiB và string S3 key.
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

## Backend payment snapshot — 2026-11-05

Implemented in source: teacher-only20k/30d purchases,5min new-order expiry, random provider references with separate idempotency hash, legacy-compatible lookup, signed checkout, verified IPN/querydr/authenticated Return, atomic PLUS/outbox, effective entitlement expiry, bounded email retries and safe503 query unavailability. [PaymentUsecase](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java), [tests](../backend/src/test/java/com/wedu/exam_creation/payment/PaymentUsecaseTest.java).

Validation 2026-11-05: payment/entitlement/gateway suite 64 passed, 5 Mongo integration tests skipped (no dedicated replica set). Full mvn -q test: 445 tests, 438 passed, 2 existing failures (AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit; ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite), 5 skipped. No real VNPay/SMTP end-to-end certification.

Known operational limits: sandbox querydr may return unsigned99 and IPN registration/delivery must be verified with VNPay. A code commit does not certify payment environment, reconstruct a deleted order or recover a lost signed Return receipt. Two existing AI/moderator test failures are outside payment scope.

## Frontend payment snapshot — 2026-11-06

Implemented: teacher-only upgrade navigation, navbar/layout,20k/30day plan and account expiry display, new-tab checkout, persisted retry key/reference, owned-order polling,30s reconciliation/focus throttle, explicit recovery after missing-order404, signed Return forwarding, safe verify503 and exact GET IPN rewrite. Backend authority and authenticated BFF remain intact. Matching backend payment endpoints are already available in main through PR #131; this frontend commit adds the UI and BFF on that updated base.

Validation 2026-11-06: npm run build, TypeScript --noEmit --incremental false and ESLint for all20 frontend payment files passed. Six temporary harnesses passed: missing-order, signed Return, Return BFF, new-tab, reconciliation and IPN proxy. They mock services/React JSDOM and do not certify live sandbox payment, SMTP delivery or a browser checkout.

Scope does not include backend code or merchant configuration. A missing/deleted order cannot be recovered as paid from its reference alone. A lost Return receipt still needs verified IPN/querydr; the provider's unsigned99 is not proof of payment. [Implementation](../frontend/presentation/hooks/Payment/usePayment.ts), [API](api-reference.md#frontend-payment-contracts--2026-11-06).

## ADMIN transaction lookup — 2026-11-08

**Implemented in source:** ADMIN navigation and landing/search pages, paginated local filters, authenticated BFF/backend, selected-order signed VNPay lookup, independent local/provider statuses, amount mismatch warning, explicit empty/not-found/unavailable states. Lookup has no payment/entitlement/outbox writes. [Usecase](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecase.java), [hook](../frontend/presentation/hooks/Admin/useAdminTransactions.ts).

Validation: targeted Maven suite AdminTransaction*Test, Payment*Test, PlusEntitlementTest, Vnpay*Test: 84 tests, 79 passed, 5 existing Mongo integration tests skipped. Includes 15 new ADMIN controller/usecase/repository tests. Frontend npm run build, tsc --noEmit --incremental false and ESLint on all changed frontend files passed. Temporary React/JSDOM/BFF harness passed list-without-VNPay, duplicate suppression, stale-response rejection, date validation, error/loading/empty states, auth/input validation, no-store and safe provider-error masking.

Initial sandbox checks failed accessing Maven cache/Google Fonts; checks passed outside sandbox. A new repository test assertion was corrected to compare Mongo regex Pattern content, then the suite passed. Full backend suite was not rerun; prior unrelated AI/moderator failures are not certified resolved.

**Unknown/runtime limits:** no live VNPay, browser/manual acceptance, deployed Mongo index validation or load test performed. Unsigned provider errors remain unavailable, not proof of absence. Missing local orders cannot be queried. Email substring queries may scan; local/provider statuses can differ by design.
