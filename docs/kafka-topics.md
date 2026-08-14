# Kafka Topic Registry

All topics: 3 partitions, replication factor 1 (single-broker dev cluster), keyed by the natural entity id noted below. Event payload classes live in `rolecall-common` (`com.rolecall.common.event`) so producer and consumer stay byte-for-byte compatible. Failed consumption retries with exponential backoff and routes to a `<topic>.DLT` dead-letter topic after exhausting retries.

| Topic | Key | Producer | Consumer(s) | Payload class | Purpose |
|---|---|---|---|---|---|
| `rolecall.user.registered` | `userId` | auth-service | user-service | `UserRegisteredEvent` | Create the profile shell row asynchronously after signup |
| `rolecall.application.submitted` | `jobId` | application-service | job-service | `ApplicationSubmittedEvent` | Increment `Job.applicantCount` |
| `rolecall.application.status-changed` | `applicationId` | application-service | *(none yet)* | `ApplicationStatusChangedEvent` | Forward-compatible broadcast point for a future notification/audit service |
| `rolecall.payment.completed` | `jobId` | payment-service | job-service | `PaymentCompletedEvent` | Activate a featured listing (`status=FEATURED`, `featuredUntil` set) |
| `rolecall.payment.failed` | `jobId` | payment-service | job-service | `PaymentFailedEvent` | Revert any pending "featuring" state |
