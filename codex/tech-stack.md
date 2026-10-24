# Tech stack và môi trường WEdu

## Phạm vi bằng chứng

Snapshot source ngày 2026-10-24. Phiên bản trong bảng là range/giá trị **khai báo**, không phải phiên bản runtime đã triển khai. Trạng thái `implemented/partial/missing/unknown` được định nghĩa tại [current-status.md](current-status.md). Không truy cập production, `.env` hoặc secrets.

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
| `spring-boot-starter-mail` | Do Boot quản lý | JavaMailSender gửi link reset. |
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

Backend avatar giới hạn 5 MiB; question-file 50 MiB, Spring multipart vẫn 50 MB/file và 65 MB/request theo configuration. Frontend upload page còn text 25 MB, chưa đồng bộ type string key ở StorageService. Không coi đây là một package MIME/DOCX mới.

Có test source mới [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java), [StorageQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageQuestionFilesTest.java), [ModeratorQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java). Có test legacy endpoint chưa khớp controller hiện tại; lượt này chỉ kiểm tra tài liệu và source, không chạy build/test hoặc gọi AWS.
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
