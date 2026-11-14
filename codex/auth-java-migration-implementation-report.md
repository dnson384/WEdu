# Migration auth bằng Java — 2026-11-14

## Kết quả

Đã thay hoàn toàn hai file JS/CJS trong backend/scripts bằng Java/JUnit. Backend không cần Node.js/mongosh để chạy migration hay test của migration. Không thêm dependency/framework migration; formatter/POM hiện có giữ nguyên.

AuthMigrationRunner chỉ được đăng ký khi auth.migration.enabled=true (AUTH_MIGRATION_ENABLED, mặc định false). Runner có Order.HIGHEST_PRECEDENCE, AuthIndexConfig có thứ tự ngay sau. Khi flag tắt, không có runner ghi migration; guard/index initialization thông thường vẫn hoạt động. Không thêm HTTP endpoint hoặc đổi API đăng ký/OTP/Google/JWT.

Runner → AuthMigrationUsecase → IAuthMigrationRepository/AuthMigrationRepositoryImpl. Repository đọc projection legacy Users, giữ nguyên kiểu _id (ObjectId/string) bằng opaque ID. Usecase kiểm ID/email, duplicate normalized email/Google subject, subject rỗng và credential MIXED/LOCAL-link không nhất quán trên toàn bộ snapshot trước khi ghi migration. Sau đó bỏ các index chứa emailKey, partial bulk update email canonical/unset emailKey và chỉ đổi LOCAL đã có Google subject + password thành MIXED. LOCAL chưa link/GOOGLE-only giữ phương thức. Không tạo/merge/delete user hoặc đổi password, role, accountType, plusExpiresAt. AuthIndexConfig bảo đảm unique email/Google và TTL indexes sau migration.

DDL và bulk updates không nằm trong một transaction. Lỗi preflight chưa có ghi migration; lỗi DB sau DDL/bulk có thể để lại tiến độ một phần. Chạy lại idempotent sau khi xử lý lỗi, trong maintenance window không có writers/traffic/jobs cạnh tranh. Không log email, password, token hay Mongo URI; runner chỉ log số user được xử lý.

## Cách chạy

Trong backend, dùng environment ứng dụng hiện có (Spring không tự nạp .env). Sau backup và dừng writers/traffic/jobs, chạy một lần:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--auth.migration.enabled=true"
```

Hoặc thêm Program argument --auth.migration.enabled=true trong IDE; có thể dùng AUTH_MIGRATION_ENABLED=true. Khi migration/index initialization hoàn tất, tắt flag và restart bình thường. Default false ngăn tự ghi migration ở các lần khởi động thông thường. Chưa chạy command này vào DB thật trong phiên implement.

## Validation thực chạy

- Spotless apply và check theo đúng11 Java files trong scope:9 file được format,2 file đã đúng. Không format file Java ngoài scope.
- Maven targeted suite AuthController*Test, AuthUsecase*Test, AuthMigration*Test, AuthIndexConfigTest, Verification*Test, GoogleSecurityConfigTest, JwtOAuthCallbackFilterTest, UserRepositoryEmailTest, UserUsecaseGoogleTest: **147 tests,0 failures/errors/skipped**.
- Migration mới gồm14 usecase cases,5 repository cases và3 runner cases; các cases script Node cũ được thay bằng JUnit. Test flag default-off/false và flag true + startup ordering đạt. Repository tests xác nhận giữ opaque IDs và không ghi password/role/PLUS; no-collection/no-change không ghi.
- Tests dùng Mockito/MockMvc/ApplicationContextRunner và metadata mocks, không gọi MongoDB/Google/SMTP thật. Chưa thực hiện migration, tạo/drop index trên DB thật hoặc deploy. Không chạy lại full backend suite;3 lỗi baseline của các lần trước không nằm trong targeted suite. Frontend không đổi nên không lặp frontend build/lint.

## Files thay đổi của đợt này

**26 files**, so với snapshot trước yêu cầu chuyển sang Java. Giữ các thay đổi OTP/Google/format đang có trong working tree; không stage/commit/push.

| File | Thay đổi |
| --- | --- |
| `backend/scripts/prepare-auth-indexes.js` (đã xóa) | Xóa migration mongosh JS, thay bằng migration Java opt-in. |
| `backend/scripts/prepare-auth-indexes.test.cjs` (đã xóa) | Xóa test Node, thay bằng JUnit. |
| [backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/AuthMigrationChangeEntity.java](<../backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/AuthMigrationChangeEntity.java>) | Thêm domain changes chỉ chứa ID/email/optional loginMethod, không chứa password/role/PLUS. |
| [backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/AuthMigrationUserEntity.java](<../backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/AuthMigrationUserEntity.java>) | Thêm domain snapshot legacy, opaque ID và fields cần preflight; không log nội dung credential. |
| [backend/src/main/java/com/wedu/exam_creation/auth/domain/repository/IAuthMigrationRepository.java](<../backend/src/main/java/com/wedu/exam_creation/auth/domain/repository/IAuthMigrationRepository.java>) | Thêm port findUsers/dropLegacyIndexes/apply, tách persistence khỏi usecase. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java>) | Đặt thứ tự sau migration; thay hướng dẫn chạy JS bằng flag Java. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRunner.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRunner.java>) | Thêm runner opt-in theo flag, chạy trước AuthIndexConfig và chỉ log số user xử lý. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/repository/AuthMigrationRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/repository/AuthMigrationRepositoryImpl.java>) | Thêm MongoTemplate adapter đọc legacy projection, giữ nguyên ID và thực hiện DDL/partial bulk updates. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthMigrationUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthMigrationUsecase.java>) | Thêm preflight toàn bộ snapshot trước ghi, normalize email, kiểm duplicate/credential và nâng LOCAL link hợp lệ thành MIXED. |
| [backend/src/main/resources/application.yml](<../backend/src/main/resources/application.yml>) | Thêm auth.migration.enabled=${AUTH_MIGRATION_ENABLED:false}. |
| [backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfigTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfigTest.java>) | Thông báo guard phải hướng dẫn --auth.migration.enabled=true. |
| [backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRepositoryTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRepositoryTest.java>) | 5 cases cho raw ID/type, scope drop index, partial writes và zero-write paths. |
| [backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRunnerTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRunnerTest.java>) | 3 cases cho flag missing/false/true và thứ tự trước index guard. |
| [backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthMigrationUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthMigrationUsecaseTest.java>) | 14 cases JUnit cho preflight, preservation, failures, empty data và idempotency. |
| [codex/api-reference.md](<../codex/api-reference.md>) | Ghi migration không có HTTP endpoint và không đổi contract auth. |
| [codex/architecture.md](<../codex/architecture.md>) | Ghi các layer migration, startup ordering, opaque ID và ranh giới DDL/bulk không atomic. |
| `codex/auth-java-migration-implementation-report.md` (đã xóa) | Báo cáo behavior, validation, cách chạy và từng file của đợt chuyển Java. |
| [codex/business-flows.md](<../codex/business-flows.md>) | Ghi sequence migration Java, default-off và lỗi trước/sau ghi. |
| [codex/current-status.md](<../codex/current-status.md>) | Ghi implementation Java, test thực chạy và MongoDB thật còn chưa xác nhận. |
| [codex/database-schema.md](<../codex/database-schema.md>) | Cập nhật migration bảo toàn schema đích/indexes và không thêm collection. |
| [codex/development-decisions.md](<../codex/development-decisions.md>) | Ghi approval chỉ dùng Java cho migration/test trong backend. |
| [codex/email-login-method-implementation-report.md](<../codex/email-login-method-implementation-report.md>) | Thay hướng dẫn migration, giữ validation Node cũ dưới dạng snapshot lịch sử và sửa link inventory. |
| [codex/google-auth-implementation-report.md](<../codex/google-auth-implementation-report.md>) | Thay hướng dẫn hiện hành; đánh dấu script/test Node cũ là lịch sử, gỡ link tới file đã xóa. |
| [codex/module-map.md](<../codex/module-map.md>) | Inventory runner/usecase/port/repository/entities/test symbols mới. |
| [codex/source-catalog.md](<../codex/source-catalog.md>) | Thêm inventory Java và xóa references hiện hành tới scripts đã bỏ. |
| [codex/tech-stack.md](<../codex/tech-stack.md>) | Hướng dẫn command Java, flag mặc định false và môi trường/maintenance. |
