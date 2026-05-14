# Đặc tả: Hệ thống Thông báo

## Mô tả
Tính năng này cung cấp hệ thống thông báo tự động qua email cho các sự kiện quan trọng của hệ thống (đăng ký workshop, xác nhận thanh toán, nhắc nhở trước workshop, check-in thành công, v.v.). Notification Service là service độc lập, nhận events từ RabbitMQ, và gửi email qua SMTP. Hệ thống hỗ trợ template email động, localization tiếng Việt, và embedded content (QR code).

## Luồng chính

### Luồng Thông báo Đăng ký (Registration Confirmation)
1. User hoàn tất đăng ký workshop, Workshop Service publish event `registration.confirmed`
2. Event chứa: userId, userEmail, userName, workshopId, workshopName, registrationId
3. Notification Service nhận event từ RabbitMQ
4. Tạo QR code từ registration token (dùng cho check-in offline)
5. Load template `registration-confirmation.html` (Thymeleaf)
6. Render template với dữ liệu: 
   - Tên workshop
   - Ngày/giờ workshop
   - Thông tin thanh toán (nếu có)
   - QR code embedded
7. Gửi email qua Gmail SMTP
8. Log transaction: email sent, timestamp, status

### Luồng Thông báo Thanh toán (Payment Confirmation)
1. Payment Service hoàn tất giao dịch, publish `payment.completed`
2. Event chứa: registrationId, paymentId, amount, method, timestamp
3. Notification Service tạo template `payment-confirmation.html`
4. Render: tên workshop, số tiền, phương thức, mã giao dịch
5. Gửi email tới địa chỉ email liên kết với registration

### Luồng Thông báo Nhắc nhở (Workshop Reminder)
1. Cron job chạy hàng ngày lúc 10 AM
2. Tìm các workshop sắp diễn ra trong 24 giờ tới
3. Tìm tất cả registrations của workshop đó
4. Publish event `workshop.reminder` cho mỗi registration
5. Notification Service gửi email với nội dung:
   - Tên workshop, địa điểm, thời gian
   - Hướng dẫn check-in
   - Link QR code (nếu check-in offline)

### Luồng Template Rendering
1. Load template từ `/resources/templates/email/[language]/[event-type].html`
2. Render Thymeleaf template với data context
3. Replace variables: ${workshopName}, ${qrCode}, v.v.
4. Xử lý i18n: thời gian theo timezone VN, text tiếng Việt
5. Embed QR code as base64 trong email (không attachment)

## Kịch bản lỗi
- **Email address không hợp lệ**: Log warning, skip gửi, đánh dấu trong database
- **SMTP connection timeout**: Retry với exponential backoff (1s, 2s, 4s, 8s), tối đa 5 lần
- **RabbitMQ connection lost**: Notification Service tự động reconnect, message queue lưu lại
- **Template file không tìm thấy**: Log error, sử dụng template fallback (default/generic template)
- **QR code generation fail**: Ghi log, gửi email mà không QR code, registration vẫn có hiệu lực
- **Thymeleaf rendering error**: Log detailed error, không gửi email, retry sau 5 phút
- **Email quá large** (> 10MB): Reject, log warning, không gửi
- **Rate limit Gmail API**: Retry sau 1 giờ, queue message lại
- **User unsubscribe**: Check flag trong User.emailNotificationEnabled, skip gửi

## Ràng buộc
- **Email provider**: Gmail SMTP (credentials từ environment variables, không hardcode)
- **Template location**: `/src/main/resources/templates/email/vi/` cho tiếng Việt
- **Timeout**: SMTP request phải xong trong 10 giây
- **Rate limiting**: Tối đa 100 emails per minute per account
- **Retry policy**: Exponential backoff, tối đa 5 lần retry
- **QR code size**: 200x200 pixels, format PNG embedded as base64
- **Email encoding**: UTF-8, content-type: text/html
- **Time zone**: Vietnam Time (UTC+7)
- **Async**: Gửi email không được blocking request đến các service khác
- **Audit log**: Mỗi email gửi được log: recipient, event type, timestamp, status
- **Unsubscribe**: Hỗ trợ unsubscribe link (mailto hoặc web link)

## Tiêu chí chấp nhận
1. Sau khi user đăng ký workshop, nhận email xác nhận trong 5 giây
2. Email chứa tên workshop, ngày/giờ, QR code, hướng dẫn check-in
3. QR code có thể scan được để check-in
4. Email viết bằng tiếng Việt, format đẹp mắt
5. Khi thanh toán xong, nhận email xác nhận thanh toán
6. Email nhắc nhở được gửi 24 giờ trước workshop
7. User có thể tắt/bật thông báo email trong cài đặt
8. Nếu SMTP fail, email được retry tự động, không mất
9. Admin có thể xem log gửi email (who, when, status)
10. Template email responsive trên mobile
11. Email không bao giờ chứa password hoặc token nhạy cảm
12. Không gửi email lặp lại (có cơ chế dedup)