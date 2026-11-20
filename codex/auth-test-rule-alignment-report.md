# Chuẩn hóa test auth — 2026-11-20

## Phạm vi và kết quả

Đối chiếu testing rule trong AGENTS.md với source tại HEAD `e9eda93`. Bộ auth và hai test security liên quan chạy đạt trước sửa (138 cases), nhưng chưa đúng quan hệ class/package 1–1, tên method, DisplayName, explicit type và fixture độc lập thời gian/random. Register test thuộc VerificationController nhưng mang tên AuthControllerRegisterTest; mô tả success ghi 200 dù assertion đã là 202, còn case email trùng mock BadRequestException thay vì ConflictException theo source.

Sau sửa: 11 test classes, 172 test methods / 178 cases đều đạt trên clean build. Các case đang có được giữ; bổ sung 40 cases cho refresh/session/password/metadata và repository boundaries. Tất cả test methods dùng `test_<productionMethod>_<NNN>`, số liên tục theo method và DisplayName tiếng Việt dạng `trả về ... khi ...`. Những method cũ chưa có số được cấp số trong lần chuẩn hóa này. Repository tests cùng package và tên `*RepositoryImplTest`; security tests tương ứng SecurityConfig và JwtAuthenticationFilter.

Production code/config, API, DTO, schema và nghiệp vụ không thay đổi. Register vẫn 202/chưa cấp JWT; email trùng 409. Test đổi mật khẩu kiểm ID từ principal. Các usecase mock dependency ngoài unit; MockMvc giữ kiểm tra HTTP validation/response/cookie; ApplicationContextRunner chỉ dùng để kiểm conditional bean/order. Thời gian/ObjectId cố định; SecureRandom được mock tại construction boundary của test OTP, không đổi cách sinh mã production. Không gọi Google/SMTP/MongoDB/S3 thật trong targeted auth suite.

## Files test hiện hành

Các đường dẫn trong bảng tương đối với `backend/src/test/java/com/wedu/exam_creation/`.

| File | Thay đổi |
| --- | --- |
| [auth/controller/AuthControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerTest.java) | Gộp login/logout; chuẩn hóa 33 case cũ; thêm HTTP contract refresh, forgot/reset/change password, missing cookie/header, principal ID và lỗi token. 43 cases. |
| [auth/controller/VerificationControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/controller/VerificationControllerTest.java) | Gộp 25 register cases với 5 OTP/metadata/exchange cases; sửa mô tả 202, mock/assert email trùng 409; dùng fixture thời gian cố định. 30 cases. |
| [auth/usecase/AuthUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseTest.java) | Gộp login/logout; thêm refresh, forgot/reset/change password, saveUserAndRevokeRT và generateToken; kiểm not-found, invalid credentials, dịch vụ lỗi, rollback flag và giữ phiên hiện tại. 36 cases. |
| [auth/usecase/VerificationUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/VerificationUsecaseTest.java) | Chuẩn hóa 22 cases OTP/Google; mock AuthDTOMapper và random; thêm metadata happy path, token sai định dạng và session không tồn tại. 25 cases. |
| [auth/usecase/AuthMigrationUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthMigrationUsecaseTest.java) | Chuẩn hóa tên/DisplayName, bỏ var, dùng captor generic cho danh sách thay đổi; giữ preflight/idempotency/failure tests. 14 cases. |
| [auth/infrastructure/AuthIndexConfigTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfigTest.java) | Chuẩn hóa tên/DisplayName và kiểu UserDocument; giữ kiểm email/index migration guard. 4 cases. |
| [auth/infrastructure/AuthMigrationRunnerTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthMigrationRunnerTest.java) | Chuẩn hóa tên/DisplayName/explicit type; giữ conditional flag và thứ tự chạy trước index guard. 3 cases. |
| [auth/infrastructure/repository/AuthMigrationRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/repository/AuthMigrationRepositoryImplTest.java) | Chuyển package/đổi tên theo production; bỏ var, cố định ObjectId; giữ partial update và scope xóa index. 5 cases. |
| [auth/infrastructure/repository/VerificationRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/repository/VerificationRepositoryImplTest.java) | Chuyển package/đổi tên; mock mapper, explicit types; thêm revision conflict, OTP không tồn tại, handoff binding/expiry predicates và find not-found. 10 cases. |
| [config/SecurityConfigTest.java](../backend/src/test/java/com/wedu/exam_creation/config/SecurityConfigTest.java) | Chuyển từ GoogleSecurityConfigTest về class/package tương ứng SecurityConfig; chuẩn hóa method/DisplayName/explicit types; giữ state/nonce/binding, callback lỗi/cancel và protected paths. 5 cases. |
| [security/infrastructure/filter/JwtAuthenticationFilterTest.java](../backend/src/test/java/com/wedu/exam_creation/security/infrastructure/filter/JwtAuthenticationFilterTest.java) | Chuyển từ JwtOAuthCallbackFilterTest về class/package tương ứng filter; chuẩn hóa tên/DisplayName/explicit types; giữ callback mới bypass, callback cũ/path liền kề bị chặn. 3 cases. |

Files cũ đã bỏ sau khi chuyển đầy đủ nội dung:

- `auth/controller/AuthControllerLoginTest.java` và `AuthControllerLogoutTest.java` → `AuthControllerTest.java`.
- `auth/controller/AuthControllerRegisterTest.java` → `VerificationControllerTest.java`.
- `auth/usecase/AuthUsecaseLoginTest.java` và `AuthUsecaseLogoutTest.java` → `AuthUsecaseTest.java`.
- `auth/infrastructure/AuthMigrationRepositoryTest.java` → `auth/infrastructure/repository/AuthMigrationRepositoryImplTest.java`.
- `auth/infrastructure/VerificationRepositoryTest.java` → `auth/infrastructure/repository/VerificationRepositoryImplTest.java`.
- `security/GoogleSecurityConfigTest.java` → `config/SecurityConfigTest.java`.
- `security/JwtOAuthCallbackFilterTest.java` → `security/infrastructure/filter/JwtAuthenticationFilterTest.java`.

## Files tài liệu

| File | Thay đổi |
| --- | --- |
| [current-status.md](current-status.md) | Ghi kết quả validation hiện tại và các lỗi ngoài scope đã xác nhận trên HEAD. |
| [module-map.md](module-map.md) | Đồng bộ tên test auth/security hiện hành và liên kết báo cáo. |
| [source-catalog.md](source-catalog.md) | Đồng bộ inventory 11 class/package và link sau rename/merge. |
| [auth-java-migration-implementation-report.md](auth-java-migration-implementation-report.md) | Sửa link repository test đến file mới; giữ số liệu lịch sử. |
| [email-login-method-implementation-report.md](email-login-method-implementation-report.md) | Sửa link AuthUsecaseLoginTest đến file gộp; giữ số liệu lịch sử. |
| [google-auth-implementation-report.md](google-auth-implementation-report.md) | Sửa link register/repository/security tests đến file hiện hành; giữ tên/lệnh và số liệu lịch sử. |
| [auth-test-rule-alignment-report.md](auth-test-rule-alignment-report.md) | Báo cáo phạm vi, toàn bộ files, validation và giới hạn của thay đổi này. |

Thay đổi AGENTS.md có sẵn trước task được giữ nguyên, không thuộc thay đổi của task này.

## Validation

Chạy từ `backend/`, trừ kiểm tra source/quy ước và Git từ root. Maven ngoài sandbox dùng local repository hiện có; lượt chạy trong sandbox ban đầu không truy cập được `C:\.m2\repository`.

1. Baseline auth: `mvn -q "-Dtest=com.wedu.exam_creation.auth.**.*Test,com.wedu.exam_creation.security.**.*Test" test` — 138 cases đạt.
2. Format: `mvn -q "-DspotlessFiles=.*AuthControllerTest.java,.*VerificationControllerTest.java,.*AuthUsecaseTest.java,.*VerificationUsecaseTest.java,.*AuthMigrationUsecaseTest.java,.*AuthMigrationRunnerTest.java,.*AuthIndexConfigTest.java,.*AuthMigrationRepositoryImplTest.java,.*VerificationRepositoryImplTest.java,.*JwtAuthenticationFilterTest.java,.*SecurityConfigTest.java" spotless:apply` — đạt. Spotless scoped không sửa file Java ngoài task. Những lần thử glob `**`/pipe trước đó lỗi do selector regex/Windows wrapper; đã sửa selector, không đổi formatter/config.
3. Targeted clean: `mvn -q clean "-Dtest=com.wedu.exam_creation.auth.**.*Test,com.wedu.exam_creation.security.infrastructure.filter.JwtAuthenticationFilterTest,com.wedu.exam_creation.config.SecurityConfigTest" test` — 178 cases đạt, 0 failures/errors/skipped. `clean` loại bytecode các class cũ sau rename/merge.
4. Full suite: `mvn -q test` — 603 cases: 593 passed, 2 failures, 1 error, 7 Mongo integration skipped.
5. Baseline reproduction: export riêng `HEAD e9eda93` vào thư mục Temp rồi chạy `mvn -q "-Dtest=AiUsageRepositoryImplTest,ReviewPendingQuestionUsecaseTest,BackendApplicationTests" test` — cùng 2 failures/1 error trên 29 cases. Không sửa snapshot hoặc cấu hình để né lỗi.
6. `mvn -q "-DspotlessFiles=<cùng selector 11 class ở bước 2>" spotless:check` — đạt. Rà source 11 class / 172 test methods — đạt mapping production class/package, method production tồn tại, naming/DisplayName/numbering, không var/thời gian thực/ObjectId ngẫu nhiên trong fixture. Các link test đổi tên trong inventory và báo cáo liên quan được cập nhật; kiểm whitespace bằng `git diff --check` đạt.

Lượt phát triển có một case mới đổi mật khẩu dùng field `userId` ngoài DTO nên bị 400; payload đã sửa theo DTO, giữ assertion principal ID. Không sửa production để làm test pass.

## Lỗi có sẵn và giới hạn

- `AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit`: assertion mong `_id` ở cấp gốc, query thực tế có `$and`.
- `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite`: mong BadRequestException nhưng nhận NullPointerException tại `requireCurrent`.
- `BackendApplicationTests.contextLoads`: thiếu placeholder `VNPAY_ENABLED` khi xử lý PaymentSchedulingConfig.

Ba lỗi trên tái hiện trên HEAD tách riêng, ngoài scope auth; không disable/xóa test hoặc sửa code/config để che lỗi. Các test Mongo bị skip không phải bằng chứng persistence integration đã đạt. Task này không xác nhận đăng nhập Google thật, gửi email thật, rollout hoặc coverage toàn bộ security/user/token services liên quan.
