# Xuất ma trận đề — báo cáo triển khai 2026-11-12

## Snapshot đã merge — 2026-11-13

Đối chiếu main `5baf5e1`: backend `23a4111` qua PR #140, backend tests `ba3f912` qua PR #141, exporter `ef66943` qua PR #142, frontend `97e1d67` qua PR #143. Source/config hiện tại là bằng chứng implementation. Phiên đồng bộ context này không chạy lại build/tests, chỉ kiểm source, inventory, local links và staged diff; không chứng nhận production hoặc visual Word.

Mẫu `docs/format_file_ma_tran_de.docx` và harness `exporter-service/tests/matrixExport.test.ts` có trong phiên phát triển nhưng không được lưu trong checkout/HEAD hiện tại. Các kết quả exporter 3/8 tests bên dưới giữ ý nghĩa lịch sử; không phải suite có thể chạy lại từ checkout này. Backend vẫn có đủ ba file JUnit. Các bảng inventory theo lượt ghi lại quá trình triển khai, với harness không còn trong repo được ghi rõ.

## Kết quả triển khai

Trước thay đổi, nút có trên trang xem trước nhưng handler chỉ bật submit. Sau thay đổi, giáo viên xuất snapshot matrix/matrixDetails của đề thuộc sở hữu thành một DOCX hai bảng, tải tên `<tên đề>_ma trận đề.docx`. Giữ dữ liệu snapshot và chương/bài số lượng 0; không tính theo câu hỏi thực tế. Sau lần triển khai cơ bản, style được cập nhật theo mẫu và solution đã confirm bên dưới.

API mới: Next `POST /api/exporter/matrix`, Java `POST /exporter/matrix` nhận `{examId,examName}`; Express `POST /exporter/matrix` nhận `{examName,matrix,matrixDetails}`. Java quyết định role/owner, lấy examName từ database cho attachment filename; browser dùng tên đề từ request cho filename. Tên bài kiểm tra không xuất hiện trong nội dung DOCX sau bổ sung đã confirm. Không đổi schema/database, dependency, config, endpoint xuất đề cũ hoặc thuật toán sinh ma trận. [Chi tiết contract](api-reference.md#xuất-ma-trận-đề--2026-11-12).

## Validation

Các kết quả 13 backend/3 exporter tests và frontend dưới đây thuộc lượt triển khai ban đầu; lượt style có kết quả riêng ở section tiếp theo.

- `mvn -q -Dtest=ExamMatrixUsecaseTest,ExporterMatrixUsecaseTest,ExporterMatrixControllerTest test`: 13 tests, 0 failures/errors/skips. Bao gồm snapshot, owner-scoped read, quyền teacher/role khác, input, thiếu dữ liệu, 404/422, payload HTTP, binary và lỗi exporter. Log: backend/target/matrix-tests.log.
- Exporter `npx tsc --noEmit`: đạt. `npx tsx --test tests/matrixExport.test.ts`: 3 tests đạt; kiểm tra runtime payload, hai bảng DOCX/Unicode/zero/escaping/page break, HTTP matrix và smoke xuất đề cũ với payload tối thiểu.
- Frontend `npx tsc --noEmit --incremental false`, ESLint chín file frontend thay đổi và `npm run build`: đạt. Build log: thư mục TEMP, wedu-matrix-build.log.
- Harness tạm `wedu-matrix-frontend.cjs`: BFF auth/input, binary, filename UTF-8, giữ lỗi 403/404/422/500; tải Blob/cleanup đạt. `wedu-matrix-hook.cjs`: React hook/JSDOM chống bấm lặp, tải một lần, phục hồi pending và hiển thị lỗi Blob JSON đạt. Harness nằm ngoài repository.
- Lần đầu Maven không ghi được C:/.m2, tsx không đọc được Windows userInfo; build Next gặp quyền cache/network trong sandbox. Chạy lại Maven/tsx/build ngoài sandbox thành công. Harness tsx ban đầu gặp module loading, chuyển sang harness CommonJS transpile TypeScript và chạy thành công.
- `git diff --check` và kiểm tra link nội bộ trong section bổ sung/report: thực hiện trước bàn giao. Không chạy full backend suite; không tuyên bố các lỗi lịch sử của suite đã được sửa. Chưa chạy browser end-to-end với Mongo/backend thật hoặc kiểm tra hiển thị bằng Microsoft Word; không chứng nhận production.

## Ngoài phạm vi

Giữ cấu hình exporter hiện có: `npm start` cần dist/app.js nhưng tsconfig chưa đặt outDir. Không sửa lỗi ảnh/math của xuất đề, matrix editing hoặc thuật toán sinh thiếu. Export snapshot có thể khác số câu thật khi sinh thiếu, đúng quyết định đã duyệt.

## Cập nhật style theo mẫu

User confirm format theo mẫu nguồn `docs/format_file_ma_tran_de.docx` đã cung cấp trong phiên (không lưu trong repo hiện tại), gộp Vận dụng cao vào Vận dụng và mọi câu có trọng số bằng nhau trên thang 10. Mẫu nguồn chỉ đọc trong phiên, không sửa hoặc copy vào runtime service. Không thay backend/frontend/API/schema/config/dependency ở lượt này.

- Trước: hai bảng phẳng, chưa có tổng điểm/tỷ lệ. Sau: header 4 tầng với gộp ô ngang/dọc, nhóm chương/bài, các cột loại câu × Biết/Hiểu/Vận dụng, tổng số câu/điểm/tỷ lệ. Đặc tả giữ cả dạng bài và yêu cầu cần đạt trong cùng ô nội dung. Nhãn bổ sung ngoài bộ mặc định có thêm cột để không mất dữ liệu.
- Page/font theo XML mẫu: Letter ngang 15840×12240 twips, lề 1440 twips, Arial 12, text đen/viền single 0.5pt; tiêu đề đánh số và căn giữa, bản đặc tả bắt đầu trang mới. Lượt style ban đầu có tên đề, sau đó bỏ theo confirm ở section dưới. Điều chỉnh độ rộng cột để ô nội dung dài có thêm chỗ; không cố định chiều cao dòng; 4 hàng header lặp khi bảng tiếp tục sang trang.
- N là tổng selectedCount của matrix; cả hai bảng dùng chung điểm một câu 10/N. Điểm nhóm=count×10/N, tỷ lệ=count×100/N; N=0 trả 0. Không làm tròn trung gian, hiển thị tối đa 2 số thập phân theo vi-VN. Tổng từng bảng phản ánh snapshot riêng, không tự sửa khi hai phần lệch nhau. Không chép số ví dụ 3/4/3 hoặc tỷ lệ không nhất quán từ mẫu.
- `npx tsc --noEmit`: đạt. `npx tsx --test tests/matrixExport.test.ts`: **8/8 đạt**, gồm wire validation, XML page/font/grids/merge/summary, gộp mức độ và dữ liệu gốc bất biến, equal scoring/rounding, zero, 60 bài với nội dung dài, HTTP matrix và smoke route exam. tsx ban đầu lỗi Windows userInfo trong sandbox, chạy ngoài sandbox đạt. Một assertion ban đầu đếm cả tableHeader=false; builder đã chỉ ghi tableHeader cho 4 hàng header, test cuối đạt.
- Chưa kiểm ảnh render hoặc phân trang Word: phiên không có managed workspace document runtime/loader, LibreOffice/Word hoặc browser khả dụng (`iab` trả unavailable). Chỉ xác nhận cấu trúc OOXML, không coi structural test là chứng nhận visual. Không chạy lại backend/frontend ở lượt style.

Toàn bộ file sửa/thêm trong lượt style này:

| File | Thay đổi lượt style |
| --- | --- |
| exporter-service/src/routes/exporter/usecase/util/MatrixExportData.ts | Thêm model tổng hợp, ánh xạ mức độ, nhóm dữ liệu, tính điểm/tỷ lệ và format số. |
| exporter-service/src/routes/exporter/infrastructure/builder/MatrixTableBuilder.ts | Đổi bảng phẳng sang grid nhiều tầng, gộp chương/bài và footer tổng; dùng helper chung. |
| exporter-service/src/routes/exporter/usecase/generateMatrixWordFile.ts | Page/font/title theo mẫu, chuẩn bị model tổng hợp rồi render hai bảng. |
| exporter-service/tests/matrixExport.test.ts | Mở rộng thành 8 tests cho style, tổng hợp/điểm, zero và dữ liệu dài. |
| codex/development-decisions.md | Ghi quyết định style/gộp mức độ và trọng số điểm bằng nhau đã duyệt. |
| codex/business-flows.md | Mô tả cách nhóm dữ liệu, tính điểm và xuất bố cục mới. |
| codex/current-status.md | Trạng thái style, validation mới và giới hạn chưa render. |
| codex/module-map.md | Thêm utility/model và cập nhật input của builders. |
| codex/source-catalog.md | Bổ sung utility/symbols và inventory lượt style. |
| codex/api-reference.md | Ghi semantics output mới; wire contract giữ nguyên. |
| codex/architecture.md | Ranh giới bước tổng hợp và phần dựng bảng. |
| codex/matrix-export-implementation-report.md | Ghi behavior, kết quả kiểm tra và toàn bộ file lượt style. |

## Bỏ tên bài kiểm tra khỏi nội dung DOCX

Theo solution đã confirm, xóa helper examName() cùng hai paragraph chèn tên dưới tiêu đề ma trận/đặc tả. Giữ nguyên tên attachment và tên download `<tên đề>_ma trận đề.docx`; không đổi API/schema/config hoặc frontend/backend.

Toàn bộ file thay đổi trong lượt bổ sung:

| File | Thay đổi |
| --- | --- |
| exporter-service/src/routes/exporter/usecase/generateMatrixWordFile.ts | Bỏ helper và hai paragraph tên bài kiểm tra. |
| exporter-service/tests/matrixExport.test.ts | Assert tên không có trong XML DOCX trực tiếp và qua HTTP; vẫn kiểm filename tiếng Việt. |
| codex/development-decisions.md | Ghi quyết định không render tên trong nội dung. |
| codex/business-flows.md | Đồng bộ mô tả nội dung file. |
| codex/api-reference.md | Làm rõ examName chỉ phục vụ attachment filename. |
| codex/current-status.md | Ghi trạng thái bổ sung và link validation. |
| codex/matrix-export-implementation-report.md | Báo cáo phạm vi, file thay đổi và validation lượt bổ sung. |

Validation: `npx tsc --noEmit` đạt; `npx tsx --test tests/matrixExport.test.ts` đạt 8/8, bao gồm tên không có trong nội dung và filename vẫn đúng; `git diff --check` đạt. Chưa chạy lại frontend/backend vì không có thay đổi ở các module đó. Giới hạn chưa render Word giữ như lượt style.

## Danh sách file

Các đường dẫn dưới đây liệt kê application code, backend tests và docs của tính năng, cùng một mục harness exporter lịch sử không được giữ trong repo. Không thay config.

| File | Thay đổi |
| --- | --- |
| [ExamMatrixDTO.java](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamMatrixDTO.java) | Common DTO và hai loại dòng snapshot cho giao tiếp feature/exporter. |
| [UnprocessableEntityException.java](../backend/src/main/java/com/wedu/exam_creation/common/exception/UnprocessableEntityException.java) | Exception thiếu dữ liệu snapshot. |
| [GlobalExceptionHandler.java](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java) | Map exception mới sang 422/message. |
| [ExamService.java](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamService.java) | Facade đọc ma trận theo owner. |
| [ExamUsecase.java](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java) | Đọc entity qua repository, kiểm tra snapshot và map dòng theo thứ tự. |
| [ExportMatrixRequestDTO.java](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/ExportMatrixRequestDTO.java) | Request examId/examName với NotBlank. |
| [ExporterController.java](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java) | Endpoint teacher-only, DOCX header và filename UTF-8. |
| [ExporterUsecase.java](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java) | Gửi snapshot sang Express, kiểm tra binary/MIME và xử lý lỗi. |
| [ExamMatrixUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/usecase/ExamMatrixUsecaseTest.java) | 4 tests snapshot/owner/missing data. |
| [ExporterMatrixUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/exporter/usecase/ExporterMatrixUsecaseTest.java) | 3 tests HTTP payload/binary/failure. |
| [ExporterMatrixControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/exporter/controller/ExporterMatrixControllerTest.java) | 6 tests quyền/input/filename/404/422. |
| [MatrixWordPayloadRequestDTO.ts](../exporter-service/src/routes/exporter/dto/request/MatrixWordPayloadRequestDTO.ts) | Type payload và runtime validation. |
| [MatrixTableBuilder.ts](../exporter-service/src/routes/exporter/infrastructure/builder/MatrixTableBuilder.ts) | Builder bảng ma trận và bảng đặc tả với helper chung. |
| [generateMatrixWordFile.ts](../exporter-service/src/routes/exporter/usecase/generateMatrixWordFile.ts) | Dựng DOCX hai phần, trang ngang và ngắt trang. |
| [MatrixExportData.ts](../exporter-service/src/routes/exporter/usecase/util/MatrixExportData.ts) | Tổng hợp snapshot, gộp mức độ và tính điểm/tỷ lệ bằng trọng số chung. |
| [exporterController.ts](../exporter-service/src/routes/exporter/controller/exporterController.ts) | Validate request, gọi usecase, trả DOCX hoặc lỗi JSON. |
| [exporter.route.ts](../exporter-service/src/routes/exporter/exporter.route.ts) | Đăng ký POST /matrix dưới mount /exporter. |
| `exporter-service/tests/matrixExport.test.ts` (harness lịch sử, không có trong repo hiện tại) | Đã chạy 8 tests payload, style XML, tổng hợp/điểm, zero/dữ liệu dài và HTTP, smoke route exam trong phiên triển khai. |
| [page.tsx](<../frontend/app/(site)/exam/[examId]/page.tsx>) | Giới hạn nút theo role, disabled/pending và label đang xuất. |
| [useExam.ts](../frontend/presentation/hooks/Exam/useExam.ts) | Nối exportMatrix, khóa lặp, xử lý lỗi, reset state; dùng helper tải chung. |
| [ExportService.ts](../frontend/presentation/services/ExportService.ts) | Gọi BFF matrix nhận Blob. |
| [export.schema.ts](../frontend/presentation/schemas/export.schema.ts) | Schema ExportMatrixPayload không rỗng. |
| [route.ts](../frontend/app/api/exporter/matrix/route.ts) | BFF auth/validation/binary/status/error message. |
| [ExportMatrixUsecase.ts](../frontend/usecases/exporter/ExportMatrixUsecase.ts) | Điều phối repository exportMatrix. |
| [IExporterRepository.ts](../frontend/domain/repositories/IExporterRepository.ts) | Bổ sung exportMatrix vào port. |
| [ExporterRepositoryImpl.ts](../frontend/infrastructure/repositories/ExporterRepositoryImpl.ts) | Adapter matrix; dùng HTTP helper chung với exam. |
| [downloadBlob.ts](../frontend/presentation/utils/downloadBlob.ts) | Tải file và dọn anchor/object URL, dùng chung hai thao tác xuất. |
| [development-decisions.md](development-decisions.md) | Ghi phạm vi đã duyệt, snapshot và style làm sau. |
| [api-reference.md](api-reference.md) | Routes, request/payload, binary và status contract mới. |
| [module-map.md](module-map.md) | Call chain và symbols mới. |
| [architecture.md](architecture.md) | Ranh giới quyền, common DTO và pipeline export. |
| [business-flows.md](business-flows.md) | Luồng teacher xuất/tải/error và semantics snapshot. |
| [current-status.md](current-status.md) | Trạng thái triển khai, kiểm tra đạt và giới hạn chứng nhận. |
| [source-catalog.md](source-catalog.md) | Inventory bổ sung/symbols và link danh sách chi tiết. |
| [matrix-export-implementation-report.md](matrix-export-implementation-report.md) | Báo cáo behavior, validation, giới hạn và toàn bộ file thay đổi. |
