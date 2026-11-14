# Tech stack và môi trường WEdu

## Cấu hình Auth OTP/Google — 2026-11-13

Cập nhật 2026-11-14: `application.yml` khai báo `spring.security.oauth2.client.registration.google.redirect-uri` là `{baseUrl}/auth/login/oauth2/code/{registrationId}`. SecurityConfig đồng bộ OAuth redirection baseUri `/auth/login/oauth2/code/*` và filter-chain matcher; SecurityConstants chỉ public callback Google chính xác. Với request tới `http://localhost:8080`, callback là `http://localhost:8080/auth/login/oauth2/code/google`. YAML parse/property và8 tests OAuth/JWT filter đạt; chưa xác nhận Google Console hoặc đăng nhập Google thật. Nếu có environment override redirect-uri, cập nhật override theo URL mới và khởi động lại backend.

Không thêm dependency. Backend bắt buộc OTP_SECRET riêng ít nhất32 ký tự, cấp qua environment/IDE/deployment (Spring không tự nạp .env). Dùng GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET, FRONTEND_URL và SMTP hiện có. Property frontend là app.frontend.url. Đăng ký authorized redirect URI chính xác <backend-public-origin>/auth/login/oauth2/code/google trong Google Console. Nếu reverse proxy, cấu hình SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI tới URI public đó; không dựa vào host nội bộ.

Next dùng BACKEND_URL cho server-to-server; GOOGLE_OAUTH_BASE_URL là backend URL browser truy cập được (fallback BACKEND_URL). Tất cả secrets/verifier xử lý server, không đặt NEXT_PUBLIC. Backend session OAuth path=/, HttpOnly/SameSite=Lax; đặt SESSION_COOKIE_SECURE=true khi deployment HTTPS (mặc định false cho localhost). Cần sticky sessions nếu nhiều backend replicas vì state tạm nằm trong HttpSession. JWT API vẫn stateless.

Backend migration chỉ dùng Java, không cần Node.js/mongosh. Trong maintenance window sau backup/dừng writers, chạy từ backend: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--auth.migration.enabled=true"`. Hoặc đặt AUTH_MIGRATION_ENABLED=true trong môi trường chạy/IDE. Flag mặc định false, tắt lại sau khi hoàn tất. Spring vẫn cần environment ứng dụng hiện có, không tự nạp .env. Runner chạy trước AuthIndexConfig; usecase kiểm toàn bộ dữ liệu rồi mới ghi, repository chuẩn hóa email/cleanup field+index cũ/nâng LOCAL đã có Google link thành MIXED. Không thêm dependency; DDL/bulk không atomic nên không mở traffic/writers/jobs trong maintenance. [Validation/chi tiết](auth-java-migration-implementation-report.md).

## Refund configuration and checks — 2026-11-09

No new package dependencies. Source rechecked on main `cbcb49a` (2026-11-10): `payment.refund.enabled=${REFUND_ENABLED}` controls refund mutations/provider polling and requires an explicit runtime environment value; application.yml has no fallback. Set false to disable mutations/polling, true to enable. RefundSettings.enabled is a Java boolean initialized false, but this does not supply a missing YAML placeholder. Spring does not automatically load backend/.env; the launcher/IDE/container must provide it. Owned history remains readable when disabled, and teacher AI tracking remains active for future evidence. Do not enable until index initialization and VNPay sandbox verification are complete. Refund uses existing backend VNPay merchant credentials/query URL and RestClient timeouts; no credential is exposed through NEXT_PUBLIC variables.

RefundJobs runs every 60 seconds with a 300-second persisted per-request query throttle; unresolved records stop automatic polling after 24 hours. Refund emails reuse PaymentJobs (15 seconds, maximum five attempts, 300-second lease, 60-second retry). Existing scheduling enablement and VNPay enabled configuration still apply. No long transaction wraps refund/query HTTP.

Targeted command: `mvn -q '-Dtest=Refund*Test,VnpayRefundGatewayTest,Payment*Test,VnpayGatewayTest,VnpayQueryGatewayTest,AdminTransaction*Test,PlusEntitlementTest' test`. Real concurrent Mongo tests require explicit `WEDU_PAYMENT_TEST_MONGO_URI` to a dedicated replica set and create/drop only a generated `wedu_refund_test_*` database. No URI was available in this session. Frontend checks use build, TypeScript and targeted ESLint. Exact results and limitations are in the [implementation report](refund-implementation-report.md).

## Snapshot hiện tại — 2026-11-06

Đối chiếu source/config tại commit `abcde6e`. Phiên bản khai báo và lockfile được tách rõ: frontend Next ^16.1.6 → lock 16.3.5, React 19.2.3, TypeScript ^5 → lock 5.9.3; backend Spring Boot 4.0.6/Java 17; exporter Express 5.2.1/TypeScript 6.0.3. Không suy phiên bản triển khai thực từ node_modules. Payment không thêm dependency; dùng RestClient/Mongo transaction/JavaMailSender sẵn có.

Các mục có ngày cũ là bằng chứng lịch sử của từng lần triển khai; khi mâu thuẫn, dùng snapshot hiện tại và source được dẫn. Không suy ra production hoặc kiểm thử end-to-end đã đạt từ việc có code.

## Validation checkout sạch — 2026-11-01

Các staged snapshot frontend tại `9c997a0`/`1e36f83` qua Webpack compile nhưng thất bại ở TypeScript với TS1261: Git lưu [draftRepositoryImpl.ts](../frontend/infrastructure/repositories/draftRepositoryImpl.ts) và [userRepositoryImpl.ts](../frontend/infrastructure/repositories/userRepositoryImpl.ts), trong khi imports dùng PascalCase. Working tree Windows có thể mang casing khác nên kết quả build tại working tree không chứng nhận checkout từ Git. Không đổi tên file hoặc sửa import trong lượt tài liệu; ghi nhận kết quả ở [current-status](current-status.md#snapshot-main-sau-merge-ai-và-quản-lý-tài-khoản--2026-11-01).

## Quản lý usage/budget AI — 2026-11-01

Khi đổi package/move class Java, chạy `mvn clean compile` (hoặc Maven wrapper) trước khi khởi động lại. Incremental compile có thể giữ class ở package cũ trong target/classes; component scan khi đó gặp hai bean cùng tên dù source chỉ còn một. Đã xác nhận trường hợp `ai.service` → `ai.usecase`; không xử lý bằng đổi tên bean hoặc bật bean overriding. Hai test dùng constructor package-private phải cùng package `ai.usecase`.

Không thêm dependency. Backend dùng RestClient/ObjectMapper, MongoTemplate aggregation, MapStruct và BigDecimal có sẵn; frontend dùng TanStack Query và SVG, không thêm chart/test runner.

BudgetDTO.IntegerDeserializer dùng `tools.jackson` (Jackson 3 của Spring MVC) để từ chối quota/revision JSON không nguyên, tránh coercion thập phân thành Long. ObjectMapper `com.fasterxml.jackson` hiện có của provider/JWT và JacksonConfig giữ nguyên; không thay cấu hình JSON toàn hệ thống.

[application.yml](../backend/src/main/resources/application.yml) bổ sung `ai.pricing.version=deepseek-2026-11-01-peak-usd`, map `ai.pricing.models.<model>.input-cache-hit/input-cache-miss/output` với đơn vị USD/triệu token. Giá cao điểm snapshot 01/11: deepseek-flash = 0.006/0.3/1.2; deepseek-v4-pro = 0.044/1.32/3.96. Đây là ước tính, không áp dụng lịch giảm giá; cần cập nhật cấu hình/version khi giá đổi hoặc thêm model. Nguồn: [DeepSeek pricing](https://api-docs.deepseek.com/quick_start/pricing/).

DeepSeekBalanceService dùng api-key/base-url hiện có, GET `/user/balance`, cache process-local 60 giây gồm cả attempt thất bại; connect/read timeout riêng mỗi loại 5 giây. Không đổi generation timeout/retry. Không thêm secrets hoặc NEXT_PUBLIC credentials. Mongo account cần quyền tạo hai index AiUsage. Budget do admin lưu trong DB; không có hạn mức nội bộ mặc định khi collection chưa có cấu hình.

Validation và giới hạn môi trường ở [current-status](current-status.md#quản-lý-usagebudget-ai--2026-11-01); lệnh build/test của các module giữ nguyên.

## Upload ảnh trong editor câu hỏi — 2026-10-29

Không thêm dependency. Editor dùng KaTeX/React Query hiện có. [next.config.ts](../frontend/next.config.ts) đặt experimental.proxyClientMaxBodySize = 65 MiB, đồng bộ Spring max-request-size 65 MB để proxy không cắt multipart nhiều ảnh ở mặc định 10 MiB (đối chiếu source Next cài trong node_modules). Giới hạn nghiệp vụ vẫn 5 MiB/ảnh JPEG/PNG, tối đa 10 ảnh/upload request; Next PATCH giới hạn payload JSON 1 MiB. Backend tái sử dụng S3Service.uploadImageFile, không đổi cấu hình Spring. [API](api-reference.md).

## Phạm vi bằng chứng

Đối chiếu manifest/config/lockfile ngày 2026-11-06. Phiên bản trong bảng là range/giá trị **khai báo**, không phải phiên bản runtime đã triển khai. Trạng thái `implemented/partial/missing/unknown` được định nghĩa tại [current-status.md](current-status.md). Không truy cập production, `.env` hoặc secrets.

## Backend

Nguồn chính: [pom.xml](../backend/pom.xml), [Maven wrapper](../backend/.mvn/wrapper/maven-wrapper.properties), [Dockerfile](../backend/Dockerfile).

| Dependency/cấu hình | Phiên bản | Mục đích được xác minh |
| --- | --- | --- |
| Java | 17 | Compiler target và image Temurin runtime. |
| Spring Boot parent | 4.0.6 | Quản lý dependency/plugin và ứng dụng Spring. |
| Maven wrapper distribution | 3.9.14 | Build backend; wrapperVersion 3.3.4 không phải Maven runtime. |
| `spring-boot-starter-webmvc` | Do Boot quản lý | REST controllers, servlet stack. |
| Security, security-oauth2-client | Do Boot quản lý | JWT filter, method security, Google OAuth2. |
| Data MongoDB, starter-mongodb | Do Boot quản lý | MongoTemplate, document mapping. Cả hai starter hiện cùng được khai báo. |
| `spring-boot-transaction` | Do Boot quản lý | Transaction annotations/manager. |
| `spring-boot-starter-mail` | Do Boot quản lý | JavaMailSender gửi reset password, từ chối DOCX và kết quả payment; payment có outbox retry riêng. |
| JJWT api/impl/jackson | 0.13.0 | HMAC JWT, claims, parse/validate token. |
| AWS SDK `s3` | 2.46.16 | Upload/list/delete S3 và presigned GET. |
| Apache Tika core | 3.3.1 | Detect MIME ảnh; DOCX dùng extension và ZIP-entry checks của Java ZipInputStream. |
| MapStruct + processor | 1.6.3 | Entity/document/DTO mappers; processor ở compile và testCompile. |
| Lombok | Do Boot quản lý | Accessors, constructors, builder, logging annotations. |
| springdoc-openapi-starter-webmvc-ui | 3.0.3 | OpenAPI/Swagger; paths được public trong security constants. |
| Các starter `*-test` | Do Boot quản lý | MVC, security/OAuth2, MongoDB test; code dùng JUnit Jupiter, Mockito, AssertJ, MockMvc. |

Ví dụ sử dụng: [JwtTokenProvider](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/provider/JwtTokenProvider.java), [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java), [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java). DeepSeek dùng Spring `RestClient`, exporter/Telegram dùng `RestTemplate`, không có SDK AI riêng trong manifest. Pandoc được gọi bằng `ProcessBuilder`, không phải thư viện Maven: [PandocConverter.parse](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java).

## Frontend

Nguồn range: [package.json](../frontend/package.json); version đã resolve nằm trong [package-lock.json](../frontend/package-lock.json). Bảng là dependency trực tiếp, trừ dòng Zod được ghi rõ.

| Dependency | Khai báo | Vai trò/bằng chứng |
| --- | --- | --- |
| Next.js | ^16.1.6 | App Router, route handlers, `proxy.ts`, `next/image`. |
| React / React DOM | 19.2.3 | UI, hooks, client/server component. |
| TypeScript | ^5 | Strict types; [tsconfig.json](../frontend/tsconfig.json). |
| TanStack React Query | ^5.90.21 | Cache/query/invalidate; [QueryProvider](../frontend/provider/QueryProvider.tsx). |
| Axios | ^1.13.2 | Gọi Next API và backend; [AuthRepositoryImpl](../frontend/infrastructure/repositories/AuthRepositoryImpl.ts). |
| JOSE | ^6.2.3 | Verify JWT ở [proxy.ts](../frontend/proxy.ts). |
| Tailwind CSS / @tailwindcss/postcss | ^4 | CSS utility pipeline; [globals.css](../frontend/app/globals.css), [postcss.config.mjs](../frontend/postcss.config.mjs). |
| Framer Motion | ^12.43.0 | Animation; tra imports trong `presentation/components` và trang `(site)`. |
| Lucide React | ^0.577.0 | Có import thực trong [Navbar](../frontend/presentation/components/layout/Navbar.tsx) và [QuestionFilesPage](../frontend/app/(site)/moderator/question-files/page.tsx); SVG icons cũ vẫn có. |
| KaTeX | ^0.16.32 | Render math; [QuestionContent](../frontend/presentation/components/Exam/QuestionContent.tsx). |
| isomorphic-dompurify | ^2.35.0 | Khai báo dependency; không xem việc có package là bằng chứng mọi HTML đã được sanitize. |
| dotenv | ^17.2.3 | Khai báo dependency; không suy ra Spring backend nạp `.env`. |
| ESLint / eslint-config-next | ^9 / 16.1.1 | Next vitals/TypeScript rules, lint script. |
| eslint-plugin-unused-imports | ^4.4.1 | Rule `unused-imports/no-unused-imports: error`. |
| eslint-plugin-unicorn | ^64.0.0 | Chỉ khai báo; chưa bật trong [eslint config](../frontend/eslint.config.mjs). |
| @types/node / react / react-dom | ^20 / ^19 / ^19 | Compile-time type declarations, không xác định Node runtime. |
| Zod (gián tiếp) | Lockfile 4.3.2 | Schema và `z.flattenError`; chưa là direct dependency: [auth.schema.ts](../frontend/presentation/schemas/auth.schema.ts). |

TypeScript `strict`, `noEmit`, target ES2017, bundler resolution; alias `@/*` trỏ root frontend. Không thấy formatter config hoặc automated frontend test script. Cấu hình Next image cho phép hostname S3 cụ thể: [next.config.ts](../frontend/next.config.ts). Runtime Node/deployment thật: **unknown**.

## Exporter service

Nguồn: [package.json](../exporter-service/package.json), [tsconfig.json](../exporter-service/tsconfig.json), [app.ts](../exporter-service/src/app.ts).

| Dependency | Khai báo | Mục đích |
| --- | --- | --- |
| Express / @types/express | ^5.2.1 / ^5.0.6 | HTTP route/controller và types. |
| TypeScript / tsx | ^6.0.3 / ^4.23.0 | Compile và dev watch. |
| ts-node | ^10.9.2 | Được khai báo; scripts hiện dùng tsx/tsc/node. |
| @types/node | ^26.1.0 | Node types, không phải runtime requirement đã pin. |
| docx | ^9.7.1 | Document, paragraphs, images, packing. |
| JSZip | ^3.10.1 | Mở DOCX ZIP và thay math XML. |
| mathjax-full | ^3.2.1 | LaTeX → MathML. |
| mathml2omml | ^0.5.0 | MathML → Office Math XML. |
| image-size | ^2.0.2 | Kích thước ảnh trong builders. |

[generateWordFile.ts](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts), [latexToOmml.ts](../exporter-service/src/routes/exporter/infrastructure/converter/latexToOmml.ts) và builders là bằng chứng pipeline. ESM + NodeNext, strict, noUncheckedIndexedAccess/exactOptionalPropertyTypes; `types: []`, `outDir/rootDir` comment. `npm start` đòi `dist/app.js`, nên cấu hình output hiện chưa khớp.

## Cập nhật tích hợp 24/10/2026

Không thêm dependency mới cho question-file flow. Java ZipInputStream kiểm DOCX có `[Content_Types].xml`/`word/document.xml`, Normalizer/Locale chuẩn hóa filename; Spring ContentDisposition tạo tên tải UTF-8. AWS S3 list dùng continuation token, signed GET 60 phút với attachment. React Query/Lucide phục vụ UI moderator. [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java), [files UI](../frontend/app/(site)/moderator/question-files/page.tsx).

Backend avatar giới hạn 5 MiB; question-file 50 MiB, Spring multipart vẫn 50 MB/file và 65 MB/request theo configuration. Frontend upload page ghi50 MB; StorageService.uploadFileToQuestionBank trả Promise<string> tương ứng S3 key. Không coi đây là một package MIME/DOCX mới.

Có test source mới [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java), [StorageQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageQuestionFilesTest.java), `ModeratorQuestionFilesTest` (snapshot cũ; file không còn trong source hiện tại). Có test legacy endpoint chưa khớp controller hiện tại; lượt này chỉ kiểm tra tài liệu và source, không chạy build/test hoặc gọi AWS.
## Lệnh thực tế

Chạy trong module; không có root build command.

| Module | Install | Run | Build | Lint/test |
| --- | --- | --- | --- | --- |
| frontend | `npm ci` | `npm run dev` / `npm start`, cổng 3000 | `npm run build` | `npm run lint`; không có test script |
| backend Windows | Maven wrapper tự lấy distribution/dependencies | `./mvnw.cmd spring-boot:run` | `./mvnw.cmd clean package` | `./mvnw.cmd test`; lọc: `./mvnw.cmd -Dtest=UserUsecaseMeTest test` |
| backend Unix | Như Windows | `./mvnw spring-boot:run` | `./mvnw clean package` | `./mvnw test` |
| exporter | `npm ci` | `npm run dev`, cổng 8000; `npm start` có caveat output | `npm run build` | `npm test` là placeholder exit 1; không có lint script |

Không coi việc script tồn tại là build/test đã pass. Backend Docker build bỏ tests. Không thấy coverage threshold được cấu hình trong manifest/config hiện tại.

## Cấu hình và tích hợp ngoài

Nguồn [application.yml](../backend/src/main/resources/application.yml), [S3Config](../backend/src/main/java/com/wedu/exam_creation/config/S3Config.java), [DeepSeekProperties](../backend/src/main/java/com/wedu/exam_creation/ai/properties/DeepSeekProperties.java), [frontend proxy](../frontend/proxy.ts).

| Nhóm | Tên biến/config được tham chiếu | Lưu ý |
| --- | --- | --- |
| MongoDB | `MONGO_DB_USER`, `MONGO_DB_PASSWORD` | SRV; DB `test001`; topology/index thực **unknown**. |
| JWT | `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET` | Base64 HMAC; frontend server dùng cùng access secret. |
| Google | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | OAuth scopes openid/profile/email. |
| AWS | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | StaticCredentialsProvider; region ap-southeast-2; bucket từ config. |
| AI | `DEEPSEEK_API_KEY`, `ai.deepseek.*` | Base api.deepseek.com, model config deepseek-flash; property timeout 30 giây chưa được client áp dụng. |
| SMTP | `MAIL_APP_USERNAME`, `MAIL_APP_PASSWORD` | smtp.gmail.com:587, STARTTLS. |
| Telegram | `TELEGRAM_QUESTION_BOT_TOKEN`, `TELEGRAM_S3_BOT_TOKEN`, `TELEGRAM_CHAT_ID` | Hai bot gửi HTTP sendMessage. |
| Liên service | `FRONTEND_URL`, `EXPRESS_EXPORTER_URL` | Reset link; exporter base ghép `+ "exam"`. |
| Frontend | `NEXT_PUBLIC_BACKEND_DEV_URL`, `NEXT_PUBLIC_BACKEND_PROD_URL`, `BACKEND_URL` | Repository và proxy dùng các nguồn URL khác nhau; không đặt secret vào NEXT_PUBLIC. |

Không thấy loader `.env` backend trong source/config đã khảo sát. Cần cấp biến qua môi trường/IDE/deployment. Pandoc phải có trên PATH và backend cần quyền ghi workspace `static/uploads`; Dockerfile hiện chưa chuẩn bị hai phần đó. Không xác nhận kết nối dịch vụ hay secrets thực tế.

## Dependency preview DOCX — 2026-10-25

Frontend thêm docx-preview ^0.4.1 (lock 0.4.1), phụ thuộc JSZip (lock 3.10.2), import động tại [useQuestionFilePreview](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts). Render HTML/MathML chỉ đọc phía browser, không cài converter server và không dùng viewer bên thứ ba. [package.json](../frontend/package.json), [lockfile](../frontend/package-lock.json). Không thay lệnh build/test.

## Kiểm tra kiểm duyệt tệp — 2026-10-25

Không thêm dependency/config SMTP/Telegram. Tái sử dụng JavaMailSender gửi email text/plain; lỗi chỉ log, không retry. Backend chạy `mvn -q test`; frontend không còn bộ test sau khi xóa theo yêu cầu; không có npm test script. Không đổi dependency/lockfile vì không có dependency test riêng được thêm. `mvn -q test`: **244 tests**, 0 failures/errors, gồm **27** cases [ModeratorQuestionFileReviewTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileReviewTest.java) và **1** [MailRejectionTest](../backend/src/test/java/com/wedu/exam_creation/mail/service/MailRejectionTest.java) mới. Frontend từng đạt 9 kiểm tra BFF/hook/form/preview DOCX trong JSDOM trước khi bộ test được xóa theo yêu cầu; hiện không còn file test frontend trong dự án. TypeScript, ESLint các file liên quan và production build đã đạt cho phần application code không đổi. AWS/SMTP/Pandoc thật và trình duyệt thật chưa kiểm tra; JSDOM dùng polyfill dialog.

## Backend VNPay setup — 2026-11-05

No new dependency. Java HmacSHA512/SecureRandom, Spring RestClient(connect5s/read10s), MongoTransactionManager and JavaMailSender. SMTP connect5s/read-write10s bounds email lease work. Supply VNPAY_ENABLED,VNPAY_TMN_CODE,VNPAY_HASH_SECRET,VNPAY_PAYMENT_URL,VNPAY_QUERY_URL,VNPAY_RETURN_URL; optional VNPAY_BANK_CODE(default blank),VNPAY_SERVER_IP(default127.0.0.1). Current application.yml requires the non-optional env values; Spring does not automatically load .env. Do not log/expose secrets or signed URLs.

Restart backend to load endpoints/index creation; Mongo must support transactions and creating the unique sparse request hash index. Register an externally reachable HTTPS /payment/vnpay/ipn URL with VNPay separately; source code does not configure the merchant. Return receipts and querydr are independently verified by backend. Optional Mongo integration tests require WEDU_PAYMENT_TEST_MONGO_URI pointing to a dedicated test replica set and use a fresh wedu_payment_test_<uuid> database.

Validation 2026-11-05: payment/entitlement/gateway suite 64 passed, 5 Mongo integration tests skipped (no dedicated replica set). Full mvn -q test: 445 tests, 438 passed, 2 existing failures (AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit; ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite), 5 skipped. No real VNPay/SMTP end-to-end certification.

## Frontend payment setup — 2026-11-06

No new dependency or package script; Next/React/TypeScript, TanStack Query and Axios remain in use. Set server-side BACKEND_URL to reachable Spring API; the existing adapter also retains development/production backend URL fallbacks. Never put merchant credentials in NEXT_PUBLIC variables. Rewrite creation requires BACKEND_URL and a frontend restart/build after config edits. Current development origin allows pessimism-shimmy-expansive.ngrok-free.dev.

Register the externally reachable HTTPS /payment/vnpay/ipn URL with VNPay separately. /me/bill is the Return page, not the IPN endpoint. Signed receipts are kept in memory and stripped from the browser address; reload/closing the tab can lose an unconfirmed receipt, so IPN/querydr still matter. Matching backend payment endpoints are already available in main through PR #131; this frontend commit adds the UI and BFF on that updated base.

Validation 2026-11-06: npm run build, TypeScript --noEmit --incremental false and ESLint for all20 frontend payment files passed. Six temporary harnesses passed: missing-order, signed Return, Return BFF, new-tab, reconciliation and IPN proxy. They mock services/React JSDOM and do not certify live sandbox payment, SMTP delivery or a browser checkout.
