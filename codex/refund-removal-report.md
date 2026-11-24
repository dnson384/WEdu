# Gỡ hoàn tiền và tra cứu DB — 2026-11-19

User duyệt gỡ toàn bộ flow refund và xác nhận chỉ tra cứu DB. Không còn UI/API tạo, duyệt, từ chối, hủy hoặc đối soát refund, không còn AI holds/receipt/index/jobs/email refund. ADMIN search chỉ đọc dữ liệu đã lưu; bỏ nút/BFF/backend lookup VNPay. Thanh toán checkout/IPN/Return/verify, grant PLUS, email payment và role/ownership được giữ. Không deploy/commit/push hoặc chạy DB migration trong task.

## Contract và dữ liệu

- Gỡ backend/BFF GET /payment/orders, POST /payment/orders/{id}/refunds, GET /payment/refunds[/{id}], POST /payment/refunds/{id}/cancel; GET /admin/refunds[/{id}], POST /admin/refunds/{id}/approve|reject|reconcile. POST /payment/orders tạo checkout vẫn còn.
- Gỡ POST /admin/transactions/{id}/lookup; GET /admin/transactions giữ Item/Page, filtering và ROLE_ADMIN/private-no-store. Không có Provider/Lookup DTO.
- Gỡ frontend /me/bill/refunds và /admin/transactions/refunds. Tra cứu /admin/transactions/search còn ngày/mã đơn/email/status/mã VNPay local.
- Gỡ model/index runtime RefundRequests/RefundAccounts/RefundAiReceipts; không drop collections hay xóa dữ liệu thật. Quyền PLUS lịch sử không tự khôi phục/thu hồi. Event refund cũ trong PaymentEmails bị loại khỏi claim bởi refundId=null.
- Bỏ field Java plusStartsAt/refundedAt/plusLegacyProtected và refundId/message, không unset dữ liệu lịch sử tự động. Opt-in migration storage hiện có chỉ bỏ dependency RefundAccounts; vẫn migrate provider embedded/ObjectId và legacy refundTracked khi chủ động chạy migration.
- querydr vẫn được checkout verify dùng; không được ADMIN search gọi. Lỗi99 sandbox chưa được tuyên bố đã sửa.

## Validation

- Backend Spotless apply/check scoped28 file Java đạt bằng Maven đã cài, cùng cấu hình repo; wrapper đã có lỗi khởi động từ các lượt trước.
- Backend clean và chạy nhóm `Payment*Test,Vnpay*Test,AdminTransaction*Test,PlusEntitlementTest,QuestionService*Test,AIQuestionGenerationUsageTest,*User*Test`: **184 tests, 177 passed, 7 Mongo integration skipped, 0 failures/errors**. Riêng QuestionServicePlusAccessTest4 cases và ADMIN local search/security tests đạt. Mongo integration thiếu WEDU_PAYMENT_TEST_MONGO_URI.
- Frontend Prettier check và ESLint/fix13 file trong scope đạt. Next typegen + TypeScript noEmit đạt. Production `npm run build -- --webpack` đạt, route build không còn refund/ADMIN lookup và vẫn có /api/payment/orders cho checkout.
- Harness tạm ngoài repo dùng NextRequest/NextResponse thật, mock adapter và React SSR:18 checks đạt (checkout POST, refund GET đã bỏ, cookie auth, invalid filter, local SUCCEEDED/transactionNo, no-store, bảng không có provider action). Không thêm test runner/dependency.
- Lần kiểm tra đầu gặp dependency compile còn sót sau khi gỡ refund, đã sửa; generated .next/dev/types và .next/types tham chiếu route cũ được regenerate. Build sandbox bị lỗi tải Google Fonts/ghi trace, chạy lại ngoài sandbox đạt. Đây không phải các lỗi application baseline chưa xử lý.
- Không chạy full backend suite hoặc gọi VNPay/Mongo/SMTP thật, không kiểm thử browser end-to-end, không chạy migration hoặc xóa dữ liệu DB thật. Lỗi querydr99 vẫn ngoài phạm vi khắc phục; ADMIN tra cứu không còn gọi querydr.
- Kiểm tra links/inventory và scoped git diff --check thực hiện trước bàn giao; tất cả application/test/config/docs thuộc task được liệt kê bên dưới.

## Inventory đầy đủ của task

So với working tree ngay trước task, không phải diff so với HEAD. Các sửa đổi payment/auth/AGENTS.md có sẵn ngoài scope được giữ. File đã xóa được ghi dạng code để tránh link hỏng.

| File | Thao tác | Thay đổi |
| --- | --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java) | Sửa | Bỏ handler AdminTransactionException đã không còn dùng. |
| [backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java) | Sửa | Bỏ sendRefundResultEmail; giữ email payment/auth. |
| [backend/src/main/java/com/wedu/exam_creation/payment/controller/AdminTransactionController.java](../backend/src/main/java/com/wedu/exam_creation/payment/controller/AdminTransactionController.java) | Sửa | Giữ GET list ROLE_ADMIN; bỏ POST lookup và handler exception provider. |
| `backend/src/main/java/com/wedu/exam_creation/payment/controller/RefundController.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentEmailEntity.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentEmailEntity.java) | Sửa | Bỏ refundId/message khỏi domain outbox. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java) | Sửa | Bỏ plusStartsAt/refundedAt/plusLegacyProtected khỏi domain. |
| `backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundAccountEntity.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/RefundEntity.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentRepository.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentRepository.java) | Sửa | Bỏ ordersForUser chỉ dùng refund. |
| `backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IRefundRepository.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/dto/AdminTransactionDTO.java](../backend/src/main/java/com/wedu/exam_creation/payment/dto/AdminTransactionDTO.java) | Sửa | Giữ Item/Page, xóa Provider/Lookup. |
| `backend/src/main/java/com/wedu/exam_creation/payment/dto/RefundDTO.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/RefundIndexConfig.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java) | Sửa | Bỏ refund/validRefund/queryRefund; giữ paymentUrl, ký/xác minh và query thanh toán. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentEmailDocument.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentEmailDocument.java) | Sửa | Bỏ mapping refundId/message; dữ liệu legacy giữ trên DB. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java) | Sửa | Bỏ mapping fields chỉ dùng refund; không cleanup DB thật. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAccountDocument.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundAiReceiptDocument.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/RefundDocument.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/mapper/RefundMapper.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java) | Sửa | Bỏ ordersForUser/ghi field refund; claim email loại refundId legacy. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImpl.java) | Sửa | Bỏ preflight coverage/ghi RefundAccounts; giữ migration embedded VNPay/ObjectId. |
| `backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/RefundRepositoryImpl.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionException.java` | Xóa | Xóa exception chỉ dùng cho refund/provider lookup. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecase.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecase.java) | Sửa | Chỉ search local DB; bỏ gateway/config/provider lookup. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentJobs.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentJobs.java) | Sửa | Chỉ gửi email payment; giữ lease/retry và expireOrders. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java) | Sửa | Bỏ khởi tạo RefundAccount và snapshot entitlement phục vụ hoàn tiền; giữ grantPaidPlus/outbox. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundAiService.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundEntitlementService.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundJobs.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundSettings.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundTransactions.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundUsecase.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java) | Sửa | Bỏ refund holds/AI receipt; đọc lại active/effective PLUS qua UserService trước AI. |
| [backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java) | Sửa | Bỏ revokePaidTime; giữ grantPlus/updateOnly và các cập nhật user khác. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java) | Sửa | Bỏ paidPlusExpiry/hasLegacyPlus/revokePaidTime; giữ grantPaidPlus/findById. |
| [backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java) | Sửa | Bỏ helper chỉ dùng thu hồi PLUS/refund. |
| [backend/src/main/resources/application.yml](../backend/src/main/resources/application.yml) | Sửa | Bỏ payment.refund.enabled và bắt buộc REFUND_ENABLED. |
| [backend/src/test/java/com/wedu/exam_creation/payment/AdminTransactionControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/controller/AdminTransactionControllerTest.java) | Sửa | Giữ list/private-no-store/role tests; bỏ lookup API tests. |
| [backend/src/test/java/com/wedu/exam_creation/payment/AdminTransactionUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecaseTest.java) | Sửa | Bỏ lookup tests; kiểm search trả SUCCEEDED/transactionNo local mà không writes/provider. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentMongoIntegrationTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/PaymentMongoIntegrationTest.java) | Sửa | Cập nhật migration integration không tạo/yêu cầu RefundAccounts. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentPersistenceTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImplTest.java) | Sửa | Thêm kiểm tra query claim email loại event refund lịch sử. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentStorageMappingTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocumentTest.java) | Sửa | Bỏ mapping RefundAccount; giữ ObjectId/nested provider tests. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentStorageMigrationTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImplTest.java) | Sửa | Bỏ coverage dependency; kiểm legacy refund flag không yêu cầu RefundAccounts. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/usecase/PaymentUsecaseTest.java) | Sửa | Bỏ test RefundAccount initialization; giữ payment idempotency/signature/PLUS tests. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundAiServiceTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundControllerTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundEntitlementTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundMongoIntegrationTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundPersistenceTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `backend/src/test/java/com/wedu/exam_creation/payment/RefundUsecaseTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayQueryGatewayTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayGatewayTest.java) | Sửa | Giữ checksum độc lập cho query thanh toán; bỏ queryRefund và optional TransactionNo. |
| `backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayRefundGatewayTest.java` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServicePlusAccessTest.java](../backend/src/test/java/com/wedu/exam_creation/question/usecase/QuestionServiceTest.java) | Thêm | Kiểm tra stale/locked PLUS bị chặn, active PLUS tới provider, FREE không gọi AI. |
| [codex/api-reference.md](../codex/api-reference.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/architecture.md](../codex/architecture.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/business-flows.md](../codex/business-flows.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/current-status.md](../codex/current-status.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/database-schema.md](../codex/database-schema.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/development-decisions.md](../codex/development-decisions.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/module-map.md](../codex/module-map.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/payment-storage-refactor-report.md](../codex/payment-storage-refactor-report.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/refund-implementation-report.md](../codex/refund-implementation-report.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| `codex/refund-removal-report.md` | Xóa | Inventory đầy đủ, contract thay đổi, dữ liệu legacy và validation. |
| [codex/source-catalog.md](../codex/source-catalog.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [codex/tech-stack.md](../codex/tech-stack.md) | Sửa | Đồng bộ scope/symbol/contract hiện hành và links sau khi gỡ refund; đánh dấu snapshot lịch sử. |
| [frontend/app/(site)/admin/transactions/page.tsx](../frontend/app/(site)/admin/transactions/page.tsx) | Sửa | Gỡ link/nội dung refund/provider lookup; giữ nâng cấp hoặc tra cứu DB. |
| `frontend/app/(site)/admin/transactions/refunds/page.tsx` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/app/(site)/admin/transactions/search/page.tsx](../frontend/app/(site)/admin/transactions/search/page.tsx) | Sửa | Gỡ link/nội dung refund/provider lookup; giữ nâng cấp hoặc tra cứu DB. |
| [frontend/app/(site)/me/bill/page.tsx](../frontend/app/(site)/me/bill/page.tsx) | Sửa | Gỡ link/nội dung refund/provider lookup; giữ nâng cấp hoặc tra cứu DB. |
| `frontend/app/(site)/me/bill/refunds/page.tsx` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/refunds/[id]/approve/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/refunds/[id]/reconcile/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/refunds/[id]/reject/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/refunds/[id]/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/refunds/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/admin/transactions/[id]/lookup/route.ts` | Xóa | Xóa endpoint gọi tra cứu provider. |
| [frontend/app/api/admin/transactions/handler.ts](../frontend/app/api/admin/transactions/handler.ts) | Sửa | BFF ADMIN chỉ xử lý list/filter và lỗi danh sách, không nhận id/lookup. |
| `frontend/app/api/payment/orders/[id]/refunds/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/app/api/payment/orders/route.ts](../frontend/app/api/payment/orders/route.ts) | Sửa | Bỏ GET orders của refund, giữ POST tạo checkout qua handlePayment. |
| `frontend/app/api/payment/refunds/[id]/cancel/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/payment/refunds/[id]/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/payment/refunds/route.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/app/api/refund-handler.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/domain/entities/adminTransaction.entity.ts](../frontend/domain/entities/adminTransaction.entity.ts) | Sửa | Bỏ TransactionLookup/provider contract; giữ TransactionPage/Item. |
| `frontend/domain/entities/refund.entity.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/domain/repositories/IAdminTransactionRepository.ts](../frontend/domain/repositories/IAdminTransactionRepository.ts) | Sửa | Chỉ giữ search local. |
| `frontend/domain/repositories/IRefundRepository.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/infrastructure/repositories/AdminTransactionRepositoryImpl.ts](../frontend/infrastructure/repositories/AdminTransactionRepositoryImpl.ts) | Sửa | Bỏ POST lookup; giữ authenticated GET search. |
| `frontend/infrastructure/repositories/RefundRepositoryImpl.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/presentation/components/Admin/TransactionDetail.tsx` | Xóa | Xóa chi tiết tra cứu HTTP VNPay. |
| [frontend/presentation/components/Admin/TransactionTable.tsx](../frontend/presentation/components/Admin/TransactionTable.tsx) | Sửa | Bỏ cột thao tác/nút Tra cứu VNPay; giữ dữ liệu/filter pagination local. |
| `frontend/presentation/components/Payment/RefundDetail.tsx` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| `frontend/presentation/components/Payment/RefundManager.tsx` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/presentation/hooks/Admin/useAdminTransactions.ts](../frontend/presentation/hooks/Admin/useAdminTransactions.ts) | Sửa | Bỏ lookup state/request/abort; giữ search/filter/page với user-scoped query. |
| `frontend/presentation/hooks/Payment/useRefunds.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/presentation/services/AdminTransactionService.ts](../frontend/presentation/services/AdminTransactionService.ts) | Sửa | Bỏ lookup HTTP; giữ GET list. |
| `frontend/presentation/services/RefundService.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
| [frontend/presentation/utils/transactionDisplay.ts](../frontend/presentation/utils/transactionDisplay.ts) | Sửa | Bỏ providerStatus chỉ dùng provider lookup. |
| [frontend/usecases/admin/AdminTransactionUsecase.ts](../frontend/usecases/admin/AdminTransactionUsecase.ts) | Sửa | Bỏ lookup; chỉ giữ search. |
| `frontend/usecases/payment/RefundUsecase.ts` | Xóa | Xóa file chuyên cho flow hoàn tiền và các entrypoint/dependency tương ứng. |
