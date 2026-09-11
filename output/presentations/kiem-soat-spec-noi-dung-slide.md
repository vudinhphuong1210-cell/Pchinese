# Kiểm soát spec trong phát triển phần mềm có AI hỗ trợ
## Nội dung thuyết trình và ghi chú bảo vệ chuyên sâu trên dự án schinese

**Thời lượng đề xuất:** 40 phút trình bày + 10–15 phút phản biện.  
**Cấu trúc:** 30 slide chính + 6 slide phụ lục.  
**Phạm vi:** Nội dung để tự thiết kế slide; không phải một bộ PowerPoint đã thiết kế.  
**Mốc đối chiếu:** working tree schinese ngày 11/09/2026. Trong tài liệu dự án, tên sản phẩm là Pchinese.

## Luận điểm xuyên suốt

> Kiểm soát spec là quản lý chuỗi từ nhu cầu, yêu cầu, quyết định kiến trúc đến mã nguồn và bằng chứng kiểm thử; đồng thời kiểm soát sự thay đổi của cả chuỗi. Mức độ kiểm soát phải được chứng minh bằng bằng chứng phù hợp với rủi ro.

Câu anh nên dùng khi mở bài:

> “Trong dự án này, tôi quan tâm đến việc một yêu cầu được hiểu đúng như thế nào, được thực thi ở đâu và được kiểm chứng bằng cách nào. Tôi dùng schinese để minh họa chuỗi đó, đồng thời chỉ rõ phần đã kiểm chứng và phần còn phải hoàn thiện.”

Bốn nội dung ban đầu của anh được giữ đầy đủ: hiểu spec; kiểm soát đầu vào và đầu ra; dựng spec; kiến trúc. Bổ sung ba phần cần thiết cho một buổi bảo vệ: quy mô và độ khó; bằng chứng và kiểm soát thay đổi; đo lường, giới hạn và lộ trình.

## Ba yêu cầu về cách thể hiện

| Yêu cầu | Cách thể hiện trong bài | Bằng chứng nên dùng |
| --- | --- | --- |
| Chỉn chu | Định nghĩa nhất quán, phân biệt hiện trạng với thiết kế, mỗi nhận định có nguồn và phạm vi | Mã yêu cầu, đường dẫn, phiên bản, kết quả test |
| Thể hiện cái khó | Đi vào quyền truy cập, nhiều trạng thái, retry, tranh chấp dữ liệu và đầu ra AI | Một luồng có cả thành công, lỗi, lặp và thao tác đồng thời |
| Thể hiện quy mô | Trình bày phụ thuộc giữa feature và tác động dây chuyền khi đổi yêu cầu | Feature map, ranh giới module, bản đồ thay đổi xuyên tầng |

Đây là ba yêu cầu về chất lượng lập luận. Chúng khác với ba mức độ chi tiết của spec ở slide 17.

## Table of contents

| Phần | Nội dung | Các mục | Slide | Thời gian |
| --- | --- | --- | --- | --- |
| Mở đầu | Chủ đề và mạch bảo vệ | Tiêu đề; mục lục | 01–02 | 1 phút |
| 1 | Hiểu spec và mục tiêu kiểm soát | 1.1 Vấn đề; 1.2 Định nghĩa; 1.3 Ba tầng đúng | 03–05 | 4 phút |
| 2 | Quy mô và độ khó của schinese | 2.1 Phạm vi; 2.2 Phụ thuộc; 2.3 Tình huống khó | 06–08 | 4 phút |
| 3 | Kiểm soát đầu vào và đầu ra | 3.1 Đầu vào; 3.2 Điều kiện sẵn sàng; 3.3 Đầu ra; 3.4 Mức bằng chứng | 09–12 | 5 phút |
| 4 | Dựng spec có thể kiểm chứng | 4.1 Cấu trúc; 4.2 Quy trình; 4.3 Viết yêu cầu; 4.4 Nghiệm thu; 4.5 Độ sâu | 13–17 | 7 phút |
| 5 | Kiến trúc thực thi spec | 5.1 Kiến trúc hệ thống; 5.2 Quyền quyết định; 5.3 Luồng AI | 18–20 | 5 phút |
| 6 | Bằng chứng và kiểm soát thay đổi | 6.1 Truy vết; 6.2 Test; 6.3 Độ lệch; 6.4 Thay đổi; 6.5 Giới hạn | 21–25 | 8 phút |
| 7 | Đánh giá và triển khai tiếp | 7.1 Chỉ số; 7.2 Chất lượng sản phẩm; 7.3 Thực nghiệm; 7.4 Lộ trình; 7.5 Kết luận | 26–30 | 6 phút |

# Nội dung 30 slide chính

## Slide 01 — Kiểm soát spec trong phát triển phần mềm có AI hỗ trợ

**Nội dung đưa lên slide**

- Nghiên cứu tình huống: schinese / Pchinese.
- Từ yêu cầu nghiệp vụ đến bằng chứng kiểm chứng.
- Câu hỏi nghiên cứu: “Có thể kiểm soát đến đâu, và chứng minh bằng gì?”

**Lời nói gợi ý**

“AI có thể hỗ trợ triển khai nhiều công việc, nhưng người phát triển vẫn chịu trách nhiệm xác định điều cần làm và chấp nhận kết quả. Bài trình bày tập trung vào cơ chế kiểm soát đó trên một dự án đang phát triển.”

**Gợi ý minh họa:** Một chuỗi ngắn: Nhu cầu → Spec → Triển khai → Kiểm chứng.

## Slide 02 — Mục lục

**Nội dung đưa lên slide**

1. Hiểu spec và mục tiêu kiểm soát.
2. Quy mô và độ khó của schinese.
3. Kiểm soát đầu vào và đầu ra.
4. Dựng spec có thể kiểm chứng.
5. Kiến trúc thực thi spec.
6. Bằng chứng và kiểm soát thay đổi.
7. Đánh giá và lộ trình tiếp theo.

**Lời nói gợi ý**

“Tôi đi từ khái niệm đến một yêu cầu cụ thể, theo yêu cầu đó qua kiến trúc, mã nguồn và test, sau đó đánh giá giới hạn của bằng chứng.”

## Slide 03 — 1.1. Vấn đề cần kiểm soát khi AI tham gia phát triển

**Nội dung đưa lên slide**

- Một yêu cầu mơ hồ có thể tạo ra nhiều cách triển khai hợp lý nhưng khác nhau.
- Code chạy được chưa thể hiện đầy đủ quyền truy cập, điều kiện biên và trạng thái lỗi.
- Một quyết định sai có thể lan sang API, dữ liệu và các feature phụ thuộc.
- Cần một chuẩn để triển khai, kiểm tra và quản lý thay đổi.

**Lời nói gợi ý**

“Ví dụ, yêu cầu ‘Admin được xuất bản bài học’ chưa trả lời được bài học thiếu segment có được xuất bản không, media chưa duyệt thì sao, hoặc hai Admin sửa cùng lúc thì xử lý thế nào. Các câu trả lời này quyết định hành vi sản phẩm.”

**Nguồn:** Sách §5.1, §8.5; F04 spec.

## Slide 04 — 1.2. Spec là gì?

**Nội dung đưa lên slide**

- Spec là đặc tả thống nhất hành vi, ràng buộc và điều kiện chấp nhận của hệ thống.
- Xác định: ai thực hiện, trong điều kiện nào, hệ thống phản hồi ra sao.
- Bao gồm: trạng thái, lỗi, dữ liệu, giới hạn và phạm vi loại trừ.
- Spec có thể làm đầu vào cho thiết kế, triển khai, review và kiểm thử.

**Lời nói gợi ý**

“Sách mô tả spec như giao diện giữa ý định của con người và việc thực thi của AI. Trong dự án, tôi cụ thể hóa điều đó thành yêu cầu có mã, tình huống nghiệm thu và các hợp đồng dữ liệu/API. Một file Markdown chưa tự trở thành kiểm thử chạy được; cần nối nó với cơ chế kiểm chứng.”

**Nguồn:** Sách §5.1–5.2.

## Slide 05 — 1.3. Kiểm soát ‘đúng’ ở ba tầng

**Nội dung đưa lên slide**

| Tầng đánh giá | Câu hỏi | Cách kiểm chứng |
| --- | --- | --- |
| Đúng nhu cầu | Yêu cầu có giải quyết vấn đề thực của người học? | Review nghiệp vụ, dùng thử, phản hồi |
| Đúng đặc tả | Code có thực hiện hành vi đã thống nhất? | Review, test theo yêu cầu |
| Đúng khi vận hành | Hệ thống có giữ chất lượng trong điều kiện thực tế? | E2E, tải, lỗi phụ thuộc, quan sát vận hành |

**Lời nói gợi ý**

“Có thể viết code khớp hoàn toàn với một spec sai nghiệp vụ. Vì vậy tôi cần kiểm tra cả tính đúng của yêu cầu và tính phù hợp của bản triển khai. Đây là cơ sở để giới hạn kết luận khi dự án chưa hoàn thành.”

**Câu nhấn:** “Đúng spec là một tiêu chí quan trọng của chất lượng; kết quả sử dụng thực tế cần bằng chứng bổ sung.”

**Nguồn:** Sách §8.5; khung tổng hợp cho bài bảo vệ.

## Slide 06 — 2.1. Phạm vi sản phẩm schinese

**Nội dung đưa lên slide**

- Nền tảng học tiếng Trung dành cho người học nói tiếng Việt.
- Nội dung học: topic, lesson, segment và video.
- Hoạt động học: dictation, shadowing, từ vựng, SRS và AI Buddy.
- Nền tảng dùng chung: tài khoản, quyền, tiến độ và hạn mức AI.
- MVP theo feature map hiện tại: Free; thanh toán và kích hoạt Premium được hoãn.

**Lời nói gợi ý**

“Tôi dùng spec để trình bày phạm vi dự kiến của sản phẩm. Các luồng trong sơ đồ không đồng nghĩa đã triển khai xong. Feature map hiện có F00–F11; F12 quản trị chính sách AI và monitoring có spec Draft riêng, cần đồng bộ lại với bản đồ tổng.”

**Nguồn:** specs/000a-mvp-feature-map/spec.md; specs/F12-ai-plan-administration-monitoring/spec.md.

## Slide 07 — 2.2. Quy mô thể hiện qua phụ thuộc giữa các feature

**Nội dung đưa lên slide**

- F01 tài khoản → F03 entitlement / allowance → F11 AI Buddy.
- F04 quản trị nội dung + F03 quyền truy cập → F05 catalog.
- F05 + F01 → F06 học và tiến độ → F07 dictation / F08 shadowing.
- F09 từ vựng cá nhân → F10 lịch ôn SRS.

**Lời nói gợi ý**

“Đây là các chuỗi phụ thuộc tiêu biểu, đã lược bớt một số cạnh để dễ trình bày. Khi F04 thay đổi trạng thái xuất bản, F05 phải thay đổi khả năng hiển thị và F06 phải thay đổi khả năng mở bài học. Quy mô ở đây là số quyết định liên quan và phạm vi ảnh hưởng của chúng. Dự án chưa có số đo tải để kết luận quy mô phục vụ người dùng.”

**Gợi ý minh họa:** Vẽ bốn chuỗi phụ thuộc, tô F01/F03/F04 là các nền tảng dùng chung.

**Nguồn:** MVP Feature Inventory và Delivery Boundaries.

## Slide 08 — 2.3. Một thao tác nhỏ chứa nhiều điều kiện khó

**Nội dung đưa lên slide**

**Thao tác: gửi một tin nhắn AI Buddy**

- Đúng người sở hữu cuộc hội thoại?
- Nội dung hợp lệ và còn hạn mức?
- Gửi lại cùng yêu cầu có bị tính thêm lượt?
- Provider chậm, lỗi hoặc trả dữ liệu sai thì xử lý thế nào?
- Hai yêu cầu đồng thời có phá vỡ giới hạn?

**Lời nói gợi ý**

“Nút gửi là phần nhìn thấy của một luồng có nhiều điều kiện. Điểm khó nằm ở việc giữ đúng các điều kiện khi xảy ra lỗi, retry và đồng thời. F11 dùng F03 cho hạn mức, nên một quyết định ở đây còn ảnh hưởng tới hợp đồng giữa hai feature.”

**Nguồn:** F11 FR-003, FR-004, FR-007, FR-009, FR-015; F03 spec.

## Slide 09 — 3.1. Kiểm soát đầu vào của quá trình phát triển

**Nội dung đưa lên slide**

- Nhu cầu và kết quả người dùng mong muốn.
- Phạm vi MVP, actor, quyền và nguồn dữ liệu hiện có.
- Quy tắc chung, kiến trúc và hợp đồng API/data.
- Các giả định, điểm chưa rõ và rủi ro cần giải quyết.

**Lời nói gợi ý**

“Đầu vào ở đây trước hết là đầu vào cho quá trình phát triển: tài liệu, quyết định và bối cảnh đưa cho người hoặc agent. Nó khác với đầu vào runtime như một HTTP request. Cả hai đều cần kiểm soát, nhưng diễn ra ở các thời điểm khác nhau.”

**Nguồn:** CONSTITUTION.md; AGENT.md; API.md; DATA_short.md; sách §5.2.

## Slide 10 — 3.2. Điều kiện để một spec sẵn sàng triển khai

**Nội dung đưa lên slide**

- Có actor, mục tiêu, phạm vi và mã yêu cầu.
- Các điểm mơ hồ quan trọng đã có quyết định.
- Có tình huống thành công, lỗi và điều kiện biên.
- Nhất quán với kiến trúc, dữ liệu và feature phụ thuộc.
- Có người chịu trách nhiệm, phiên bản và dấu vết review.

**Lời nói gợi ý**

“Trong feature map, một câu hỏi làm rõ đã chốt MVP chỉ Free khi chưa có thanh toán tự động. Quyết định này ảnh hưởng tới catalog, nội dung và quota. Checklist giúp kiểm tra sự đầy đủ, còn quyết định nghiệp vụ vẫn cần người có trách nhiệm xác nhận. Hiện nhiều spec còn ghi Draft, nên trạng thái phê duyệt cần được làm rõ.”

**Gợi ý minh họa:** Chụp một clarification thật trong feature map, không chụp toàn bộ trang tài liệu.

**Nguồn:** MVP map phần Clarifications; F04 checklists/requirements.md.

## Slide 11 — 3.3. Kiểm soát đầu ra qua các bước chuyển giao

**Nội dung đưa lên slide**

| Bước chuyển | Đầu ra cần kiểm tra | Điều kiện đi tiếp |
| --- | --- | --- |
| Spec → Plan | Thiết kế giải quyết đủ yêu cầu | Không vượt ranh giới hoặc bỏ sót yêu cầu |
| Plan → Tasks | Công việc theo phụ thuộc | Có phần triển khai và kiểm chứng |
| Tasks → Code | Code, contract, migration khi cần | Có truy vết và review |
| Code → Nghiệm thu | Test, demo, báo cáo | Bằng chứng đạt tiêu chí đã thống nhất |

**Lời nói gợi ý**

“Một quality gate là điều kiện phải đạt trước khi chuyển bước. Việc ghi điều kiện trong tài liệu và việc tự động chặn một thay đổi không đạt là hai trạng thái khác nhau. Tôi đề xuất tự động hóa các điều kiện quan trọng trong CI, còn review nghiệp vụ là trách nhiệm con người.”

**Nguồn:** CONSTITUTION.md Điều 5, 7; sách §7.3, §13.3. Bảng là mô hình áp dụng đề xuất.

## Slide 12 — 3.4. Mức độ bằng chứng cho mỗi yêu cầu

**Nội dung đưa lên slide**

1. Đã đặc tả: có yêu cầu và tiêu chí chấp nhận.
2. Đã ánh xạ: có plan, task và phần triển khai liên quan.
3. Đã kiểm thử: có kết quả chạy test trên phiên bản xác định.
4. Đã nghiệm thu luồng: có kiểm tra tích hợp/E2E phù hợp.
5. Đã đánh giá sử dụng: có dữ liệu thực tế hoặc nghiên cứu người dùng.

**Lời nói gợi ý**

“Đây là thang theo dõi đề xuất cho bài bảo vệ, không phải chuẩn trưởng thành được trích nguyên từ sách. Dự án có thể ở nhiều mức khác nhau theo từng yêu cầu. Một test file hoặc một ô task đã tick chưa cho biết test gần nhất có chạy hay bị skip.”

**Câu nhấn:** “Kết luận phải dừng ở mức mà bằng chứng hiện có hỗ trợ.”

## Slide 13 — 4.1. Cấu trúc bộ spec của dự án

**Nội dung đưa lên slide**

| Tầng | Tài liệu | Vai trò |
| --- | --- | --- |
| Quy tắc dự án | CONSTITUTION.md | Ràng buộc bắt buộc |
| Bối cảnh và hợp đồng chung | AGENT.md, CLAUDE.md, API.md, DATA_short.md | Kiến trúc, quy trình, API và dữ liệu |
| Phạm vi sản phẩm | MVP feature map | Chia feature, dependency và phần hoãn |
| Đặc tả feature | spec.md | Hành vi và nghiệm thu |
| Thiết kế, thực thi, kiểm chứng | plan.md, research.md, data-model.md, contracts/, tasks.md, quickstart.md | Cách triển khai và xác nhận |

**Lời nói gợi ý**

“Bộ spec được chia để mỗi tài liệu trả lời một nhóm câu hỏi. Trong schinese, spec.md ưu tiên hành vi; quyết định kỹ thuật nằm ở plan, research và contract. DATA_short.md là hợp đồng dữ liệu MVP; DATA.md mô tả hướng tương lai. Cần xác định nguồn có thẩm quyền để tài liệu không mâu thuẫn.”

**Nguồn:** .specify/memory/constitution.md; backend/CONSTITUTION.md; cấu trúc specs/.

## Slide 14 — 4.2. Quy trình dựng và cập nhật spec

**Nội dung đưa lên slide**

Khảo sát hiện trạng → Chia feature → Viết spec → Làm rõ → Lập plan/contract → Chia task → Kiểm tra nhất quán → Triển khai → Kiểm chứng và cập nhật.

- Mỗi lần tập trung vào một phần chức năng có thể nghiệm thu.
- Ghi lại lý do của quyết định quan trọng.
- Phát hiện thiếu hoặc sai yêu cầu thì quay lại bước tương ứng.
- Mỗi thay đổi có phiên bản và phạm vi ảnh hưởng.

**Lời nói gợi ý**

“Các thao tác Spec Kit hỗ trợ từng bước, nhưng tên lệnh không thay thế nội dung cần review. Chẳng hạn analyze kiểm tra nhất quán giữa các tài liệu; việc đối chiếu code và hành vi còn cần review, test và vòng hội tụ. Tôi không chốt mọi chi tiết của toàn dự án trước khi nhận phản hồi.”

**Nguồn:** Sách §6, §7.2–7.3, §8.2; các bộ spec/plan/tasks hiện có.

## Slide 15 — 4.3. Viết yêu cầu có thể kiểm tra

**Nội dung đưa lên slide**

**Câu khởi đầu:** “Admin có thể xuất bản bài học.”

**Cụ thể hóa theo F04:**

- Actor có quyền ADMIN.
- Topic cha đã PUBLISHED.
- Lesson có ít nhất một segment.
- Mọi segment đã PUBLISHED và media đã APPROVED.
- MVP dùng FREE; phiên bản cũ bị từ chối.

**Lời nói gợi ý**

“Đây là ví dụ biên soạn lại để trình bày, không phải lịch sử trước–sau của một commit. Các điều kiện được tổng hợp từ F04 FR-001, FR-004, FR-005 và FR-007. Có thể viết theo cấu trúc EARS: khi sự kiện xảy ra trong điều kiện xác định, hệ thống phải thực hiện hành vi xác định.”

**Câu nhấn:** “Từ ‘hợp lệ’ phải được phân rã thành những điều kiện kiểm tra được.”

**Nguồn:** Sách §5.3; F04 FR-001/004/005/007.

## Slide 16 — 4.4. Chuyển yêu cầu thành tình huống nghiệm thu

**Nội dung đưa lên slide**

| Given | When | Then |
| --- | --- | --- |
| Đủ điều kiện xuất bản | Admin gửi lệnh với phiên bản hiện tại | Lesson chuyển PUBLISHED |
| Lesson chưa có segment | Admin gửi lệnh xuất bản | Bị từ chối; giữ trạng thái |
| Có segment còn Draft | Admin gửi lệnh xuất bản | Bị từ chối; giữ trạng thái |
| Phiên bản gửi lên đã cũ | Admin gửi lệnh cập nhật | Trả conflict; yêu cầu tải lại |

**Lời nói gợi ý**

“Mỗi dòng có tiền điều kiện, hành động và kết quả quan sát được. Trong lần kiểm chứng này, ba dòng đầu có unit test tương ứng; dòng phiên bản cũ thuộc phần cần đối chiếu và chạy ở API/integration. Các điều kiện khác như media chưa duyệt còn phải có case riêng.”

**Nguồn:** F04 spec, contracts, quickstart; ContentPublicationServiceTest.java.

## Slide 17 — 4.5. Độ sâu spec theo rủi ro

**Nội dung đưa lên slide**

| Mức | Áp dụng trong schinese | Cần làm rõ |
| --- | --- | --- |
| Phác thảo | Định dạng thời gian, nhãn hiển thị | Input/output và ví dụ |
| Chi tiết | Profile, lọc danh sách, thao tác từ vựng | Quy tắc, lỗi, dữ liệu, nghiệm thu |
| Chặt chẽ kèm trạng thái | Auth, quyền, quota AI, xuất bản nội dung | Invariant, state transition, đồng thời, review và test rủi ro |

**Lời nói gợi ý**

“Độ sâu phụ thuộc vào hậu quả nếu sai và số trường hợp phải xử lý. Một thay đổi nhỏ trong code vẫn có thể cần đặc tả chặt nếu liên quan quyền hoặc dữ liệu. Sách gọi mức cao là Formal; trong bài này, có state diagram không có nghĩa là đã chứng minh hình thức bằng toán học.”

**Nguồn:** Sách §5.4, §13.2, §13.6. Phân loại schinese là đề xuất áp dụng.

## Slide 18 — 5.1. Kiến trúc hệ thống theo hợp đồng dự án

**Nội dung đưa lên slide**

- React SPA: giao diện và sử dụng public API.
- Spring Boot: nghiệp vụ, quyền, giao dịch và dữ liệu sản phẩm.
- PostgreSQL: lưu dữ liệu; truy cập ứng dụng qua Spring Data JPA.
- ai-service riêng tư: Mastra điều phối AI qua hợp đồng nội bộ.
- YouTube: playback bằng mã video đã kiểm soát.

**Sơ đồ để anh dựng lại**

~~~text
React SPA ──public API──> Spring Boot ──JPA──> PostgreSQL
    │                         │
    │                         └──private contract──> ai-service ──> AI provider
    └──approved video ID──> YouTube embed
~~~

**Lời nói gợi ý**

“Đây là kiến trúc được tài liệu quy định và đang được triển khai từng phần. Backend tổ chức theo modular monolith; ai-service là ngoại lệ hỗ trợ AI đã được kiến trúc cho phép. Nó không sở hữu dữ liệu hay quyền quyết định sản phẩm. Sơ đồ không biểu thị toàn bộ luồng đã nghiệm thu.”

**Nguồn:** CLAUDE.md phần 3, Mastra AI Service boundary; CONSTITUTION.md Điều 1.

## Slide 19 — 5.2. Kiến trúc xác định nơi thực thi từng quy tắc

**Nội dung đưa lên slide**

| Quy tắc | Nơi chịu trách nhiệm |
| --- | --- |
| Quyền ADMIN, ownership, entitlement | Backend service |
| Quota, retry và chuyển trạng thái | Backend + transaction/constraint phù hợp |
| API payload và lỗi | DTO, validation, contract và handler |
| Câu trả lời AI | ai-service sinh; backend kiểm tra trước lưu |
| Hiển thị, loading, error, retry | Frontend theo kết quả backend |

**Lời nói gợi ý**

“Điểm cần giải thích là vì sao chọn nơi này để enforce. Backend có dữ liệu và giao dịch để kiểm tra quyền và trạng thái thống nhất. UI giúp người dùng hiểu thao tác, còn quyết định truy cập phải được thực thi ở server. Modular monolith giúp giảm số ranh giới triển khai và giữ các giao dịch nghiệp vụ gần nhau ở giai đoạn hiện tại.”

**Nguồn:** CLAUDE.md ADR-001, ADR-005; các Constitution theo tầng.

## Slide 20 — 5.3. Kiểm soát input/output của AI khi hệ thống chạy

**Nội dung đưa lên slide**

1. Xác thực → ownership → kiểm tra message, rate limit và retry.
2. Reserve quota theo yêu cầu logic hợp lệ.
3. Gửi context tối thiểu qua ranh giới nội bộ có xác thực.
4. Kiểm tra schema, nội dung và lỗi đầu ra.
5. Lưu kết quả hoặc hoàn quota theo chính sách khi thất bại.

**Thông số trong spec F11**

- Message tối đa 1.000 ký tự.
- Context tối đa 10 message đã hoàn tất của cùng conversation.
- Tối đa 5 send attempt/phút; retry cùng message không tính lượt mới.
- Kết quả cuối trong 30 giây theo điều kiện của đặc tả.

**Lời nói gợi ý**

“Các con số là yêu cầu mục tiêu của F11, chưa phải kết quả đo. Retry cần được nhận diện trước khi coi là một lần gửi mới. Schema chỉ kiểm tra được cấu trúc; tính đúng tiếng Trung, mức phù hợp và an toàn cần bộ đánh giá nội dung riêng. Spec cũng phải làm rõ tình huống bất khả thi như mất mạng hoàn toàn.”

**Nguồn:** F11 FR-003/004/007/008/009/012/013/014/015; plan.md.

## Slide 21 — 6.1. Truy vết một yêu cầu tới code và test

**Nội dung đưa lên slide**

**F04 FR-004: điều kiện để bài học được xuất bản**

- Spec: topic, segment và media phải thỏa điều kiện.
- Task T025: bổ sung kiểm tra đầy đủ điều kiện xuất bản.
- Code: ContentPublicationService.publishLesson().
- Test: đủ điều kiện; không có segment; segment còn Draft.
- Kết quả chạy: các test trên pass trong lần kiểm chứng.

**Lời nói gợi ý**

“Đây là lát cắt bằng chứng cụ thể nhất của bài. Tôi có thể mở từng file để đi từ câu yêu cầu đến điều kiện trong code và test. Phần FR-004 liên quan việc xuất hiện trong catalog còn cần kiểm chứng F05; test service xuất bản chưa chứng minh toàn bộ yêu cầu hiển thị.”

**Gợi ý minh họa:** Năm điểm nối theo hàng ngang; chỉ trích đoạn 4–6 dòng code quan trọng.

**Nguồn trực tiếp**

- [F04 FR-004](D:/schinese/specs/F04-content-administration/spec.md:160)
- [Task T025](D:/schinese/specs/F04-content-administration/tasks.md:77)
- [publishLesson](D:/schinese/backend/src/main/java/net/pchinese/content/application/ContentPublicationService.java:143)
- [Test trường hợp không có segment](D:/schinese/backend/src/test/java/net/pchinese/content/ContentPublicationServiceTest.java:44)

## Slide 22 — 6.2. Kết quả kiểm chứng chọn lọc

**Nội dung đưa lên slide**

**Ngày 11/09/2026: 15 unit test pass; 0 fail, 0 error, 0 skip**

| Nhóm | Test case | Nội dung kiểm tra |
| --- | ---: | --- |
| ContentPublicationServiceTest | 5 | Điều kiện xuất bản, ngăn sửa segment đang live, archive cascade |
| MediaIntakePolicyTest | 2 | Chuẩn hóa video ID và từ chối đầu vào không hỗ trợ |
| AiAllowanceServiceTest | 8 | Reserve, quota exhausted, retry, conflict, refund, rollover |

**Lời nói gợi ý**

“Tôi đã chạy các test này trên working tree hiện tại. Nhóm service dùng mock repository; nhóm media kiểm tra policy thuần. Chúng hỗ trợ kết luận về những case được kiểm tra, chưa chứng minh giao dịch thật, race condition, API/E2E hoặc chất lượng toàn bộ AI Buddy.”

**Lệnh đã chạy**

~~~powershell
mvn '-Dtest=ContentPublicationServiceTest,MediaIntakePolicyTest,AiAllowanceServiceTest' test
~~~

**Mốc kết quả:** build kết thúc 20:42:48, GMT+7, 11/09/2026. Tổng số trên đếm JUnit test case; một test case có thể chứa nhiều input/assertion.

## Slide 23 — 6.3. Phát hiện và xử lý độ lệch giữa spec và triển khai

**Nội dung đưa lên slide**

- Spec có yêu cầu nhưng code thiếu hoặc làm khác.
- Code có hành vi mới nhưng tài liệu chưa cập nhật.
- Task đánh dấu hoàn thành nhưng bằng chứng chưa đủ.
- Thay đổi ở một tầng làm API, dữ liệu hoặc UI lệch nhau.

**Ví dụ từ F04**

- Phần Convergence ghi nhận T021–T030.
- Có các mục về audit, token storage, điều kiện publish và API contract.
- T025 được đối chiếu với code và test hiện tại.

**Lời nói gợi ý**

“Mười mục Convergence là những đầu việc tài liệu ghi lại, không tự động tương đương mười lỗi đã được chứng minh bằng thực nghiệm. Tôi đã kiểm tra một lát cắt cụ thể để xác nhận code hiện tại phản ánh yêu cầu. Muốn chứng minh lịch sử cải thiện phải bổ sung phiên bản trước, sau và kết quả kiểm thử tương ứng.”

**Nguồn:** Sách §7.3; F04 tasks.md Phase 6.

## Slide 24 — 6.4. Kiểm soát một thay đổi thật: YouTube-only media intake

**Nội dung đưa lên slide**

**Quyết định đã ghi ngày 11/09:** nhập link/ID YouTube hợp lệ; giữ canonical video ID.

**Chuỗi cần đồng bộ**

Clarification → FR-010/FR-016 → Data model → API → Backend → Frontend → Test → Migration.

- Không tải video lên server hoặc tải video về.
- Link không hỗ trợ bị từ chối.
- Player về sau sử dụng mã video qua embedded playback.

**Lời nói gợi ý**

“Đây là ví dụ thay đổi đi qua nhiều tầng dù giao diện chỉ thay một ô nhập liệu. F04 tasks có Phase 8 T032–T039 cho thay đổi này. Spec hiện còn câu clarification cũ nhắc tới file upload; nên đánh dấu nó là quyết định lịch sử đã bị thay thế để agent và reviewer nhận diện rõ yêu cầu hiệu lực.”

**Nguồn:** F04 Clarifications ngày 11/09, FR-010/016; tasks.md Phase 8; data-model.md; contracts/f04-openapi.yaml; MediaIntakePolicyTest.java.

## Slide 25 — 6.5. Mức kiểm soát hiện tại và giới hạn bằng chứng

**Nội dung đưa lên slide**

**Có thể trình bày bằng chứng**

- Có bộ tài liệu quy tắc, spec/plan/task và contract.
- Có truy vết cụ thể ở F04.
- Có 15 unit test vừa được chạy thành công.

**Cần hoàn thiện**

- Trạng thái Draft và dấu vết phê duyệt còn phải đồng bộ.
- F04 T031 về kiểm chứng đầy đủ vẫn chưa tick.
- F11 có code đang triển khai; tasks chưa xác nhận hoàn thành.
- F12 Draft chưa nằm trong feature map F00–F11.

**Lời nói gợi ý**

“Checklist chất lượng spec của F04 đã tick không có nghĩa sản phẩm đã được nghiệm thu đầy đủ. T039 cũng ghi loại trừ Docker checks còn hoãn. Kết luận phù hợp hiện tại là đã có nền tảng kiểm soát và bằng chứng cho một số lát cắt; chưa đủ để kết luận dự án hoàn thiện.”

**Ghi chú chuẩn bị:** Khi chụp bằng chứng, chỉ dùng spec, code nghiệp vụ đã chọn và test summary. Cấu hình hiện tại chứa credential trực tiếp, lệch với nguyên tắc quản lý secret; cần xử lý trước khi dùng cấu hình trong bất kỳ tài liệu chia sẻ nào. Không đưa giá trị bí mật lên slide.

## Slide 26 — 7.1. Đo mức độ kiểm soát spec

**Nội dung đưa lên slide**

| Chỉ số đề xuất | Cách đo |
| --- | --- |
| Bao phủ truy vết | Yêu cầu trong phạm vi có liên kết triển khai và kiểm chứng / tổng yêu cầu trong phạm vi |
| Hoàn tất nghiệm thu | Acceptance scenario đã chạy đạt / tổng scenario của phiên bản đã chốt |
| Rủi ro còn mở | Số vấn đề chưa xử lý theo mức độ và thời gian tồn tại |
| Đồng bộ thay đổi | Thay đổi nghiệp vụ có cập nhật đủ tài liệu và test bị ảnh hưởng / tổng thay đổi được kiểm tra |

**Lời nói gợi ý**

“Các chỉ số này là đề xuất cần bắt đầu đo, không phải kết quả đã có. Phải chốt mẫu số, phiên bản và trạng thái pass/fail/skip/not-run. Với bảo mật hoặc quota, một trường hợp nghiêm trọng chưa kiểm chứng có thể chặn nghiệm thu dù tỷ lệ trung bình cao.”

**Câu nhấn:** “Không lấy tỷ lệ task đã tick làm phần trăm đúng của sản phẩm.”

## Slide 27 — 7.2. Một dự án tốt cần những kết quả gì?

**Nội dung đưa lên slide**

- Đúng nghiệp vụ: người học hoàn thành được luồng cần thiết.
- Tin cậy: trạng thái, quota và dữ liệu đúng cả khi lỗi hoặc retry.
- Bảo vệ dữ liệu: ownership và quyền được giữ qua các luồng.
- Dễ thay đổi: truy được các phần bị ảnh hưởng khi sửa yêu cầu.
- Có giá trị học tập: kết quả được kiểm tra bằng người dùng và chuyên môn.

**Lời nói gợi ý**

“Spec hỗ trợ biến các thuộc tính chất lượng thành tiêu chí có thể quan sát. Để kết luận AI Buddy hữu ích, ngoài API và schema, tôi cần đánh giá câu trả lời về độ chính xác tiếng Trung, giải thích tiếng Việt và mức phù hợp. Để kết luận người học tiến bộ, cần một thiết kế đánh giá riêng.”

**Nguồn:** Tổng hợp mục tiêu sản phẩm và tiêu chí dự án. Chưa có dữ liệu thực nghiệm về hiệu quả học tập trong đợt đối chiếu.

## Slide 28 — 7.3. Thiết kế thực nghiệm kiểm tra lợi ích của kiểm soát spec

**Nội dung đưa lên slide**

**Đề xuất, chưa thực hiện**

- Chọn một lát cắt có nhiều điều kiện, như xuất bản F04.
- So sánh yêu cầu ngắn với spec đã làm rõ.
- Giữ cùng điểm xuất phát, phạm vi, công cụ và ngân sách.
- Người review chuẩn bị bộ nghiệm thu độc lập; không đưa hết cho agent.
- So sánh lỗi nghiệp vụ, lỗi biên, số lần sửa và tổng công sức.

**Lời nói gợi ý**

“Tổng công sức phải tính cả thời gian chuẩn bị spec, review, triển khai và sửa lỗi. Nên thực hiện lặp trên nhiều nhiệm vụ, ghi phiên bản model/công cụ và hoán đổi thứ tự để giảm thiên lệch. Một cặp thử chỉ là minh họa. Kết quả mới có thể bổ sung cho lập luận lợi ích, và vẫn cần nói rõ giới hạn tổng quát hóa.”

**Câu nhấn:** “Case study hiện tại minh họa cơ chế kiểm soát; thực nghiệm đối chứng bổ sung bằng chứng so sánh.”

## Slide 29 — 7.4. Lộ trình triển khai tiếp

**Nội dung đưa lên slide**

1. **Chốt baseline:** cập nhật trạng thái review, owner, feature map và quyết định hiệu lực.
2. **Hoàn thiện lát cắt F04:** chạy kiểm tra migration sạch/nâng cấp, integration, E2E; lưu bằng chứng.
3. **Tự động hóa gate:** test, contract, migration và kiểm tra secret phù hợp; quy định điều kiện merge.
4. **Nghiệm thu F11 theo rủi ro:** ownership, retry/concurrency, output lỗi, timeout và xóa dữ liệu.
5. **Đánh giá sản phẩm:** dùng thử, kiểm tra chất lượng AI; đo các chỉ số đã chọn.

**Lời nói gợi ý**

“Tôi ưu tiên hoàn thiện một chuỗi kiểm chứng trước khi mở rộng số feature. Mỗi giai đoạn phải có đầu ra cụ thể: baseline được review; báo cáo pass/fail/skip; gate hoạt động; hoặc kết quả dùng thử. F12 nên được đưa vào bản đồ phụ thuộc khi ranh giới chính sách và dữ liệu đo đã được xác nhận.”

## Slide 30 — 7.5. Kết luận và cam kết kiểm chứng

**Nội dung đưa lên slide**

- Spec làm rõ điều cần xây dựng và điều kiện chấp nhận.
- Kiến trúc đặt quy tắc vào đúng nơi thực thi.
- Review, test và truy vết tạo bằng chứng cho mức kiểm soát.
- Kiểm soát thay đổi giữ các tài liệu và hành vi nhất quán.
- Schinese đã có các lát cắt chứng minh; bước tiếp theo là hoàn thiện bằng chứng tích hợp và sử dụng.

**Lời kết gợi ý**

“Qua schinese, tôi có thể chỉ ra một yêu cầu được đặc tả, triển khai và kiểm thử ở đâu. Tôi cũng xác định được phần chưa đủ bằng chứng. Mục tiêu tiếp theo là hoàn thiện chuỗi kiểm chứng này theo từng rủi ro, để mỗi tuyên bố về chất lượng đều có cơ sở xem xét lại.”

# Sáu slide phụ lục để phản biện

## Slide A1 — Một bộ đặc tả cần làm rõ những gì?

**Nội dung đưa lên slide**

1. Context và mục tiêu.
2. Actor, quyền và ownership.
3. Yêu cầu chức năng.
4. Yêu cầu chất lượng có cách đo.
5. Dữ liệu và invariant.
6. Lỗi và cách xử lý.
7. Acceptance criteria.
8. Out of scope.

**Ghi chú:** Tám nhóm của sách §5.2 có thể nằm ở nhiều file. Trong schinese, dữ liệu chi tiết thuộc data-model.md và DATA_short.md, hợp đồng API thuộc contracts/, cách triển khai thuộc plan.md. Không cần nhồi toàn bộ vào spec.md.

**Cách trả lời khi bị hỏi “spec càng nhiều càng tốt?”:** “Tôi chọn độ sâu theo rủi ro, và kiểm tra thông tin đó giúp đưa ra quyết định hay kiểm chứng điều gì.”

## Slide A2 — Ma trận tình huống xuất bản cần kiểm chứng

| Case | Kết quả cần có | Bằng chứng trong đợt này |
| --- | --- | --- |
| Đủ điều kiện | Publish thành công | Unit test đã pass |
| Không có segment | Từ chối | Unit test đã pass |
| Có segment Draft | Từ chối | Unit test đã pass |
| Parent chưa Published | Từ chối | Đã thấy guard trong code; chưa chạy riêng case |
| Media chưa Approved | Từ chối | Đã thấy guard trong code; chưa chạy riêng case |
| Version cũ | Conflict, giữ dữ liệu | Đã thấy guard; cần kết quả API/integration |
| Không có ADMIN | Từ chối truy cập/thao tác | Cần kết quả authorization integration |
| Hai thao tác đồng thời | Giữ đúng version/invariant | Cần kiểm tra database thật |

**Ghi chú:** Không suy luận tất cả case đều pass từ năm unit test của service. Bảng này cũng cho thấy còn gì cần triển khai tiếp.

## Slide A3 — Đánh giá chất lượng đầu ra AI

**Nội dung đưa lên slide**

- Cấu trúc: schema, field bắt buộc, kích thước và số gợi ý.
- Nghiệp vụ: đúng activity, đúng context của learner.
- Nội dung: chính xác tiếng Trung, giải thích tiếng Việt, phù hợp trình độ.
- Chính sách: từ chối đúng và chấp nhận đúng; đo cả chặn nhầm lẫn bỏ lọt.
- Vận hành: timeout, lỗi provider, chi phí, quota và retry.

**Ghi chú:** Các tuyên bố “100% unsafe input bị từ chối” trong spec cần xác định chính sách, bộ dữ liệu và phạm vi thử. Không thể coi một bộ quy tắc keyword hay JSON schema là bằng chứng an toàn tuyệt đối cho ngôn ngữ tự nhiên. Khác biệt giữa “5 lần gửi/phút” và “nội dung phù hợp” là một bên có phép kiểm tra rõ, bên kia cần rubric và bộ đánh giá.

## Slide A4 — Bộ bằng chứng chuẩn bị trước buổi bảo vệ

**Nội dung đưa lên slide**

- Một phiên bản source/spec xác định; ghi commit và tình trạng working tree.
- Một bảng Requirement → Task → Code → Test → Kết quả.
- Test report có ngày chạy, môi trường, pass/fail/skip.
- Demo gồm thành công, một lỗi biên và một thao tác bị từ chối.
- Danh sách khoảng trống và kế hoạch xử lý.

**Demo đề xuất 3–4 phút**

1. Mở F04 FR-004, chỉ ra điều kiện có ít nhất một segment.
2. Mở T025 và publishLesson().
3. Mở test no-segment và draft-segment.
4. Chạy lại bộ unit test chọn lọc hoặc dùng log/video đã chuẩn bị.
5. Kết luận đúng phạm vi: các case vừa chạy đạt; catalog/concurrency cần bằng chứng khác.

**Ghi chú:** Nếu chưa có E2E môi trường sẵn sàng, dùng demo test để bảo vệ lát cắt. Không mô tả đó là demo toàn bộ sản phẩm.

## Slide A5 — Phản biện về phương pháp

**1. Ai kiểm tra spec đúng nghiệp vụ?**  
Người chịu trách nhiệm sản phẩm cùng người hiểu domain review theo tình huống sử dụng. AI có thể phát hiện chỗ thiếu, nhưng kết quả vẫn phải được đối chiếu với nhu cầu và phản hồi thực tế.

**2. SDD có trở thành Waterfall?**  
Rủi ro có nếu cố hoàn thiện mọi yêu cầu toàn dự án từ đầu. Cách áp dụng ở đây là feature nhỏ, baseline có phiên bản, nhận phản hồi rồi cập nhật có truy vết.

**3. Vì sao test pass vẫn có thể sai?**  
Bộ test có thể thiếu tình huống hoặc mang cùng giả định sai với code/spec. Cần review độc lập, test âm, integration, dùng thử và kiểm tra mục tiêu nghiệp vụ.

**4. Khác gì với tài liệu yêu cầu truyền thống?**  
Các nguyên lý đặc tả và kiểm chứng đã có trong kỹ nghệ phần mềm. Điểm bài này nhấn mạnh là sử dụng bộ đặc tả có cấu trúc làm đầu vào thực thi cho agent, quản lý context và nối với các gate kiểm chứng. Không nên tuyên bố SDD tạo ra toàn bộ những nguyên lý này.

## Slide A6 — Phản biện về bằng chứng của schinese

**5. Dự án còn dở chứng minh được gì?**  
Chứng minh cơ chế và một số lát cắt đã kiểm chứng: có yêu cầu, code, test và kết quả. Chưa chứng minh sản phẩm hoàn thiện, khả năng phục vụ tải lớn hoặc hiệu quả học tập.

**6. Có thể nói spec đúng 100% không?**  
Chỉ có thể nói về một tập tiêu chí và phạm vi kiểm tra xác định. Cần ghi rõ mẫu số, phiên bản, môi trường và phần chưa chạy; tránh dùng một tỷ lệ tổng cho toàn bộ chất lượng.

**7. AI viết cả code và test thì có đáng tin?**  
Kết quả cần được đối chiếu với tiêu chí độc lập và review của con người. Có thể bổ sung mutation test hoặc cố tình tạo biến thể sai trong môi trường thử để xem test có phát hiện; chưa thực hiện việc này trong đợt kiểm chứng.

**8. Dùng ai-service có trái modular monolith không?**  
Constitution cho phép một supporting service riêng tư cho AI. Backend vẫn giữ dữ liệu và nghiệp vụ sản phẩm. Phải chứng minh ranh giới đó trong triển khai; sơ đồ đẹp hoặc tên module chưa đủ.

# Nguồn để ghi chân slide và chuẩn bị phản biện

## Sách

**LinhNDM — Playbook: Spec-Driven & Agent-Driven Development**, file do anh cung cấp, 375 trang PDF. Đợt này đối chiếu các phần liên quan, không xác minh độc lập mọi số liệu hoặc nhận định khác trong sách.

Trang dưới đây dùng **số trang in ở chân sách**; vị trí trong PDF của các phần này lớn hơn 1 trang.

| Nội dung dùng | Mục sách | Trang in / trang PDF |
| --- | --- | --- |
| Định nghĩa spec | §5.1 | 108 / PDF 109 |
| Tám nhóm nội dung | §5.2 | 110–114 / PDF 111–115 |
| EARS | §5.3 | 115 trở đi / PDF 116 trở đi |
| Độ sâu theo rủi ro | §5.4 | 121–124 / PDF 122–125 |
| Constitution | §7.1 | 169 trở đi / PDF 170 trở đi |
| Làm rõ trước planning | §7.2 | 173 trở đi / PDF 174 trở đi |
| Nhất quán tài liệu và code | §7.3 | 177–178 / PDF 178–179 |
| Quản lý quy mô spec | §7.5 | 186 trở đi / PDF 187 trở đi |
| Phản biện SDD/Waterfall | §8.2 | 207 / PDF 208 |
| Spec sai nghiệp vụ | §8.5 | 214 / PDF 215 |
| Hybrid Core & Shell | §13.1 | 318 / PDF 319 |
| Độ sâu, tự chủ và rủi ro | §13.2 | 320 / PDF 321 |
| Hybrid workflow | §13.3 | 323 trở đi / PDF 324 trở đi |
| Over-specification | §13.6 | 336 / PDF 337 |

**Lưu ý học thuật:** Không dùng các tỷ lệ năng suất hoặc hiệu quả trong ví dụ của sách như số đo của schinese. “Formal” theo cách đặt tên trong sách không đồng nghĩa dự án đã áp dụng formal verification.

## Bằng chứng dự án

| Nguồn | Mục đích sử dụng |
| --- | --- |
| [CONSTITUTION.md](D:/schinese/CONSTITUTION.md) | Quy tắc kiến trúc, bảo mật, kiểm thử, review |
| [CLAUDE.md](D:/schinese/CLAUDE.md) | Kiến trúc hệ thống, module, ADR và AI boundary |
| [MVP feature map](D:/schinese/specs/000a-mvp-feature-map/spec.md) | Phạm vi, dependency, quyết định Free MVP |
| [F04 spec](D:/schinese/specs/F04-content-administration/spec.md) | Yêu cầu, clarification và nghiệm thu |
| [F04 tasks](D:/schinese/specs/F04-content-administration/tasks.md) | T025, T031, Convergence và thay đổi YouTube |
| [F04 data model](D:/schinese/specs/F04-content-administration/data-model.md) | State transition và invariant |
| [F04 OpenAPI](D:/schinese/specs/F04-content-administration/contracts/f04-openapi.yaml) | Contract của request/response và conflict |
| [F04 checklist](D:/schinese/specs/F04-content-administration/checklists/requirements.md) | Chất lượng đặc tả; không phải kết quả nghiệm thu |
| [F04 quickstart](D:/schinese/specs/F04-content-administration/quickstart.md) | Quy trình kiểm chứng dự kiến |
| [Publication service](D:/schinese/backend/src/main/java/net/pchinese/content/application/ContentPublicationService.java) | Điều kiện publish thực tế trong code |
| [Publication unit test](D:/schinese/backend/src/test/java/net/pchinese/content/ContentPublicationServiceTest.java) | 5 test đã chạy |
| [Media policy unit test](D:/schinese/backend/src/test/java/net/pchinese/media/MediaIntakePolicyTest.java) | 2 test đã chạy |
| [Allowance unit test](D:/schinese/backend/src/test/java/net/pchinese/allowance/AiAllowanceServiceTest.java) | 8 test đã chạy |
| [F11 spec](D:/schinese/specs/F11-ai-learning-buddy/spec.md) | Kiểm soát input/output, retry và ownership |
| [F11 plan](D:/schinese/specs/F11-ai-learning-buddy/plan.md) | Thiết kế boundary, context, thời gian |
| [F11 tasks](D:/schinese/specs/F11-ai-learning-buddy/tasks.md) | Phần triển khai còn phải xác nhận |
| [F12 Draft](D:/schinese/specs/F12-ai-plan-administration-monitoring/spec.md) | Hướng mở rộng quản trị/monitoring cần đồng bộ map |

Các kết quả test là ảnh chụp trạng thái working tree tại thời điểm chạy. Khi source hoặc spec thay đổi, cần chạy lại các kiểm tra bị ảnh hưởng trước buổi bảo vệ.

# Cách nói để bài bảo vệ có trọng tâm

1. Dùng một câu hỏi xuyên suốt: “Yêu cầu này được kiểm soát và kiểm chứng ở đâu?”
2. Giữ F04 xuất bản nội dung làm ví dụ chính; F11 cho thấy độ khó và hướng mở rộng.
3. Giải thích một điều kiện nghiệp vụ rồi mới mở code; tránh đọc danh sách framework.
4. Nêu cả case bị từ chối, quyền bị giới hạn và hậu quả khi xử lý sai.
5. Mỗi kết luận có phạm vi: mục tiêu trong spec, quan sát trong code, kết quả đã chạy, hoặc đề xuất.
6. Dành thời gian cho slide 21–25; đây là phần biến lý thuyết thành lập luận có thể kiểm tra.
7. Khi nói “tạo ra dự án tốt”, chỉ rõ thuộc tính chất lượng và phép đánh giá tương ứng.
8. Nếu câu hỏi vượt bằng chứng, nêu phần chưa biết và cách sẽ kiểm chứng nó.

**Thông điệp quan trọng nhất anh nên giữ:**  
“Giá trị của kiểm soát spec nằm ở việc biến yêu cầu thành quyết định và hành vi có thể kiểm tra, đồng thời làm rõ những phần còn chưa được chứng minh.”

