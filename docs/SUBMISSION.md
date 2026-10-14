# Bàn giao và nộp bài

## GitHub

Repository: [Tienchan27/quiet-call](https://github.com/Tienchan27/quiet-call), nhánh `main`, remote `origin`. Bộ project nằm trong `deliverables/`, xem README để cài và dùng thử. Không push `.tooling`, `local.properties`, khóa ký, `deliverables` hoặc `build`. Workflow `.github/workflows/android.yml` build APK và upload artifact cho mỗi lần push/PR/manual run.

```powershell
git add .
git commit -m "Build QuietCall Android brand call blocker"
git branch -M main
git push -u origin main
```

Nếu đã có git/remote thì không tạo lại. File LICENSE chọn MIT cho mã nguồn.

## APK và Google Drive

APK bàn giao nằm trong `deliverables/QuietCall-1.0.0-debug.apk` sau build thành công. Đây là debug APK đã ký, cài được để chấm đồ án. Upload tệp lên Drive, bật quyền “Anyone with the link / Viewer”, rồi kiểm tra link ở cửa sổ chưa đăng nhập. Nếu muốn bản production, dùng Android Studio → Generate Signed Bundle/APK với khóa ký riêng.

Kèm mã SHA-256 trong `deliverables/SHA256.txt` để đối chiếu tệp. Không đưa khóa ký hoặc dữ liệu số điện thoại thật lên repo.

## Email mẫu

**Tiêu đề:** [Phát triển ứng dụng đa nền tảng] Nộp đồ án QuietCall – [Họ tên] – [MSSV]

Kính gửi Thầy,

Em xin gửi sản phẩm đồ án QuietCall, ứng dụng Android chặn cuộc gọi theo thương hiệu. Người dùng có thể nhập tên thương hiệu, quản lý nhiều số liên quan và bật chặn cho toàn bộ nhóm. Ứng dụng có danh sách cho phép, lịch sử xử lý, nhập/xuất danh mục và mô phỏng để kiểm tra quy tắc.

- Repository: https://github.com/Tienchan27/quiet-call
- APK: [DÁN LINK GOOGLE DRIVE]
- Hướng dẫn sử dụng và demo: README và docs/DEMO.md trong repository.

Yêu cầu Android 10 trở lên. Sau khi cài, chọn QuietCall làm ứng dụng lọc cuộc gọi trong hộp thoại hệ thống. Nhận diện thương hiệu dựa trên danh mục số đã lưu; số mới cần bổ sung. Dữ liệu mẫu là giả lập.

Em cảm ơn Thầy.

[Họ tên] – [MSSV] – [Lớp]

## Trước khi gửi

Điền tên/MSSV/lớp, kiểm tra repo truy cập được, Drive không yêu cầu xin quyền và APK đúng bản. Không gửi email tự động khi chưa có địa chỉ giảng viên và nội dung cá nhân hoàn chỉnh.
