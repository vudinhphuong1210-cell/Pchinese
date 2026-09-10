# Feature Specification: AI Plan Administration and Monitoring

**Feature Branch**: `[F12-ai-plan-administration-monitoring]`

**Created**: 2026-09-08

**Status**: Draft

**Input**: User description: "Hiện tại chưa có feature để admin có thể quản lý token AI, các thống kê về Ai, chưa có biện pháp monitoring về AI được dùng như nào và nếu có thay đổi thông tin về với premium thì không thể thay đổi từ phía admin mà phải sửa code"

## Clarifications

### Session 2026-09-08

- Q: Khi Admin đổi quota/token của một gói, thay đổi nên áp dụng cho learner đang có entitlement từ thời điểm nào? → A: Learner mới áp dụng ngay; learner hiện tại áp dụng ở chu kỳ allowance kế tiếp.
- Q: Thông tin giá của gói Premium nên được Admin quản lý theo cách nào? → A: Giá số + mã tiền tệ + chu kỳ tháng/năm + nhãn hiển thị tùy chọn.
- Q: Ngoài dashboard, F12 cần gửi cảnh báo AI qua kênh nào? → A: Chỉ dashboard nội bộ.
- Q: Mỗi rule monitoring nên đánh giá số liệu AI trong khung thời gian nào? → A: Chọn cửa sổ cuộn 1 giờ hoặc 24 giờ theo từng rule.
- Q: Admin cần xem lịch sử thống kê AI tổng hợp trong bao lâu? → A: Lưu 4 tháng; mỗi báo cáo lọc tối đa 50 ngày.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage AI plan policies (Priority: P1)

As an authorized administrator, I want to manage the Free and Premium plan information and their AI-usage policies so that approved business changes do not require a source-code change.

**Why this priority**: Plan information and AI allowance policy are currently fixed in the application, making ordinary operational changes slow and error-prone.

**Independent Test**: An administrator can create a revision of the Premium plan, set its business information and AI allowance, publish it, and verify that the changed policy is recorded without manually changing any learner's entitlement.

**Acceptance Scenarios**:

1. **Given** an authorized administrator and a current Free or Premium plan policy, **When** the administrator saves a valid revision with a reason, **Then** the system preserves the previous version, records the actor and time, and makes the new policy available immediately for newly eligible learners.
2. **Given** a current learner entitlement, **When** an administrator publishes a plan-policy revision, **Then** the learner's current allowance, usage and cycle are not reset or retroactively changed, and the revised allowance applies only at that learner's next allowance-cycle boundary.
3. **Given** a Premium plan policy while payment and Premium enrollment remain disabled, **When** an administrator updates its description, benefits, recurring price amount, currency, monthly or yearly price interval, optional display label or AI allowance, **Then** no learner is enrolled, charged, upgraded, downgraded or given an upgrade journey.
4. **Given** two administrators edit the same plan policy, **When** one administrator saves first, **Then** the other is told to review the latest policy before replacing it.

---

### User Story 2 - Understand aggregate AI use (Priority: P1)

As an authorized administrator, I want a privacy-safe AI operations dashboard so that I can understand consumption, cost and reliability by plan and AI learning capability without accessing learner-private content.

**Why this priority**: The team needs evidence to operate AI capacity, price plans and detect abnormal consumption rather than relying on code changes or raw logs.

**Independent Test**: With representative successful, denied, refunded and failed AI activities, an administrator filters one reporting period and sees correct aggregate counts, allowance units, provider-metered tokens/cost where available, and outcome trends without learner identifiers or content.

**Acceptance Scenarios**:

1. **Given** recorded AI activity in a selected reporting period, **When** an administrator filters by date range, plan and AI capability, **Then** the dashboard shows aggregate requests, outcomes, allowance units used/refunded, and provider-metered token and cost values where the provider supplied them.
2. **Given** a provider does not report a token or cost value, **When** its activity is displayed, **Then** the corresponding metric is marked unavailable rather than represented as zero.
3. **Given** any dashboard result, **When** the administrator views it, **Then** it contains no learner identifier, account data, raw prompt, response, recording, transcript or other private learning content.
4. **Given** aggregate AI reporting data from the most recent four months, **When** an administrator selects a period of no more than 50 days, **Then** the dashboard returns the matching aggregate report or an explicit partial-data state.

---

### User Story 3 - Monitor AI health and anomalies (Priority: P2)

As an authorized administrator, I want to configure and review in-dashboard AI monitoring alerts so that I can respond to abnormal cost, usage, quota-denial, latency or failure patterns before they degrade learning.

**Why this priority**: Aggregate reporting explains the past; actionable monitoring makes emerging capacity and reliability problems visible in time to investigate.

**Independent Test**: An administrator configures a monitoring threshold, representative AI activity breaches it, and the dashboard shows a privacy-safe alert with its condition, first-seen time and current status.

**Acceptance Scenarios**:

1. **Given** an authorized administrator, **When** they set a valid threshold for tokens, estimated cost, request volume, quota denials, failure rate or response-time trend and select a rolling one-hour or 24-hour evaluation window, **Then** the monitoring rule is saved with its scope and is auditable.
2. **Given** measured activity crosses an enabled threshold, **When** the dashboard is next evaluated, **Then** it displays an alert identifying the aggregate condition, affected period, plan/capability scope and current state without exposing private learner data or provider secrets.
3. **Given** an open alert, **When** an administrator acknowledges it, **Then** the acknowledgement, actor, time and optional operational note are recorded; the alert remains visible in the dashboard until its measured condition is no longer breached.

### Edge Cases

- A plan policy cannot be published if required business information, a non-negative allowance, or a valid allowance period is missing; the current published policy remains unchanged.
- An administrator cannot delete or overwrite a plan-policy version that is referenced by current or historical entitlements, usage or reports; retirement stops only future selection where doing so leaves a valid Free policy.
- A stale edit, a rejected save or a failed monitoring-rule change leaves the existing policy, rule and alerts unchanged and returns a recoverable explanation.
- AI activities retried under the existing idempotency rule must appear once in reporting and monitoring, according to their final recorded outcome.
- Late, corrected or refunded AI activity must update the appropriate aggregate outcome without exposing an event-level learner record or causing an allowance unit or provider cost to be counted twice.
- An unavailable provider, missing metering data, or a delayed measurement source must show a clear partial-data state and must not fabricate zero usage, zero cost or a healthy status.
- A requested reporting period longer than 50 days, or a period older than the available four-month history, must be rejected or constrained with a clear recoverable explanation.
- A non-ADMIN, locked account, or expired session cannot read reports, configure policies, acknowledge alerts or infer protected data from an error response.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide an AI administration workspace only to currently authorized `ADMIN` accounts; every report read and policy, monitoring or acknowledgement action MUST be authorized server-side.
- **FR-002**: The system MUST let an authorized administrator manage the Free and Premium plan catalogue information: plan code, display name, description, benefits, recurring price amount, recognized currency, monthly or yearly price interval, optional display label, availability state, AI allowance amount and allowance period.
- **FR-003**: Each plan-policy change MUST create an immutable historical version containing the old and new business policy, authorizing administrator, reason, recorded time and applicability rule. Existing entitlements and AI usage retain the version applicable to them.
- **FR-004**: A plan-policy change MUST apply immediately to newly eligible learners and at the next allowance-cycle boundary for learners with a current entitlement. It MUST NOT reset, reprice, grant, revoke or otherwise alter an individual learner's current entitlement, remaining allowance, usage record or current allowance-cycle boundary.
- **FR-005**: The system MUST maintain at least one valid published Free plan policy for eligible learners. A referenced plan-policy version can be retired from future use but not destroyed.
- **FR-006**: The system MUST preserve the existing restriction that an administrator cannot inspect, grant, revoke, edit or reset an individual learner's entitlement or AI allowance.
- **FR-007**: The AI administration workspace MUST provide aggregate, filterable reporting by reporting period, plan-policy version, AI capability and final activity outcome. It MUST include requests, successful results, quota denials, failed/refunded results, allowance units reserved/used/refunded, provider-metered input/output/total tokens and estimated cost where supplied, and response-time trends.
- **FR-008**: The system MUST record sufficient privacy-safe operational measurements for every eligible AI activity to support the required reporting and monitoring dimensions. It MUST distinguish unavailable provider metering from a measured value of zero.
- **FR-009**: Reports, alert details and administrative audit records MUST exclude learner identifiers, account/profile data, raw prompts, AI replies, recordings, transcripts, provider credentials, full provider error bodies and any other learner-private content.
- **FR-010**: The system MUST let an authorized administrator configure enabled monitoring rules for aggregate token volume, estimated cost, request volume, quota-denial rate, failure rate and response-time trend, each with a selected rolling one-hour or 24-hour evaluation window and optional plan or AI-capability scope.
- **FR-011**: When an enabled rule is breached, the system MUST create or update an in-dashboard alert that shows the violated aggregate condition, scope, first-seen time, latest measured value and current state. An authorized administrator can acknowledge an alert; acknowledgement does not suppress continued measurement or resolution. This feature MUST NOT deliver alert notifications through email, chat or pager channels.
- **FR-012**: The system MUST keep an immutable, privacy-safe administrative audit history for plan-policy revisions, plan retirement, monitoring-rule changes and alert acknowledgements, including actor, time, action, affected configuration and supplied reason or operational note.
- **FR-013**: The system MUST present an explicit empty or partial-data state when the selected range has no measured AI activity, an AI capability is not yet active, provider metering is unavailable, or reporting is delayed.
- **FR-014**: Editing Premium plan information or its AI policy MUST NOT add payment collection, checkout, automated billing, learner-visible upgrade flow, or manual Premium activation to this feature.
- **FR-015**: The system MUST provide a recoverable conflict outcome when an administrator attempts to save a plan policy or monitoring rule that has changed since their review; it MUST not silently replace the newer configuration.
- **FR-016**: The system MUST make privacy-safe aggregate AI reporting data available for the most recent four months and limit one selected reporting period to no more than 50 days.

### Key Entities *(include if feature involves data)*

- **Plan policy version**: A dated, immutable definition of a Free or Premium plan's business information, including its recurring price amount, currency, price interval, optional display label, availability state, AI allowance policy and applicability rule.
- **AI operational measurement**: The privacy-safe metering and outcome facts for one eligible AI activity, including allowance outcome and provider usage figures when provided; it excludes learner content and identifiers from administrative views.
- **Aggregate AI report**: A filtered summary of operational measurements by period, plan-policy version, AI capability and final outcome.
- **Monitoring rule**: An administrator-owned threshold definition for an aggregate AI usage, cost, reliability or response-time condition, evaluated in a selected rolling one-hour or 24-hour window.
- **Monitoring alert**: The current and historical record that a monitoring rule's aggregate condition was breached, including acknowledgement and resolution state.
- **AI administration audit event**: An immutable record of a privileged configuration, retirement or alert-acknowledgement action without private learner content or secrets.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized administrator can revise and publish valid Free or Premium plan information and AI allowance policy, with a reason, in no more than three minutes; 100% of accepted changes have a retrievable historical version and audit record.
- **SC-002**: For a verified reporting data set, 100% of displayed aggregate request counts, final outcomes, allowance units and supplied provider-metering values reconcile with the selected period and filters, with idempotent retries counted once.
- **SC-003**: At least 95% of reporting queries for a period of up to 50 days within the available four-month history display their aggregate result or an explicit partial-data state within five seconds under the supported operating load.
- **SC-004**: 100% of AI administration reports, alerts and audit records reviewed in authorization and privacy tests contain no learner identifier, learner-private content, provider credential or raw provider error payload.
- **SC-005**: For a verified threshold breach, the corresponding alert is visible to an authorized administrator within five minutes of the measurement becoming available; 100% of alert acknowledgements record an actor and time.
- **SC-006**: 100% of Premium plan-policy changes leave learner enrollment, billing, upgrade availability, current entitlements and current allowance cycles unchanged.

## Assumptions

- "AI token" means provider-metered input/output token use and estimated cost where a provider reports it, in addition to the learner-facing AI allowance units already governed by F03. A missing provider value is reported as unavailable, not estimated or zero.
- This feature is an Administrator-only operational capability. It relies on F01 for server-managed `ADMIN` authorization; it relies on F03 for the entitlement and allowance ledger; F08 and F11 supply their applicable AI activity measurements when those features are active.
- Premium administration in this feature is catalogue and future policy administration only. Automated billing, payments, learner self-service upgrades and any manual learner entitlement operation require separately approved scope.
- A Premium price is a non-negative recurring catalogue amount in a recognized currency with a monthly or yearly interval; its optional display label does not replace these structured values.
- The dashboard retains aggregate AI reporting data for the most recent four months and supports a selected reporting range of up to 50 days. It has no per-learner drill-down and no external email, chat or pager notification delivery; administrators review alerts in the workspace.
- Each monitoring rule evaluates its aggregate condition using the administrator-selected rolling one-hour or 24-hour window; calendar-day alert evaluation is not part of this feature.
- The existing feature map must be revised and approved to add this Administrator capability before planning or implementation, because the current MVP map limits the Administrator dashboard to F01 Account/Roles and F04 Content/Media.
- Data retention, access control, audit protection and privacy controls follow the project constitution and canonical privacy policy. This feature does not loosen the prohibition on Admin access to learner-private learning or AI data.
