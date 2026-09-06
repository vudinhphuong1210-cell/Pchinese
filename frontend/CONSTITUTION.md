# CONSTITUTION.md — Frontend Law

> Tài liệu này ràng buộc mọi thay đổi trong `frontend/`. `../CONSTITUTION.md` là luật cấp dự án và có quyền ưu tiên; thay đổi tài liệu này cần review của team frontend và không được làm suy yếu luật gốc.

## ARTICLE 1 — Stack và cấu trúc bất biến

- Dùng React 18, TypeScript strict mode, Vite, Tailwind CSS 3.x và npm.
- Không dùng class component hoặc `any`.
- Giữ cấu trúc theo feature; component chung không chứa nghiệp vụ feature và feature không gọi API ngoài typed client.
- Không thêm framework, state manager, design system hay dependency lớn nếu chưa có feature spec/phê duyệt.

## ARTICLE 2 — Backend là authority

- React không được quyết định hoặc thay thế authentication, `ADMIN`, ownership, entitlement, quota AI, score, SRS, state transition hay retention.
- Không dùng route visibility, local state, claim tự giải mã hay cờ từ client làm authorization.
- UI phải xử lý phản hồi chuẩn từ backend, bao gồm `401`, `403`, `404`, `409` và `429`, mà không tiết lộ dữ liệu private.

## ARTICLE 3 — Security và privacy

- Không commit secret, API key, `.env`, token, prompt hệ thống, raw recording hay dữ liệu learner vào source/log/test fixture không được phê duyệt.
- Không lưu token trong browser storage. Access token chỉ ở memory; refresh và CSRF theo contract backend.
- Không gọi AI/media provider trực tiếp từ browser; không cho client truyền arbitrary provider URL làm authority.
- Client validation chỉ phục vụ UX, không được thay validation backend. Hiển thị lỗi an toàn, không stack trace/provider internals.

## ARTICLE 4 — UX, design và accessibility

- `DESIGN.md` là chuẩn thiết kế authenticated app. Dùng semantic token, không hard-code theme color.
- UI phải có keyboard support, focus visible, accessible names, contrast WCAG AA, responsive layout và touch target tối thiểu 44×44px.
- Không dựa vào màu, hover hoặc animation để truyền thông tin quan trọng; tôn trọng `prefers-reduced-motion`.
- Loading, empty, error và retry là một phần của feature, không phải xử lý sau cùng.

## ARTICLE 5 — Chất lượng và review

- Không merge khi TypeScript, lint, format hoặc test liên quan còn lỗi.
- Test phải dùng stable `data-testid` cho E2E, không couple vào styling class hay AI output ngẫu nhiên.
- Mọi thay đổi API-facing phải cập nhật typed client và UI error states cùng lúc.
- Không duyệt thay đổi vi phạm `AGENT.md`, tài liệu này, design contract hay feature spec.
