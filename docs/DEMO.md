# Kịch bản demo và kiểm tra thủ công

Không có automated tests theo yêu cầu. Checklist này là hướng dẫn xác minh trực tiếp, không phải tuyên bố mọi mục đã chạy. Kết quả thực tế nằm trong VERIFICATION.md.

## Demo 5–7 phút

1. **30 giây — Vấn đề:** “Chặn từng số không đủ khi một dịch vụ dùng nhiều số. QuietCall gom số theo thương hiệu.” Nêu giới hạn catalog: số chưa biết chưa thể chặn theo tên.
2. **1 phút — Thiết lập:** Mở Tổng quan, kích hoạt vai trò Caller ID & spam. Nêu quyền danh bạ là tùy chọn và dữ liệu offline.
3. **1 phút — Thương hiệu:** Nạp demo trong Cài đặt; tìm “aster”; bật công tắc chặn Aster Finance · Demo; bấm thẻ thương hiệu để mở chỉnh sửa và chỉ ra ba số. Dấu cộng ở đầu trang thêm nhóm mới.
4. **1 phút — Mô phỏng:** Nhập `0900000001` → Đã chặn; `+84 900 000 002` → Đã chặn; `0900000011` → Cho phép vì Nova chưa bị chặn.
5. **1 phút — Ưu tiên:** Thêm `0900000001` vào danh sách cho phép → mô phỏng lại được cho phép. Xóa khỏi whitelist; bật Chỉ tắt chuông → kết quả Tắt chuông. Tắt bảo vệ → Cho phép.
6. **1 phút — Lịch sử và dữ liệu:** Xem nhãn Mô phỏng/Hệ thống và màu kết quả; bấm cuộc gọi để xem đầy đủ lý do và các thao tác. Xuất JSON, nhập lại; danh mục không bị nhân đôi. Lịch sử không được xuất.
7. **30 giây — Kiến trúc:** UI → Repository → SQLite; Android Telecom → Service → Engine; response trước hạn 5 giây; không gọi mạng khi chặn.

## Cuộc gọi thật với hai điện thoại

- Máy A cài app Android 10+, có SIM; máy B là nguồn gọi do bạn kiểm soát.
- Cấp vai trò lọc, bật protection, tắt silence.
- Để số B ngoài danh bạ A, hoặc cấp READ_CONTACTS rồi tắt Bảo vệ số trong danh bạ.
- Thêm thương hiệu “Nguồn demo của tôi”, thêm số B, bật chặn.
- B gọi A: A không đổ chuông; Android từ chối; history QuietCall có CUỘC GỌI HỆ THỐNG và đúng thương hiệu.
- Tắt công tắc thương hiệu, B gọi lại: A đổ chuông bình thường.
- Bật chặn, đưa B vào whitelist, gọi lại: được cho phép.
- Bật silence và bỏ B khỏi whitelist: không đổ chuông; giao diện cuộc gọi có thể vẫn hiện theo hệ thống.
- Không gọi các số giả lập trong sample.

## Checklist UX / dữ liệu

| Tình huống | Kết quả mong đợi |
|---|---|
| Từ chối vai trò lọc | Hiển thị chưa kích hoạt; vẫn dùng được mô phỏng |
| Từ chối READ_CONTACTS | Không crash; giải thích hành vi ngoài danh bạ |
| Tên rỗng / 1 ký tự | Form báo lỗi, không đóng |
| Số có chữ / ít hơn 8 chữ số | Báo lỗi, không lưu |
| +84 và 0 cùng số | Chỉ lưu một số chuẩn hóa |
| Một số thuộc hai thương hiệu | Không lưu, giải thích xung đột |
| Tên có dấu / không dấu | Tìm kiếm khớp |
| Mở lại app | Giữ danh mục, settings, whitelist, history |
| Nhập JSON hỏng / schema lạ / số xung đột | Báo lỗi, giữ nguyên toàn bộ dữ liệu |
| Nhập lại JSON đã xuất | Gộp, không nhân đôi; giữ cờ chặn hiện có |
| JSON mới có blocked=true | Thương hiệu mới vẫn chưa bị chặn |
| Xóa thương hiệu | Xóa cả số trong nhóm; history cũ vẫn giữ lý do |
| Xóa lịch sử | Danh mục và settings không đổi |
| Cuộc gọi số chưa có trong catalog | Cho phép |
| App không mở trên màn hình | Service vẫn được Android gọi nếu còn vai trò |

## Khắc phục

- Không chặn: kiểm tra vai trò Caller ID & spam, protection, công tắc thương hiệu, số chuẩn hóa, whitelist và bảo vệ danh bạ.
- Không có history: hệ thống có thể không gửi cuộc gọi danh bạ nếu chưa cấp READ_CONTACTS. QuietCall không đọc lịch sử toàn hệ thống.
- Hãng máy không hỗ trợ vai trò: sử dụng mô phỏng, thử điện thoại khác hỗ trợ Android Telecom CallScreeningService.
- App khác nhận vai trò lọc: mở QuietCall và chọn lại.
