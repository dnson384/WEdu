# Đối chiếu và bổ sung test admin — 2026-11-20

## Phạm vi và kết quả

Đã đọc snapshot/quyết định ADMIN trong development-decisions, context quản lý tài khoản trong current-status/module-map/business-flows, inventory/API trong source-catalog/api-reference và stack/test instructions trong tech-stack/AGENTS.md. Trace AdminController, AdminUsecase, UserService, các method UserUsecase liên quan, UserCommonDTOMapper, CommonUserResponseDTO/CommonUserResponseAllDTO và SecurityConfig/GlobalExceptionHandler hiện tại. Người dùng đã confirm phương án thêm test cho hai production classes thuộc package admin.

Trước thay đổi không có test riêng cho AdminController hoặc AdminUsecase. AdminTransaction tests thuộc payment và không gọi hai classes này. Không có baseline admin tests để chạy; không dùng kết quả payment hoặc frontend harness làm chứng nhận coverage module admin.

Sau thay đổi có2 test classes,83 methods/105 invocations: AdminControllerTest44 methods/56 cases và AdminUsecaseTest39 methods/49 cases. Không thay production/config/API/DTO/schema/dependency/quyền hoặc nghiệp vụ; không thêm frontend test framework. Không sửa module user, AI hoặc payment.

## Toàn bộ files thuộc thay đổi

| File | Thay đổi và behavior kiểm tra | Cases |
| --- | --- | ---: |
| [AdminControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/admin/controller/AdminControllerTest.java) | Mới. MockMvc/SecurityConfig thật, mock usecase và JWT/OAuth dependencies. Kiểm cả6 endpoints:200/body/principal,401 không gọi usecase,403 với TEACHER/MODERATOR/USER,404 và500 có thông báo. Missing keyword/role400; keyword Unicode có khoảng trắng/ký tự đặc biệt truyền nguyên vẹn; role không hợp lệ400, thao tác bản thân400, ADMIN khác403, list/search rỗng200 và xóa false200. DTO response không có hashedPassword/loginMethod. |56|
| [AdminUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/admin/usecase/AdminUsecaseTest.java) | Mới. Mock UserService và UserCommonDTOMapper. List/search giữ thứ tự/keyword, empty/not-found/validation/dependency failures. Đổi role chấp nhận3 ROLE_* hiện tại, từ chối null/empty/blank/unknown/case/space sai; chặn bản thân/ADMIN khác; not-found không ghi. Lock/unlock success và trạng thái đã đúng không ghi lại, self/not-found/admin guards, lỗi đọc/ghi/mapping. Delete trả đúng true/false từ service, self/not-found/admin guards và lỗi đọc/xóa. |49|
| [current-status.md](current-status.md) | Thêm snapshot admin tests, behavior hiện tại và validation; giữ nội dung đang có. |—|
| [source-catalog.md](source-catalog.md) | Thêm inventory2 test classes và link báo cáo; giữ nội dung đang có. |—|
| [admin-test-rule-alignment-report.md](admin-test-rule-alignment-report.md) | Báo cáo phạm vi, files, rules, validation và giới hạn. |—|

## Đối chiếu testing rules

- Hai test classes bằng Java trong src/test/java, tên *Test, cùng package và production class tương ứng1–1.
- Sau lượt rà soát lại được người dùng phê duyệt, 83/83 methods đúng test_<productionMethod>_<NNN>; số tăng xuyên suốt mỗi test class, không reset khi chuyển production method: controller001–044, usecase001–039. Controller dùng đúng tên unLockUser của source; usecase dùng unlockUser. Không đổi tên production để đồng nhất test.
- Mỗi test có DisplayName tiếng Việt theo mẫu trả về <kết quả> khi <điều kiện>. Private isValidateRole được kiểm qua behavior updateUserRole, không gọi reflection hoặc test private helper trực tiếp.
- 105 invocations gồm83 methods, cộng12 invocations phân quyền (mỗi endpoint có3 role không phải ADMIN),2 invocations role hợp lệ,7 invocations role không hợp lệ và1 invocation delete true/false.
- JUnit Jupiter/Mockito/AssertJ; controller dùng MVC slice vì cần method security, routing, query binding, serialization và exception handling. Usecase test không dùng Spring context; mock dependency ngoài unit.
- Fixtures cố định, DTO timestamps cố định; không phụ thuộc thời gian thực, random, thứ tự test hoặc network. Không var, raw collections, sleep hoặc disabled tests.
- Guards kiểm observable exception và không gọi đọc/ghi/map không hợp lệ; lock/unlock đã đúng trạng thái kiểm không ghi lại. Test mapping failure không khẳng định persistence rollback khi lỗi xảy ra sau service write.
- Không gọi MongoDB, S3, AI, Telegram, email, Google hoặc VNPay thật. Không dùng shared UserService/UserCommonDTOMapper thật để kiểm lại logic module user trong test admin.
- Không thêm403/owner predicate vào usecase: quyền ADMIN nằm ở controller @PreAuthorize và SecurityConfig. Test controller dùng security thật để kiểm boundary; usecase kiểm self/target-admin guards theo source.

## Behavior và giới hạn giữ nguyên

AdminUsecase.updateUserRole chấp nhận ROLE_ADMIN, ROLE_MODERATOR và ROLE_TEACHER cho tài khoản đích hiện không phải ADMIN; UI/BFF chỉ cho chọn TEACHER/MODERATOR. Test ghi nhận backend behavior, không coi giới hạn UI là validation backend đã có và không tự thay đổi chính sách cấp ADMIN.

getAllUsers/findUsers trực tiếp chuyển kết quả/lỗi từ UserService. getAllUsers dịch vụ thật có thể ném NotFound khi rỗng, nhưng nếu dependency trả List rỗng thì admin vẫn chuyển ra200; cả kết quả rỗng và lỗi NotFound được kiểm đúng boundary. deleteUser trả boolean từ service, không chuyển false thành NotFound.

Không tự thêm contract cho legacy isActive=null hoặc target role=null bằng cách coi NullPointerException của implementation là business requirement. Không thêm conflict/concurrency, session revocation hoặc user cascade cleanup trong task test admin. Shared user mapper/repository, AI management và transaction management cần scope riêng nếu muốn audit test.

## Validation thực tế

Từ backend:

1. Lượt gọi Spotless đầu dùng selector chứa dấu | không tương thích lệnh .cmd trên Windows; Maven chưa khởi động. Đã sửa selector thành `.*admin.*Test.java`, không thay formatter/config.
2. `.\mvnw.cmd "-DspotlessFiles=.*admin.*Test.java" spotless:apply`: wrapper lỗi Cannot index into a null array / Cannot start maven from wrapper. Không sửa wrapper; dùng Maven cài sẵn với cùng plugin/config repository.
3. `mvn -q "-DspotlessFiles=.*admin.*Test.java" spotless:apply`: đạt, scoped đúng2 files admin. Sau điều chỉnh fixture controller, format lại trước validation.
4. `mvn -q "-Dtest=AdminControllerTest,AdminUsecaseTest" test`: **105 tests passed,0 failures/errors/skipped** ngay lượt targeted đầu. Log %TEMP%/wedu-admin-targeted-tests.log.
5. `mvn -q test`: **997 tests,987 passed,2 failures,1 error,7 skipped**. Cả105 admin cases tiếp tục đạt. Log %TEMP%/wedu-admin-full-tests.log.
6. `mvn -q "-DspotlessFiles=.*admin.*Test.java" spotless:check`: đạt. Ghi nhận audit sequence trước đây không chính xác: số thứ tự đã reset theo production method và trùng trong cùng class. Lượt rà soát lại bên dưới đã sửa và kiểm tra theo rule hiện tại. Không có thay đổi production/config hoặc formatting ngoài scope.

### Rà soát lại và chuẩn hóa số thứ tự — 2026-11-20

Baseline working tree đã có hai test classes trên; lượt này chỉ đổi hậu tố số test sau khi người dùng phê duyệt, giữ nguyên assertions, fixtures, DisplayName và 105 invocations. Không thêm test trùng behavior. Files sửa trong lượt này: AdminControllerTest.java (001–044), AdminUsecaseTest.java (001–039), báo cáo này (sửa kết luận audit), current-status.md (ghi kết quả rà soát) và source-catalog.md (đồng bộ quy tắc số thứ tự).

- `.\mvnw.cmd "-DspotlessFiles=.*admin.*Test.java" spotless:apply`: không khởi động được wrapper, lỗi Cannot index into a null array / Cannot start maven from wrapper.
- `mvn -q "-DspotlessFiles=.*admin.*Test.java" spotless:apply` và `spotless:check`: đạt với Maven cài sẵn, cùng cấu hình repository, scoped hai test files.
- `mvn -q "-Dtest=AdminControllerTest,AdminUsecaseTest" test`: 105/105 đạt (controller56, usecase49), không failures/errors/skipped. Log `%TEMP%/wedu-admin-renumber-tests.log`.
- Audit trực tiếp83 method declarations: số tăng từ001, không trùng/reset trong từng class; đủ83 DisplayName theo mẫu tiếng Việt. `git diff --check`: đạt.
- Không chạy lại full suite vì chỉ đổi tên test; kết quả full suite phía trên thuộc lượt trước, không phải chứng nhận mới. Không đổi production/API/schema/config. Bộ lọc chạy riêng theo tên test cũ cần dùng hậu tố mới.

Ba lỗi ngoài admin trùng baseline đã ghi nhận trong các báo cáo auth/draft/chapter/exam:

- AiUsageRepositoryImplTest.updatesOnlyExpectedRevisionAndAllowsClearingOneLimit: assertion kỳ vọng _id/revision ở root, trong khi query dùng $and.
- ReviewPendingQuestionUsecaseTest.invalidInputDoesNotReadOrWrite: kỳ vọng BadRequestException nhưng gặp NullPointerException tại requireCurrent.
- BackendApplicationTests.contextLoads: thiếu placeholder VNPAY_ENABLED.

7 Mongo integration cases skipped theo điều kiện môi trường có sẵn. Full suite không được coi là đạt. Không sửa/disable test ngoài scope. Chưa chứng nhận persistence, side effects dịch vụ thật, browser/end-to-end hoặc deployment. Giữ thay đổi AGENTS/auth/draft/exam/module-map/docs và outputs đang có trước task; không stage/commit.
