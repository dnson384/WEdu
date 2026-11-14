# Chỉnh sửa email trực tiếp và MIXED — 2026-11-13

## Migration đã chuyển sang Java — 2026-11-14

Hai script JS/CJS được ghi trong snapshot bên dưới đã xóa. Các kết quả Node6 tests là validation lịch sử, không phải lệnh hiện tại. Dùng Java với auth.migration.enabled=true (mặc định false), format bằng Spotless và test JUnit. [Hướng dẫn và kết quả mới](auth-java-migration-implementation-report.md).

## Kết quả

- Bỏ emailKey khỏi UserDocument, lookup và save. Common EmailNormalizer chuẩn hóa trim/lowercase Locale.ROOT; Users lưu trực tiếp email canonical và có full unique index trên email. Cả LOCAL và Google kiểm tài khoản tồn tại bằng email; không có findByGoogleSubject. Google sub chỉ kiểm liên kết sau lookup email và vẫn có unique partial index để chống dùng cùng identity cho hai user.
- LOCAL có password liên kết Google thành công → MIXED; MIXED giữ MIXED, GOOGLE-only giữ GOOGLE. Repository ghi googleSubject/loginMethod/updatedAt cùng partial conditional write trong transaction hiện có. Giữ hashedPassword, role, accountType và plusExpiresAt. Password login chấp nhận LOCAL/MIXED có credential, vẫn kiểm mật khẩu; GOOGLE-only bị từ chối. Không thêm luồng thiết lập mật khẩu mới.
- Google email thay đổi không tự nhận diện user cũ bằng sub. Nếu cùng identity đã gắn user khác, unique sub trả409 và transaction rollback; không tạo/link tài khoản trùng hoặc cấp JWT. Email ngoài đã có liên kết đúng sub không OTP lại. Các quy tắc OTP/role/active giữ behavior đã duyệt.
- Không đổi HTTP method/path/response shape. NewUserRequestDTO và schema frontend vẫn chỉ cho LOCAL; MIXED do server quyết định sau liên kết, không do client yêu cầu. Không sửa frontend trong đợt này.

## Validation thực chạy

- node --test scripts/prepare-auth-indexes.test.cjs: **6/6 passed**, không thêm dependency/test runner.
- mvn -q test: **567 tests,556 passed,2 failures,1 error,8 skipped**. Còn đúng các lỗi có sẵn được xác nhận ở lần triển khai trước: AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit; ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite; BackendApplicationTests.contextLoads thiếu VNPAY_ENABLED. Không sửa ngoài scope.
- Sau bổ sung mapping duplicate Google subject →409, chạy lại bộ auth/security và hai class user mới: **121 tests,0 failures/errors**. Bao gồm AuthIndexConfigTest4, UserRepositoryEmailTest4, UserUsecaseGoogleTest5, AuthUsecaseLoginTest8 và VerificationUsecaseTest22.
- Các test dùng Mockito/MockMvc và Mongo query/index metadata mocks; migration chạy trên DB giả trong Node VM. Chưa chạy migration/transaction/index trên MongoDB thật, chưa gọi Google/SMTP thật. Frontend không đổi nên không lặp build/lint frontend; validation frontend trước đó là snapshot ghi trong báo cáo ban đầu.

## Migration và tương thích

Dùng migration Java đã thay script cũ, bật --auth.migration.enabled=true trong maintenance window sau backup/dừng writers; mặc định false. Preflight, cleanup legacy field/index, normalization và nâng MIXED giữ behavior đã duyệt. DDL/bulk không atomic, chạy lại an toàn nếu phải khôi phục sau lỗi. [Cách chạy hiện tại](auth-java-migration-implementation-report.md).

Nếu DB đã canonical và không còn index dẫn xuất cũ, backend không yêu cầu field mới; AuthIndexConfig bảo đảm unique email/Google và TTL indexes. Nếu email chưa canonical hoặc còn index dẫn xuất, thông báo yêu cầu script để bảo vệ lookup/uniqueness. Migration thực chưa được thực hiện trong phiên này. Chưa commit/push.

## Files của đợt sửa này

**26 files** so với trạng thái trước yêu cầu chỉnh sửa; các thay đổi OTP/Google đã có trong working tree được giữ nguyên ngoài những phần cần cập nhật. Không thêm/sửa config hay dependency trong đợt này.

| File | Thay đổi |
| --- | --- |
| `backend/scripts/prepare-auth-indexes.js` (đã xóa) | Preflight email/sub/credential; chuẩn hóa email, gỡ field/index dẫn xuất cũ, tạo unique email index, LOCAL đã có Google link và password chuyển MIXED. |
| `backend/scripts/prepare-auth-indexes.test.cjs` (đã xóa) | 6 test Node không DB: normalization/cleanup/MIXED, giữ LOCAL/GOOGLE-only, duplicate email/sub, thiếu credential và idempotency. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java>) | Bỏ guard thiếu emailKey; kiểm email canonical/legacy index, tái sử dụng full unique email index hoặc tạo mới, giữ Google/TTL indexes. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java>) | LOCAL password login cho LOCAL/MIXED có password; từ chối GOOGLE và credential thiếu. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationUsecase.java>) | Google lookup chỉ theo email; kiểm sub sau lookup, recheck email trong transaction; duplicate sub trả409; dùng EmailNormalizer chung. |
| [backend/src/main/java/com/wedu/exam_creation/common/util/EmailNormalizer.java](<../backend/src/main/java/com/wedu/exam_creation/common/util/EmailNormalizer.java>) | Common utility normalize trim/lowercase Locale.ROOT và kiểm canonical. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java](<../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java>) | Bỏ field emailKey; giữ email và googleSubject. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java>) | Lookup/save dùng email chuẩn hóa; bỏ findByGoogleSubject; linkGoogle nhận target method và conditional partial update sub/loginMethod. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java>) | Thay facade findByGoogleSubject bằng hasGoogleLink(email,subject). |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java>) | Kiểm liên kết sau lookup email; xác định LOCAL/MIXED có password → MIXED, GOOGLE → GOOGLE trước ghi link. |
| [backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfigTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfigTest.java>) | 4 tests startup không cần derived field, canonical guard, cleanup guard và reuse unique email index. |
| [backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLoginTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLoginTest.java>) | Bổ sung login MIXED thành công và từ chối MIXED thiếu password. |
| [backend/src/test/java/com/wedu/exam_creation/auth/usecase/VerificationUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/usecase/VerificationUsecaseTest.java>) | Google hiện lookup email; linked external email không OTP lại; đổi email không tìm theo sub; mismatch/duplicate subject rollback và không cấp JWT. |
| [backend/src/test/java/com/wedu/exam_creation/user/infrastructure/UserRepositoryEmailTest.java](<../backend/src/test/java/com/wedu/exam_creation/user/infrastructure/UserRepositoryEmailTest.java>) | 4 tests lookup chỉ email, save canonical và partial link bảo toàn password/PLUS, conflict không overwrite. |
| [backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseGoogleTest.java](<../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseGoogleTest.java>) | 5 cases LOCAL/MIXED/GOOGLE, giữ password, từ chối credential thiếu và xác minh sub sau email. |
| [codex/api-reference.md](<../codex/api-reference.md>) | Ghi semantics email-only/MIXED; HTTP endpoints và request register LOCAL không đổi. |
| [codex/architecture.md](<../codex/architecture.md>) | Ranh giới EmailNormalizer, lookup email và link sub/method có transaction. |
| [codex/business-flows.md](<../codex/business-flows.md>) | Cập nhật lookup Google theo email và chuyển LOCAL thành MIXED sau link thành công. |
| [codex/current-status.md](<../codex/current-status.md>) | Implementation hiện tại, migration tương thích và link validation mới. |
| [codex/database-schema.md](<../codex/database-schema.md>) | Bỏ derived field; unique email index, enum string MIXED và cleanup migration. |
| [codex/development-decisions.md](<../codex/development-decisions.md>) | Ghi yêu cầu email-only và approval MIXED thay lựa chọn trước. |
| [codex/email-login-method-implementation-report.md](<../codex/email-login-method-implementation-report.md>) | Báo cáo đợt sửa này, test thực chạy, rollout và inventory từng file. |
| [codex/google-auth-implementation-report.md](<../codex/google-auth-implementation-report.md>) | Đồng bộ behavior/schema/rollout; đánh dấu validation ban đầu là snapshot và dẫn tới báo cáo bổ sung. |
| [codex/module-map.md](<../codex/module-map.md>) | Symbols EmailNormalizer/hasGoogleLink/linkGoogle mới; bỏ lookup theo subject. |
| [codex/source-catalog.md](<../codex/source-catalog.md>) | Inventory utility, symbol thay thế và các test/migration test mới. |
| [codex/tech-stack.md](<../codex/tech-stack.md>) | Hướng dẫn startup/migration với index trực tiếp email và cleanup script cũ. |
