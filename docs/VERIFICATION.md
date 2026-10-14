# Kết quả xác minh — 14/10/2026

Không viết hay chạy automated tests theo yêu cầu. Đã biên dịch, chạy Android Lint, cài ứng dụng và kiểm tra thủ công trên máy ảo Android của máy này.

## Môi trường

Windows · JDK 21.0.10 · Gradle 8.9 · Android SDK 35 · Android Emulator có WHPX · AVD QuietCall, Pixel 2, Android 10 / API 29 / x86_64. Thiết bị ADB: emulator-5554. Source hiện tại chạy bằng Kotlin/Android native, không phải website mô phỏng.

## Build và phân tích

- `assembleDebug`: BUILD SUCCESSFUL.
- `lintDebug`: BUILD SUCCESSFUL; báo cáo text cuối cùng: **No issues found.**
- Cài bản build bằng ADB: **Success**.
- Khởi động Activity: thành công; không có AndroidRuntime error trong các luồng đã kiểm tra.
- `run-local.ps1 -SkipBuild` được chạy bằng Windows PowerShell 5 qua đúng cách mà `run-local.cmd` dùng: mở máy ảo từ trạng thái đã tắt, đợi boot, cài và mở ứng dụng thành công.
- `demo-call.ps1 -Number ...` và `-Hangup`: chạy được bằng Windows PowerShell 5.

## Kiểm tra thực tế đã hoàn thành

| Thao tác | Kết quả quan sát |
|---|---|
| Mở app trên API 29 | Bốn tab hiển thị, không crash do WindowInsets API mới |
| Chuyển tab sau khi cuộn | Đặt lại vị trí đầu trang sau khi layout của tab mới hoàn tất |
| Xin vai trò Caller ID & spam bằng UI | Hộp thoại Android nhận diện QuietCall; sau xác nhận app báo bảo vệ hoạt động |
| Nạp dữ liệu demo bằng UI | 3 thương hiệu, 7 số; mặc định chưa chặn |
| Bật chặn Aster bằng UI | Quy tắc được lưu vào SQLite |
| Telecom gọi 0900000001 | Bị từ chối; dumpsys Telecom ghi SCREENING_COMPLETED Reject bởi QuietCall; response đầu khoảng 130 ms |
| Telecom gọi 0900000002 | Cùng thương hiệu, cũng bị chặn; danh sách cuộc gọi modem không còn ringing |
| Telecom gọi +84900000001 | Khớp 0900000001, BLOCK đúng Aster |
| Telecom gọi 0900000011, Nova chưa chặn | ALLOW, modem còn incoming; đã ngắt cuộc gọi sau kiểm tra |
| Bật Chỉ tắt chuông rồi gọi 0900000003 | SILENCE trong history; cuộc gọi vẫn tồn tại, không bị reject |
| Thêm 0900000003 vào whitelist từ history | Gọi lại được ALLOW, lý do Số thuộc danh sách cho phép |
| Mô phỏng +84900000001 | BLOCK, có nhãn mô phỏng, số được chuẩn hóa |
| Mô phỏng cùng số và chọn có trong danh bạ | ALLOW, lý do Bảo vệ số trong danh bạ |
| Xuất JSON qua Storage Access Framework | Tạo tệp hợp lệ có schemaVersion 1, thương hiệu, số và whitelist; không xuất history |
| Nhập lại chính tệp vừa xuất bằng picker | Vẫn 3 thương hiệu, 7 số, 1 số cho phép; Aster vẫn chặn; không nhân đôi |
| Form tên rỗng / nhóm bật chặn nhưng chưa có số | Không lưu; form tiếp tục mở, dữ liệu không thay đổi |
| Tạo thương hiệu tạm với 0900000099 bằng UI | Lưu được; cuộc gọi Telecom tiếp theo bị BLOCK đúng thương hiệu mới |
| Xóa thương hiệu tạm qua UI và xác nhận | Trở về 3 thương hiệu / 7 số; số đi kèm được cascade delete |
| Mở history rồi nhận cuộc gọi bị chặn | History cập nhật trực tiếp, không cần đổi tab |
| Tắt / mở lại emulator và cài cập nhật | Danh mục, trạng thái, whitelist và history được giữ nguyên |

Đã dọn thương hiệu kiểm tra tạm và làm mới lịch sử demo bằng UI. Bộ demo bàn giao: Aster bật chặn, Nova và Orbit cho phép, 0900000003 nằm trong whitelist, chế độ chặn hoàn toàn (silence tắt). Dữ liệu demo là giả lập.

## Phần chưa được xác minh

- **Chưa thử cuộc gọi qua SIM trên điện thoại vật lý.** Các cuộc gọi Telecom ở trên do console emulator tạo; nhãn CUỘC GỌI THẬT trong ứng dụng nghĩa là nhận callback hệ thống, không phải mô phỏng nội bộ. Trên emulator, chúng vẫn không đi qua mạng điện thoại thật.
- Chưa chạy trên mọi hãng máy hoặc trên API 30–35; build và Lint kiểm tra tương thích, không thay thế chạy trên các thiết bị đó.
- Bảo vệ danh bạ đã kiểm tra qua mô phỏng; chưa thử contact provider với danh bạ thật.
- Không ép lỗi storage/timeout, không kiểm tra tải lớn hoặc mọi loại tệp nhập hỏng. Những nhánh này có transaction, validation và fail-open trong code; không tuyên bố đã chạy xác minh tất cả.
- Không tự nhận diện số mới của thương hiệu chưa có trong danh mục; không chặn Zalo/Messenger; không có bản iOS.

Project sẵn sàng dùng thử cục bộ qua `run-local.cmd` và `demo-call.cmd`. Bước này chưa xuất gói bàn giao, push GitHub hay upload Drive.

## Lần rà soát và cải tiến tiếp theo

Đã cải tiến sản phẩm theo PRODUCT_REVIEW.md, giữ phạm vi chặn nhóm số đã biết. Kết quả kiểm tra bổ sung:

- Build và Lint lại thành công, báo cáo **No issues found.**
- Migration database từ v1 sang v2 chạy được: giữ nguyên 4 thương hiệu, 8 số và 1 ngoại lệ đã có trước lần rà soát; journal_mode chuyển sang WAL. Không reset dữ liệu người dùng. Bản sao SQL trước nâng cấp chỉ lưu cục bộ trong `.tooling/qa/pre-review.sql`, không đưa vào Git.
- Cuộc gọi Telecom 0900000001 vẫn BLOCK đúng Aster; 0900000003 vẫn ALLOW do whitelist.
- EXPLAIN QUERY PLAN xác nhận tra numbers bằng primary-key index, brands bằng integer primary key, allowed bằng covering index; không quét toàn catalog để lọc một cuộc gọi. Đây là xác minh cách truy vấn, chưa phải benchmark tải lớn.
- Nhập tệp kiểm tra với 1 thương hiệu mới, 3 dòng số (2 số khác nhau): preview báo đúng 1 thương hiệu mới, 2 số mới, 1 trùng; checkbox khôi phục ngoại lệ mặc định chưa chọn.
- Xác nhận nhập khi chưa chọn checkbox: thêm đúng 2 số, thương hiệu mới vẫn blocked=false dù tệp có blocked=true; whitelist không tăng; có updated_at.
- Nhập lại: 0 thương hiệu/số mới, số không bị nhân đôi. Khi chọn checkbox khôi phục, whitelist mới tăng đúng 1 số.
- Tệp xung đột gán số Aster sang thương hiệu khác: thông báo rõ và không có nút commit; tổng số thương hiệu/số không thay đổi, cả mục hợp lệ đi cùng cũng chưa được ghi.
- Tìm thương hiệu và tìm số trong lịch sử hoạt động; card hiển thị ngoại lệ và ngày cập nhật; editor hiển thị số hợp lệ/trùng/lỗi. Preview có thể cuộn, lịch sử dùng query 25 mục mỗi trang.
- Đã xóa riêng thương hiệu và ngoại lệ kiểm tra do phiên rà soát tạo qua UI. Cuối cùng trở về đúng 4 thương hiệu / 8 số / 1 ngoại lệ; foreign_key_check không báo lỗi. Không xóa lịch sử hoặc danh mục của người dùng.

Nhãn cuộc gọi được đổi thành **CUỘC GỌI HỆ THỐNG** để không gây hiểu nhầm cuộc gọi emulator là cuộc gọi SIM. Giới hạn lưu hiện tại là riêng 500 lượt hệ thống và 100 mô phỏng. Chưa kiểm tra đạt giới hạn với hàng trăm bản ghi, chạy trên điện thoại SIM thật, mọi trường hợp hủy/rotation trong I/O, hoặc benchmark dữ liệu lớn. Những giới hạn xác minh này vẫn giữ nguyên.

## Cải tiến giao diện trực quan

- Tổng quan có thẻ gradient, hình khiên, trạng thái ngắn, thống kê bằng icon và hai thao tác nhanh. Thanh điều hướng bốn tab có icon và dấu chọn rõ.
- Thương hiệu có avatar chữ đầu, công tắc, nhãn bật/tắt và ngoại lệ; bấm cả thẻ để chỉnh sửa. Giữ ghi chú/ngày cập nhật trong chi tiết.
- Lịch sử rút gọn thành số, thương hiệu/lý do ngắn, nhãn màu, thời gian và nguồn; bấm để mở đầy đủ lý do và thao tác. Đã sửa lỗi nhãn bên phải bị co chiều rộng và xác minh trên bản cài cuối.
- Cài đặt chia nhóm quyền, cách bảo vệ, ngoại lệ, dữ liệu và dọn lịch sử; giải thích dài mở từ mục quyền riêng tư. Icon vẽ bằng Canvas với Path tạo sẵn, không thêm thư viện UI hay tải tài nguyên mạng.
- Build cuối: assembleDebug và lintDebug thành công; No issues found. Cài lại thành công và mở ứng dụng trên emulator.
- Đã kiểm tra trực tiếp bốn tab, mở editor từ thẻ, mở form thêm từ dấu cộng, mở mô phỏng từ Tổng quan, mở lý do cuộc gọi, bộ lọc nhóm đã chặn, tìm Aster, xóa tìm kiếm và cuộn Cài đặt đến mục quyền riêng tư. Không ghi dữ liệu qua các form trong lần kiểm tra UI này.
- Truy vấn database cuối vẫn 4 thương hiệu / 8 số / 1 ngoại lệ. Logcat AndroidRuntime không có lỗi trong các thao tác kiểm tra. Emulator được để tại Tổng quan; ảnh mới ở docs/screenshots/overview.png.
- Lần này tập trung UI; không chạy lại toàn bộ các kịch bản Telecom/import hoặc kiểm tra mọi cỡ chữ, độ rộng màn hình. Các giới hạn thiết bị vật lý nêu trên vẫn áp dụng.
