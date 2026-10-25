# Repository Guidelines

## Phạm vi và cấu trúc

Đọc context liên quan trước khi sửa code. Tài liệu là snapshot tại lần cập nhật gần nhất, không mặc định đồng bộ hoàn toàn với working tree hiện tại và không chứng nhận production hoặc test đã pass. Source/config hiện tại là nguồn sự thật về implementation; giữ thay đổi đang có của người dùng và không tự tái cấu trúc ngoài yêu cầu.

| Task                                      | Tài liệu phải đọc                                                                                                                                                                                       |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Bắt đầu task, đánh giá phạm vi            | Đọc bảng snapshot/section liên quan trong [development-decisions](codex/development-decisions.md), rồi phần liên quan trong [current-status](codex/current-status.md) và [module-map](codex/module-map.md) |
| API/contract, tìm symbol cụ thể           | [api-reference](codex/api-reference.md), [source-catalog](codex/source-catalog.md) theo module cần sửa                                                                                                    |
| Kiến trúc, liên service, auth/security    | [architecture](codex/architecture.md), [business-flows](codex/business-flows.md)                                                                                                                          |
| Dependency, setup, build/run/lint/test    | [tech-stack](codex/tech-stack.md)                                                                                                                                                                        |
| Model, persistence, query, index          | [database-schema](codex/database-schema.md), module-map                                                                                                                                                  |
| Nghiệp vụ, role, review, AI, PLUS/payment | business-flows, current-status                                                                                                                                                                          |

Theo link bằng chứng và đọc source hiện tại trước khi triển khai; nếu docs lệch code, cập nhật theo bằng chứng. `implemented` có code xử lý; `partial` có mắt xích thiếu/lệch; `missing` không tìm thấy trong source khảo sát; `unknown` chưa đủ bằng chứng. Không coi UI “chờ duyệt”/Telegram message là trạng thái database hoặc suy ra payment/PLUS provisioning đã có từ accountType.

Để giảm context/token, không mặc định đọc toàn bộ mọi tài liệu. Với `development-decisions.md`, đọc snapshot quyết định và section liên quan trực tiếp đến task; chỉ đọc toàn file khi task cắt ngang nhiều feature. Với `api-reference.md`, `source-catalog.md`, `database-schema.md` và các inventory/reference khác, chỉ đọc module/symbol/range cần thiết.

- `frontend/`: Next.js App Router/React/TypeScript. Luồng: component → hook → `presentation/services` → `app/api` → `usecases` → domain repository → infrastructure adapter → backend. Assets ở `public/`.
- `backend/`: Java 17/Spring Boot, package `com.wedu.exam_creation`; feature chia `controller`, `usecase`, `domain`, `dto`, `infrastructure`. MongoTemplate truy vấn; MapStruct chuyển entity/document/DTO. Liên feature dùng service và `common/dto` theo code lân cận.
- `exporter-service/`: Express/TypeScript; route → controller → use case → builder/converter tạo DOCX. `images/` chứa ảnh tài liệu.

## Lệnh phát triển và kiểm tra

Chạy trong module, không có root build script. Hai module npm dùng `npm ci` và lockfile riêng.

| Module   | Lệnh                                                                        |
| -------- | --------------------------------------------------------------------------- |
| Frontend | `npm run dev` (3000), `npm run build`, `npm start`, `npm run lint`          |
| Backend  | `./mvnw.cmd spring-boot:run`, `./mvnw.cmd test`, `./mvnw.cmd clean package` |
| Exporter | `npm run dev` (8000), `npm run build`, `npm start`                          |

Unix dùng `./mvnw`. Exporter thiếu `outDir` dù start cần `dist/app.js`. Backend dùng JUnit Jupiter/Mockito/AssertJ/MockMvc; tests ở `src/test/java`, tên `*Test.java`. Frontend chưa có test script; exporter test là placeholder thất bại. Không có coverage threshold cấu hình. Báo rõ kiểm tra đã chạy và lỗi có sẵn.

## Quy tắc thêm tính năng

- Giữ controller/route mỏng, nghiệp vụ trong use case/service, persistence trong repository; tách entity/document/DTO.
- Giữ owner filter `userId` cho draft/exam, quyền `ROLE_*`, JWT/cookie/refresh contract. Backend quyết định quyền.
- Đồng bộ schema, types, HTTP method/path, DTO và payload exporter khi đổi contract. Giữ placeholder `<math_N>`, `<img_N>` và S3 object key.
- Theo style lân cận: TypeScript hai spaces, Java bốn; PascalCase cho class/component, camelCase cho hàm, `I*Repository`, `*RepositoryImpl`, `*Usecase`, `*DTO`. Frontend dùng `@/`; exporter ESM import `.js`. ESLint cấm unused imports.
- Với frontend, tách code giao diện/presentation khỏi logic xử lý, state phức tạp, data fetching và business logic; page/component tập trung render và interaction, còn logic đặt trong
hook/service/usecase phù hợp với kiến trúc hiện có.
- Không lặp lại UI hoặc implementation giống nhau ở nhiều nơi. UI/pattern giao diện dùng lại phải tách thành reusable component; logic không thuộc UI bị lặp phải tách thành hook/service dùng chung phù hợp, không copy-paste giữa các component/page.
- Không ghi/log secrets, password, token hoặc presigned URL; không đặt credentials trong `NEXT_PUBLIC_*`. Không mặc định Spring tự nạp `.env`.

## Commit và PR

Commit message dùng Conventional Commits với format `<type>(<module>/<feature>): <description>`. `<module>` bắt buộc là `backend`, `frontend`, `exporter-service` hoặc `repo`; `<feature>` phải mô tả rõ tính năng/module đang thay đổi, không dùng scope chung chung hoặc chỉ có module. Mặc định commit body gồm các gạch đầu dòng tóm tắt thay đổi chính; nếu người dùng yêu cầu commit một dòng thì chỉ dùng tiêu đề.

Ví dụ: `feat(backend/question-file-management): add file approval`, `fix(frontend/auth): handle expired access token`, `refactor(exporter-service/document-export): simplify image handling`, `chore(repo/codex-guidelines): update commit safety rules`.

PR mô tả hành vi, module/contract bị ảnh hưởng, kết quả kiểm tra, issue và ảnh nếu đổi UI.

## Cập nhật context cùng thay đổi

API reference và source catalogue cũng phải được đồng bộ khi thêm/xóa/đổi tên route, DTO, model hoặc symbol.

Khi thay đổi kiến trúc, API, dependency, model/index, quyền hoặc nghiệp vụ, cập nhật docs tương ứng **trong cùng thay đổi**: architecture cho giao tiếp/ranh giới; tech-stack cho version/lệnh/config; module-map cho symbols/routes/contracts; database-schema cho fields/references/states; business-flows cho trình tự/side effects; current-status cho mức hoàn thiện, vấn đề và unknown đã giải quyết. Gắn path/symbol bằng chứng, ghi rõ kiểm tra đã chạy; không biến dự định thành implemented hoặc ghi giá trị secrets. Kiểm tra links/inventory sau khi rename/thêm/xóa file. Khi task chỉ phân tích, không tự sửa application code vì tài liệu phát hiện vấn đề.

## Quyết định phát triển

`docs/development-decisions.md` là nguồn quyết định về phạm vi phát triển.

Nếu một vấn đề được ghi nhận trong current-status.md, architecture.md hoặc business-flows.md nhưng development-decisions.md đánh dấu DEFERRED, FROZEN, OUT_OF_SCOPE hoặc INVESTIGATE_ONLY thì không tự sửa vấn đề đó.

Khi xác định **business intent / development scope**, ưu tiên:

1. Yêu cầu hiện tại của người dùng
2. `development-decisions.md`
3. `AGENTS.md`
4. `business-flows.md`

Khi xác định **implementation hiện tại**, ưu tiên:

1. Source code/config hiện tại
2. `current-status.md`
3. `architecture.md`
4. `module-map.md`
5. Các inventory/reference docs

Nếu source khác docs, source quyết định implementation hiện tại; báo mismatch và cập nhật docs khi task làm thay đổi implementation. Không dùng source để tự thay đổi business decision, development scope hoặc roadmap.

## Quy trình phê duyệt trước khi thay đổi code

Đối với task implement feature, fix bug, refactor, thay đổi API/contract, database/schema hoặc business logic, thực hiện theo hai giai đoạn.

### Phase 1 — Analyze & Propose

Khi nhận task:

1. Đọc context/docs bắt buộc theo hướng dẫn trong `AGENTS.md`.
2. Đọc và trace source code liên quan.
3. Xác định root cause hoặc behavior hiện tại.
4. Đề xuất solution trước khi sửa code.
5. Solution phải nêu:
   - nguyên nhân/root cause;
   - behavior hiện tại;
   - behavior mong muốn;
   - hướng sửa đề xuất;
   - files/symbols dự kiến thay đổi;
   - contract/schema/API bị ảnh hưởng nếu có;
   - rủi ro hoặc side effects;
   - validation/test dự kiến chạy;
   - docs cần cập nhật.
6. Nếu có nhiều hướng hợp lý, trình bày các phương án và recommendation.
7. Không sửa application code, config, database/schema hoặc docs trong phase này.

Kết thúc Phase 1 bằng việc chờ người dùng phê duyệt.

### Phase 2 — Implement

Chỉ bắt đầu khi người dùng xác nhận rõ ràng solution, ví dụ:

- `approved`
- `confirm`
- `implement`
- `tiến hành`
- hoặc chỉ định một solution cụ thể để thực hiện.

Sau khi được phê duyệt:

1. Implement đúng solution đã được duyệt.
2. Không tự mở rộng scope.
3. Nếu trong lúc implement phát hiện vấn đề làm thay đổi đáng kể solution đã duyệt, dừng lại và báo lại để xin phê duyệt mới.
4. Chạy validation/test phù hợp.
5. Cập nhật docs theo quy định của repository nếu implementation làm thay đổi context.
6. Báo cáo:
   - toàn bộ files đã thay đổi, không bỏ sót application code, test, config hoặc docs;
   - với mỗi file, mô tả ngắn gọn thay đổi đã thực hiện;
   - thay đổi chính và behavior trước/sau;
   - contract/schema/API bị thay đổi nếu có;
   - validation/test đã chạy và kết quả;
   - lỗi validation có sẵn từ trước, nếu có;
   - vấn đề phát hiện thêm nhưng không xử lý vì ngoài scope.

Không coi yêu cầu ban đầu về việc fix/implement là approval cho Phase 2 nếu chưa trình bày solution.

### Bỏ qua Phase 1

Chỉ bỏ qua approval workflow khi người dùng nói rõ rằng có thể triển khai ngay,
ví dụ: `implement directly`, `fix luôn`, `không cần chờ duyệt`.

Trong trường hợp đó vẫn phải đọc context và kiểm tra phạm vi trước khi sửa.

### Git commit safety

Khi chuẩn bị commit:

- Chỉ stage các thay đổi thuộc scope được yêu cầu; không stage thay đổi không liên quan.
- Nếu một file chứa cả thay đổi trong và ngoài scope, chỉ stage các hunk/range cần thiết.
- Kiểm tra staged snapshot hoàn chỉnh và nhất quán, bao gồm đầy đủ dependency và các file add/delete/move/rename liên quan.
- Đề xuất commit message theo convention ở mục **Commit và PR**; mặc định gồm tiêu đề và commit body dạng gạch đầu dòng, trừ khi người dùng yêu cầu commit một dòng.
- Trước commit chỉ báo danh sách file staged, validation result và commit message đề xuất; không in diff hoặc mô tả chi tiết theo từng file.
- Dừng và chờ confirm trước khi `git commit`.
- Commit đúng message và staged snapshot đã được duyệt; không tự thêm thay đổi sau khi confirm.
- Sau commit, báo commit hash/message và các thay đổi còn lại trong working tree.
- Commit approval không bao gồm push approval; luôn chờ confirm riêng trước khi `git push`.
