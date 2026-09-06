# AGENT.md — Frontend Context for AI Agents

> **Phạm vi:** Chỉ áp dụng cho `frontend/`. Tài liệu gốc ở thư mục cha vẫn có hiệu lực; khi có xung đột, `../CONSTITUTION.md` và `../AGENT.md` được ưu tiên.

## 1. Đọc trước khi thay đổi

1. `../AGENT.md` — tech stack và quy tắc vận hành cấp dự án.
2. `../CONSTITUTION.md` — luật bắt buộc của dự án.
3. `../CLAUDE.md` — domain, API contract và các flow sản phẩm.
4. `DESIGN.md` — design system bắt buộc cho ứng dụng sau đăng nhập.
5. Feature spec liên quan tại `../specs/<feature>/spec.md`.

## 2. Tech stack và phạm vi sở hữu

- React 18 + Vite.
- Tailwind CSS 3.x; npm là package manager.
- Jest cho kiểm thử frontend. Không thêm thư viện UI, state-management hay router mới nếu feature spec/chủ dự án chưa phê duyệt.
- Frontend sở hữu route composition, UI state, accessibility, presentation formatting và typed API client.
- Backend là nguồn chuẩn cho authentication, authorization, ownership, entitlement, quota AI, state transition, scoring, SRS và dữ liệu tiến độ.

## 3. Quy tắc triển khai bắt buộc

- Không dùng `any`, class component, client-side database access, raw provider call hay secret trong bundle.
- Mọi gọi API đi qua typed client trong `src/api/`; không rải `fetch` tùy tiện trong component.
- Tôn trọng response envelope `{ success, data, error, meta }`, error code ổn định và metadata phân trang từ backend.
- Không tin hoặc tự gửi `isAdmin`, plan, score, quota hay quyền sở hữu như một nguồn quyền. Route guard và UI chỉ cải thiện trải nghiệm; backend luôn quyết định.
- Access token chỉ ở memory. Không lưu access/refresh token vào `localStorage`, `sessionStorage`, IndexedDB hay JavaScript-readable cookie. Xử lý refresh theo session contract trong `../CLAUDE.md`.
- Không gọi `AI Service` hoặc `Media Provider` trực tiếp từ browser. Không đưa API key, prompt hệ thống, signed URL bền vững hoặc credential vào code/UI log.
- Form validation ở client chỉ để UX; luôn hiển thị lỗi backend chuẩn và không thay thế validation server.
- Không render dữ liệu riêng tư, transcript, recording, chat hoặc lỗi provider vào console, analytics hay toast vượt quá dữ liệu cần thiết.

## 4. Quy tắc UI, theme và accessibility

- `DESIGN.md` là chuẩn visual cho authenticated app: app shell, sidebar/drawer, workspace, lesson rail và responsive behavior.
- Dùng semantic CSS token và Tailwind mapping; không hard-code màu theme trong component. Dark-pink là theme mặc định.
- Component tái sử dụng, không gắn với feature, đặt tại `src/components/`; feature-specific UI đặt tại `src/features/<feature>/`.
- Dùng `PascalCase` cho component, `camelCase` cho utility/hook; JSON/API field dùng camelCase.
- Bảo đảm keyboard navigation, focus-visible, accessible name cho icon button, contrast WCAG AA, target chạm tối thiểu 44×44px và `prefers-reduced-motion`.
- Mọi data state phải có loading, empty, error và retry phù hợp. Không dùng mock metric như dữ liệu thật.

## 5. Definition of Done cho frontend

- [ ] Đã xác định actor, route, API contract, trạng thái tải/lỗi/rỗng và quy tắc access liên quan.
- [ ] TypeScript, lint và format không lỗi; không có `any`, debug code hay TODO chưa xử lý.
- [ ] Typed API client, component/hook và UI state được cập nhật đồng bộ.
- [ ] Có Jest test phù hợp cho hành vi component/utility; E2E dùng `data-testid` ổn định khi cần.
- [ ] Không suy diễn hoặc thực thi quyền/quota/nghiệp vụ chỉ ở React.
- [ ] Giao diện tuân thủ `DESIGN.md`, responsive và accessible.
