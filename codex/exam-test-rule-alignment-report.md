# Đối chiếu và bổ sung test exam — 2026-11-20

## Phạm vi và kết quả

Đối chiếu source hiện tại với testing rules của AGENTS.md sau khi đọc snapshot/quyết định exam, matrix và AI trong development-decisions; context liên quan trong current-status, module-map, api-reference, source-catalog, architecture, business-flows, database-schema và tech-stack. Người dùng đã confirm phương án chuẩn hóa và bổ sung test cho 7 production classes của exam.

Trước thay đổi chỉ có ExamMatrixUsecaseTest với4 methods/cases cho getExamMatrixById. Baseline4/4 đạt nhưng sai tên class theo quan hệ1–1, cả4 method không theo test_<productionMethod>_<NNN>, không có DisplayName và có3 biến var. Test exporter mock ExamService không thay thế kiểm thử exam.

Sau thay đổi có7 test classes,87 methods/94 invocations:20 controller,32 usecase,5 service,16 repository,9 response mapper,4 draft snapshot mapper và8 document mapper. Giữ đầy đủ4 cases cũ tại test_getExamMatrixById_001–004; thêm83 methods. Không xóa hoặc disable test để làm suite đạt. Không sửa production/config/API/schema/dependency/quyền hoặc nghiệp vụ; không thêm framework frontend.

## Toàn bộ files thuộc thay đổi

| File | Thay đổi và behavior kiểm tra | Cases |
| --- | --- | ---: |
| [ExamControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/controller/ExamControllerTest.java) | Mới. MockMvc với SecurityConfig thật, mock JWT/OAuth/usecase/service. Cả5 endpoints kiểm200/JSON,401 không gọi nghiệp vụ,404 và500. Kiểm đúng userId/accountType từ principal và routing recent/user-exams. |20|
| [ExamUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/usecase/ExamUsecaseTest.java) | Chuyển/chuẩn hóa từ ExamMatrixUsecaseTest; thêm behavior sinh đề FREE/PLUS, lọc selectedCount >0, context theo chương, snapshot và ID câu hỏi/errors/timestamps, nhiều chương/bài, lỗi nháp/chapter/question/save và null generation. Kiểm hydrate IDs, list/recent thứ tự, empty/not-found, xóa theo owner và không xóa câu hỏi; ma trận đủ trường, thứ tự, zero/null/rỗng/thiếu snapshot, lỗi repository. |32|
| ExamMatrixUsecaseTest.java (đường dẫn cũ trong exam/usecase) | Xóa đường dẫn cũ khi chuyển sang ExamUsecaseTest; cả4 behavior được giữ. Không còn class thứ hai cùng kiểm ExamUsecase. |—|
| [ExamServiceTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/usecase/ExamServiceTest.java) | Mới. getExamById/getExamMatrixById truyền owner/examId, trả đúng DTO, truyền not-found và missing snapshot. |5|
| [ExamRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/infrastructure/repository/ExamRepositoryImplTest.java) | Mới. Insert/mapping/failure, truy vấn ID kết hợp owner, all-user query/not-found, recent owner+updatedAt DESC+limit5 và empty/error, xóa0/1/>1 và lỗi Mongo. MongoTemplate/mapper được mock. |16|
| [ExamResponseDTOMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/dto/mapper/ExamResponseDTOMapperTest.java) | Mới. Metadata và đếm ID qua mọi nhóm; empty chapters/lessons/question IDs. Chi tiết lọc đúng chương/bài, nhóm theo type+difficulty, loại câu ngoài phạm vi, không gọi S3 khi không có ảnh; giữ placeholders/math, ký ảnh body/options bằng S3 mock, giữ dữ liệu nguồn cả khi ký ảnh lỗi. |9|
| [ExamCommonDraftDTOMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/dto/mapper/ExamCommonDraftDTOMapperTest.java) | Mới. MapStruct thật giữ snapshot nested/thứ tự/zero, snapshot độc lập khi sửa nháp, null/rỗng/thiếu fields. |4|
| [ExamMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/exam/infrastructure/mapper/ExamMapperTest.java) | Mới. MapStruct thật entity ↔ document giữ chương/bài/matrix/details/question IDs/timestamps, không dùng chung list và snapshot, null/rỗng/optional fields. |8|
| [current-status.md](current-status.md) | Thêm snapshot exam tests và kết quả validation, giữ nội dung đang có. |—|
| [source-catalog.md](source-catalog.md) | Thêm inventory7 test class, rename và link báo cáo, giữ nội dung đang có. |—|
| [matrix-export-implementation-report.md](matrix-export-implementation-report.md) | Sửa source link từ class cũ sang ExamUsecaseTest; giữ lệnh/kết quả lịch sử của lượt matrix export. |—|
| [exam-test-rule-alignment-report.md](exam-test-rule-alignment-report.md) | Báo cáo phạm vi, file, testing rules, validation và giới hạn. |—|

## Đối chiếu rules và giới hạn

- Toàn bộ test Java trong src/test/java, class *Test cùng package và tương ứng production1–1; không test Lombok getter/setter chỉ để tăng coverage.
- 87/87 methods đúng test_<productionMethod>_<NNN>, đánh số001 liên tục, không trùng trong từng method/class; đủ DisplayName tiếng Việt theo mẫu trả về <kết quả> khi <điều kiện>.4 methods cũ chưa có số nên được đặt001–004, các case mới tiếp nối005.
- 94 invocations gồm87 methods, cộng1 invocation accountType FREE/PLUS,5 invocations snapshot thiếu và1 invocation lỗi ký ảnh body/option. Không var, raw collections, test disabled hoặc sleep.
- Fixtures và thời điểm assert cố định; mock Instant.now bằng try-with-resources ở assertion timestamp. Không assert thứ tự nhóm từ HashMap; chỉ assert membership, metadata và thứ tự có ý nghĩa trong snapshot/repository result. Document fixtures đặt ID cụ thể.
- Unit tests mock dependency ngoài unit; controller dùng MVC slice để kiểm routing/serialization/security. MapStruct tests dùng generated mapper thật. Không gọi Mongo/S3/AI/Telegram/SMTP/VNPay/exporter thật.
- Quyền exam hiện là authenticated, repository lọc userId. Test không tự thêm teacher-only,403 theo role hoặc conflict/validation mà source chưa có. API exam không nhận write DTO; lỗi thiếu snapshot là422 tại usecase.
- Không mở lại AI fallback/validation/retry hoặc matrix algorithm FROZEN/DEFERRED. Chỉ kiểm sự điều phối QuestionService tại boundary. Không khẳng định Mongo transaction rollback từ mock repository hoặc assertion exception khi deletedCount >1.

## Validation thực tế

Từ backend:

1. Baseline `mvn -q "-Dtest=Exam*Test" test`:4/4 đạt; log %TEMP%/wedu-exam-baseline-tests.log.
2. `.\mvnw.cmd "-DspotlessFiles=.*exam/.*Test.java" spotless:apply`: wrapper lỗi trước khi khởi động Maven (Cannot index into a null array / Cannot start maven from wrapper). Không sửa wrapper hoặc thay formatter.
3. `mvn -q "-DspotlessFiles=.*Exam.*Test.java" spotless:apply`: đạt với Maven cài sẵn, dùng đúng plugin/config repository. Chỉ các test exam thay đổi; production/config và code ngoài scope không đổi.
4. Lượt targeted đầu92 cases có1 error trong assertion mới: JSON matcher không hỗ trợ scalar boolean true. Đã sửa content.string, bổ sung2 cases nhiều chương/bài và format lại; không thay production.
5. `mvn -q clean "-Dtest=Exam*Test" test`: **94 tests passed,0 failures/errors/skipped**. Clean build loại bỏ artifacts cũ sau rename. Log %TEMP%/wedu-exam-targeted-tests.log.
6. `mvn -q test`: **892 tests,882 passed,2 failures,1 error,7 skipped**. Tất cả94 exam cases tiếp tục đạt. Log %TEMP%/wedu-exam-full-tests.log.
7. `mvn -q "-DspotlessFiles=.*Exam.*Test.java" spotless:check`: đạt. Audit87 methods về tên/DisplayName/sequence/package/production method đạt; kiểm whitespace, link và inventory đạt.

Ba lỗi ngoài exam trùng các lỗi đã ghi nhận trong báo cáo auth/draft/chapter:

- AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit: assertion kỳ vọng _id/revision ở root trong khi query dùng $and.
- ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite: kỳ vọng BadRequestException nhưng gặp NullPointerException tại requireCurrent.
- BackendApplicationTests.contextLoads: thiếu placeholder VNPAY_ENABLED.

7 cases Mongo integration skipped theo điều kiện môi trường có sẵn. Full suite không được coi là đạt. Không sửa/disable test ngoài scope. Chưa chứng nhận Mongo persistence/transaction integration, browser/end-to-end hoặc deploy. Giữ nguyên thay đổi AGENTS/auth/draft/module-map/docs và outputs có sẵn trước task; không stage/commit.
