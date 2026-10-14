# QuietCall — Định hướng sản phẩm và kế hoạch

## 1. Chuyển yêu cầu đề bài thành sản phẩm

**Vấn đề:** Một dịch vụ thay nhiều số quảng cáo khiến chặn từng số tốn công. Người dùng muốn nhập một tên và quản lý toàn bộ số liên quan.

**Mục tiêu:** Chặn cuộc gọi trước khi đổ chuông cho nhóm số đã biết; giải thích được lý do chặn và cho phép sửa sai nhanh. Android-only là phạm vi MVP hợp lệ theo đề bài.

**Persona:** Người dùng nhận nhiều cuộc gọi quảng cáo; sinh viên cần demo đáng tin cậy trên điện thoại Android; giảng viên cần cài APK và kiểm tra quy tắc nhanh.

**Định nghĩa thành công:** Cài được APK; cấp vai trò lọc; thêm thương hiệu có hai số; cả hai số đều bị chặn khi bật công tắc; bỏ chặn hoặc cho phép riêng hoạt động; dữ liệu giữ lại sau mở lại ứng dụng.

## 2. Brainstorm và chọn hướng

| Phương án | Ưu điểm | Chi phí / giới hạn | Quyết định |
|---|---|---|---|
| Chặn số đơn lẻ | Dễ triển khai | Không giải quyết nhóm thương hiệu | Dùng làm dữ liệu nền |
| Danh mục thương hiệu–số cục bộ | Offline, dễ giải thích, phù hợp bài tập | Số mới chưa nhận diện được | Chọn MVP |
| API caller-ID bên ngoài | Có thể phủ nhiều số | Cần giấy phép, khóa API, mạng và dữ liệu tin cậy | Ngoài phạm vi đã chốt |
| Cộng đồng báo cáo số | Danh mục phát triển dần | Cần kiểm duyệt, chống báo cáo sai; số phải được xác nhận | Chỉ cân nhắc chia sẻ catalog đã duyệt |
| Chặn đầu số / mọi số lạ | Bao phủ mạnh | Dễ chặn nhầm người thân, shipper, bệnh viện | Không đưa vào MVP |
| Flutter + native bridge | UI dùng chung Android/iOS | Android vẫn cần service Kotlin; iOS khác mô hình | Hợp lý khi bắt buộc UI đa nền tảng |
| Kotlin Android native | Trực tiếp dùng Telecom API, ít lớp tích hợp | Chỉ chạy Android | Chọn theo phạm vi đề cho phép |

Không thể suy ra một thương hiệu từ mọi số điện thoại bằng một vài chuỗi ký tự. Vì vậy tên là **khóa tìm danh mục**, không phải dữ liệu mà hệ thống điện thoại bảo đảm gửi kèm.

## 3. User stories và tiêu chí chấp nhận

| Story | Tiêu chí |
|---|---|
| Tôi nhập tên thương hiệu để chặn | Tìm không dấu; một công tắc áp dụng cho mọi số trong thương hiệu |
| Tôi bổ sung số mới | Cho nhập nhiều dòng; chuẩn hóa +84/0; không cho số trùng giữa hai thương hiệu |
| Tôi không muốn chặn số quan trọng | Whitelist ưu tiên cao; có bảo vệ danh bạ tùy chọn |
| Tôi muốn biết tại sao cuộc gọi bị xử lý | Lịch sử có hành động, số, lý do, thời gian, nhãn thật/mô phỏng |
| Tôi muốn chia sẻ danh mục | JSON có schema version; nhập gộp trong transaction; không tự kích hoạt quy tắc mới |
| Tôi muốn demo không cần SIM | Mô phỏng sử dụng chính ScreeningEngine; không tạo cuộc gọi thật |

## 4. Luồng và thiết kế

Thiết kế tiếng Việt, xanh trầm và nền sáng; chữ rõ, nút chính dễ bấm, bốn tab:

1. **Tổng quan:** Trạng thái vai trò hệ thống và bảo vệ; số thương hiệu chặn; số lượt thật đã xử lý trong lịch sử còn lưu; thêm thương hiệu; mô phỏng.
2. **Thương hiệu:** Tìm tên, lọc thương hiệu chặn, bật/tắt, sửa danh sách số và ghi chú nguồn.
3. **Lịch sử:** Lọc thật/mô phỏng/chặn; thêm số vào whitelist hoặc gán thương hiệu.
4. **Cài đặt:** Quyền hệ thống, chế độ chặn/tắt chuông, bảo vệ danh bạ, whitelist, nhập/xuất, riêng tư.

Không có đăng nhập, onboarding nhiều bước, paywall hoặc đồng bộ nền. Hướng dẫn kích hoạt nằm ngay trên Tổng quan.

## 5. Kế hoạch triển khai chi tiết

| Giai đoạn | Công việc | Sản phẩm / điều kiện hoàn tất |
|---|---|---|
| A — Nghiên cứu | Đọc API Telecom, giới hạn nhận diện thương hiệu, vai trò hệ thống và quyền | Chốt Android 10+, danh mục offline; ghi ADR |
| B — Nền tảng | Cấu hình Gradle, manifest, icon, theme, SQLite, chuẩn hóa số | Project compile được; model rõ ràng |
| C — Bộ chặn | Engine ưu tiên quy tắc; service nhận cuộc gọi; watchdog trước hạn 5 giây | Response hợp lệ; lỗi/timeout cho phép cuộc gọi |
| D — Sản phẩm | Bốn tab, CRUD thương hiệu, công tắc, quyền, whitelist | Người dùng tự thiết lập được bộ chặn |
| E — Dữ liệu & demo | JSON atomic merge, mẫu giả lập, mô phỏng, history riêng | Demo không cần mạng; không nhầm thật/mô phỏng |
| F — Hoàn thiện | Biên dịch APK, kiểm tra thủ công có thể thực hiện; sửa lỗi build/UI | APK và báo cáo xác minh trung thực |
| G — Bàn giao | Wrapper, CI artifact, README, kiến trúc, demo, email mẫu | Source có thể push GitHub và APK sẵn để upload Drive |

Không viết automated tests theo yêu cầu. Kiểm tra bằng compiler, công cụ Android và chạy thủ công; ghi rõ phần chưa có thiết bị xác minh.

## 6. Rủi ro và cách xử lý

- **Danh mục thiếu:** Số chưa biết được cho phép; user thêm số, nhập catalog từ nguồn được tin cậy.
- **Sai danh tính:** Ghi chú nguồn, chỉnh sửa dễ; demo giả lập; không quảng cáo nhận diện tuyệt đối.
- **Chặn nhầm:** Whitelist và bảo vệ danh bạ; không có chặn hàng loạt số lạ.
- **Chậm xử lý:** Không network trong đường cuộc gọi; watchdog 3,5 giây; Android cho hạn phản hồi 5 giây.
- **Khác hãng máy:** Cần thử trên máy Android thật; tắt chuông có thể vẫn hiện giao diện.
- **Vai trò bị thay đổi:** Kiểm tra lại khi ứng dụng resume; yêu cầu user chọn lại nếu mất vai trò.

## 7. Hướng phát triển sau đồ án

Tách domain sang Kotlin Multiplatform; UI Compose Multiplatform hoặc Flutter nếu mở rộng frontend; adapter iOS Call Directory cần danh sách số theo API iOS và không có callback tương đương Android. Backend có thể dùng PostgreSQL và API catalog có version/chữ ký; luôn tải trước, không tra mạng khi cuộc gọi đến. Bổ sung quy trình duyệt báo cáo, dấu nguồn dữ liệu, cập nhật tăng dần và xác minh danh tính doanh nghiệp trước khi phát triển danh mục cộng đồng.
