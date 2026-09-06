# S Chinese — Dashboard Design System

> **Nguồn tham chiếu:** giao diện học viên tại trang Shadowing của S Chinese và ảnh desktop được cung cấp, kiểm tra ngày 2026-09-03.
>
> **Phạm vi:** design system cho các màn hình ứng dụng sau khi đăng nhập: Trang chủ, Dictation, Shadowing, Luyện nói, Luyện từ vựng, Video của tôi và các màn hình thư viện. Landing page marketing có thể dùng bố cục khác, nhưng không được thay thế app shell dưới đây.

## 1. Tinh thần giao diện

S Chinese là một **study dashboard tối, ấm và tập trung vào nội dung**. Nền gần đen pha rượu vang tạo cảm giác yên tĩnh khi học lâu; hồng fuchsia là tín hiệu hành động, không phải nền trang trí. Nội dung bài học — ảnh video, chữ Hán, trạng thái HSK, thời lượng và tiến độ — phải dễ quét trong một nhịp nhìn.

Đây không phải landing page tối giản và cũng không phải bảng quản trị lạnh:

- Sidebar giữ nguyên ngữ cảnh học tập và các công cụ luôn sẵn sàng.
- Header cho biết người học đang ở kỹ năng nào và tiến độ tổng quan.
- Bộ lọc giúp duyệt thư viện nhanh mà không che nội dung.
- Bài học xuất hiện thành các hàng ngang có thể cuộn; ảnh thumbnail dẫn mắt, dữ liệu học tập chỉ đóng vai trò phụ trợ.
- Bề mặt tối được phân lớp bằng chênh lệch màu và viền mảnh, không bằng shadow nặng hay gradient.

Mặc định phải khớp theme trong ảnh: **Hồng tối (dark-pink)**. Người học có thể đổi theme; mọi component luôn lấy màu từ semantic token, không dùng màu hồng hard-code.

## 2. Cấu trúc ứng dụng

~~~text
App shell (min-height: 100dvh)
├── Sidebar desktop / navigation drawer mobile
│   ├── Brand + nút thu gọn
│   ├── Nhóm điều hướng
│   ├── Điều khiển theme
│   └── Hồ sơ người học
└── Main workspace
    ├── Workspace header (biểu tượng, tiêu đề, số liệu, hành động)
    ├── Filter dock (chủ đề và cấp độ)
    └── Scrollable lesson feed
        ├── Hàng “Bài học mới”
        ├── Các hàng theo chủ đề
        └── Trạng thái rỗng / đang tải / lỗi
~~~

Trên desktop, sidebar là cột cố định bên trái rộng 320px. Phần workspace cuộn dọc độc lập, còn từng lesson row có thể cuộn ngang. Không đặt nội dung chính trong một card khổng lồ; nền workspace là nền ứng dụng.

## 3. Foundations

### 3.1. Màu semantic — dark-pink mặc định

Các giá trị dưới đây là HSL để dễ duy trì cùng một hệ token ở tất cả theme. Implementation phải dùng biến CSS semantic thay vì màu cứng.

| Token | HSL dark-pink | Vai trò |
| --- | --- | --- |
| {colors.background} | 330 40% 5% | Nền app và vùng cuộn chính, gần đen pha burgundy |
| {colors.foreground} | 330 20% 98% | Chữ và icon chính trên nền tối |
| {colors.card} | 330 40% 8% | Thân lesson card và các bề mặt nổi nhẹ |
| {colors.popover} | 330 40% 8% | Menu, theme picker, tooltip, dialog |
| {colors.primary} | 330 80% 60% | Hành động chính, chip đang chọn, link hành động |
| {colors.primary-foreground} | 0 0% 100% | Chữ/icon trên primary |
| {colors.secondary} | 330 20% 15% | Nav active, badge trung tính, ô điều khiển tĩnh |
| {colors.secondary-foreground} | 330 20% 98% | Chữ trên secondary |
| {colors.muted} | 330 30% 15% | Bề mặt ít quan trọng, placeholder |
| {colors.muted-foreground} | 330 10% 65% | Mô tả, số lượng, icon không active |
| {colors.accent} | 330 30% 15% | Hover/focus background cho item không active |
| {colors.border} | 330 20% 15% | Divider và viền card 1px |
| {colors.input} | 330 20% 15% | Viền input |
| {colors.ring} | 330 80% 60% | Focus ring |
| {colors.success} | 152 55% 38% | Hoàn thành, đúng, tiến độ tích cực |
| {colors.warning} | 38 85% 45% | Cảnh báo, HSK trung cấp, chú ý |
| {colors.info} | 210 75% 50% | Thông tin, HSK 3 |
| {colors.destructive} | 0 63% 50% | Xóa, lỗi, hành động phá hủy |

**Phân lớp tối.** Background là vùng sâu nhất. Header, filter dock và sidebar vẫn nằm trên nền tối, tách nhau bằng border thay vì nâng sáng quá nhiều. Chỉ nav đang chọn, popover, card và media overlay mới dùng surface rõ ràng hơn.

**Màu không được hard-code trong component.** Ví dụ badge completed dùng {colors.success}, link “Xem thêm” dùng {colors.primary}, phần mô tả dùng {colors.muted-foreground}. Nhờ vậy menu theme đổi được toàn bộ UI đúng cách.

### 3.2. Theme picker

Theme picker ở cuối sidebar là một menu nổi hẹp. Mỗi dòng có icon nhận diện, nhãn tiếng Việt và trạng thái chọn. Không hiển thị bảng màu lớn.

| Cặp theme | Hue chủ đạo | Token primary light / dark |
| --- | --- | --- |
| Sáng / Tối | grayscale | 0 0% 9% / 0 0% 98% |
| Tím sáng / Tím tối | 265 / 260 | 265 89% 65% |
| Hồng sáng / **Hồng tối** | 330 | 330 80% 60% |
| Xanh sáng / Xanh tối | 220 | 220 80% 50% / 220 80% 60% |
| Đỏ sáng / Đỏ tối | 355 / 0 | 355 78% 62% |

- Cặp theme màu dùng cùng cấu trúc semantic: nền tối ở lightness 5%, card 8%, secondary/muted 15%.
- Nút trigger hiển thị icon theme hiện tại và nằm ngay trên profile card.
- Popup mở về phía trên để không bị che bởi cạnh dưới viewport; rộng khoảng 190–210px, padding 6px, radius 10px, viền 1px {colors.border}.
- Danh sách trong ảnh là: Sáng, Tối, Tím Sáng, Tím Tối, Hồng Sáng, Hồng Tối, Xanh Sáng, Xanh Tối, Đỏ Sáng, Đỏ Tối.

### 3.3. Màu ngữ nghĩa cho HSK và media

HSK là metadata thị giác, không thay màu primary của ứng dụng.

| Nhóm | Badge | Cách dùng |
| --- | --- | --- |
| HSK 1–2 | xanh lá | Bài cơ bản |
| HSK 3 | xanh dương | Bài trung cấp thấp |
| HSK 4 | amber | Bài trung cấp |
| HSK 5 | cam | Bài cao cấp |
| HSK 6 | đỏ san hô | Bài thành thạo |

Badge phải có chữ trắng đậm và độ tương phản đủ cao. Duration overlay luôn là nền đen 70–80% opacity, chữ trắng, không phụ thuộc theme.

### 3.4. Typography

| Token | Font | Size / line-height | Weight | Dùng cho |
| --- | --- | --- | --- | --- |
| {typography.app-title} | Quicksand | 24px / 30px | 700 | “Luyện Shadowing”, tiêu đề workspace |
| {typography.section-title} | Quicksand | 22px / 28px | 700 | “Bài học mới”, tiêu đề nhóm |
| {typography.nav} | Quicksand | 18px / 24px | 600 | Item sidebar |
| {typography.card-title} | Quicksand + Hanzi fallback | 18px / 26px | 700 | Tên bài học, clamp hai dòng |
| {typography.body} | Quicksand | 16px / 24px | 500 | Chip, nút, nội dung thông thường |
| {typography.meta} | Quicksand | 14px / 20px | 500 | Số phân đoạn, mô tả, thống kê |
| {typography.label} | Quicksand | 12px / 16px | 700 | Nhãn sidebar, badge, uppercase label |
| {typography.metric} | Quicksand | 16px / 20px | 600 | Số liệu ở header |

- Font UI: Quicksand, Inter, Roboto, Noto Sans, system-ui, sans-serif.
- Chữ Hán: var(--font-hanzi), Noto Sans SC, PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif.
- Chỉ dùng Kaite, STKaiti, KaiTi, Noto Serif CJK SC, serif cho nội dung cần cảm giác viết tay, pinyin hoặc nội dung học chuyên biệt; không dùng cho navigation, button hay metric.
- Quicksand tạo nhịp chữ tròn, thân thiện. Dùng 500–700; tránh thin text trên nền tối.
- Chữ tiếng Việt dùng sentence case. Nhãn nhóm sidebar dùng chữ hoa, tracking 0.06–0.08em.

### 3.5. Spacing, kích thước và hình khối

| Token | Giá trị | Ứng dụng |
| --- | --- | --- |
| {space.1} | 4px | khoảng sát icon/text, metadata |
| {space.2} | 8px | badge, control compact |
| {space.3} | 12px | padding card nhỏ, khoảng icon |
| {space.4} | 16px | gap chuẩn giữa control |
| {space.5} | 20px | khoảng card body |
| {space.6} | 24px | padding sidebar/nav, gap section |
| {space.8} | 32px | gutter desktop nhỏ |
| {space.10} | 40px | padding workspace desktop |
| {space.12} | 48px | khoảng giữa lesson rows |

| Token radius | Giá trị | Ứng dụng |
| --- | --- | --- |
| {radius.sm} | 8px | badge, duration overlay, button nhỏ |
| {radius.md} | 10px | nav active, popover |
| {radius.lg} | 14px | lesson card và ảnh thumbnail |
| {radius.xl} | 16px | profile card, dialog |
| {radius.pill} | 9999px | filter chip, status pill |

Không sử dụng góc vuông sắc trong các thành phần tương tác. Không biến mọi phần tử thành pill: card và ảnh bài học luôn giữ radius lg, menu/nav dùng md.

### 3.6. Viền, shadow và motion

- Divider/card border: 1px solid {colors.border}; trên nền tối, viền phải rất kín đáo.
- Card không có shadow lớn. Có thể dùng 0 8px 24px rgb(0 0 0 / 0.12) cho popover hoặc lesson card khi hover, không dùng ở trạng thái mặc định.
- Ảnh thumbnail là nơi có độ tương phản mạnh nhất; overlay chỉ dùng để bảo vệ badge và thời lượng.
- Transition chuẩn: 150–200ms ease-out cho color, background, border và transform.
- Hover card: ảnh tăng scale tối đa 1.02 trong khung đã overflow hidden; card border sáng nhẹ. Không nâng card quá 2px.
- Active button/card: transform scale 0.98.
- Focus-visible: ring 2px {colors.ring}, offset 2px trên nền tối. Không bỏ outline.
- Respect prefers-reduced-motion: loại bỏ scale và transition không cần thiết.

## 4. App shell và Sidebar

### 4.1. Sidebar desktop

Sidebar rộng 320px, fixed/sticky ở trái, cao 100dvh. Có border phải 1px {colors.border}, nền {colors.background} và padding ngang 20–24px. Brand row cao khoảng 88px; không được cuộn cùng danh sách bài học.

**Brand row**

- Logo mark hồng ở trái, khoảng 40px.
- Wordmark “PCHINESE” viết hoa, Quicksand 20px/700, tracking rộng vừa phải.
- Nút collapse dạng icon ở mép phải, hit target tối thiểu 44 × 44px.

**Navigation groups**

- Nhãn nhóm: 12px, 700, uppercase, {colors.muted-foreground}; có dashed/soft divider kéo về bên phải.
- Các nhóm chính: Tổng quan, Luyện tập, Thư viện và các tool phụ trợ.
- Các item nhìn thấy trong app shell gồm Trang chủ, Dictation, Shadowing, Luyện nói, Luyện từ vựng và Video của tôi. Có thể bổ sung item mới, nhưng phải giữ cùng grammar icon + label.
- Mỗi item cao 52px, padding ngang 16–20px, gap icon/text 12px. Icon outline 24px.
- Item active dùng {colors.secondary}, foreground chính và radius md. Không dùng primary làm nguyên một background nav; hồng chỉ dành cho điểm nhấn nhỏ hoặc trạng thái chọn rõ ràng.
- Hover của item inactive dùng {colors.accent}; icon và chữ thay foreground chính.

**Vùng đáy**

- Theme trigger nằm trên profile card.
- Profile card là card bo xl với avatar tròn 40–44px, tên một dòng ellipsis, mũi tên mở menu và thông tin plan nhỏ (“Miễn phí” hoặc Premium).
- Các quick action nhỏ, ví dụ theme và ngôn ngữ, giữ icon button 36–40px và tooltip có nhãn.

### 4.2. Workspace

- Vùng chính có min-width 0, nền {colors.background} và vertical scroll.
- Header/filter nằm sát đầu workspace, không phủ sidebar.
- Desktop content padding: 40px ngang, 28px đầu trang; desktop rất rộng có thể tăng đến 42–48px nhưng không đặt max-width hẹp làm mất carousel.
- Lesson feed giữ khoảng 40px từ filter dock; mỗi row cách row kế tiếp 44–52px.
- Chỉ vùng nội dung chính cuộn dọc. Sidebar và header còn nhìn thấy trong lúc người học duyệt danh sách.

## 5. Workspace header và filter dock

### 5.1. Workspace header

Workspace header cao tối thiểu 88px, có divider đáy. Bên trái là icon kỹ năng trong ô sáng, sau đó là title/subtitle; bên phải là metrics và action.

| Thành phần | Quy cách |
| --- | --- |
| Skill icon tile | 44 × 44px, nền sáng gần trắng, icon tối 22–24px, radius 12px. Ví dụ Shadowing dùng microphone. |
| Tiêu đề | {typography.app-title}, ví dụ “Luyện Shadowing”. |
| Subtitle | {typography.meta}, {colors.muted-foreground}, ví dụ “Chọn chủ đề để luyện kỹ năng nói”. |
| Metrics | cụm ngang icon + số đậm + nhãn muted: đang học, đã hoàn thành, trung bình. Ngăn cách bằng divider dọc mềm. |
| Utility action | button outline tối, icon grid và nhãn “Xem chủ đề”; cao khoảng 40px, radius 8px. |

Metrics chỉ hiển thị dữ liệu thật. Khi chưa có dữ liệu, giữ 0 rõ ràng thay vì placeholder hoặc skeleton vô thời hạn.

### 5.2. Filter dock

Filter dock nằm ngay dưới workspace header và kết thúc bằng divider. Ở desktop, có hai hàng:

1. **Chủ đề:** bắt đầu bằng chip primary “Tất cả”, theo sau là các topic text-chip có count nhỏ muted; ví dụ Chinese Listening Hub, Công nghệ – Khoa học máy tính – 科技, Everyday Chinese Stories, Gia đình – 家庭.
2. **Cấp độ:** chip primary “Tất cả cấp độ”, sau đó HSK 1 đến HSK 6.

- Hàng chip dùng flex-wrap trên desktop trung bình; không cắt cụt tên topic dài.
- Selected chip cao 46–48px, padding 14–20px, radius pill, nền {colors.primary}, chữ {colors.primary-foreground}.
- Unselected chip không có filled background; dùng body 16px/500, foreground giảm nhẹ, padding tương đương để vùng bấm vẫn đủ rộng.
- Count dùng 14px và đặt cách nhãn 6–8px; không cạnh tranh với label.
- Filter phải quản lý với aria-pressed/role phù hợp và có thể thao tác bằng keyboard.

## 6. Lesson feed

### 6.1. Lesson row

Lesson row có header ngang và một rail card:

- Header: section title bên trái, optional count badge trung tính sau title, action link “Xem thêm” bên phải.
- Rail: display flex, gap 16px, overflow-x auto, scroll snap theo card. Ẩn scrollbar chỉ khi vẫn còn affordance rõ ràng; trên desktop có thể hiện arrow circle hai mép rail khi còn nội dung.
- Không nhét rail vào grid co card quá hẹp. Card giữ chiều rộng tối thiểu để thumbnail và chữ Hán đọc được.
- Row đầu tiên là “Bài học mới”. Các row sau theo topic, ví dụ “Công nghệ – Khoa học máy tính – 科技”.
- Khi một topic không có bài, để empty state trong vùng row; không loại bỏ title/filter khiến người học tưởng dữ liệu chưa tải.

### 6.2. Lesson card

Lesson card là một mục điều hướng đến bài học, không phải bộ điều khiển media.

| Phần | Quy cách |
| --- | --- |
| Card | Rộng 280–320px trên desktop, nền {colors.card}, border 1px {colors.border}, radius lg, overflow hidden. |
| Thumbnail | Tỉ lệ gần 16:9, cover crop; ảnh đủ sáng, không đặt text lớn của app chồng lên ảnh. |
| HSK badge | Góc trên-trái thumbnail, inset 10–12px, height 28–32px, radius sm, background theo HSK. |
| Duration | Góc dưới-phải thumbnail, icon clock + thời lượng, nền đen 70–80%, radius sm, 14px/600. |
| Card body | Padding 16px, min-height đủ cho title hai dòng và metadata đáy. |
| Title | Chữ Hán hoặc song ngữ, {typography.card-title}, clamp 2 dòng. Không thay bằng ellipsis ngay sau một dòng. |
| Metadata | Ví dụ “12 phân đoạn”, {typography.meta}, {colors.muted-foreground}, nằm dưới title với khoảng 12–16px. |
| Progress tùy chọn | Khi đã bắt đầu: progress mảnh 4px và nhãn “Đang học”/phần trăm; trạng thái không bắt đầu không có progress giả. |

Favorite, archive hoặc menu ba chấm chỉ xuất hiện khi hover/focus hoặc khi card nhận keyboard focus. Những control đó không được che title, HSK hay duration.

### 6.3. Topic card và state nội dung

- Topic card dùng cùng surface/radius lesson card, nhưng ảnh/cover minh họa topic và có title, số video, completed count, progress; không dùng thay lesson card ở rail “Bài học mới”.
- Loading: skeleton giữ đúng kích thước card, gồm block thumbnail và hai dòng text; animation shimmer rất nhẹ.
- Empty: icon outline 40–48px, tiêu đề “Không tìm thấy bài học”, mô tả hướng dẫn đổi search/filter. Không dùng modal.
- Error: mô tả ngắn, action thử lại dạng outline; không tô toàn màn hình đỏ.

## 7. Component grammar

### 7.1. Buttons và icon buttons

| Component | Quy cách |
| --- | --- |
| Button primary | Nền {colors.primary}, foreground trắng, height 40–44px, radius 8px hoặc pill khi là filter, Quicksand 14–16px/700. Dùng cho CTA chính. |
| Button outline | Nền transparent, border {colors.border}, foreground chính. Dùng cho “Xem chủ đề”, retry, utility action. |
| Button ghost | Nền transparent, foreground muted; hover {colors.accent}. Dùng cho action thứ cấp. |
| Icon button | Ô 40–44px, icon 20–24px, radius 8px. Tooltip bắt buộc khi icon không có nhãn kề bên. |
| Carousel arrow | Circle 44px với border subtle và icon mũi tên; chỉ hiện khi rail có thể cuộn thêm. |

Button primary không nên xuất hiện dày đặc trên lesson feed. “Xem thêm” ưu tiên text action primary để page giữ sự yên tĩnh.

### 7.2. Status và badge

- Metric: icon outline 18px + number {typography.metric} + label meta. Completed dùng icon/check {colors.success}.
- Count badge: min-width 24px, height 24px, nền {colors.secondary}, foreground chính, radius 8px; dùng cho count cạnh section hoặc nav.
- HSK badge: chỉ cho HSK; không tái sử dụng cho tag hay trạng thái hoàn thành.
- Progress: track {colors.secondary}, fill {colors.primary} hoặc {colors.success} khi complete, cao 4–6px, radius pill.

### 7.3. Menu, popover và dialog

- Nền {colors.popover}, border 1px {colors.border}, radius md hoặc xl, shadow chỉ để phân tách khỏi workspace.
- Menu item cao ít nhất 40px, icon 18–20px, padding ngang 12px.
- Item destructive luôn có icon và {colors.destructive}, nhưng dialog xác nhận mới là nơi thao tác xóa cuối cùng.
- Close bằng Escape, click outside khi phù hợp và trả focus về trigger.

## 8. Responsive behavior

| Breakpoint | Hành vi |
| --- | --- |
| ≥ 1440px | Sidebar 320px; workspace padding 40–48px; lesson rail hiển thị khoảng 5 card hoặc hơn tùy viewport. |
| 1024–1439px | Sidebar 272–288px; metrics rút gọn label nếu cần; card vẫn tối thiểu 280px. |
| 768–1023px | Sidebar chuyển icon rail hẹp hoặc navigation drawer; header metric có thể xuống hàng; filter wrap rõ ràng. |
| 480–767px | Sidebar thành drawer; top bar có brand + menu; card rail rộng 78–86vw; workspace padding 16–20px. |
| < 480px | Metric chỉ còn number/icon quan trọng; header action chuyển icon button có accessible name; filter cuộn ngang theo từng hàng, không tạo chip nhỏ hơn touch target. |

- Target tương tác tối thiểu 44 × 44px trên touch.
- Không yêu cầu hover để truy cập action quan trọng; focus và menu button phải hoạt động với keyboard/touch.
- Thumbnail luôn giữ aspect ratio, title tiếp tục clamp 2 dòng.
- Người dùng có thể cuộn ngang một rail mà không vô tình kéo toàn trang ngang.
- Sidebar không được ép workspace overflow ngang; luôn có min-width 0 ở flex child.

## 9. Accessibility và nội dung

- Đạt contrast WCAG AA: foreground/muted text phải được kiểm tra trên từng theme, đặc biệt Hồng tối và Đỏ tối.
- Không dựa vào màu duy nhất cho HSK/progress/completion: HSK có label chữ, progress có số hoặc trạng thái kèm theo.
- Thumbnail phải có alt mô tả bài học; icon button có accessible name bằng tiếng Việt.
- Card là một target điều hướng duy nhất; các control phụ phải có focus order rõ ràng và không tạo nested button/link invalid.
- Hỗ trợ font fallback tiếng Trung và pinyin; không dùng ảnh để thay text tiêu đề, subtitle, duration hoặc tiến độ.
- Nội dung dynamic như count, completion, skeleton/error cần thông báo phù hợp cho screen reader mà không làm spam live region.

## 10. Do và Don't

### Do

- Dùng {colors.background} + {colors.card} + {colors.border} để tạo chiều sâu nhẹ trên dark UI.
- Đặt bài học mới và lesson rail gần đầu viewport; user mở app để học hoặc chọn bài, không phải xem dashboard decoration.
- Giữ badge HSK, duration và số phân đoạn nhất quán ở mọi lesson card.
- Dùng Hồng tối làm default theo ảnh tham chiếu, đồng thời bảo toàn đầy đủ semantic token khi đổi theme.
- Cho text tiếng Trung đủ khoảng thở và ưu tiên fallback Hanzi đúng nét.
- Duy trì sidebar, workspace header và filter grammar giống nhau giữa Dictation, Shadowing, Luyện nói và thư viện.

### Don't

- Không dùng bảng màu Apple cũ, nền trắng/parchment, SF Pro, hero product tile, CTA xanh hoặc navigation hai tầng kiểu Apple.
- Không phủ gradient trang trí lên nền/card; thương hiệu đến từ tone dark-pink, ảnh bài học và typography Quicksand.
- Không biến mọi navigation item thành pill hồng. Active nav dùng secondary; primary dành cho filter/CTA/link quan trọng.
- Không dùng shadow dày, glassmorphism sáng hoặc blur toàn màn hình.
- Không để card quá hẹp làm title tiếng Trung chỉ còn một ký tự mỗi dòng.
- Không đặt text dài trực tiếp lên thumbnail nếu không có overlay bảo đảm độ đọc.
- Không hard-code màu theme trong component; chỉ tham chiếu semantic CSS variable.

## 11. Checklist nghiệm thu thị giác

- [ ] Ở desktop rộng, sidebar 320px cố định; lesson workspace bắt đầu ngay sau divider dọc.
- [ ] Dark-pink hiển thị nền gần đen pha hồng, card đậm hơn nền vừa đủ, primary là hồng fuchsia.
- [ ] Header Shadowing có icon microphone sáng, title/subtitle, 3 cụm metric và action “Xem chủ đề”.
- [ ] Filter có hai hàng: chủ đề và HSK; chip đang chọn là primary pill.
- [ ] Hàng “Bài học mới” hiển thị lesson card thumbnail 16:9 với HSK góc trái và duration góc phải.
- [ ] Sidebar có nhóm nhãn chữ hoa, active state cho Shadowing, theme picker và profile card ở đáy.
- [ ] Đổi giữa 10 theme không làm mất contrast, spacing, trạng thái selected hay ý nghĩa success/warning/error.
- [ ] Mobile có drawer/top bar thay sidebar, đủ touch target và không gây page overflow ngang.
