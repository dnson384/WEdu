# Báo cáo triển khai đăng ký OTP và Google — 2026-11-13

## Migration đã chuyển sang Java — 2026-11-14

Hai script JS/CJS được ghi trong snapshot bên dưới đã xóa. Các kết quả Node6 tests là validation lịch sử, không phải lệnh hiện tại. Dùng Java với auth.migration.enabled=true (mặc định false), format bằng Spotless và test JUnit. [Hướng dẫn và kết quả mới](auth-java-migration-implementation-report.md).

## Callback Google dưới /auth — 2026-11-14

Theo yêu cầu user, callback backend là **/auth/login/oauth2/code/google**. YAML trước đợt sửa có đường dẫn tùy chỉnh trong khi filter chain/processing vẫn dùng callback mặc định, nên chỉ đổi redirect-uri/Google Console chưa đủ. Đã đồng bộ redirect template, OAuth processing baseUri và chain matcher/public paths. Không có alias public cho callback cũ. Endpoint bắt đầu Google và bước chuyển sang frontend /api/auth/google/callback giữ nguyên.

Validation: **mvn -q -Dtest=GoogleSecurityConfigTest,JwtOAuthCallbackFilterTest test đạt8 tests,0 failures/errors**. MockMvc kiểm redirect_uri được tạo từ property YAML, state/nonce, callback failure/cancel và protected routes; JWT filter thật kiểm skip callback mới nhưng chặn callback cũ/liền kề. YAML parse và property value đạt. Không chạy lại full backend/frontend suites cho thay đổi route này; các kết quả suite đầy đủ ở phần dưới là snapshot các lần trước. Chưa đăng nhập Google thật, chưa sửa Google Console hoặc deploy.

Sau restart backend, đăng ký **http://localhost:8080/auth/login/oauth2/code/google** vào Authorized redirect URIs của đúng OAuth Client (hoặc URL backend public tương ứng). Nếu environment đang override SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI, cập nhật override theo path mới. Không cần đổi FRONTEND_URL cho thay đổi callback backend này.

14 files của đợt sửa callback; giữ các thay đổi trước đó trong working tree, không commit/push:

| File | Thay đổi |
| --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java](<../backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java>) | Đổi callback matcher và khai báo OAuth redirection baseUri /auth/login/oauth2/code/*. |
| [backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java](<../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java>) | JWT/public allowlist chuyển sang đúng callback Google mới. |
| [backend/src/main/resources/application.yml](<../backend/src/main/resources/application.yml>) | Redirect template thành {baseUrl}/auth/login/oauth2/code/{registrationId}. |
| [backend/src/test/java/com/wedu/exam_creation/security/GoogleSecurityConfigTest.java](<../backend/src/test/java/com/wedu/exam_creation/security/GoogleSecurityConfigTest.java>) | Đọc template từ YAML; kiểm redirect_uri gửi Google, callback mới/cancel và khóa callback cũ/đường dẫn liền kề (5 tests). |
| [backend/src/test/java/com/wedu/exam_creation/security/JwtOAuthCallbackFilterTest.java](<../backend/src/test/java/com/wedu/exam_creation/security/JwtOAuthCallbackFilterTest.java>) | 3 tests với JWT filter thật: chỉ callback mới bypass JWT, path cũ và liền kề bị chặn. |
| [codex/api-reference.md](<../codex/api-reference.md>) | Đổi declaration GET Google callback sang /auth/login/oauth2/code/google. |
| [codex/architecture.md](<../codex/architecture.md>) | Đồng bộ endpoint xử lý Spring filter và ranh giới public/JWT. |
| [codex/business-flows.md](<../codex/business-flows.md>) | Cập nhật Google→backend callback mới→Next handoff và yêu cầu Console. |
| [codex/current-status.md](<../codex/current-status.md>) | Ghi implementation callback mới,8 tests và giới hạn validation. |
| [codex/development-decisions.md](<../codex/development-decisions.md>) | Ghi user-approved callback prefix /auth; không public alias cũ. |
| [codex/google-auth-implementation-report.md](<../codex/google-auth-implementation-report.md>) | Cập nhật URI rollout và báo cáo đầy đủ files/validation đợt sửa callback. |
| [codex/module-map.md](<../codex/module-map.md>) | Đồng bộ filter matcher/baseUri/property và test symbols. |
| [codex/source-catalog.md](<../codex/source-catalog.md>) | Đồng bộ route inventory và thêm JwtOAuthCallbackFilterTest. |
| [codex/tech-stack.md](<../codex/tech-stack.md>) | Hướng dẫn redirect template/Console/environment override theo callback mới. |


## Cập nhật email/MIXED

User đã duyệt bỏ field dẫn xuất, lookup trực tiếp email và hỗ trợ MIXED. [Báo cáo chỉnh sửa, validation mới và từng file](email-login-method-implementation-report.md). Các kết quả103/550 tests bên dưới là snapshot trước chỉnh sửa này; frontend không đổi trong chỉnh sửa.

## Kết quả và contract

Đăng ký LOCAL/Google mới chỉ tạo Users và JWT sau OTP đúng. LOCAL kiểm tra email tồn tại trước khi gửi; Google kiểm tra OIDC rồi yêu cầu OTP nếu user mới. User mới ROLE_TEACHER/FREE/active, tên đăng ký/Google, avatar S3 mặc định. OTP6 số/5 phút/5 lần sai, resend120 giây, tối đa5 lần gửi/email trong rolling60 phút, phiên tạm30 phút. Resend vô hiệu mã trước; gửi lỗi không làm code ready và vẫn giữ quota/cooldown.

Google chỉ cho teacher active; tìm theo email, sau đó kiểm tra sub đã liên kết. Chưa liên kết: Gmail/Workspace verified trùng tài khoản thì link trực tiếp, email ngoài OTP trước link. Giữ mật khẩu/quyền lợi cũ, chuyển LOCAL thành MIXED sau liên kết; không đổi role hoặc migrate ROLE_USER. Transaction bao consume OTP/create/link/refresh metadata. SMTP ngoài transaction. DB uniqueness + CAS bảo vệ cạnh tranh; chưa kiểm chứng Mongo concurrency thật.

POST /auth/register và /api/auth/register đổi từ200 user/JWT thành202 metadata chờ OTP. Thêm metadata/verify/resend và Google handoff exchange; [API đầy đủ](api-reference.md#auth-otpgoogle--2026-11-13). Next browser JSON không chứa JWT/token xác minh; cookie HttpOnly/SameSite=Lax/Secure production. OAuth dùng state/nonce và session10 phút, handoff60 giây/dùng một lần/hash lưu/ràng buộc verifier cookie. Callback không đặt JWT trong URL, không log token/provider payload.

## Validation

- Maven compile đạt. Bộ xác thực **103 tests,0 failures/errors**: login/logout regression, register25 cases, usecase19, controller5, repository6, security4.
- Final **mvn -q clean test**: **550 tests,539 passed,2 failures,1 error,8 skipped**. Các lỗi tương tự đã tái hiện trên snapshot HEAD gốc được xuất sang Temp: AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit; ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite; BackendApplicationTests.contextLoads do thiếu VNPAY_ENABLED. Baseline chạy29 tests có đúng2 failures/1 error. Không sửa các vấn đề ngoài scope này.
- Frontend TypeScript --noEmit --incremental false và ESLint vùng auth/files liên quan đạt. npm run build đạt khi chạy ngoài sandbox; lần đầu trong sandbox lỗi tải Google Fonts/cache permissions.
- Harness tạm React DOM/JSDOM/Next BFF đạt **28 assertions**:202 pending/no JWT, provider spoofing, Origin guard, leading-zero OTP, cookie mapping theo tên,429 propagation, browser binding/callback, UI masked email/autofill/countdown, double-submit/resend suppression, cache clear/redirect và LOCAL password cleanup. Không thêm dependency hoặc test runner. Harness ở Temp, không phải repo test suite.
- `git diff --check` đạt; inventory bao phủ đủ69 files, các link tương đối mới/thay đổi đã kiểm tra tồn tại. Sau thay đổi header no-store, chạy lại103 tests auth và harness/TypeScript/ESLint đều đạt.
- Chưa gọi Google/SMTP thật, chưa chạy migration hoặc Mongo transaction/concurrency trên DB thật, chưa deploy. DOM harness không chứng minh pixel layout hay browser cookie policy thực.8 test skipped thuộc suite có sẵn; không suy ra integration pass.

## Cấu hình và rollout

1. Backend: cấp OTP_SECRET độc lập ngẫu nhiên ít nhất32 ký tự; Google client ID/secret, SMTP và FRONTEND_URL qua environment/IDE/deployment. Không tự nạp .env. Frontend property backend là app.frontend.url. Không đưa secrets vào NEXT_PUBLIC.
2. Next: BACKEND_URL dùng server-to-server, GOOGLE_OAUTH_BASE_URL dùng browser→backend (fallback BACKEND_URL). Google Console authorized redirect URI phải chính xác <backend-public-origin>/auth/login/oauth2/code/google. Khi reverse proxy, set SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI tới public URI; NEXT_PUBLIC không cần cho luồng auth mới.
3. HTTPS: set SESSION_COOKIE_SECURE=true ở backend; Next secure cookies theo NODE_ENV=production. OAuth state nằm trong HttpSession nên nhiều backend replicas cần sticky sessions. App JWT API giữ stateless.
4. Trong maintenance window sau backup/dừng writers, chạy backend với --auth.migration.enabled=true để thực hiện migration Java trước AuthIndexConfig, rồi tắt flag và restart. [Cách chạy/kiểm tra](auth-java-migration-implementation-report.md).
5. Deploy backend/frontend đồng bộ sau migration vì register contract thay đổi. Kiểm thử OAuth/OTP/SMTP/refresh/logout với teacher test account trong môi trường cấu hình trước mở traffic. Theo dõi HTTP error codes OTP/OAuth và429; không log OTP/JWT/verifier/presigned URLs.

## Files thay đổi

Inventory dưới đây là **69 files của phạm vi ban đầu**; các file thêm/sửa cho email/MIXED được liệt kê trong báo cáo bổ sung ở trên. Chưa commit/push.

| File | Thay đổi |
| --- | --- |
| `backend/scripts/prepare-auth-indexes.js` (đã xóa) | Preflight duplicate email/sub rồi chuẩn hóa trực tiếp email, dọn field/index dẫn xuất cũ, tạo indexes; không merge/delete user. |
| [backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java](<../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java>) | Bỏ endpoint register tạo user/JWT ngay; giữ login/logout hiện có. |
| [backend/src/main/java/com/wedu/exam_creation/auth/controller/VerificationController.java](<../backend/src/main/java/com/wedu/exam_creation/auth/controller/VerificationController.java>) | Khai báo register202, metadata, resend, verify và Google exchange; đặt/xóa cookies. |
| [backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/VerificationEntity.java](<../backend/src/main/java/com/wedu/exam_creation/auth/domain/entity/VerificationEntity.java>) | Domain hồ sơ đăng ký/liên kết/handoff tạm, OTP và expiry/revision. |
| [backend/src/main/java/com/wedu/exam_creation/auth/domain/repository/IVerificationRepository.java](<../backend/src/main/java/com/wedu/exam_creation/auth/domain/repository/IVerificationRepository.java>) | Port persistence cho OTP CAS, email quota và handoff dùng một lần. |
| [backend/src/main/java/com/wedu/exam_creation/auth/dto/request/GoogleExchangeRequestDTO.java](<../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/GoogleExchangeRequestDTO.java>) | Validate code/verifier64hex của handoff. |
| [backend/src/main/java/com/wedu/exam_creation/auth/dto/request/VerifyOtpRequestDTO.java](<../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/VerifyOtpRequestDTO.java>) | Validate OTP6 chữ số, giữ số0 đầu. |
| [backend/src/main/java/com/wedu/exam_creation/auth/dto/response/VerificationResponseDTO.java](<../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/VerificationResponseDTO.java>) | Metadata email che bớt, expiry/cooldown/attempts và serverTime. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/AuthIndexConfig.java>) | Kiểm chuẩn hóa email và tạo unique/TTL indexes khi startup. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/document/OtpRateLimitDocument.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/document/OtpRateLimitDocument.java>) | Lưu rolling send timestamps, resendAt, revision và TTL. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/document/VerificationDocument.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/document/VerificationDocument.java>) | Mongo document riêng cho hồ sơ xác minh/handoff, không chứa OTP thô. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/mapper/VerificationMapper.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/mapper/VerificationMapper.java>) | MapStruct giữa domain và Mongo document. |
| [backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/repository/VerificationRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/auth/infrastructure/repository/VerificationRepositoryImpl.java>) | Mongo queries/CAS cho quota, resend, attempts và consume OTP/handoff. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java>) | Bỏ register cũ; tái sử dụng generateToken và refresh metadata cho luồng mới. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationException.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationException.java>) | Lỗi có HTTP status, machine code và retryAfter. |
| [backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/auth/usecase/VerificationUsecase.java>) | Điều phối LOCAL/Google/OTP/link, HMAC, transactions và phát phiên. |
| [backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/NewUserRequestDTO.java](<../backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/NewUserRequestDTO.java>) | Biểu mẫu đăng ký bắt buộc loginMethod LOCAL, chặn giả mạo provider. |
| [backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java](<../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java>) | Map lỗi xác minh và429/Retry-After/no-store. |
| [backend/src/main/java/com/wedu/exam_creation/config/AppConfig.java](<../backend/src/main/java/com/wedu/exam_creation/config/AppConfig.java>) | Cung cấp Clock UTC cho auth và test thời gian. |
| [backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java](<../backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java>) | Chain OIDC riêng với state/nonce/session/binding; giữ JWT API stateless. |
| [backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java](<../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java>) | Gửi email OTP6 chữ số và thông báo hiệu lực. |
| [backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java](<../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java>) | Mở public chính xác các endpoint xác minh/Google cần thiết. |
| [backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java](<../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java>) | Tạo handoff ràng buộc browser, redirect Next và xóa session; không cấp JWT trực tiếp. |
| [backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java](<../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java>) | Chuyển sang OidcUserService chỉ tải identity/email verified, không tạo user. |
| [backend/src/main/java/com/wedu/exam_creation/user/domain/entity/UserEntity.java](<../backend/src/main/java/com/wedu/exam_creation/user/domain/entity/UserEntity.java>) | Thêm googleSubject và giữ constructors tương thích. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java](<../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java>) | Thêm googleSubject, email được chuẩn hóa trực tiếp cho persistence và indexes. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java](<../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java>) | Lookup trực tiếp email chuẩn hóa; link Google bằng partial conditional update googleSubject/loginMethod. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java>) | Facade hasGoogleLink/linkGoogle cho auth sau lookup email. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java](<../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java>) | Map/facade Google identity qua user repository. |
| [backend/src/main/resources/application.yml](<../backend/src/main/resources/application.yml>) | OTP_SECRET và cookie session OAuth HttpOnly/Lax/Secure qua environment. |
| [backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerRegisterTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerRegisterTest.java>) | Giữ25 validation cases, đổi thành202/chỉ verification cookie. |
| [backend/src/test/java/com/wedu/exam_creation/auth/controller/VerificationControllerTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/controller/VerificationControllerTest.java>) | 5 tests OTP validation/cookies/expiry/rate-limit/exchange response. |
| [backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/VerificationRepositoryTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/infrastructure/VerificationRepositoryTest.java>) | 6 tests rolling quota/cooldown/revision CAS và consume predicates. |
| `backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseRegisterTest.java` (deleted) | Xóa test luồng đăng ký trực tiếp; thay bằng VerificationUsecaseTest. |
| [backend/src/test/java/com/wedu/exam_creation/auth/usecase/VerificationUsecaseTest.java](<../backend/src/test/java/com/wedu/exam_creation/auth/usecase/VerificationUsecaseTest.java>) | 19 tests OTP, SMTP, replay/expiry, transaction rollback, Google linking và role/active. |
| [backend/src/test/java/com/wedu/exam_creation/security/GoogleSecurityConfigTest.java](<../backend/src/test/java/com/wedu/exam_creation/security/GoogleSecurityConfigTest.java>) | 4 tests OAuth state/nonce/binding, callback lỗi/cancel và protected route. |
| [codex/api-reference.md](<../codex/api-reference.md>) | Endpoints,202 response, DTO/cookies và error codes. |
| [codex/architecture.md](<../codex/architecture.md>) | Cập nhật ranh giới OTP/OIDC/BFF, session tạm và transaction. |
| [codex/business-flows.md](<../codex/business-flows.md>) | Ghi luồng LOCAL/Google/link/OTP và side effects/error behavior. |
| [codex/current-status.md](<../codex/current-status.md>) | Cập nhật implementation, prerequisites và giới hạn validation. |
| [codex/database-schema.md](<../codex/database-schema.md>) | Fields, collections, unique/TTL indexes và migration. |
| [codex/development-decisions.md](<../codex/development-decisions.md>) | Ghi approval mở Google scope và bắt buộc OTP cho đăng ký. |
| `codex/google-auth-implementation-report.md` (deleted) | Báo cáo behavior/contracts, validation, rollout và inventory từng file. |
| [codex/module-map.md](<../codex/module-map.md>) | Inventory symbols/layers và contract đăng ký mới. |
| [codex/source-catalog.md](<../codex/source-catalog.md>) | Inventory auth mới và thay reference test đã xóa. |
| [codex/tech-stack.md](<../codex/tech-stack.md>) | Biến môi trường, redirect URI, session config và rollout. |
| [frontend/app/(site)/auth/google-error/page.tsx](<../frontend/app/(site)/auth/google-error/page.tsx>) | Màn hình lỗi/hủy OAuth và thao tác thử lại. |
| [frontend/app/(site)/auth/login/page.tsx](<../frontend/app/(site)/auth/login/page.tsx>) | Gắn shared GoogleAuthButton tại trang đăng nhập. |
| [frontend/app/(site)/auth/register/page.tsx](<../frontend/app/(site)/auth/register/page.tsx>) | Gắn shared GoogleAuthButton tại trang đăng ký. |
| [frontend/app/(site)/auth/verify-email/page.tsx](<../frontend/app/(site)/auth/verify-email/page.tsx>) | Màn hình OTP dùng chung với countdown/resend/error/restart. |
| [frontend/app/api/auth/flow.ts](<../frontend/app/api/auth/flow.ts>) | Shared Origin guard, error propagation/no-store và ánh xạ cookie không lộ JWT. |
| [frontend/app/api/auth/google/callback/route.ts](<../frontend/app/api/auth/google/callback/route.ts>) | Đổi handoff phía server, áp cookies và redirect OTP/home/error; xóa verifier. |
| [frontend/app/api/auth/google/start/route.ts](<../frontend/app/api/auth/google/start/route.ts>) | Tạo verifier HttpOnly và redirect backend với hash binding. |
| [frontend/app/api/auth/register/route.ts](<../frontend/app/api/auth/register/route.ts>) | BFF validate Origin/input, trả202 metadata và verification cookie, không JWT. |
| [frontend/app/api/auth/verification/resend/route.ts](<../frontend/app/api/auth/verification/resend/route.ts>) | BFF resend theo verification cookie, bảo toàn429/Retry-After. |
| [frontend/app/api/auth/verification/route.ts](<../frontend/app/api/auth/verification/route.ts>) | BFF metadata phiên tạm để phục hồi sau reload. |
| [frontend/app/api/auth/verification/verify/route.ts](<../frontend/app/api/auth/verification/verify/route.ts>) | BFF validate OTP/Origin, hoàn tất user và cookies. |
| [frontend/domain/entities/verification.entity.ts](<../frontend/domain/entities/verification.entity.ts>) | Types metadata xác minh và transport server data/status/cookies. |
| [frontend/domain/repositories/IAuthRepository.ts](<../frontend/domain/repositories/IAuthRepository.ts>) | Đổi contract register thành AuthFlowResult phía server. |
| [frontend/domain/repositories/IVerificationRepository.ts](<../frontend/domain/repositories/IVerificationRepository.ts>) | Port server cho register, metadata, verify, resend và Google exchange. |
| [frontend/infrastructure/repositories/AuthRepositoryImpl.ts](<../frontend/infrastructure/repositories/AuthRepositoryImpl.ts>) | Register dùng transport server mới thay vì kỳ vọng JWT ngay. |
| [frontend/infrastructure/repositories/VerificationRepositoryImpl.ts](<../frontend/infrastructure/repositories/VerificationRepositoryImpl.ts>) | Adapter server BACKEND_URL, truyền verification cookie và nhận cookies theo tên. |
| [frontend/presentation/components/Auth/GoogleAuthButton.tsx](<../frontend/presentation/components/Auth/GoogleAuthButton.tsx>) | Nút Google dùng chung cho login/register, điều hướng đến Next start. |
| [frontend/presentation/hooks/Auth/useRegister.ts](<../frontend/presentation/hooks/Auth/useRegister.ts>) | Đăng ký chuyển đến OTP, xóa dữ liệu mật khẩu và xử lý lỗi. |
| [frontend/presentation/hooks/Auth/useVerifyEmail.ts](<../frontend/presentation/hooks/Auth/useVerifyEmail.ts>) | Metadata/countdown theo server, OTP/resend, chống mutation lặp, cache và redirect. |
| [frontend/presentation/schemas/auth.schema.ts](<../frontend/presentation/schemas/auth.schema.ts>) | Schema đăng ký thường chỉ cho LOCAL. |
| [frontend/presentation/services/AuthService.ts](<../frontend/presentation/services/AuthService.ts>) | Register trả metadata chờ OTP; thêm metadata/verifyOtp/resendOtp. |
| [frontend/proxy.ts](<../frontend/proxy.ts>) | Public đúng routes UI/BFF OTP và Google mới. |
| [frontend/usecases/auth/VerificationUsecase.ts](<../frontend/usecases/auth/VerificationUsecase.ts>) | Usecase Next cho metadata/verify/resend/Google exchange. |
