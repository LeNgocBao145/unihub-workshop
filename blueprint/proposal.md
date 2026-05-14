# UniHub Workshop — Project Proposal

## Vấn đề
Hiện nay, Trường Đại học A đang tổ chức sự kiện “Tuần lễ kỹ năng và nghề nghiệp” hàng năm, diễn ra trong 5 ngày với khoảng 8–12 workshop hoạt động song song mỗi ngày tại nhiều phòng khác nhau. Quy trình quản lý đăng ký hiện tại đang được thực hiện thông qua Google Form và việc gửi thông báo được làm thủ công qua email. Quy trình này đã bộc lộ nhiều hạn chế và không còn khả năng đáp ứng được nhu cầu thực tế khi quy mô của sự kiện ngày càng mở rộng.

## Mục tiêu
Mục tiêu cốt lõi của dự án là xây dựng một hệ thống mang tên **UniHub Workshop** nhằm số hóa toàn bộ quy trình của sự kiện, từ giai đoạn đăng ký cho đến khi check-in tại cửa phòng.
Về mặt hiệu năng, hệ thống phải đáp ứng và chịu tải được lượng truy cập khổng lồ: dự kiến có khoảng 12.000 sinh viên sẽ truy cập hệ thống trong 10 phút đầu tiên khi mở cổng đăng ký, đặc biệt là 60% lượng truy cập này sẽ dồn dập trong 3 phút đầu.

## Người dùng và nhu cầu
Hệ thống phục vụ 3 nhóm người dùng chính với các quyền hạn được kiểm soát chặt chẽ:
* **Sinh viên:** Có nhu cầu xem lịch trình các workshop, tiến hành đăng ký, nhận thông báo xác nhận và thực hiện check-in khi đến tham dự sự kiện. Họ chỉ có quyền xem và đăng ký.
* **Ban tổ chức:** Có nhu cầu tạo mới và quản lý workshop, cũng như theo dõi số lượng sinh viên đã đăng ký. Nhóm này có quyền hạn cao nhất bao gồm tạo, sửa, hủy workshop và xem các báo cáo thống kê.
* **Nhân sự check-in:** Cần một công cụ tiện lợi để xác nhận sinh viên tham dự ngay tại cửa phòng thông qua mobile app. Họ chỉ được cấp quyền truy cập vào chức năng quét mã QR.

## Phạm vi
**Trong phạm vi dự án (In-scope):**
* Cung cấp danh sách workshop theo thời gian thực (số chỗ còn lại, thông tin diễn giả, phòng).
* Hỗ trợ luồng đăng ký có phí và miễn phí, cấp mã QR check-in.
* Hệ thống thông báo qua email, app (có thiết kế mở để dễ dàng thêm kênh Telegram sau này).
* Trang Web Admin cho ban tổ chức quản lý sự kiện.
* Mobile App cho nhân sự quét mã QR check-in.
* Tích hợp mô hình AI để xử lý, làm sạch và tóm tắt file PDF giới thiệu workshop.
* Luồng nhập và đồng bộ dữ liệu sinh viên định kỳ.

**Ngoài phạm vi dự án (Out-of-scope):**
* Hệ thống không tích hợp API trực tiếp với hệ thống quản lý sinh viên cũ của trường.
* Không xây dựng cổng thanh toán riêng mà chỉ tích hợp và xử lý các kịch bản lỗi khi làm việc với cổng thanh toán bên thứ 3.

## Rủi ro và ràng buộc
Hệ thống cần được thiết kế kiến trúc để giải quyết 5 rủi ro và ràng buộc kỹ thuật chính:
1. **Tranh chấp chỗ ngồi:** Hệ thống phải đảm bảo không xảy ra tình trạng hai người cùng lấy được chỗ cuối cùng khi có hàng trăm sinh viên tranh nhau 60 suất đăng ký cùng lúc.
2. **Tải trọng đột biến:** Phải có cơ chế bảo vệ backend API, đảm bảo tính công bằng và tránh sập hệ thống khi 12.000 sinh viên ập vào đăng ký trong thời gian ngắn.
3. **Thanh toán không ổn định:** Khi cổng thanh toán gặp lỗi hoặc timeout, các tính năng xem lịch vẫn phải hoạt động bình thường. Đặc biệt, phải chống được việc trừ tiền hai lần khi xử lý giao dịch.
4. **Check-in offline:** Tại các khu vực sóng yếu hoặc mất mạng, mobile app phải cho phép check-in tạm thời và đảm bảo không mất dữ liệu khi tự động đồng bộ lại lúc có mạng.
5. **Tích hợp một chiều:** Chỉ có thể lấy dữ liệu sinh viên qua file CSV export định kỳ vào ban đêm. Hệ thống phải xử lý được các rủi ro như file lỗi, dữ liệu trùng lặp mà không làm gián đoạn dịch vụ đang chạy.