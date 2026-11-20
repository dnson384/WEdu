# Bổ sung test draft theo testing rules — 2026-11-20

## Phạm vi snapshot commit trên backend/test/draft

Snapshot gồm7 test class,3 file source draft và3 tài liệu draft (current-status.md, source-catalog.md, báo cáo này). Ba file source là dependency bắt buộc của test theo contract mới: [DraftController.java](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) truyền draftId trước principal userId; [DraftUsecase.java](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java) và [DraftRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java) nhận `(draftId, userId)`. Đây là sửa đổi của người dùng; lúc chuẩn bị commit đã chạy Spotless cho từng file và chỉ giữ formatting của method deleteDraft, phục hồi formatting ngoài scope từ snapshot trước formatter. HTTP API/schema không đổi. Không đưa thay đổi AGENTS.md hoặc tài liệu auth đang dở vào commit draft.

## Kiểm tra lại sau khi người dùng sửa thứ tự deleteDraft — 2026-11-20

Người dùng đã sửa DraftUsecase/DraftRepositoryImpl và sau đó DraftController. Source hiện tại thống nhất `deleteDraft(draftId, userId)` tại interface, service, usecase, repository; controller truyền `(draftId, principal.getUser().getId())`. Quét toàn bộ backend/src/main không còn nơi gọi deleteDraft theo thứ tự cũ. HTTP method/path/body/response và schema không đổi.

Lượt kiểm tra lại chỉ sửa ba file test, giữ nguyên source người dùng vừa sửa:
- [DraftControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/controller/DraftControllerTest.java): đổi stub/verify success và not-found sang `(draftId, userId)`; request path và owner khác nhau để phát hiện đảo ID.
- [DraftUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/usecase/DraftUsecaseTest.java): đổi input/stub/verify trong ba test deleteDraft sang contract mới; giữ số test 001–003.
- [DraftRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImplTest.java): đổi input của năm test deleteDraft; giữ assertion `_id=DRAFT` và `userId=OWNER` để kiểm đúng nghĩa hai tham số. Invalid draft ID nằm ở tham số đầu.

DraftServiceTest đã dùng `(draftId, userId)` nên không cần sửa. Hai tài liệu cập nhật trong lượt này là current-status.md và báo cáo này. Inventory 7 test class/136 cases không đổi. Các phần bên dưới ghi lại lượt bổ sung test ban đầu; nhận định lệch tham số cũ đã được giải quyết bằng thay đổi source của người dùng và test được đồng bộ trong lượt kiểm tra lại.

Validation lượt kiểm tra lại: `mvn -q "-DspotlessFiles=.*Draft.*Test.java" spotless:apply` và `spotless:check` đạt; `mvn -q "-Dtest=Draft*Test#test_deleteDraft_*" test` đạt14/14; `mvn -q "-Dtest=Draft*Test" test` đạt136/136, không failures/errors/skipped. `mvn -q clean test`:739 tests,729 passed,2 failures,1 error,7 skipped;136 draft cases tiếp tục đạt. Ba lỗi ngoài draft không đổi: AiUsageRepositoryImplTest query assertion, ReviewPendingQuestionUsecaseTest invalid-input và BackendApplicationTests thiếu VNPAY_ENABLED. Log mới tại `%TEMP%/wedu-draft-delete-recheck-full-tests.log`; không sửa/disable các test đó. Kiểm links/whitespace/rule naming cũng đạt.

## Kết quả đối chiếu và phạm vi

Trước thay đổi, không có test class riêng cho module `draft` trong `backend/src/test/java`; test exam/exporter có dùng draft làm fixture hoặc dependency nhưng không thay thế test draft. Đã thêm 7 test class, 136 test methods, theo 7 production class tương ứng và cùng package. Không sửa application code, API, DTO, schema, dependency, formatter configuration hoặc thuật toán sinh matrix. Không thêm framework/test frontend vì frontend chưa có test infrastructure chuẩn và repository không cho tự bổ sung.

Đã đọc snapshot/quyết định matrix trong [development-decisions](development-decisions.md), phần draft trong [current-status](current-status.md), [module-map](module-map.md), [business-flows](business-flows.md), inventory draft trong [api-reference](api-reference.md)/[source-catalog](source-catalog.md), tech-stack và source hiện tại. Automatic matrix generation vẫn FROZEN, manual matrix editing vẫn DEFERRED; test giữ thuật toán hiện hành, không tự đặt chính sách mới.

## Toàn bộ file thuộc thay đổi này

| File | Thay đổi và behavior kiểm tra | Cases |
| --- | --- | ---: |
| [DraftControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/controller/DraftControllerTest.java) | 9 endpoint: success body, principal owner truyền xuống usecase, chưa xác thực 401, not-found/dependency HTTP errors; JSON không đọc được, thiếu body, deserialize create/update payload. Dùng MockMvc với SecurityConfig thật, mock JWT/OAuth/usecase ở boundary. | 33 |
| [DraftUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/usecase/DraftUsecaseTest.java) | Tạo nháp với thời gian cố định; CRUD payload, map DTO/list giữ thứ tự, false/empty/not-found; sinh matrix/details, owner propagation và chặn ghi khi read/catalog/generation thất bại; persistence failure. | 32 |
| [DraftServiceTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/usecase/DraftServiceTest.java) | Đọc/map DTO, not-found/read failure, delete delegation theo signature interface `(draftId, userId)`, repository trả false và throw. Repository hiện đã được người dùng đồng bộ theo signature này; unit test vẫn mock boundary, không gọi Mongo thật. | 6 |
| [DraftRepositoryImplTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImplTest.java) | Insert/read, owner predicates khi đọc/xóa, thêm mới/bỏ mục đã tồn tại, pull, no-op, không có draft/chapter, ObjectId không hợp lệ, modifiedCount, bulk matrix/details và array filters, delete count guard, sort/limit recent=5/all không limit, Mongo failure. Mock MongoTemplate/bulk/mapper; assertions trên query/update ở boundary, không gọi Mongo thật. | 39 |
| [DraftUtilTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/usecase/util/DraftUtilTest.java) | hasMatrix/hasMatrixDetail bằng/vượt/thiếu ngưỡng, null counts và dữ liệu rỗng; tỷ lệ loại câu 30/40/30, difficulty 40/30/20/10, phần dư, round-robin nhiều chương/bài; zero/negative/no lessons; details tích outcome/dạng bài, deduplicate, fallback và giữ nguyên bài chưa có matrix. | 14 |
| [DraftDTOMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/dto/mapper/DraftDTOMapperTest.java) | MapStruct thật: dữ liệu lồng nhau, timestamp, null input/optional fields, round-trip và detailDTOToEntity. | 7 |
| [DraftMapperTest.java](../backend/src/test/java/com/wedu/exam_creation/draft/infrastructure/mapper/DraftMapperTest.java) | MapStruct thật: entity/document lồng nhau, null input, round-trip, questionTypes mặc định rỗng khi entity có questionTypes=null. | 5 |
| [current-status.md](current-status.md) | Thêm snapshot test draft, kết quả targeted/full suite và vấn đề ngoài scope; giữ các nội dung đang có. | — |
| [source-catalog.md](source-catalog.md) | Thêm inventory 7 test class theo package và liên kết báo cáo. | — |
| [draft-test-rule-alignment-report.md](draft-test-rule-alignment-report.md) | Báo cáo phạm vi, inventory, đối chiếu rules, validation và giới hạn. | — |

## Testing rules

- Tất cả 136 method dùng `test_<productionMethod>_<NNN>`, bắt đầu 001 và liên tục cho từng production method trong từng class.
- Mọi test có `@DisplayName` tiếng Việt theo `trả về <kết quả> khi <điều kiện>`; audit kiểm đủ 136/136 annotations, tên và sequence.
- Java test tại đúng package, 1–1 production class, JUnit Jupiter/Mockito/AssertJ/MockMvc; không dùng var/raw collection/response type mơ hồ hoặc disabled test.
- Fixture IDs/nội dung cố định; `createDraft` mock static `Instant.now` với instant cố định và đóng mock sau test. Không assert bằng thời gian thực hay UUID random.
- Unit tests mock dependency ngoài unit. Mapper tests chạy implementation MapStruct thật để kiểm contract conversion. Chỉ controller dùng MVC slice vì cần framework routing/JSON/security; không dùng full context trong test draft.
- Happy path cùng các edge/failure liên quan được kiểm ở layer nhỏ nhất: ownership predicate ở repository, owner propagation ở usecase/controller, lỗi dependency và không ghi sau precondition failure. Không tự thêm conflict contract khi draft hiện chưa có revision/conflict workflow.
- Test không gọi MongoDB, S3, AI, Telegram, SMTP, Google, VNPay hoặc exporter thật. Chưa chứng nhận Mongo mapping/persistence integration hoặc end-to-end qua frontend.

## Validation

Từ `backend/`:

1. `.\mvnw.cmd "-DspotlessFiles=src/test/java/com/wedu/exam_creation/draft/**/*.java" spotless:apply`: wrapper Windows lỗi trước khi khởi động Maven (`Cannot index into a null array / Cannot start maven from wrapper`). Không thay wrapper/config; dùng Maven đã cài với cùng plugin repository.
2. `mvn -q "-DspotlessFiles=.*Draft.*Test.java" spotless:apply`: đạt, chỉ format 7 test draft; diff không có application code/formatting ngoài scope. Lần đầu chọn glob path cho spotlessFiles bị lỗi regex, các lựa chọn path với slash không match Windows; đổi selector regex theo tên file, log xác nhận format đủ 7 file. Một số lệnh sandbox bị `LocalRepositoryNotAccessibleException`, đã chạy lại ngoài sandbox.
3. `mvn -q "-Dtest=Draft*Test" test`: **136 passed**, 0 failures/errors/skipped. Lượt đầu có 1 failure và 2 errors do test mới kỳ vọng sai default questionTypes và cast cấu trúc Spring Data $each; đã đối chiếu generated mapper/source Spring Data cục bộ, sửa assertion test chính xác và format trước chạy lại. Không sửa production để làm test pass.
4. `mvn -q clean test`: **739 tests, 729 passed, 2 failures, 1 error, 7 skipped**. Toàn bộ 136 draft tests tiếp tục đạt trong clean full run.
5. `mvn -q "-DspotlessFiles=.*Draft.*Test.java" spotless:check`: đạt. Audit tên method/DisplayName/sequence đạt136/136; whitespace/links/inventory đạt,0 link hỏng trong báo cáo. Không có production diff hoặc formatting ngoài scope. Snapshot cuối tại [current-status](current-status.md).

Ba lỗi full suite trùng kết quả baseline của phiên chuẩn hóa test auth trước đó, không nằm trong source/test draft và không được sửa/disable:
- `AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit`: assertion kỳ vọng _id/revision ở root trong khi query chứa $and.
- `ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite`: kỳ vọng BadRequestException nhưng nhận NullPointerException tại requireCurrent.
- `BackendApplicationTests.contextLoads`: thiếu placeholder `VNPAY_ENABLED`.

7 Mongo integration tests skip do không có `WEDU_PAYMENT_TEST_MONGO_URI`. Không bật real integration service để làm suite pass. Lượt này không export/rerun baseline HEAD riêng vì source/test ngoài draft không thay đổi so với trước task và các lỗi trên đã có baseline xác nhận trong báo cáo auth; không coi full suite là đạt.

Log full suite tại `%TEMP%/wedu-draft-full-tests.log`; kết quả từng class trong `backend/target/surefire-reports` (generated artifacts, không commit).

## Phát hiện giữ ngoài scope

1. **Lệch thứ tự xóa draft ở snapshot ban đầu — hiện đã được người dùng xử lý.** Interface/service vốn nhận `(draftId, userId)` nhưng repository/usecase ban đầu nhận `(userId, draftId)`. Người dùng đã sửa repository, usecase và controller để thống nhất `(draftId, userId)`. Test đã được đồng bộ và kiểm query `_id=DRAFT`, `userId=OWNER`; xem kết quả kiểm tra lại ở đầu báo cáo. Không còn coi lệch thứ tự là vấn đề đang mở; chưa chạy Mongo integration thật.
2. **Owner check trước bulk write.** Repository updateMatrix/updateMatrixDetails chỉ lọc _id và array filters; usecase đọc draft theo owner trước khi sinh/ghi. Không mô tả mọi write đều có owner predicate độc lập.
3. **Thiếu validation payload nghiệp vụ.** CreateDraftDTO/UpdateDraftDTO và controller hiện không có Bean Validation. Test HTTP chỉ xác nhận 400 cho JSON không đọc được/missing body, không tự giả định questionsCount âm/null, questionTypes rỗng hoặc add=null phải trả 400. Không thêm validation qua task test.
4. Query/update tests ở MongoTemplate boundary không xác nhận mapping nested `id/_id` hay hành vi trên Mongo thật. Automatic matrix generation không được sửa, không mở manual editing hoặc stock-aware allocation.
