# Payment embedded VNPay refactor - 2026-11-18

> **Snapshot lịch sử, cập nhật phạm vi 2026-11-19:** user đã bỏ toàn bộ flow refund và ADMIN provider lookup. Các field/symbol/validation refund trong báo cáo này mô tả thời điểm trước khi gỡ, không phải runtime hiện hành. [Báo cáo gỡ hoàn tiền và tra cứu DB](refund-removal-report.md) mô tả implementation và validation thay thế. Không chạy migration hoặc xóa DB thật trong task gỡ refund.

The user confirmed the final design: one PaymentOrders collection with an embedded vnpayTransaction object. It supersedes the intermediate split-collection design. Domain and persistence retain separate VnpayTransactionEntity/VnpayTransactionDocument types.

## Resulting schema and behavior

PaymentOrders declares 16 root fields: _id, idempotencyKeyHash, userId, email, amount, durationDays, status, createdAt, expiresAt, paidAt, plusStartsAt, plusExpiresAt, refundedAt, revision, plusLegacyProtected, vnpayTransaction. userId is BSON ObjectId/Java String. The embedded object contains transactionNo, responseCode, transactionStatus and lastVerifiedAt. Both VNPay types have no id; the document has no @Document or ID annotation. No separate provider collection is created by normal runtime.

PaymentMapper maps nested objects directly. Repository reads/inserts one document and validates embedded presence. Order revision updates set nested result fields individually, preserving lastVerifiedAt. Verification atomically checks _id, userId, embedded presence and the 30-second nested throttle in a single update. Search/count use vnpayTransaction.transactionNo; its index replaces the obsolete flat transactionNo index. Entitlement/outbox mutations retain the existing transaction. No HTTP path/DTO/frontend/dependency change.

clientIp is request-scoped; manualReview remains computed as SUCCEEDED with absent plusExpiresAt and snapshotted in PaymentEmails. Refund coverage uses trackedSince <= paidAt; new grants initialize tracking first. plusLegacyProtected is retained.

## Migration and operation

Java-only property payment.storage-migration.enabled defaults false. Normal startup requires ObjectId ownership, embedded VNPay data and no obsolete root fields. The migration has not been run on real data.

1. Back up affected data. Stop other application instances and payment/AI/refund writers; prevent incoming requests during maintenance.
2. Start with --payment.storage-migration.enabled=true --payment.vnpay.enabled=false --payment.refund.enabled=false. Existing runtime configuration requirements still apply.
3. Preflight scans all orders/accounts before writes, accepts original flat, embedded and intermediate split layouts, validates field types and rejects mismatching non-null VNPay values. Invalid owner IDs, inconsistent manualReview or contradictory legacy AI evidence also stop migration. It never infers provider success or fills missing evidence.
4. Per-order Mongo transaction merges supported provider fields into the embedded object, conditionally updates the original order and removes obsolete root fields. It preserves all other order fields. Explicit refundTracked evidence can lower trackedSince to paidAt to preserve old millisecond ordering; account revision increments. Orders already converted to timestamp evidence do not require the removed flag.
5. After reading back and checking the embedded copy, it conditionally removes the corresponding legacy VnpayTransactions source. Changed source/copy failure rolls back that order. Previous successful orders remain converted and reruns resume safely. Orphan provider records and the legacy collection itself are preserved; no collection drop is performed.
6. Final readiness validation precedes removing the obsolete flat PaymentOrders.transactionNo index. ApplicationReadyEvent creates the nested index. Stop the maintenance instance and restart with normal runtime flags and migration disabled.

Preflight holds orders/accounts in memory and startup scans orders. Large-dataset cost is unmeasured. No real Mongo/VNPay/SMTP actions were performed.

## Validation

- Final targeted payment/refund/admin/gateway/PLUS suite: 156 tests, **146 passed, 10 skipped, 0 failures/errors**. Command: mvn -q '-Dtest=Refund*Test,VnpayRefundGatewayTest,Payment*Test,VnpayGatewayTest,VnpayQueryGatewayTest,AdminTransaction*Test,PlusEntitlementTest' test.
- All 10 skips are dedicated Mongo replica-set integration tests because WEDU_PAYMENT_TEST_MONGO_URI is absent. Real transactional migration/VNPay runtime remains unverified.
- Scoped Spotless apply/check passed for all 28 Java files in the cumulative change. During editing, formatter/compile checks caught syntax and outdated test-fixture references; those were corrected before the successful run. Formatting outside the task was restored; no formatter configuration changed.
- git diff --check passed. Complete inventory and added documentation links were checked. No full backend suite/frontend build was run; no frontend code or HTTP contracts changed. Historical unrelated failures are not claimed as current results.
- Logs: backend/target/payment-embedded-tests.log, payment-embedded-format.log and payment-embedded-format-check.log (ignored build output).

## Changed files

The inventory includes both the approved storage split and the domain entity follow-up.

| File | Change |
| --- | --- |
| [backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java](../backend/src/main/java/com/wedu/exam_creation/payment/controller/PaymentController.java) | Forward current request IP for get, verify and Return checkout responses. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/PaymentOrderEntity.java) | Keep separate nested VNPay entity, derived manualReview and legacy PLUS protection; remove flat provider/IP/flag fields. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/VnpayTransactionEntity.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/entity/VnpayTransactionEntity.java) | Define four nested VNPay fields without a duplicate ID. |
| [backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentStorageMigrationRepository.java](../backend/src/main/java/com/wedu/exam_creation/payment/domain/repository/IPaymentStorageMigrationRepository.java) | Declare preflight, migration, readiness and index cleanup storage operations. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/AdminTransactionIndexConfig.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/AdminTransactionIndexConfig.java) | Index PaymentOrders.vnpayTransaction.transactionNo alongside existing order search indexes. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/PaymentStorageMigrationRunner.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/PaymentStorageMigrationRunner.java) | Add opt-in migration/default readiness check before application-ready indexes. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/VnpayGateway.java) | Accept request IP for checkout and read the original payment number from VnpayTransactionEntity for refunds. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocument.java) | Store BSON ObjectId userId and embed vnpayTransaction; remove flat provider fields and redundant flags/IP. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/VnpayTransactionDocument.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/document/VnpayTransactionDocument.java) | Define an embedded four-field model without @Document, @MongoId or id. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/mapper/PaymentMapper.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/mapper/PaymentMapper.java) | Map order and nested VNPay entity/document pairs directly. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImpl.java) | Read/insert one document, update nested results with revision CAS, preserve throttle and atomically check ownership; search/count nested transactionNo. |
| [backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImpl.java](../backend/src/main/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImpl.java) | Preflight and merge flat/nested/split data; verify embedded copy before conditionally deleting split source; preserve AI evidence and owner ID. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecase.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecase.java) | Read transactionNo from the related VNPay entity while retaining the existing DTO. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentStorageMigrationUsecase.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentStorageMigrationUsecase.java) | Coordinate global preflight and one Mongo transaction per migrated order, followed by validation/index cleanup. |
| [backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java](../backend/src/main/java/com/wedu/exam_creation/payment/usecase/PaymentUsecase.java) | Initialize embedded VNPay entity, update nested receipt, forward request IP and preserve transaction/AI timing. |
| backend/src/main/java/com/wedu/exam_creation/payment/usecase/RefundUsecase.java (đã xóa) | Use trackedSince/paidAt coverage for AI eligibility and read provider evidence from the nested VNPay entity. |
| [backend/src/test/java/com/wedu/exam_creation/payment/AdminTransactionRepositoryTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImplTest.java) | Verify shared search/count predicates, nested transactionNo path, pagination and nested index. |
| [backend/src/test/java/com/wedu/exam_creation/payment/AdminTransactionUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/usecase/AdminTransactionUsecaseTest.java) | Initialize the separate VNPay entity in ADMIN test fixtures. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentControllerTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/controller/PaymentControllerTest.java) | Adapt MockMvc stubs to request-IP forwarding signatures. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentMongoIntegrationTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/PaymentMongoIntegrationTest.java) | Use valid owner IDs and verify flat/split migration replay, embedded storage, search/throttle and outbox rollback on a dedicated replica set. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentPersistenceTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentRepositoryImplTest.java) | Verify nested aggregate mapping and existing hash/outbox behavior. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentStorageMappingTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/document/PaymentOrderDocumentTest.java) | Verify nested mapper/BSON round trips, no duplicate embedded ID/collection annotation, atomic owner throttle, stale revision and timestamp preservation. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentStorageMigrationTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/repository/PaymentStorageMigrationRepositoryImplTest.java) | Test invalid owner IDs, flat/split/nested conflicts, evidence, embedded verification before source deletion, replay and transaction rollback. |
| [backend/src/test/java/com/wedu/exam_creation/payment/PaymentUsecaseTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/usecase/PaymentUsecaseTest.java) | Adapt request-IP calls; verify fresh IP on retry/reload and tracking initialization before paidAt. |
| backend/src/test/java/com/wedu/exam_creation/payment/RefundMongoIntegrationTest.java (đã xóa) | Use embedded VNPay, ObjectId owner and timestamp coverage fixtures for refund concurrency tests. |
| backend/src/test/java/com/wedu/exam_creation/payment/RefundUsecaseTest.java (đã xóa) | Replace flag fixtures with tracking timestamps; test equal and absent coverage timestamps. |
| [backend/src/test/java/com/wedu/exam_creation/payment/VnpayGatewayTest.java](../backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayGatewayTest.java) | Verify signed checkout using explicit request IP. |
| backend/src/test/java/com/wedu/exam_creation/payment/infrastructure/VnpayRefundGatewayTest.java (đã xóa) | Build the related VNPay entity and verify refund signing with its original transaction number. |
| [codex/api-reference.md](../codex/api-reference.md) | Document stable HTTP/JSON contracts and derived manualReview/request IP behavior. |
| [codex/architecture.md](../codex/architecture.md) | Document single-document embedded persistence and Java migration boundaries. |
| [codex/business-flows.md](../codex/business-flows.md) | Document nested provider updates/search, ownership/throttle, derived evidence and migration. |
| [codex/current-status.md](../codex/current-status.md) | Record final embedded implementation and validation/runtime limits. |
| [codex/database-schema.md](../codex/database-schema.md) | Describe 16 root fields plus four embedded VNPay fields, no new collection, indexes and conversion. |
| [codex/development-decisions.md](../codex/development-decisions.md) | Record final approved embedded design superseding intermediate split storage. |
| [codex/module-map.md](../codex/module-map.md) | Map embedded types, nested queries/indexes and migration symbols. |
| [codex/payment-storage-refactor-report.md](../codex/payment-storage-refactor-report.md) | Record behavior, schema, migration procedure, validation and complete changed-file inventory. |
| [codex/source-catalog.md](../codex/source-catalog.md) | Register embedded domain/persistence models and current symbols. |
| [codex/tech-stack.md](../codex/tech-stack.md) | Document migration maintenance flags and existing format/test requirements. |


## Remaining scope and limitations

No new migration has been executed. Mongo integration requires an explicitly configured dedicated replica set. Existing indefinite PLUS purchase behavior remains outside this refactor; plusLegacyProtected is preserved.
