# Dùng thử trực tiếp trên máy này

Project đã có Android SDK, Gradle và máy ảo **QuietCall / Pixel 2 / Android 10** trong `.tooling` (không đưa lên Git). JDK 21 của máy được dùng để biên dịch.

## Mở lại ứng dụng

Nhấp đúp `run-local.cmd` ở thư mục project, hoặc chạy:

```powershell
.\scripts\run-local.ps1
```

Script biên dịch source, mở cửa sổ máy ảo, cài bản build phát triển và chạy QuietCall. Đây là bản chạy cục bộ, chưa phải gói APK bàn giao. Nếu không sửa source, mở nhanh:

```powershell
.\scripts\run-local.ps1 -SkipBuild
```

Máy ảo dùng cổng 5554. Không chạy song song một máy ảo khác trên cùng cổng. Nếu bạn đóng cửa sổ Android, chạy script để mở lại; dữ liệu SQLite vẫn được lưu.

Script đặt cửa sổ emulator ở vị trí (20, 20) và tỷ lệ 25% để thấy toàn bộ điện thoại trên màn hình laptop. Có thể kéo góc cửa sổ để phóng lớn tùy màn hình. Để cuộn nội dung ứng dụng, kéo bên trong màn hình điện thoại; kéo thanh tiêu đề chỉ di chuyển cửa sổ emulator.

## Tương tác

Dùng chuột bấm nút, kéo màn hình để cuộn, nhập số qua bàn phím Android. Bốn tab nằm dưới cùng. Dữ liệu demo ghi rõ Demo và không đại diện doanh nghiệp thật.

1. **Thương hiệu**: bật/tắt nhóm Aster Finance; xem các số trong nhóm.
2. **Tổng quan → Mở mô phỏng**: nhập `0900000001` để xem quyết định.
3. **Cài đặt**: thêm whitelist, thay chế độ tắt chuông, nhập/xuất JSON; xem trước thay đổi trước khi xác nhận gộp và chỉ khôi phục whitelist khi chọn rõ.
4. **Lịch sử**: phân biệt mô phỏng với cuộc gọi do Telecom máy ảo chuyển vào.

## Cuộc gọi Telecom trên emulator

Máy ảo có thể nhận cuộc gọi do console giả lập để kiểm tra tích hợp Android, không cần gọi số thật:

Nhấp đúp **demo-call.cmd** để chọn số gọi đến từ menu, hoặc dùng lệnh bên dưới. Menu chỉ tạo cuộc gọi trên máy ảo; không liên hệ các số thật.

```powershell
.\.tooling\sdk\platform-tools\adb.exe -s emulator-5554 emu gsm call 0900000001
.\.tooling\sdk\platform-tools\adb.exe -s emulator-5554 emu gsm cancel 0900000001
```

Chọn vai trò lọc, bật protection và chặn thương hiệu tương ứng trước. Nhãn CUỘC GỌI HỆ THỐNG trong app nghĩa là callback Telecom (khác mô phỏng nội bộ); trên emulator nó vẫn là cuộc gọi giả lập của hệ thống, không phải cuộc gọi qua SIM.

## Mở trong IDE

Mở thư mục project trong Android Studio, chọn JDK 17/21 và SDK local hoặc SDK của IDE. Với máy này `local.properties` đã trỏ vào `.tooling/sdk`. Không commit file đó vì đường dẫn phụ thuộc máy.
