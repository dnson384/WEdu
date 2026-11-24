---
name: generate-test-report
description: Generate Markdown test reports from existing Java JUnit test files.
---

## Output Format — Markdown Test Report

### 1. File output

- Định dạng: `.md`
- Encoding: UTF-8
- Đường dẫn: `E:\TLU\KLTN\Docs\Test`
- Tên file: `<module>_TestReport.md`

Ví dụ:

`E:\TLU\KLTN\Docs\Test\auth_TestReport.md`

Mỗi module được xuất thành một file Markdown riêng biệt.

### 2. Data source and test execution

- Đọc các file `*Test.java` hiện có trong module được yêu cầu.
- Phân tích `@Test`, `@ParameterizedTest`, `@DisplayName`, mock, assertion và exception.
- Đọc source code tương ứng khi cần xác định hành vi.
- Không tự tạo test case.
- Không sửa production code hoặc test code chỉ để sinh báo cáo.

#### Test execution

- Khi người dùng yêu cầu generate test report, mặc định phải thực thi các test thuộc module được yêu cầu.
- Sử dụng Maven Wrapper của backend để chạy JUnit.
- Chỉ chạy các test class thuộc phạm vi module.
- Không tự chạy toàn bộ backend nếu người dùng chỉ yêu cầu báo cáo một module.
- Không sửa hoặc vô hiệu hóa test để làm kết quả Passed.
- Không bỏ qua test thất bại.
- Nếu quá trình chạy test gặp lỗi, phải ghi nhận nguyên nhân.
- Không sử dụng kết quả test cũ nếu không xác minh được kết quả đó tương ứng với source code hiện tại.

#### Test result collection

Sau khi chạy test:

1. Đọc các file `target/surefire-reports/TEST-*.xml`.
2. Xác định tên test class và test method.
3. Xác định trạng thái thực thi.
4. Thu thập failure message, exception và assertion error nếu có.
5. Thu thập timestamp thực thi nếu được cung cấp.
6. Đối chiếu kết quả với từng test case trong source code.
7. Không ghép kết quả chỉ dựa trên tên test method khi có nhiều test class sử dụng cùng tên.

Sử dụng khóa định danh:

`<FullyQualifiedTestClassName>#<TestMethodName>`

Đối với parameterized test, phải phân biệt từng invocation khi có đủ dữ liệu.

Không tự suy diễn kết quả khi không thể đối chiếu chính xác.

### 3. Document structure

Tài liệu gồm:

`# <module> Test Report`

Tiếp theo là bảng Markdown có đúng 8 cột theo thứ tự:

| ID | Description | Pre-condition | Step | Expected output | Actual Output | Result | Test date |
|---|---|---|---|---|---|---|---|

Mỗi test case tương ứng một dòng trong bảng.

### 4. Field rules

**ID**
- Lấy trực tiếp từ tên test method theo định dạng `test_<method>_<NNN>`.
- Ví dụ: `test_login_001`, `test_logout_004`.
- Giữ nguyên tên test method và số thứ tự đã khai báo trong source code.
- Không tự đánh lại ID khi xuất tài liệu.
- Mỗi test class có dãy số thứ tự độc lập, bắt đầu từ `001`.
- Số thứ tự `NNN` tăng liên tục trong phạm vi toàn bộ test class, không reset khi chuyển sang production method khác.
- ID chỉ bắt buộc duy nhất trong phạm vi một test class, không bắt buộc duy nhất giữa các test class.
- Hai test case thuộc hai test class khác nhau được phép có cùng ID.
- Ví dụ: `AdminControllerTest.test_getAllUsers_001` và `AdminUsecaseTest.test_getAllUsers_001` là hai test case độc lập, hợp lệ.
- Trong báo cáo Markdown, cột `ID` vẫn giữ nguyên `test_getAllUsers_001` cho cả hai test case.
- Không được xóa, gộp, đổi tên hoặc đánh lại số test case chỉ vì ID giống nhau giữa các test class.
- Để xác định duy nhất một test case khi phân tích và validation, sử dụng khóa nội bộ `<FullyQualifiedTestClassName>#<TestMethodName>`.
- Không sử dụng khóa nội bộ thay thế cho giá trị hiển thị trong cột `ID`.

**Description**
- Ưu tiên nội dung `@DisplayName`.
- Nếu không có, diễn giải dựa trên logic test.

**Pre-condition**
- Mô tả trạng thái dữ liệu và điều kiện cần thiết trước khi kiểm thử.
- Bao gồm các mock/stub liên quan.

**Step**
- Mô tả các bước kiểm thử theo thứ tự thực tế.
- Sử dụng `<br>` để phân tách các bước trong cùng ô Markdown.
- Không thêm bước không tồn tại trong test.

**Expected output**
- Xác định từ assertion, exception và các điều kiện xác minh.
- Mô tả kết quả mong đợi bằng tiếng Việt.

**Actual Output**

- Bắt buộc lấy trạng thái kiểm thử từ kết quả thực thi JUnit/Surefire.
- Nếu `Result = Passed`, nội dung `Actual Output` phải giống hoàn toàn nội dung `Expected output`.
- Khi `Passed`, sao chép nguyên văn `Expected output` sang `Actual Output`, không diễn giải lại, không rút gọn và không thay đổi nội dung.
- Chỉ áp dụng quy tắc sao chép khi test case đã được xác nhận `Passed` bằng kết quả thực thi hợp lệ.
- Nếu `Result = Failed`, ghi kết quả thực tế từ assertion failure, exception hoặc thông báo lỗi.
- Nếu `Result = Blocked`, ghi nguyên nhân khiến test không thể hoàn thành.
- Nếu `Result = Pending`, để trống `Actual Output`.
- Không tự suy diễn kết quả thực thi.
- Không sao chép `Expected output` sang `Actual Output` nếu test chưa được xác nhận `Passed`.

**Result**

Chỉ sử dụng 4 trạng thái:

- `Passed`: Test đã chạy thành công, toàn bộ assertion và verification đều đạt.
- `Failed`: Test đã chạy nhưng assertion thất bại hoặc phát sinh lỗi không mong đợi.
- `Blocked`: Test không thể hoàn thành do trở ngại đã được xác nhận.
- `Pending`: Test chưa chạy hoặc chưa có kết quả đáng tin cậy.

**Consistency Rules**

1. Nếu `Result = Passed` thì `Actual Output == Expected output`.
2. Nếu `Result = Failed` thì `Actual Output` phải phản ánh lỗi thực tế.
3. Nếu `Result = Blocked` thì `Actual Output` phải mô tả nguyên nhân cản trở.
4. Nếu `Result = Pending` thì `Actual Output` phải để trống.
5. Không được đánh dấu `Passed` nếu thiếu bằng chứng thực thi tương ứng.
6. Nếu test đã Passed nhưng Expected output chưa được xác định rõ, phải phân tích assertion để hoàn thiện Expected output trước khi sao chép.
7. Không được tự thay đổi trạng thái test để bảo đảm hai cột giống nhau.

**Test date**

- Định dạng: `dd/MM/yyyy`.
- Lấy ngày thực thi từ timestamp của kết quả JUnit/Surefire nếu có.
- Nếu test vừa được thực thi trong phiên hiện tại và xác định được thời điểm chạy, sử dụng ngày thực thi đó.
- Không sử dụng ngày tạo file Markdown làm ngày kiểm thử.
- Nếu không xác định được ngày thực thi, để trống.
- Không tự điền ngày hiện tại cho test Pending chưa chạy.

### 5. Markdown styling

- Sử dụng Markdown chuẩn.
- Tiêu đề tài liệu sử dụng H1.
- Không sử dụng HTML table.
- Không sử dụng CSS hoặc màu sắc trang trí.
- Không sử dụng emoji.
- Không thêm cột ngoài 8 cột đã quy định.
- Escape ký tự `|` trong nội dung ô thành `\|`.
- Chuyển xuống dòng trong ô thành `<br>`.
- Không đưa Markdown table vào code fence trong file đầu ra.
- Không tự rút gọn nội dung test case.
- Không gộp nhiều test case thành một dòng.
- Giữ thứ tự test case tăng dần theo số thứ tự trong ID.

### 6. Validation

Trước khi hoàn thành:

1. Kiểm tra file Markdown tồn tại.
2. Kiểm tra encoding UTF-8.
3. Kiểm tra đúng 8 cột theo thứ tự quy định.
4. Kiểm tra mỗi test case tương ứng một dòng.
5. Kiểm tra ID trùng lặp trong phạm vi từng test class, không kiểm tra trùng lặp trên toàn bộ module.
6. Sử dụng khóa `<FullyQualifiedTestClassName>#<TestMethodName>` để xác định duy nhất từng test case.
7. Cho phép các test class khác nhau sử dụng cùng một ID.
8. Không báo lỗi, không loại bỏ và không tự đổi tên test case khi ID giống nhau nhưng thuộc các test class khác nhau.
9. Kiểm tra tổng số test case trong báo cáo khớp với tổng số test case được phân tích từ các test class.
10. Kiểm tra Result chỉ thuộc 4 trạng thái: `Passed`, `Failed`, `Blocked`, `Pending`.
11. Kiểm tra bảng Markdown hiển thị hợp lệ.
12. Kiểm tra không có dữ liệu kiểm thử được tự suy diễn.
13. Kiểm tra thứ tự test case tăng dần theo `NNN` trong từng test class.
14. Không ghi đè file cũ nếu chưa được phép.

### Test Execution Validation

Trước khi xuất báo cáo:

1. Xác định danh sách test class thuộc module.
2. Chạy test bằng Maven Wrapper.
3. Kiểm tra Maven exit code.
4. Đọc Surefire XML được tạo từ lần chạy hiện tại.
5. Đối chiếu đầy đủ các test case với kết quả thực thi.
6. Không sử dụng kết quả từ lần chạy trước nếu không xác minh được tính phù hợp.
7. Không nhầm lẫn test case giữa các test class có cùng tên method.
8. Đối với test Passed, Actual Output phải mô tả kết quả đã được xác nhận bằng assertion hoặc verification.
9. Đối với test Failed, Actual Output phải ghi rõ lỗi thực tế nếu có.
10. Đối với test Blocked, Actual Output phải ghi rõ nguyên nhân cản trở.
11. Đối với test Pending, Actual Output được phép để trống.
12. Test date phải phản ánh thời điểm thực thi thực tế.
13. Tổng số Passed + Failed + Blocked + Pending phải bằng tổng số test case trong báo cáo.
14. Không báo cáo tất cả test Passed nếu không có bằng chứng thực thi tương ứng.

Nếu không thể chạy test, vẫn sinh báo cáo từ source code nhưng phải ghi rõ lý do và sử dụng Pending cho những test chưa có kết quả đáng tin cậy.

Không tự sửa source code, test code hoặc cấu hình để ép test chạy thành công.

### 7. Restrictions

- Không tạo file `.xlsx`, `.xls`, `.csv` hoặc `.docx`.
- Không sử dụng `openpyxl`.
- Không sử dụng thư viện tạo Excel.
- Không tạo script xuất Excel.
- Chỉ xuất file Markdown theo yêu cầu.