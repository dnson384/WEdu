# Đối chiếu và bổ sung test chapter — 2026-11-20

## Phạm vi và kết quả

Trước thay đổi không có test class riêng cho backend chapter. Test draft/moderator mock hoặc dùng chapter làm fixture không thay thế kiểm thử module. Sau khi người dùng duyệt proposal, đã thêm 6 test class theo production class tương ứng, cùng package, tổng 58 methods/63 test cases. Không sửa production code, config, API, DTO, schema, dependency hoặc business behavior. Các entity/document chỉ chứa dữ liệu và Lombok không được thêm test getter/setter chỉ để tăng coverage.

Đã đọc snapshot và quyết định chapter/importer trong [development-decisions](development-decisions.md), context chapter của [current-status](current-status.md), [module-map](module-map.md), [api-reference](api-reference.md), [source-catalog](source-catalog.md), [business-flows](business-flows.md), [database-schema](database-schema.md), [tech-stack](tech-stack.md) và trace source hiện tại. Merge chapter name-only là ACCEPTED_CURRENT_SCOPE cho môn Vật lý; importer/Pandoc FROZEN. Test giữ nguyên các quyết định này.

## Toàn bộ files thuộc thay đổi này

| File | Thay đổi và behavior kiểm tra | Cases |
| --- | --- | ---: |
| [ChapterControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/controller/ChapterControllerTest.java) | MockMvc với SecurityConfig thật, mock JWT/OAuth/usecase: GET trả JSON chương/bài/bankStats; rỗng; TEACHER/MODERATOR/ADMIN đã xác thực được đọc; anonymous401 không gọi usecase; NotFoundException404, InternalServerException500; POST405. | 8 |
| [ChapterUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/usecase/ChapterUsecaseTest.java) | getAll map đúng DTO/thứ tự, danh sách rỗng không gọi mapper, repository lỗi. | 3 |
| [ChapterServiceTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/usecase/ChapterServiceTest.java) | insert truyền subject/name/lesson/timestamps và nhận saved IDs; mapping lỗi không ghi, persistence lỗi. getAll map/empty/read failure; getByIds giữ dữ liệu và thứ tự repository, empty/not-found/read failure. | 10 |
| [ChapterRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImplTest.java) | Tạo chương và ID bài, query name-only; thêm bài giữ bài cũ/createAt; merge stat trùng cộng count nhiều lần, tách riêng khi khác từng metadata (4 invocations); bankStats mới null/rỗng không ghi; Mongo lookup/save lỗi. getAll thứ tự/empty/error, getById success/not-found/error, getByIds query $in, kết quả thiếu một phần và thứ tự Mongo, không có ID, danh sách ID rỗng và lỗi. | 22 |
| [ChapterDTOMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/dto/mapper/ChapterDTOMapperTest.java) | MapStruct thật cho entityToDTO/newLessonDTOToEntity/lessonEntityToDTO: dữ liệu lồng nhau đầy đủ, input null, list rỗng, optional fields/list null. | 12 |
| [ChapterMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/chapter/infrastructure/mapper/ChapterMapperTest.java) | MapStruct thật cho entity ↔ document: đủ nested bankStats/timestamps, không dùng chung nested list, input null, list rỗng và optional fields null. | 8 |
| [current-status.md](current-status.md) | Thêm snapshot chapter tests và kết quả validation, giữ nội dung đang có. | — |
| [source-catalog.md](source-catalog.md) | Thêm inventory 6 test class và liên kết báo cáo, giữ nội dung đang có. | — |
| [chapter-test-rule-alignment-report.md](chapter-test-rule-alignment-report.md) | Báo cáo phạm vi, từng file, rules, kết quả và giới hạn. | — |

## Đối chiếu testing rules

- Java tests ở src/test/java, tên class *Test, đúng package và quan hệ 1–1; JUnit Jupiter/Mockito/AssertJ/MockMvc theo stack hiện có. Controller dùng MVC slice để kiểm routing/JSON/security; các unit khác không dùng Spring context.
- Đủ58/58 method theo `test_<productionMethod>_<NNN>`, sequence từ001 liên tục và không trùng theo từng production method. Mọi test có DisplayName tiếng Việt `trả về <kết quả> khi <điều kiện>`.
- Tổng63 invocations gồm58 methods, cộng2 invocations role của controller và3 invocations metadata của repository. Không có disabled tests, var, raw collections hoặc response type mơ hồ.
- Fixtures cố định, mock Instant.now trong các assertions timestamp; static mock được đóng bằng try-with-resources. ObjectId sinh bởi source được kiểm tra tính hợp lệ và quan hệ với saved/returned ID, không yêu cầu giá trị ngẫu nhiên cụ thể hay thứ tự chạy.
- Mock MongoTemplate và dependency ngoài unit; không gọi Mongo, S3, AI, Google, SMTP, VNPay hoặc exporter thật trong chapter tests. Mapper chạy generated implementation thật để kiểm conversion contract.
- Happy paths, empty/null ở mapping và bankStats, not-found tại repository, lỗi đọc/ghi/mapping và quyền xác thực tại MVC được kiểm ở boundary phù hợp. API GET hiện không nhận body/parameter và không có role/owner predicate riêng, conflict/revision hay validation write API; không tự đặt các business rules đó trong test.
- Không sửa production chỉ để test pass. Không đổi name-only merge, normalize metadata, thêm transaction hoặc sửa importer. Giới hạn input như thiếu lesson hay null legacy metadata chưa có validation business contract; không biến exception ngẫu nhiên của implementation thành requirement mới.

## Validation

Từ backend:

1. `.\mvnw.cmd "-DspotlessFiles=.*Chapter.*Test.java" spotless:apply`: wrapper lỗi trước khi khởi động Maven (`Cannot index into a null array / Cannot start maven from wrapper`). Không sửa wrapper/config, dùng Maven đã cài với cùng plugin repository.
2. `mvn -q "-DspotlessFiles=.*Chapter.*Test.java" spotless:apply`: đạt; selector chỉ tác động 6 test class chapter. Kiểm diff không có thay đổi production hoặc formatting ngoài scope.
3. `mvn -q "-Dtest=Chapter*Test" test`: bản cuối **63 tests passed,0 failures/errors/skipped**. Lượt đầu58/58 đạt; bổ sung5 cases null và tăng assertion thứ tự, format rồi chạy lại63/63 đạt. Lần chạy trong sandbox bị `LocalRepositoryNotAccessibleException` tại C:\.m2\repository, đã chạy lại ngoài sandbox. Log cuối tại `%TEMP%/wedu-chapter-targeted-tests.log`.
4. `mvn -q test`: **802 tests,792 passed,2 failures,1 error,7 skipped**. Cả63 chapter cases tiếp tục đạt. Log `%TEMP%/wedu-chapter-full-tests.log`.
5. `mvn -q "-DspotlessFiles=.*Chapter.*Test.java" spotless:check`: đạt. Audit58 method names/DisplayNames/sequence đạt. Kiểm whitespace/links và inventory đạt.

Ba lỗi ngoài chapter trùng baseline auth/draft đã ghi nhận, không bị sửa/disable:

- AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit: query assertion kỳ vọng _id/revision ở root trong khi query dùng $and.
- ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite: kỳ vọng BadRequestException nhưng nhận NullPointerException tại requireCurrent.
- BackendApplicationTests.contextLoads: thiếu placeholder VNPAY_ENABLED.

7 cases Mongo integration skipped theo điều kiện môi trường có sẵn. Full suite không được coi là đạt; test chapter không chứng nhận Mongo persistence integration, deploy hoặc end-to-end frontend. Không thêm framework frontend. Các thay đổi AGENTS/auth/draft/module-map và docs đang có trước task được giữ nguyên; không stage/commit.
