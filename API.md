# API.md — Pchinese API Registry

> Phiên bản: 1.1 · 2026-09-05  
> Nguồn chuẩn: `AGENT.md`, `CONSTITUTION.md`, `CLAUDE.md`, tài liệu `frontend/`, `backend/`, `ai-service/`, `DATA_short.md` và các specs F00–F11.  
> Mục đích: registry hợp nhất giữa URL SPA, public API Spring Boot và private contract của ai-service. React không tự ghép URL; Spring Boot là authority duy nhất cho product data và business rule. Mọi public response dùng envelope `{ success, data, error, meta }`.

## 1. Trạng thái và quy tắc cập nhật

| Nhãn | Ý nghĩa |
| --- | --- |
| `LOCKED` | Endpoint/behaviour đã được quy định rõ trong `CLAUDE.md`; không đổi route hoặc semantics nếu chưa cập nhật tài liệu nguồn/spec. |
| `MVP` | Canonical route được chốt trong tài liệu này để triển khai schema `DATA_short.md`. Cần tạo feature spec trước khi code. |
| `PHASE 2` | Contract đã được kiến trúc định hướng nhưng không thuộc 23 bảng MVP. Không triển khai sớm hơn feature spec. |

Khi tài liệu mâu thuẫn, áp dụng theo thứ tự: `CONSTITUTION.md` → feature spec đã được làm rõ → `DATA_short.md` → `CLAUDE.md`/`AGENT.md` → file này. API.md không tự mở rộng scope của feature.

Khi thay đổi một API, người thực hiện phải cập nhật cùng pull request:

1. dòng endpoint trong file này;
2. OpenAPI/Swagger schema;
3. DTO backend, API client frontend theo contract và test contract;
4. frontend route nếu entry point thay đổi.

Không được đổi URL chỉ ở frontend hoặc chỉ ở controller. Không tạo endpoint mà chưa thêm vào registry này.

## 2. Địa chỉ chuẩn và cách frontend gọi backend

### 2.1 Tách frontend URL khỏi backend URL

| Môi trường | Frontend SPA | Backend API base | Quy tắc |
| --- | --- | --- | --- |
| Local development | `http://localhost:5173` | `http://localhost:8080/api/v1` | Giá trị backend lấy từ `VITE_API_BASE_URL`; không hard-code trong component. |
| Production | cấu hình deploy, ví dụ `https://app.<domain>` | cấu hình deploy, ví dụ `https://api.<domain>/api/v1` | Chỉ lấy từ environment build/deploy; CORS allowlist origin frontend cụ thể. |

`VITE_API_BASE_URL` phải bao gồm `/api/v1`, ví dụ:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Frontend browser routes không có `/api/v1`; backend REST endpoints luôn có `/api/v1`. Ví dụ:

```text
Trang học:       /learn/han-ngu-hsk-1-bai-1
Gọi API:         GET {VITE_API_BASE_URL}/lessons/{lessonId}
Không hợp lệ:    GET /learn/han-ngu-hsk-1-bai-1/api/lessons
Không hợp lệ:    GET /api/lessons
```

### 2.2 API client duy nhất

- Chỉ `src/api/` (hoặc `src/lib/apiClient.js` tương đương) đọc `VITE_API_BASE_URL`, gắn headers, parse standard envelope và xử lý refresh token single-flight.
- Feature chỉ gọi API methods như `api.auth.login()`, `api.lessons.getById()`; không viết `fetch('/api/...')`, `axios.get(...)` hoặc lặp string URL trong page/component.
- Khai báo tất cả path params bằng **ID UUID**. Frontend có thể dùng `slug` trong URL đẹp, nhưng phải load lesson/topic rồi gọi endpoint bằng `lessonId`/`topicId` trả về từ API.
- Không gọi AI vendor hoặc Media Provider trực tiếp từ browser. Playback metadata đi qua backend endpoint; API key/provider prompt không được trả về frontend.

### 2.3 Headers và token

| Tình huống | Header/cookie bắt buộc |
| --- | --- |
| API đã đăng nhập | `Authorization: Bearer <access-token>`; access token chỉ ở JavaScript memory. |
| Request JSON | `Content-Type: application/json`, `Accept: application/json`. |
| Upload recording/media | `Content-Type: multipart/form-data`; để browser tự đặt boundary. |
| `POST /auth/refresh` web | `__Host-pchinese-refresh` HttpOnly cookie, `X-Refresh-Request-Id: <UUID>`, `X-CSRF-Token`, Origin/Referer allowlisted. |
| Login/logout/reset/verification web dùng cookie | Cookie/CSRF/Origin protections theo backend policy; refresh token không xuất hiện trong JSON. |
| Correlation | Client có thể gửi `X-Correlation-Id` UUID; nếu không, backend tạo và trả an toàn qua `meta.correlationId`. |

Access JWT sống 10 phút. Khi nhận `401 ACCESS_TOKEN_EXPIRED`, `apiClient` chỉ gửi **một** refresh request; refresh thất bại thì xóa auth state trong memory và chuyển người dùng đến `/login`.

## 3. Frontend route registry

Frontend route guard chỉ phục vụ UX; backend vẫn kiểm tra authentication, ownership, `ADMIN` và entitlement ở mọi endpoint.

| Frontend URL | Page/component | Backend API chính | Guard UX |
| --- | --- | --- | --- |
| `/` | Home/Catalog | `GET /topics`, `GET /lessons` | Public |
| `/login` | SignInPage | auth login/refresh | Guest only |
| `/register` | RegisterPage | auth register/verification | Guest only |
| `/verify-email` | VerifyEmailPage | email verification confirm | Guest only |
| `/forgot-password` | ForgotPasswordPage | password reset request | Guest only |
| `/reset-password` | ResetPasswordPage | password reset confirm | Guest only |
| `/learn` | LessonCatalogPage | `GET /topics`, `GET /lessons` | Public; MVP không hiển thị Premium lock/upgrade journey |
| `/learn/:lessonSlug` | Lesson summary / Player | lesson detail, playback, progress, attempts | Guest chỉ xem summary; Player/playback/practice cần learner đăng nhập |
| `/shadowing/:lessonSlug/:segmentId` | ShadowingPage | recordings, shadowing attempts | Learner; dedicated screen, backend kiểm tra context lesson/segment |
| `/dictionary` | DictionaryPage | dictionary search/detail | Public |
| `/vocabulary` | SavedWordsPage | saved words | Authenticated learner |
| `/review` | ReviewPage | due reviews/submit review | Authenticated learner |
| `/progress` | ProgressPage | lesson progress/overview | Authenticated learner |
| `/ai-buddy` | AiBuddyPage | conversations/messages | Authenticated learner; backend checks quota |
| `/settings/profile` | ProfileSettingsPage | `GET/PATCH /me` | Authenticated learner |
| `/settings/sessions` | SessionSettingsPage | auth sessions/logout | Authenticated learner |
| `/admin/content` | AdminContentPage | topics/lessons/segments/media writes | `ADMIN` UX guard + backend `ADMIN` |
| `/admin/users` | AdminUsersPage | safe paginated user management, role, lock/unlock | `ADMIN` UX guard + backend `ADMIN`; no email/profile/learner-data browsing |
| `/admin/ai` | AiAdministrationWorkspace | future plan policy, aggregate AI report and in-dashboard monitoring | `ADMIN` UX guard + backend `ADMIN`; no learner/event-level data, billing or entitlement mutation |

Không render URL signed media hoặc provider identifier vào route. Nếu lesson `UNPUBLISHED`/`ARCHIVED`, UI nhận lỗi chuẩn từ backend và không cố dùng cached media URL.

## 4. Envelope, phân trang và lỗi chung

### 4.1 Response envelope bắt buộc

Mọi endpoint, kể cả Admin/upload, trả đúng cấu trúc:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {
    "correlationId": "uuid"
  }
}
```

Khi lỗi:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The submitted answer is invalid."
  },
  "meta": {
    "correlationId": "uuid"
  }
}
```

Không trả JPA entity, stack trace, password, raw JWT/refresh token, API key, provider prompt, raw object key hay provider internal error.

### 4.2 Query phân trang chuẩn

Tất cả list có lịch sử/tăng không giới hạn nhận các query sau, trừ khi endpoint ghi khác:

```text
page=0&size=20&sort=createdAt,desc
```

`page` zero-based; `size` mặc định 20, tối đa 100. `meta` trả:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 41,
  "totalPages": 3,
  "hasNext": true,
  "correlationId": "uuid"
}
```

Ngoại lệ F10: `GET /reviews/due` luôn trả tối đa 20 schedule đến hạn, theo `dueAt` cũ nhất trước; client không truyền/tăng `limit`. Khi hết batch, client gọi lại route để lấy batch kế tiếp.

### 4.3 Error code cần frontend xử lý

| HTTP | Code | Frontend behavior |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | Hiện lỗi field/form an toàn. |
| `401` | `ACCESS_TOKEN_EXPIRED` | apiClient refresh một lần. |
| `401` | `UNAUTHENTICATED`, `TOKEN_REVOKED`, `REFRESH_TOKEN_INVALID` | Clear state và về `/login`. |
| `403` | `AUTHORIZATION_DENIED` | Không tiết lộ resource; hiện trang cấm truy cập. |
| `403` | `ENTITLEMENT_REQUIRED` | Hiện unavailable/access state; MVP không hiển thị upgrade hoặc tự cấp Premium. |
| `404` | `RESOURCE_NOT_FOUND` | Hiện not found; resource learner-owned không phân biệt absent/unowned. |
| `409` | `DUPLICATE_RESOURCE`, `STATE_CONFLICT`, `IDEMPOTENCY_CONFLICT` | Hiện action phù hợp, sau đó reload state server. |
| `413`/`415` | `PAYLOAD_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE` | Báo file upload không hợp lệ trước khi retry. |
| `429` | `RATE_LIMITED`, `AI_QUOTA_EXCEEDED` | Disable retry tức thì; hiển thị cooldown/quota. |
| `502`/`503` | `PROVIDER_ERROR`, `SERVICE_UNAVAILABLE` | Giữ learner input/recording state để có thể retry an toàn. |

## 5. Backend endpoint registry — Authentication

Các endpoint của phần này là `LOCKED` theo `CLAUDE.md`.

| API key frontend | Method + backend URL | Request chính | Thành công | Access / lỗi quan trọng |
| --- | --- | --- | --- | --- |
| `auth.register` | `POST /auth/register` | `{ email, password }` | `202`, `{ accepted: true }` | Public; response trung lập, không tiết lộ account tồn tại; `400`, `429`. |
| `auth.requestEmailVerification` | `POST /auth/email-verifications` | `{ email }` | `202` | Public; không tiết lộ account tồn tại; `400`, `429`. |
| `auth.confirmEmailVerification` | `POST /auth/email-verifications/confirm` | `{ verificationToken }` | `200`, verified account state | Public; `400`, `409 STATE_CONFLICT`. |
| `auth.login` | `POST /auth/login` | `{ email, password, deviceId, deviceLabel, platform }` | `200`, access-session result kèm các role hiện tại do server xác nhận; web nhận refresh cookie | Verified user; `401`, `403`. |
| `auth.refresh` | `POST /auth/refresh` | Web: cookie + CSRF + `X-Refresh-Request-Id`; Mobile: secure-store token + request ID | `200`, rotated session result kèm các role hiện tại do server xác nhận | `401 REFRESH_TOKEN_INVALID`; client single-flight. |
| `auth.logout` | `POST /auth/logout` | Web refresh cookie / mobile refresh credential | `200` | Current session; `401 REFRESH_TOKEN_INVALID`. |
| `auth.requestPasswordReset` | `POST /auth/password-resets` | `{ email }` | `202` | Public; không lộ account; `400`, `429`. |
| `auth.confirmPasswordReset` | `POST /auth/password-resets/confirm` | `{ resetToken, newPassword }` | `200` | Public; `400`; thành công revoke mọi sessions. |

**URL đầy đủ ví dụ:** `POST http://localhost:8080/api/v1/auth/login`. Trong code frontend chỉ dùng `api.auth.login`, không dùng URL này trực tiếp.

Web browser supports concurrent accounts in separate tabs. `browserSessionId` is a non-credential,
per-tab selector sent as `X-Browser-Session-Id`; it routes refresh/logout to the matching named
cookie pair. It may be retained only in `sessionStorage`. Access JWTs and raw refresh values remain
outside every browser storage mechanism.

## 6. Backend endpoint registry — Account và catalog

Các route đánh dấu `MVP` được chuẩn hóa trong file này. Response DTO chỉ đưa field cần hiển thị, không trả entity/raw media record.

| API key frontend | Method + backend URL | Request/query | `data` tối thiểu | Access | Status |
| --- | --- | --- | --- | --- | --- |
| `me.get` | `GET /me` | — | profile, `profileVersion` | Current user | MVP |
| `me.update` | `PATCH /me` | `{ displayName?, nativeLanguageCode?, interfaceLocale?, timeZone?, targetHskLevel: 1..6, dailyGoalMinutes: 1..240, expectedProfileVersion }` | updated profile | Current user; stale version returns `409 STATE_CONFLICT` without overwrite | MVP |
| `admin.auditEvents.list` | `GET /admin/audit-events` | `page` zero-based, `size` 1–50 | system-safe event type/time and permitted actor/target account-name labels + page metadata | `ADMIN` only; learners receive `403`; no UUIDs, audit details, role/session/correlation metadata or profile values | MVP |
| `topics.list` | `GET /topics` | `query?`, `hskLevel?`, pagination | published topic summaries | Public; title/summary/HSK/duration/published count only | MVP |
| `topics.getById` | `GET /topics/{topicId}` | Path UUID | topic + permitted lesson summaries | Public | MVP |
| `lessons.list` | `GET /lessons` | `topicId?`, `query?`, `hskLevel?`, pagination | published Free lesson cards | Public; default admin sort order; no Premium journey in MVP | MVP |
| `lessons.getById` | `GET /lessons/{lessonId}` | Path UUID | public lesson summary; learner-owned completion projection only when permitted | Visitor gets no transcript, playlist, media URL or practice content | MVP |
| `lessons.getPlayback` | `GET /lessons/{lessonId}/playback` | Path UUID | approved playback metadata, current unlocked ordered segments, owned progress/watermark | Signed-in learner only; first permitted Player entry lazy creates/reuses progress | MVP |
| `lessonProgress.get` | `GET /lesson-progress` | `lessonId` UUID | caller's progress row or not-started projection | Learner owner; catalog/summary read creates no row | MVP |
| `lessonProgress.playbackEvent` | `POST /lesson-progress/{lessonId}/playback-events` | `{ segmentId, event: "PROGRESS" | "ENDED", positionMs, clientEventId }` | authoritative progress, playback watermark, completion state | Only current unlocked segment; accepted automatic end completes once and duplicate/concurrent event returns latest state | MVP |

Quy tắc playback: route chỉ trả metadata đã được backend phê duyệt sau publication/access check. Media Provider vẫn không là authorization source; không nhận media URL/identifier do client gửi. Player chỉ cho seek lùi trong server-validated watermark, không cho seek tiến vượt watermark. Chỉ accepted `ENDED` của current segment mới complete segment, mở đúng segment tiếp theo, và chỉ complete lesson khi tất cả segment theo thứ tự đã complete. Dictation/Shadowing không được gọi route này để unlock hoặc complete segment.

## 7. Backend endpoint registry — Dictation và Shadowing

| API key frontend | Method + backend URL | Request | `data` tối thiểu | Access / rule | Status |
| --- | --- | --- | --- | --- | --- |
| `dictation.start` | `POST /dictation-attempts` | `{ segmentId }` | `{ dictationAttemptId, status }` | Learner; chỉ current unlocked hoặc earlier completed segment | MVP |
| `dictation.submit` | `POST /dictation-attempts/{dictationAttemptId}/submit` | `{ answer, clientSubmissionId }` | overallScore, accuracyPercent, generalGuidance, status | Owner; retry cùng logical submission trả result cũ | MVP |
| `dictation.list` | `GET /dictation-attempts` | `lessonId?`, `segmentId?`, pagination | own attempt summaries | Learner owner, exclude deleted | MVP |
| `dictation.get` | `GET /dictation-attempts/{dictationAttemptId}` | Path UUID | own full result | Learner owner | MVP |
| `recordings.upload` | `POST /recordings` | multipart: `file`, `segmentId` | `{ recordingId, status }` | Learner + segment practiceable; type/size/malware validation. Default `ASSESSMENT_ONLY`. | MVP |
| `shadowing.assess` | `POST /shadowing-attempts` | `{ segmentId, recordingId, clientRequestId }` | attempt ID, `PROCESSING`/`COMPLETED`/`FAILED`/`EXPIRED`, score/feedback only when complete | Owner of eligible recording; Spring Boot reserves one actual-assessment allowance idempotently | MVP |
| `shadowing.list` | `GET /shadowing-attempts` | `lessonId?`, `segmentId?`, pagination | own attempt summaries | Learner owner | MVP |
| `shadowing.get` | `GET /shadowing-attempts/{shadowingAttemptId}` | Path UUID | score/feedback permitted to owner | Learner owner; never transcript | MVP |

`dictation.submit` và `shadowing.assess` không nhận `score`, `feedback`, `userId`, `lessonId` hay `accessLevel` từ client. Backend lấy segment/lesson, kiểm tra access/ownership và ghi attempt trong domain transaction. Approved outcome chỉ có thể cập nhật practice summary hoặc best score — tuyệt đối không complete/unlock segment hoặc thay đổi lesson completion.

Dictation là activity trong Player, deterministic hoàn toàn tại Spring Boot: answer chỉ Simplified Hanzi, normalize Unicode/whitespace/punctuation; pinyin và Traditional Hanzi không tương đương. Sau normalization, chỉ exact match nhận `overallScore`/`accuracyPercent` bằng 100; mọi khác biệt character hoặc thứ tự nhận 0, không dùng character edit distance. Response không trả per-character analysis hoặc full expected answer; retake tạo owned history record mới và chỉ nâng best score khi cao hơn. F07 không gọi `ai-service` và không tiêu thụ AI allowance.

Shadowing là màn hình riêng, chỉ mở từ Player với selected context được giữ; future locked segment bị từ chối. F08 stream audio đã validate qua private `ai-service`, không gửi storage credential/object key/signed URL và không persist/expose speech transcript. Opening screen, preparing/uploading recording hoặc view existing result không tốn quota. Failure sau reserve nhưng không tạo valid result phải map về một `FAILED_REFUNDED` allowance outcome, không lộ provider detail.

## 8. Backend endpoint registry — Dictionary, vocabulary, SRS và progress

| API key frontend | Method + backend URL | Request/query | `data` tối thiểu | Access / rule | Status |
| --- | --- | --- | --- | --- | --- |
| `dictionary.search` | `GET /dictionary` | `query` bắt buộc, `hskLevel?`, pagination | published matching entry summaries | Public; Simplified/Traditional Hanzi, pinyin bỏ tone, Vietnamese meaning; bounded results | MVP |
| `dictionary.getById` | `GET /dictionary/{dictionaryEntryId}` | Path UUID | Simplified/available Traditional Hanzi, pinyin, Vietnamese meanings, examples; only published/available assets | Public published detail | MVP |
| `savedWords.create` | `POST /saved-words` | `{ dictionaryEntryId, personalNote? }` | saved word + schedule summary | Learner; plain-text note tối đa 500; idempotent theo learner + entry | MVP |
| `savedWords.list` | `GET /saved-words` | `status?`, `dueOnly?`, pagination | own words + next review/unavailable projection | Learner owner; withdrawn dictionary detail bị giữ lại | MVP |
| `savedWords.update` | `PATCH /saved-words/{savedWordId}` | `{ personalNote?, status? }` | updated word | Learner owner; plain text ≤500, server allowlist status | MVP |
| `savedWords.delete` | `DELETE /saved-words/{savedWordId}` | Path UUID | safe removed result | Learner owner; không xóa shared entry, suspend schedule/history | MVP |
| `reviews.listDue` | `GET /reviews/due` | — | tối đa 20 due saved-word cards + schedule version | Learner owner; dueAt cũ nhất trước, load batch mới sau khi xong queue | MVP |
| `reviews.submit` | `POST /reviews/{srsScheduleId}/submit` | `{ rating: "AGAIN" | "HARD" | "GOOD" | "EASY", clientReviewId, expectedScheduleVersion }` | authoritative updated schedule/next due | Owner; same logical retry trả original, stale khác trả latest schedule không tạo event | MVP |
| `progress.overview` | `GET /progress/overview` | `from?`, `to?` | total practice, completed lessons, due count, latest progress | Learner owner | MVP |

`query` dictionary phải trim và có giới hạn độ dài. Saved word tham chiếu entry đã unpublished vẫn giữ record/note của owner nhưng hiện unavailable, không lộ withdrawn detail. Save lại entry đã remove phải restore đúng `savedWordId`, personal note và schedule cũ; không tạo duplicate hay reset history.

Lần save đầu tiên tạo hoặc reuse đúng một `LEARNING` schedule due ngay. Initial review: AGAIN 10 phút, HARD 1 ngày, GOOD 3 ngày, EASY 7 ngày. Lần sau: AGAIN 10 phút và ease −0.2; HARD interval ×1.2 và ease −0.15; GOOD interval × prior ease; EASY interval × (prior ease +0.15); ease luôn 1.3–2.5. Server tính/persist toàn bộ interval/ease/next due và immutable review event. React không gửi score, interval, ease, dueAt hoặc schedule state để quyết định kết quả. API pagination không bao giờ trả toàn bộ attempt/chat/history.

## 9. Backend endpoint registry — AI Buddy

| API key frontend | Method + backend URL | Request | `data` tối thiểu | Access / rule | Status |
| --- | --- | --- | --- | --- | --- |
| `aiConversations.list` | `GET /ai-conversations` | pagination | own conversation summaries | Learner owner | MVP |
| `aiConversations.create` | `POST /ai-conversations` | `{ scenarioCode }` | `{ aiConversationId, scenarioCode, title, createdAt }` | Learner; scenario bắt buộc/immutable: `DAILY_CONVERSATION`, `VOCABULARY_GRAMMAR`, `ROLE_PLAY`; create không tốn allowance | MVP |
| `aiConversations.get` | `GET /ai-conversations/{aiConversationId}` | Path UUID, message pagination | own conversation/messages | Learner owner | MVP |
| `aiConversations.rename` | `PATCH /ai-conversations/{aiConversationId}` | `{ title }` | updated conversation | Learner owner; không đổi scenario | MVP |
| `aiConversations.delete` | `DELETE /ai-conversations/{aiConversationId}` | Path UUID | `200` | Learner owner; hide immediately, hard-delete workflow when implemented | MVP |
| `aiMessages.create` | `POST /ai-conversations/{aiConversationId}/messages` | `{ content, clientRequestId }` | learner message + schema-validated assistant reply + safe allowance summary | Learner owner; plain text ≤1,000, scenario/safety/rate/quota/idempotency trước internal call | MVP |

Không có endpoint frontend nào nhận provider prompt/API key. AI Buddy chỉ hỗ trợ Chinese learning trong selected scenario, không là general chat. Rich text, attachment, unsafe hoặc out-of-scenario message bị reject trước ai-service call, allowance reservation và persistence: không có learner/assistant message hay allowance event.

Request hợp lệ gửi private service chỉ scenario, current validated message và tối đa 10 `COMPLETE` message gần nhất của cùng conversation. Persisted assistant reply phải có Chinese response, Vietnamese explanation ngắn và tối đa một next-practice suggestion. Cùng `clientRequestId` với cùng immutable fingerprint reuse result; request ID bị tái sử dụng cho activity khác/fingerprint khác trả `409 IDEMPOTENCY_CONFLICT` trước AI call/quota mutation. `429 AI_QUOTA_EXCEEDED` được kiểm tra trước private call; MVP chỉ hiện quota state, không upgrade journey.

### F03 allowance contract áp dụng cho F08 và F11

Mỗi eligible learner tự có đúng một Free entitlement hiện hành: 30 AI allowance unit cho mỗi rolling cycle 30 ngày. Chỉ hai action tiêu thụ một unit: actual Shadowing pronunciation assessment và learner AI Buddy message yêu cầu assistant reply. Create/rename/delete conversation, mở Shadowing, upload/preparing recording và xem result không tiêu thụ quota.

Spring Boot lock entitlement rồi reserve/reuse immutable AI usage event trước private call. Cùng `clientRequestId` với cùng server-computed fingerprint reuse event/result; cùng ID khác fingerprint/AI activity bị chặn an toàn trước AI call và quota mutation. Timeout, unavailable provider/service, malformed schema hay safety failure xảy ra sau reserve mà không sinh valid result phải refund đúng một unit với `FAILED_REFUNDED`. Lock/unlock account không reset cycle/quota; MVP không có billing, Premium activation, upgrade journey hay manual plan/quota API.

## 10. Private Spring Boot → ai-service contract — không phải public API

Hai route sau không có prefix `/api/v1`, không có browser caller và không xuất hiện trong frontend API client. Đây là private-network-only contract, versioned, typed và schema-validated; public error envelope chỉ do Spring Boot tạo.

| Feature | Internal route | Caller / input tối thiểu | Output được phép | Không được làm |
| --- | --- | --- | --- | --- |
| F11 AI Buddy | `POST /internal/v1/ai-buddy/respond` | Chỉ Spring Boot sau HMAC hoặc mTLS + replay protection; correlation/request ID, capability, scenario, current validated message, tối đa 10 COMPLETE message cùng conversation | Schema-validated Chinese reply, Vietnamese explanation, tối đa 1 suggestion hoặc safe internal failure | Không browser JWT/provider secret; không persist chat; không quyết ownership/quota/idempotency/progress |
| F08 Shadowing | `POST /internal/v1/shadowing/assess` | Chỉ Spring Boot sau HMAC hoặc mTLS + replay protection; correlation/request ID, minimal validated context và audio đã validate stream | Typed speech-engine assessment candidate/feedback hoặc safe internal failure | Không storage key/credential/signed URL; không persist transcript/recording; không quyết allowance, ownership hay progress |

`ai-service` là private Node.js + TypeScript Mastra boundary, stateless với learner data. Mastra Memory, Studio/default agent/workflow route và provider endpoints không được public. Dedicated speech engine tạo tín hiệu pronunciation; Spring Boot schema-validates result, quyết định approved outcome, persist và map failure thành public envelope/refund. Dictation không có internal ai-service route.

## 11. Backend endpoint registry — Admin

`POST`/`PATCH` content và mọi action admin đều yêu cầu server-side role `ADMIN`. Mọi create/update/publication/approval/unpublish/archive mutation gửi `expectedVersion`; state stale trả `409 STATE_CONFLICT`, không thay đổi content và cho Admin reload. MVP chỉ có content `FREE`.

| API key frontend | Method + backend URL | Request chính | Access | Status |
| --- | --- | --- | --- | --- |
| `admin.topics.create` | `POST /topics` | title, slug, description, hskLevel, sortOrder, expectedVersion when applicable | `ADMIN` | MVP |
| `admin.topics.update` | `PATCH /topics/{topicId}` | editable topic fields, expectedVersion | `ADMIN` | MVP |
| `admin.topics.publish` | `POST /topics/{topicId}/publish` | expectedVersion | `ADMIN`; lifecycle validation | MVP |
| `admin.topics.unpublish` | `POST /topics/{topicId}/unpublish` | expectedVersion | `ADMIN`; lifecycle validation | MVP |
| `admin.topics.archive` | `POST /topics/{topicId}/archive` | expectedVersion | `ADMIN`; final, cannot restore/republish | MVP |
| `admin.lessons.create` | `POST /lessons` | topicId, title, slug, Free content fields, expectedVersion when applicable | `ADMIN` | MVP |
| `admin.lessons.update` | `PATCH /lessons/{lessonId}` | editable lesson fields, expectedVersion | `ADMIN` | MVP |
| `admin.lessons.publish` | `POST /lessons/{lessonId}/publish` | expectedVersion | `ADMIN`; parent topic published, ≥1 published segment, all referenced media APPROVED | MVP |
| `admin.lessons.unpublish` | `POST /lessons/{lessonId}/unpublish` | expectedVersion | `ADMIN`; explicit command before changing learner-visible content | MVP |
| `admin.lessons.archive` | `POST /lessons/{lessonId}/archive` | expectedVersion | `ADMIN`; final, cannot restore/republish | MVP |
| `admin.segments.create` | `POST /segments` | lessonId, mediaAssetId, sequenceNo, transcript/timing, expectedVersion when applicable | `ADMIN` | MVP |
| `admin.segments.update` | `PATCH /segments/{segmentId}` | editable segment fields, expectedVersion | `ADMIN` | MVP |
| `admin.segments.publish` | `POST /segments/{segmentId}/publish` | expectedVersion | `ADMIN`; lifecycle validation | MVP |
| `admin.segments.unpublish` | `POST /segments/{segmentId}/unpublish` | expectedVersion | `ADMIN`; lifecycle validation | MVP |
| `admin.segments.archive` | `POST /segments/{segmentId}/archive` | expectedVersion | `ADMIN`; final, cannot restore/republish | MVP |
| `admin.media.create` | `POST /media` | multipart file or allowlisted provider metadata, expectedVersion when applicable | `ADMIN`; scan/approval first | MVP |
| `admin.media.update` | `PATCH /media/{mediaAssetId}` | allowed metadata only, expectedVersion | `ADMIN` | MVP |
| `admin.users.list` | `GET /users?page={page}&size={size}` | zero-based page; `size` 1–50 | `ADMIN`; permitted `accountName`, UUID, lifecycle state and active ADMIN role; no email/other-profile/session/entitlement/quota/learner data | MVP |
| `admin.accounts.lookup` | `GET /users/{userId}/roles` | selected user UUID | `ADMIN`; permitted `accountName`, UUID, minimal role + locked/unlocked projection; no other profile/learner data | MVP |
| `admin.users.grantRole` | `POST /users/{userId}/roles` | `{ role: "ADMIN" }` | `ADMIN`; target khác actor; audit + invalidate target sessions | MVP |
| `admin.users.revokeAdmin` | `DELETE /users/{userId}/roles/ADMIN` | — | `ADMIN`; cấm self/final Admin removal; audit + invalidate target sessions | MVP |
| `admin.users.lock` | `POST /users/{userId}/lock` | `{ reason: "SECURITY" | "POLICY" | "USER_REQUEST" | "OTHER", note? }` | `ADMIN`; target khác actor; `OTHER` cần safe note; audit + revoke active access | MVP |
| `admin.users.unlock` | `POST /users/{userId}/unlock` | `{ reason: "SECURITY" | "POLICY" | "USER_REQUEST" | "OTHER", note? }` | `ADMIN`; target khác actor; `OTHER` cần safe note; audit, learner sign in lại | MVP |

Admin write responses phải trả state server mới nhất. User Management is server-paginated and returns the permitted `accountName`, UUID, lifecycle state and ADMIN role; a selected UUID is then used for the protected detail/command routes. `accountName` is the owner-set name or “Chưa đặt tên”, never email. The directory has no search and no other learner-private data. Invalid, unavailable hoặc unmanageable target trả safe result, không tiết lộ account/profile existence. Sau role hoặc lock change, target sessions bị invalidated. Không có Admin endpoint nào để grant/revoke entitlement/quota, hoặc browse attempts, saved words, recordings, progress hay conversations của learner.

### F12 AI Operations (ADMIN only)

| API key frontend | Method + backend URL | Request chính | Access | Status |
| --- | --- | --- | --- | --- |
| `adminAi.plans.list` | `GET /admin/ai/plans` | — | `ADMIN`; catalogue projections only | MVP |
| `adminAi.plans.history` | `GET /admin/ai/plans/{planCode}/revisions` | page, size | `ADMIN`; immutable safe history | MVP |
| `adminAi.plans.publish` | `POST /admin/ai/plans/{planCode}/revisions` | expectedCurrentVersion, reason, complete policy snapshot | `ADMIN`; prospective policy only | MVP |
| `adminAi.plans.retire` | `POST /admin/ai/plans/{planCode}/retire` | expectedCurrentVersion, reason | `ADMIN`; cannot retire final Free policy | MVP |
| `adminAi.usage.report` | `GET /admin/ai/usage-report` | bounded UTC range, optional plan/policy/capability | `ADMIN`; aggregate-only and max 50 days | MVP |
| `adminAi.monitoring.*` | `/admin/ai/monitoring-rules`, `/admin/ai/monitoring-alerts`, `/admin/ai/audit-events` | validated rule/acknowledgement, pagination | `ADMIN`; dashboard alerts only | MVP |

F12 never returns an AI usage event, learner identifier, learner content, provider payload, credential or diagnostic. Policy changes are immutable and apply only to new eligibility or a later server-owned allowance-cycle boundary; no F12 route grants, resets, reprices or revokes an individual entitlement.

## 12. Privacy endpoints — Phase 2

Những route sau đã được nêu rõ trong `CLAUDE.md`, nhưng chỉ mở khi schema privacy Phase 2 và legal review hoàn tất.

| API key frontend | Method + backend URL | Contract ngắn | Status |
| --- | --- | --- | --- |
| `privacy.get` | `GET /me/privacy` | consent, retention schedule, active sharing grant, pending export/deletion | PHASE 2 / LOCKED |
| `privacy.grantConsent` | `PUT /me/consents/{purpose}` | explicit grant theo policy version | PHASE 2 / LOCKED |
| `privacy.withdrawConsent` | `DELETE /me/consents/{purpose}` | withdrawal + queue cleanup; required service purpose trả `409` | PHASE 2 / LOCKED |
| `privacy.requestRestriction` | `POST /me/processing-restrictions` | request restriction optional processing, `202` | PHASE 2 / LOCKED |
| `exports.create` | `POST /me/exports` | recent re-auth, create encrypted export, `202` | PHASE 2 / LOCKED |
| `exports.get` | `GET /me/exports/{exportId}` | owner + recent re-auth; one-time 24h download link when ready | PHASE 2 / LOCKED |
| `privacy.deleteConversation` | `DELETE /me/conversations/{conversationId}` | hide immediately, queue hard delete; absent/unowned `404` | PHASE 2 / LOCKED |
| `privacy.deleteRecording` | `DELETE /me/recordings/{recordingId}` | hide immediately, delete object/key/cache/CDN | PHASE 2 / LOCKED |
| `deletionRequests.create` | `POST /me/deletion-requests` | recent re-auth, revoke sessions, 30-day cancellation window | PHASE 2 / LOCKED |
| `deletionRequests.cancel` | `DELETE /me/deletion-requests/{requestId}` | cancel only in window + recent re-auth | PHASE 2 / LOCKED |

## 13. Kiểm tra trước khi merge API change

- [ ] Frontend route và API key có trong registry; component không chứa raw backend URL.
- [ ] Backend URL có `/api/v1`, segment `kebab-case`, resource noun; action chỉ dùng cho domain command.
- [ ] Request DTO dùng Jakarta Validation; response là typed DTO + standard envelope, không trả entity.
- [ ] Collection phân trang; `meta` nhất quán.
- [ ] Ownership, account lock, `ADMIN`, content availability, Free access, AI quota/rate limit, idempotency và version conflict có test success/forbidden/error.
- [ ] Access/refresh token, cookie, CSRF và session revoke tuân JWT/session policy; không token nào vào browser storage/log.
- [ ] F08/F11 private contract có test valid/invalid/unsafe/replay, malformed output, timeout và failure/refund; browser không thể gọi ai-service.
- [ ] Swagger/OpenAPI, frontend API client theo contract và file này cùng được cập nhật.

## 14. Quy ước đặt tên JavaScript (tham chiếu)

```js
// src/api/ hoặc src/lib/apiClient.js là nơi duy nhất ghép VITE_API_BASE_URL + path.
api.auth.login(request)
api.lessons.getPlayback(lessonId)
api.lessonProgress.sendPlaybackEvent(lessonId, request)
api.dictation.submit(dictationAttemptId, request)
api.shadowing.assess(request)
api.reviews.submit(srsScheduleId, request)
api.aiConversations.createMessage(aiConversationId, request)
api.admin.users.lock(userId, request)
```

Path params dùng tên ở registry (`lessonId`, `segmentId`, `srsScheduleId`, ...), không dùng mơ hồ như `id`. DTO JSON dùng camelCase; bảng PostgreSQL dùng snake_case. Đây là ranh giới cố định giúp React, Spring controller, service và database không bị lẫn tên/URL.
