# QuietCall

Ứng dụng Android chặn cuộc gọi theo **thương hiệu → nhiều số điện thoại**, phục vụ đồ án môn Phát triển ứng dụng đa nền tảng. Phạm vi Android được chọn theo điều kiện đề bài cho phép sản phẩm chỉ hoạt động trên Android.

## Ý tưởng và phạm vi

Người dùng thêm hoặc nhập danh mục thương hiệu, tìm tên (hỗ trợ tìm không dấu), rồi bật chặn cả nhóm số. Android không tự cung cấp danh tính thương hiệu cho mọi số gọi đến; QuietCall nhận diện bằng số đã có trong danh mục. Số mới cần được bổ sung. Không có tuyên bố nhận diện tất cả số của một doanh nghiệp.

**Tính năng:**

- Chặn/từ chối cuộc gọi đến bằng Android `CallScreeningService`; yêu cầu bỏ thông báo cuộc gọi bị chặn.
- Tùy chọn tắt chuông (cuộc gọi vẫn có thể hiện trên màn hình).
- Thêm/sửa/xóa thương hiệu, ghi chú nguồn và nhiều số; bật/tắt theo thương hiệu.
- Chuẩn hóa số Việt Nam `+84`, `0084`, `0`; số quốc tế khác giữ mã nước.
- Danh sách luôn cho phép; bảo vệ số trong danh bạ nếu có quyền.
- Lịch sử lưu riêng 500 cuộc gọi hệ thống và 100 mô phỏng; tìm kiếm và phân trang 25 mục.
- Mô phỏng dùng cùng bộ quyết định với cuộc gọi thật.
- Nhập/xuất danh mục JSON qua trình chọn tệp Android; xem trước thay đổi/xung đột, nhập gộp nguyên tử, không tự bật chặn thương hiệu mới. Khôi phục whitelist cần chọn rõ.
- Ghi chú nguồn, ngày cập nhật danh mục, số ngoại lệ theo thương hiệu, tạm dừng nhanh và sao chép số từ lịch sử.
- Giao diện bốn tab có icon, thẻ trạng thái bảo vệ, avatar thương hiệu, nhãn màu kết quả và thao tác nhanh; lý do chi tiết mở khi bấm cuộc gọi.
- Không có quyền Internet, không đọc call log, không yêu cầu tài khoản. SQLite lưu cục bộ; tắt sao lưu hệ thống.

## Yêu cầu

- Android 10 (API 29) trở lên. Máy có SIM để kiểm tra cuộc gọi thật.
- Chọn QuietCall làm ứng dụng **Caller ID & spam / lọc cuộc gọi** trong hộp thoại hệ thống.
- Quyền danh bạ là tùy chọn. Khi không cấp quyền, Android thường chỉ đưa cuộc gọi ngoài danh bạ tới dịch vụ lọc.
- Không chặn cuộc gọi VoIP trong ứng dụng Zalo/Messenger. Hành vi thông báo có thể khác theo hãng máy.

## Dùng thử trên máy đã chuẩn bị

Nhấp đúp `run-local.cmd` để mở ứng dụng trên máy ảo Android. Nhấp đúp `demo-call.cmd` để chọn cuộc gọi Telecom giả lập: thử số thứ nhất/thứ hai của Aster, số được cho phép riêng và số của Nova. Không có cuộc gọi ra mạng điện thoại thật. Xem [hướng dẫn chạy cục bộ](docs/LOCAL_RUN.md).

## Build

Mở thư mục này bằng Android Studio, dùng JDK 17 hoặc 21, cài SDK Platform 35 và Build Tools 34.0.0. Tạo `local.properties` nếu IDE chưa tự tạo:

```properties
sdk.dir=C:/Users/YOUR_USER/AppData/Local/Android/Sdk
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

macOS/Linux:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Bản debug được ký tự động, cài được và phù hợp bài nộp thử nghiệm. CI tạo cùng loại APK trong artifact `QuietCall-debug-apk`. Bản phát hành production cần khóa ký riêng; không commit khóa ký.

## Cài đặt và sử dụng

1. Chép APK vào điện thoại Android 10+, mở tệp và chọn Cài đặt. Nếu Android hỏi, cho phép ứng dụng đang mở tệp cài ứng dụng từ nguồn này.
2. Mở QuietCall, nhấn **Kích hoạt lọc cuộc gọi** và chọn QuietCall trong hộp thoại Caller ID & spam của Android.
3. Vào **Thương hiệu**, nhấn **+**, nhập tên và các số liên quan. Mỗi dòng một số hoặc ngăn cách bằng dấu phẩy/chấm phẩy. Lưu rồi bật công tắc chặn cả nhóm.
4. Bấm thẻ thương hiệu để sửa tên, ghi chú nguồn hoặc bổ sung số. Tìm kiếm hỗ trợ tên có/không dấu; có bộ lọc nhóm đang chặn.
5. **Lịch sử** hiển thị kết quả và nguồn Hệ thống/Mô phỏng. Bấm một lượt để xem đầy đủ lý do, thêm ngoại lệ, gán nhóm hoặc sao chép số.
6. **Cài đặt → Danh sách ngoại lệ** quản lý số luôn cho phép. Ngoại lệ được ưu tiên hơn quy tắc chặn. Quyền danh bạ là tùy chọn để bảo vệ số liên hệ đã lưu.
7. Bật **Chỉ tắt chuông** nếu muốn giữ cuộc gọi nhưng không đổ chuông; giao diện cuộc gọi vẫn có thể xuất hiện. Nút tạm dừng ở Tổng quan cho phép cuộc gọi tạm thời.
8. **Nhập/Xuất danh mục** dùng JSON. Xem trước rồi xác nhận gộp; nhóm mới chưa tự bật chặn. Khôi phục ngoại lệ cần chọn riêng.

Bản cài mới có danh mục trống, không kèm số điện thoại hay cấu hình cá nhân từ máy phát triển.

### Demo không cần SIM

1. **Cài đặt → Danh mục demo**, xác nhận nạp. Các nhóm mặc định chưa chặn.
2. **Thương hiệu → Aster Finance · Demo**, bật công tắc chặn.
3. **Tổng quan → Thử quy tắc**, nhập `0900000001`: **Đã chặn**. Thử `+84 900 000 002`: cũng **Đã chặn**, cùng nhóm.
4. Thử `0900000011`: **Cho phép** vì Nova chưa bật chặn.
5. Trong Lịch sử, bấm số Aster rồi chọn **Luôn cho phép số này**; mô phỏng lại: **Cho phép**.

Mô phỏng dùng cùng bộ quyết định với dịch vụ Android; trạng thái quyền hệ thống không ảnh hưởng mô phỏng. Để kiểm tra cuộc gọi thật, dùng số của điện thoại thứ hai do bạn kiểm soát và cấp vai trò lọc trên máy nhận.

**Mọi thương hiệu và số trong demo là giả lập. Không gọi các số demo.** Muốn kiểm tra cuộc gọi thật, thay bằng số của điện thoại thứ hai do bạn kiểm soát.

Nếu không chặn, kiểm tra vai trò lọc, trạng thái bảo vệ, công tắc nhóm, số đã lưu, ngoại lệ và bảo vệ danh bạ. Ứng dụng chỉ nhận diện số trong danh mục; số mới cần bổ sung. Lịch sử chỉ chứa các lượt ứng dụng nhận xử lý, không phải toàn bộ nhật ký điện thoại. Đã kiểm tra trên emulator Android 10; chưa xác minh cuộc gọi SIM trên điện thoại vật lý.

## Nộp bài

Gửi link [GitHub: Tienchan27/quiet-call](https://github.com/Tienchan27/quiet-call) và tệp `app/build/outputs/apk/debug/app-debug.apk` cho giảng viên. Nếu gửi APK qua Google Drive, upload tệp này và bật quyền xem cho người nhận.

Mã nguồn nằm trên nhánh `main`. Git bỏ qua SDK/emulator cục bộ, cache, database cá nhân, khóa ký và đầu ra build. Workflow GitHub Actions tạo APK trong artifact `QuietCall-debug-apk` khi build thành công.

## Tài liệu kỹ thuật

- [Kiến trúc và quyết định kỹ thuật](docs/ARCHITECTURE.md)
- [Chạy trên máy đã chuẩn bị](docs/LOCAL_RUN.md)
- [Danh mục JSON mẫu](samples/demo-catalog.json)

## Công nghệ

Kotlin 2.0.21 · Android Gradle Plugin 8.7.3 · Gradle 8.9 · Android SDK 35 · SQLite · Android Views. Giao diện native gọn, không cần backend hoặc khóa API; domain không phụ thuộc Android, có thể tách thành module Kotlin Multiplatform sau này.

## Tài liệu nền tảng

- [Android: Screen calls](https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls)
- [CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService)
- [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)

Mã nguồn MIT. Danh mục thực tế do người dùng cung cấp, cần kiểm tra nguồn và độ chính xác trước khi bật chặn.
