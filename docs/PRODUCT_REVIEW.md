# Rà soát và cải tiến QuietCall — 14/10/2026

## Quyết định sản phẩm

QuietCall giải quyết việc quản lý và chặn **nhóm số đã biết** theo thương hiệu. Người dùng nhập hoặc bổ sung danh mục từ nguồn họ kiểm soát. Nhận diện tự động số mới không thuộc phạm vi; không cần thêm AI, tài khoản, backend hoặc API caller-ID để hoàn thành đồ án.

Giá trị cốt lõi là giảm số thao tác chặn, làm rõ ngoại lệ, quản lý dữ liệu thuận tiện và không chặn nhầm vì trạng thái khó hiểu. Số lạ chưa có trong danh mục được cho phép.

## Những vấn đề tìm thấy và thay đổi

| Góc nhìn | Vấn đề trước đây | Cải tiến đã triển khai |
|---|---|---|
| Business | Nhập danh mục làm người dùng khó biết ảnh hưởng | Xem trước số thương hiệu mới, số mới, số trùng, số mới vào nhóm đang bật chặn và xung đột |
| Business | Không có dấu thời gian cập nhật danh mục | Lưu ngày chỉnh sửa/nhập; hiển thị khi có dữ liệu. Đây là ngày cập nhật cục bộ, không phải xác nhận nguồn đã được kiểm chứng |
| Chức năng | Whitelist từ tệp được nhập tự động | Chỉ khôi phục số cho phép khi user chọn checkbox; mặc định chỉ nhập thương hiệu |
| Chức năng | Mô phỏng có thể đẩy mất lịch sử cuộc gọi | Giới hạn riêng: 500 cuộc gọi hệ thống + 100 mô phỏng; có nút dọn riêng mô phỏng |
| Chức năng | Cần chuyển tab/đào lại dữ liệu để sửa ngoại lệ | Bật/bỏ whitelist và sao chép số ngay từ lịch sử; xem nhanh ba cuộc gọi gần đây |
| UX | “Đang chặn” dễ gây hiểu nhầm khi chưa cấp vai trò hoặc protection tắt | Phân biệt quyền chưa cấp, bảo vệ tạm dừng, chưa chọn nhóm và đang hoạt động. Trong danh mục dùng “Quy tắc chặn bật/tắt” |
| UX | Thương hiệu bị chặn có số whitelist nhưng card không thể hiện | Hiện số ngoại lệ cho phép trong từng thương hiệu |
| UX | Bật công tắc không cập nhật text/lọc ngay | Cập nhật card và danh sách lọc sau mỗi thay đổi |
| UX | Form lỗi báo vào trường số dù lỗi tên | Báo lỗi inline, đưa lỗi về trường phù hợp, cuộn đến lỗi; đếm số hợp lệ/trùng/lỗi trước lưu |
| UX | Thông tin quá dài, khó tìm cuộc gọi | Header gọn hơn, tìm số/thương hiệu trong history, phân trang và gần đây trên Tổng quan |
| UX | Chữ phụ trên nền sáng còn nhạt | Tăng độ đậm màu chữ phụ; trạng thái trống khi tìm kiếm hướng dẫn đổi từ khóa thay vì yêu cầu kích hoạt lọc |
| Hiệu năng | Mỗi cuộc gọi đọc toàn bộ thương hiệu/số và whitelist | Một câu SQL dùng các khóa đã lập chỉ mục, trả đúng thương hiệu/số/ngoại lệ và preferences trong cùng snapshot đọc |
| Hiệu năng | Lookup danh bạ cả khi số không nằm trong nhóm chặn | Chỉ lookup khi cần quyết định ngoại lệ cho số khớp quy tắc đang bật |
| Hiệu năng | Tìm thương hiệu đọc DB sau mỗi ký tự | Cache danh mục trong màn hình, debounce 220 ms; lọc tại bộ nhớ |
| Hiệu năng | 500 lịch sử tạo hàng nghìn Views trong một lần | Truy vấn/render 25 mục mỗi trang; whitelist manager 20 mục, preview cài đặt 5 số |
| Hiệu năng | Import lặp lại việc tải toàn bộ catalog mỗi thương hiệu | Tra thương hiệu theo name_key và chỉ lấy số của thương hiệu đó |
| Độ ổn định | Export chạy I/O trên main; import dùng chung helper của Activity | Preview/import/export chạy worker với repository riêng; đóng theo lifecycle, màn hình tiến trình ngăn sửa đồng thời |
| Độ ổn định | Số có + ở giữa hoặc nhiều + vẫn được chấp nhận | Kiểm tra định dạng + chỉ ở đầu, chặn mã quốc tế bắt đầu bằng 0; dùng Regex được tái sử dụng |
| Dữ liệu | Schema v1 chưa có migration thực tế | Schema v2 thêm updated_at/index lịch sử; giữ danh mục/quy tắc/whitelist, không reset database |

## Phạm vi tiếp theo phù hợp

Ưu tiên sau khi dùng thử thực tế:

1. Kiểm tra điện thoại có SIM trên các hãng máy mục tiêu, quyền danh bạ và hành vi thông báo. Đây là bước xác minh cần thiết nhất trước nộp.
2. Cân nhắc undo sau khi xóa thương hiệu, gán lại số giữa nhóm và lưu nguồn dữ liệu có cấu trúc nếu người dùng thực sự cần; hiện có xác nhận và ghi chú nguồn.
3. Khi số lượng dữ liệu lớn hơn phạm vi đồ án: chuyển UI list sang RecyclerView và tải metadata từng trang, tách Activity thành các controller/ViewModel. Hiện mỗi trang render hữu hạn nhưng danh mục thương hiệu vẫn được cache trong bộ nhớ.
4. Nếu có nhu cầu chia sẻ thật: phân phối catalog có phiên bản, nguồn và quy trình duyệt. Không suy ra tên thương hiệu cho số mới; người quản lý danh mục vẫn phải xác nhận ánh xạ.

## Những hướng chưa nên thêm

- Chặn mọi số lạ hoặc chặn đầu số rộng: tăng nguy cơ chặn người cần liên hệ.
- Đăng nhập/cloud analytics/monetization: tăng phụ thuộc và chưa tạo giá trị cho mục tiêu đồ án.
- Tuyên bố tự nhận diện tất cả số của thương hiệu: không đúng khả năng dữ liệu hiện có và không thuộc yêu cầu đã chốt.
- Nâng cấp thư viện hoặc chuyển framework chỉ để thay đổi stack: ưu tiên chức năng hoạt động và kiểm tra trên thiết bị thật.

## Cách đánh giá

Đo thành công bằng việc chọn một thương hiệu có nhiều số, chặn đúng cả nhóm, cho phép ngoại lệ đúng, xem lý do và cập nhật danh mục mà không mất dữ liệu. Không dùng số lượng màn hình hoặc lượng dependency làm thước đo chất lượng. Không viết automated tests theo yêu cầu; xác minh bằng compiler, Lint, UI và cuộc gọi Telecom emulator. Kết quả thực tế được ghi trong VERIFICATION.md.
