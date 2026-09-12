# Feature Specification: F13 — Home Experience & Marketing Dashboard

**Feature Branch**: `[F13-home-marketing-dashboard]`

**Created**: 2026-09-12

**Status**: Implemented

**Input**: User description: "Thiết kế 1 giao diện trang home giống S Chinese (https://schinese.net/) dựa vào các file cấu hình frontend, luật lệ frontend, tạo đầy đủ spec và artifact giống các feature khác."

---

## 1. Executive Summary & Goals

Trang Home của **P Chinese** là cổng trải nghiệm chính (Main Portal & Study Hub) của toàn bộ nền tảng, kết hợp giữa **Dashboard học tập cá nhân hóa** cho học viên đã đăng nhập và **Landing page giới thiệu phương pháp thực chiến** cho khách vãng lai, lấy cảm hứng trực tiếp từ kiến trúc của [S Chinese (https://schinese.net/)](https://schinese.net/).

### Mục tiêu cốt lõi:
1. **Truyền tải 4 trụ cột học tập thực chiến**:
   - **Luyện Shadowing**: Luyện phản xạ nói, bắt chước ngữ điệu theo video YouTube bản xứ với phụ đề song ngữ, pinyin tức thì và chấm điểm phát âm AI.
   - **Luyện Dictation**: Nghe chép chính tả từng câu thoại, nhận diện lỗi sai tức thì, củng cố phản xạ âm thanh và từ vựng.
   - **AI Trợ Giảng (AI Learning Buddy)**: Trò chuyện và thực hành hội thoại theo kịch bản tình huống thực tế, phân tích ngữ pháp và gợi ý diễn đạt tự nhiên.
   - **Luyện từ vựng qua Game & Lặp lại ngắt quãng (SRS)**: Flashcards thông minh theo thuật toán SM-2 kết hợp các trò chơi tương tác (Nối từ, Trắc nghiệm, Kéo thả, Bắn từ).
2. **Xây dựng thói quen học tập hàng ngày (Habit Loop & Gamification)**:
   - Widget Điểm danh hàng ngày (Daily Streak Widget) 7 ngày trong tuần với hệ thống điểm thưởng XP, chuỗi ngày liên tiếp (flame icon 🔥), kỷ lục dài nhất.
3. **Trải nghiệm bài học thực tế ngay tại Trang chủ**:
   - Bộ lọc nhanh theo cấp độ HSK (HSK 1 → HSK 6) và Chủ đề (Giao tiếp, Du lịch, Phim ảnh, Âm nhạc, Kinh doanh, Podcast).
   - Lưới bài học video YouTube tuyển chọn hiển thị sắc nét với metadata chuẩn (thời lượng, số câu thoại, cấp độ, trạng thái Free/Premium).
4. **Minh bạch lộ trình, Bảng giá & Câu hỏi thường gặp**:
   - Bảng so sánh 2 cột quyền lợi chi tiết giữa gói **Free** và gói **Premium**.
   - Khu vực FAQ dạng Accordion giải đáp toàn bộ thắc mắc về phương pháp học, thiết bị, gói cước và hỗ trợ.
5. **Tuân thủ tuyệt đối Design System P Chinese**:
   - Tương thích 100% với 10 theme màu (mặc định: Dark Pink / Hồng tối burgundy `330 40% 5%` theo `frontend/DESIGN.md`), 100% sử dụng CSS semantic variables, Tailwind CSS 3.x, không hard-code mã màu.

---

## 2. Clarifications & Architecture Context

### Session 2026-09-12
- **Q**: Trang Home hiển thị thế nào cho Khách vãng lai (Guest) so với Học viên đã đăng nhập (Learner)?
  → **A**: Cùng một cấu trúc bố cục nhất quán (Hero, Streak, 4 Pillars, Lesson Rail, Pricing, FAQs). Với Guest, Hero hiển thị nút "Bắt đầu học ngay / Đăng ký", Streak widget ở chế độ preview mời đăng nhập; với Learner, Hero hiển thị "Tiếp tục bài học gần nhất", Streak widget cho phép bấm điểm danh nhận XP và ghi nhận tiến độ.
- **Q**: Nguồn dữ liệu bài học trên trang Home lấy từ đâu?
  → **A**: Tích hợp trực tiếp với API Catalog sẵn có (`/api/v1/topics` và `/api/v1/lessons`), hỗ trợ lọc realtime theo Topic và HSK.
- **Q**: Bảng quyền lợi Premium trên Home có xử lý thanh toán trực tiếp không?
  → **A**: Không trong MVP hiện tại. Nút CTA mở `PremiumUpgradeModal` để hiển thị entitlement và hạn mức thực do backend trả về; Premium và cổng thanh toán được ghi rõ là đang hoàn thiện.
- **Q**: Theme màu và phong cách thị giác áp dụng ra sao?
  → **A**: Dark-pink làm màu chủ đạo mặc định, sử dụng biến HSL token (`--background`, `--foreground`, `--primary`, `--card`, `--border`, v.v.), đảm bảo khi người dùng chuyển qua bất kỳ theme nào trong 10 theme (Tím, Xanh, Đỏ, Sáng, Tối), toàn bộ trang Home chuyển đổi màu tự động và hoàn hảo.

---

## 3. User Scenarios & Testing *(mandatory)*

### User Story 1 - Khám phá tổng quan & Bắt đầu học ngay từ Hero Section (Priority: P1)

Là một học viên (Guest hoặc Learner), tôi muốn nhìn thấy một Hero Section hấp dẫn, truyền cảm hứng với đầy đủ thông điệp giá trị, các điểm nổi bật và nút kêu gọi hành động (CTA) rõ ràng để tôi có thể nhanh chóng bắt đầu bài học hoặc đăng ký tài khoản.

**Why this priority**: Hero section quyết định ấn tượng đầu tiên (first impression), định vị giá trị khác biệt của P Chinese so với các app học tiếng Trung truyền thống.

**Independent Test**: Truy cập trang Home, toàn bộ nội dung Hero hiển thị sắc nét, các nút CTA "Bắt đầu học ngay", "Khám phá tính năng", "Luyện Shadowing" điều hướng mượt mà đến đúng khu vực/chức năng.

**Acceptance Scenarios**:
1. **Given** người dùng truy cập trang Home, **When** màn hình tải xong, **Then** họ thấy tiêu đề chính "Học tiếng Trung thú vị và hiệu quả hơn mỗi ngày", phụ đề chi tiết về Dictation, Shadowing, AI & Tra từ, cùng các badge tính năng nổi bật.
2. **Given** người dùng là khách vãng lai (Guest), **When** bấm "Bắt đầu học ngay", **Then** hệ thống mở modal/màn hình Đăng ký / Đăng nhập hoặc dẫn tới danh mục bài học trải nghiệm miễn phí.
3. **Given** người dùng đã đăng nhập, **When** bấm "Tiếp tục bài học", **Then** hệ thống dẫn trực tiếp vào bài học gần nhất hoặc bảng điều khiển học tập.

---

### User Story 2 - Điểm danh học tập hàng ngày & Tích lũy chuỗi Streak (Priority: P1)

Là một người học đã đăng nhập, tôi muốn xem trạng thái điểm danh trong tuần, chuỗi ngày liên tiếp (Streak flame) và thực hiện điểm danh hôm nay để nhận điểm thưởng XP và duy trì thói quen học tập mỗi ngày.

**Why this priority**: Daily Streak là tính năng giữ chân người học cốt lõi (retention & habit formation).

**Independent Test**: Người dùng bấm nút "Điểm danh hôm nay", hệ thống cập nhật trạng thái điểm danh trong ngày, tăng chuỗi streak, cộng điểm XP và hiển thị lời chúc động viên phù hợp.

**Acceptance Scenarios**:
1. **Given** người học chưa điểm danh hôm nay, **When** xem widget Streak trên Home, **Then** thấy nút "Điểm danh hôm nay! (+10 XP)" kèm trạng thái các ngày trong tuần (T2 đến CN).
2. **Given** người học bấm nút điểm danh, **When** hành động hoàn tất, **Then** hiển thị thông báo chúc mừng ("Tuyệt vời! +10 điểm hôm nay"), icon ngày hôm nay đổi thành dấu tích xanh/lửa cháy, nút chuyển sang "Bạn đã hoàn thành hôm nay! ✨".
3. **Given** người học đã điểm danh trước đó trong ngày, **When** vào lại trang Home, **Then** widget giữ nguyên trạng thái đã điểm danh và hiển thị lời nhắn "Hẹn gặp lại ngày mai! Giữ chuỗi điểm danh nhé 💪".
4. **Given** khách vãng lai (chưa đăng nhập), **When** xem widget Streak, **Then** widget hiển thị ở chế độ preview cùng lời kêu gọi đăng nhập để tích lũy chuỗi ngày học.

---

### User Story 3 - Tìm hiểu 4 Trụ cột phương pháp học tập thực chiến (Priority: P1)

Là một người học, tôi muốn xem chi tiết 4 phương pháp học tập chính (Shadowing, Dictation, AI Learning Buddy, Vocabulary Games & SRS) với mô tả trực quan, lợi ích cụ thể và nút trải nghiệm nhanh từng tính năng.

**Why this priority**: Giúp người học hiểu rõ phương pháp học thực chiến và cách ứng dụng giải quyết triệt để vấn đề "nghe không kịp, nói không chuẩn, nhanh quên từ vựng".

**Independent Test**: Mỗi card trụ cột hiển thị đầy đủ icon, minh họa giao diện, danh sách ưu điểm và nút điều hướng tương ứng (Luyện Shadowing → Catalog video; Luyện Dictation → Dictation Hub; AI Buddy → AI Chat; Từ vựng → Game Hub / SRS).

**Acceptance Scenarios**:
1. **Given** người dùng cuộn đến phần "Phương pháp học tập", **When** xem qua 4 khối tính năng, **Then** mỗi khối có tiêu đề rõ ràng, mô tả súc tích và badge nhận diện (ví dụ: "AI Powered", "Spaced Repetition", "Video YouTube").
2. **Given** người dùng bấm vào card "Luyện Shadowing", **When** kích hoạt, **Then** hệ thống chuyển sang khu vực danh mục bài học video kèm bộ lọc HSK.
3. **Given** người dùng bấm vào card "Trợ giảng AI", **When** kích hoạt, **Then** hệ thống mở tính năng AI Buddy trò chuyện hoặc giới thiệu các kịch bản hội thoại mẫu.
4. **Given** người dùng bấm vào card "Luyện từ vựng & Games", **When** kích hoạt, **Then** hệ thống mở kho trò chơi từ vựng (Matching Pairs, Trắc nghiệm, Bắn từ).

---

### User Story 4 - Duyệt thư viện bài học nổi bật theo chủ đề và HSK (Priority: P2)

Là một người học, tôi muốn duyệt các bài học video YouTube thực tế mới nhất và nổi bật nhất ngay trên trang Home, với khả năng lọc nhanh theo HSK 1–6 và Chủ đề để chọn bài học phù hợp với trình độ của mình.

**Why this priority**: Cung cấp nội dung thực tế ngay tại trang chủ để người học thấy ngay kho bài học phong phú và có thể bắt đầu học ngay lập tức.

**Independent Test**: Thay đổi chip lọc HSK hoặc Chủ đề trên trang Home, danh sách lesson card cập nhật tương ứng theo đúng tiêu chí lọc với thumbnail sắc nét, thời lượng, cấp độ và số câu thoại.

**Acceptance Scenarios**:
1. **Given** danh sách bài học trên Home, **When** người dùng bấm chọn chip "HSK 2", **Then** danh sách lọc lại chỉ hiển thị các bài học thuộc cấp độ HSK 2.
2. **Given** người dùng bấm vào một lesson card, **When** là bài học Free, **Then** mở trang chi tiết bài học / trình phát bài học; nếu chưa đăng nhập, hiển thị modal tóm tắt bài học và gợi ý đăng nhập.
3. **Given** danh sách hiển thị hơn 5 bài học, **When** người dùng bấm "Xem tất cả bài học", **Then** chuyển tiếp đến kho bài học Catalog đầy đủ.

---

### User Story 5 - Khám phá bảng quyền lợi gói Free vs Premium & FAQs (Priority: P2)

Là một người học, tôi muốn so sánh chi tiết quyền lợi giữa gói Miễn phí (Free) và gói Nâng cấp (Premium) cùng các câu hỏi thường gặp (FAQs) để đưa ra quyết định nâng cấp gói dịch vụ khi có nhu cầu học chuyên sâu.

**Why this priority**: Tăng tỷ lệ chuyển đổi (conversion rate) từ người dùng miễn phí sang gói trả phí một cách minh bạch và tự nhiên.

**Independent Test**: Bảng quyền lợi hiển thị đúng gói Free đang hoạt động (30 lượt AI theo chu kỳ) và Premium đang hoàn thiện; CTA mở modal entitlement hiện có; FAQ hỗ trợ đóng/mở bằng chuột và bàn phím.

**Acceptance Scenarios**:
1. **Given** người dùng cuộn đến bảng quyền lợi, **When** xem cột Free và Premium, **Then** thấy gói Free có 30 lượt AI mỗi chu kỳ và Premium có hạn mức cao hơn theo cấu hình khi phát hành, không có tuyên bố “không giới hạn” không đúng với entitlement policy.
2. **Given** người dùng bấm CTA Premium, **When** đang ở gói Free, **Then** mở modal entitlement và thông báo rõ Premium/cổng thanh toán chưa được kích hoạt.
3. **Given** người dùng bấm vào một câu hỏi trong mục FAQs, **When** bấm vào tiêu đề câu hỏi, **Then** nội dung câu trả lời mở ra trơn tru (accordion animation) và các câu hỏi khác tự động đóng hoặc giữ trạng thái độc lập.

---

### User Story 6 - Đổi giao diện linh hoạt (10 Theme Semantic) (Priority: P3)

Là một người học, tôi muốn trang Home tự động thích ứng với bất kỳ theme nào tôi chọn (Dark Pink, Dark Purple, Dark Blue, Light Pink, Light Blue, v.v.) mà không bị lỗi màu sắc hoặc vỡ bố cục.

**Why this priority**: Đảm bảo tính nhất quán của Design System và trải nghiệm học tập thoải mái trong môi trường ban ngày lẫn ban đêm.

**Independent Test**: Đổi theme qua Theme Picker ở Sidebar / Header, toàn bộ màu nền, text, viền card, badge, button trên trang Home đồng bộ thay đổi ngay lập tức.

**Acceptance Scenarios**:
1. **Given** người dùng đổi theme sang "Dark-Blue" hoặc "Light", **When** theme thay đổi, **Then** toàn bộ trang Home áp dụng đúng các biến màu HSL semantic, không có chữ bị chìm màu hoặc viền bị sai tương phản.

---

## 4. Edge Cases & Error Handling

- **Dữ liệu bài học rỗng hoặc lỗi mạng**: Hiển thị skeleton loading thanh lịch khi đang tải dữ liệu bài học; nếu API catalog gặp sự cố, hiển thị banner thông báo nhẹ nhàng kèm nút "Thử lại", không làm crash toàn trang.
- **Trạng thái tài khoản khác nhau**:
  - *Guest*: Không hiển thị điểm streak cá nhân mà hiển thị bản demo động viên đăng ký.
  - *Free Learner*: Hiển thị tiến độ thực tế, nhắc nhở hạn mức còn lại và nút nâng cấp tinh tế.
  - *Premium Learner*: Ẩn các banner mời mua gói, thay bằng badge "Premium Member" và hiển thị các lối tắt mở khóa toàn bộ bài học nâng cao.
- **Màn hình nhỏ (Mobile/Tablet Responsive)**:
  - Thanh streak chuyển thành dạng trượt ngang hoặc lưới thu gọn 7 cột vừa vặn màn hình điện thoại.
  - Lưới bài học tự động co giãn từ 5 cột (desktop) sang 2–3 cột (tablet) và 1–2 cột (mobile).
  - Bảng so sánh gói dịch vụ hiển thị dạng tab chuyển đổi Free / Premium trên mobile.
- **Tốc độ tải & Hiệu năng hình ảnh**: Tất cả thumbnail video dùng ảnh tối ưu kích thước kèm placeholder mờ khi tải, không gây giật khung hình (layout shift / CLS = 0).

---

## 5. Functional Requirements

- **FR-001**: Hệ thống MUST hiển thị Hero Section với tiêu đề, phụ đề, CTA chính ("Bắt đầu học ngay", "Luyện Shadowing"), và nhóm badge giá trị cốt lõi.
- **FR-002**: Hệ thống MUST tích hợp widget Điểm danh hàng ngày (Daily Streak) hiển thị 7 ngày trong tuần, chuỗi streak hiện tại, điểm XP và nút tương tác check-in.
- **FR-003**: Hệ thống MUST trình bày 4 khối phương pháp học tập thực chiến (Shadowing, Dictation, AI Buddy, Vocabulary Games & SRS) kèm nút điều hướng đến từng phân hệ chức năng tương ứng.
- **FR-004**: Hệ thống MUST hiển thị danh sách bài học tuyển chọn từ Catalog API với bộ lọc nhanh theo cấp độ HSK (HSK 1 đến HSK 6) và Chủ đề.
- **FR-005**: Hệ thống MUST tích hợp bảng so sánh tính năng Free vs Premium rõ ràng, minh bạch hạn mức và nút kích hoạt nâng cấp.
- **FR-006**: Hệ thống MUST cung cấp khu vực FAQ dạng Accordion giải đáp các thắc mắc học tập và thanh toán.
- **FR-007**: Hệ thống MUST chỉ hiển thị số liệu có nguồn xác minh: dữ liệu catalog từ API hoặc các khả năng sản phẩm đã cấu hình; không dùng số lượng học viên, lượt luyện hay đánh giá giả định.
- **FR-008**: Tất cả thành phần giao diện MUST tuân thủ tuyệt đối Design System P Chinese trong `frontend/DESIGN.md`, sử dụng 100% semantic CSS tokens và tương thích hoàn toàn 10 theme màu.
- **FR-009**: Hệ thống MUST hỗ trợ responsive hoàn chỉnh từ màn hình di động (360px) đến màn hình desktop siêu rộng (2560px).

---

## 6. Key Entities & UI Components Mapping

| Component | Trách nhiệm hiển thị | Dữ liệu đầu vào / Trạng thái |
| :--- | :--- | :--- |
| `HomeHeroSection` | Hero banner, tiêu đề, CTAs, value tags | Auth state (Guest vs Learner) |
| `DailyStreakWidget` | Lịch 7 ngày, số ngày streak, nút điểm danh | Streak count, check-in status, XP |
| `LearningPillarsSection` | 4 card phương pháp (Shadowing, Dictation, AI, SRS) | Navigation callbacks |
| `HomeLessonRail` | Lưới bài học nổi bật kèm bộ lọc HSK / Topic | Danh sách lessons từ Catalog API, active filter |
| `PricingComparisonSection`| Bảng so sánh 2 cột Free vs Premium, nút nâng cấp | User entitlement state, upgrade modal trigger |
| `FaqAccordionSection` | Danh sách câu hỏi thường gặp dạng accordion | Danh sách FAQ items, active accordion index |
| `CommunityStatsSection` | Thống kê số lượng bài học, từ vựng, học viên | Dữ liệu thống kê nền tảng |
| `HomeFooterCta` | Banner kêu gọi hành động cuối trang | Auth state, register/catalog links |

---

## 7. Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: 100% người dùng (cả Guest và Learner) có thể tải và xem toàn bộ các section trên Trang chủ với thời gian phản hồi FCP < 1.2 giây trên mạng tiêu chuẩn.
- **SC-002**: Người học có thể thực hiện thao tác điểm danh và nhận phản hồi trực quan trong vòng < 200 mili-giây.
- **SC-003**: 100% các thành phần trên trang Home thay đổi màu sắc chuẩn xác theo token CSS semantic khi chuyển đổi qua lại giữa 10 theme trong hệ thống, không có bất kỳ lỗi hiển thị tương phản nào.
- **SC-004**: Bộ lọc HSK và Chủ đề phản hồi tải dữ liệu bài học tức thì, không gây lỗi layout shift (CLS = 0).
- **SC-005**: 100% automated component unit tests và integration tests liên quan đến Home Page đều pass.
