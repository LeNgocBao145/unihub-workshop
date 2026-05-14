# Đặc tả: Xử lý thanh toán

## Mô tả
Tính năng này xử lý thanh toán cho đăng ký workshop thông qua cổng thanh toán SePay (hỗ trợ chuyển khoản ngân hàng). Hệ thống tạo QR code động, theo dõi trạng thái thanh toán real-time qua webhook, và cung cấp Server-Sent Events (SSE) để client xem trạng thái ngay lập tức. Payment Service có cơ chế resilience mạnh mẽ để xử lý gateway không ổn định.

## Luồng chính

### Luồng Tạo QR Code Thanh toán
1. User click "Thanh toán" sau khi đăng ký workshop
2. Workshop Service gọi Payment Service qua gRPC: `chargePayment(registrationId, amount, description)`
3. Payment Service tạo transaction record với status = PENDING
4. Gọi SePay API để tạo QR code:
   ```
   POST https://api.sepay.vn/v3/payment_gw/pay
   body: {
     "amount": 500000,
     "description": "Workshop: Web Development - Reg: REG-12345",
     "clientId": "...",
     "clientSecret": "...",
     "returnUrl": "https://app.unihub.edu.vn/payment-callback"
   }
   ```
5. SePay trả về:
   ```json
   {
     "code": 0,
     "message": "success",
     "data": {
       "qrCode": "iVBORw0KGgo...",
       "qrUrl": "https://img.sepay.vn/qr/...",
       "transactionId": "TXN-12345"
     }
   }
   ```
6. Lưu transaction record: paymentId, transactionId, qrUrl, status = PENDING, amount, createdAt
7. Cache QR code vào Redis (TTL: 30 phút)
8. Trả về gRPC response chứa QR code URL và payment ID
9. Workshop Service trả về HTTP response với QR code, client hiển thị

### Luồng Theo dõi Trạng thái (SSE)
1. Client subscribe SSE endpoint: `/payments/{paymentId}/stream`
2. Payment Service thiết lập SSE connection, lưu connection vào memory
3. Payment Service poll SePay API mỗi 3 giây:
   ```
   GET https://api.sepay.vn/v3/payment_gw/status?transactionId={txnId}
   ```
4. Nếu status = "completed": 
   - Update database: status = COMPLETED, completedAt = now
   - Publish event `payment.completed` tới RabbitMQ
   - Gửi SSE message: `{"status": "completed", "amount": 500000}`
   - Close SSE connection
5. Nếu timeout (> 30 phút): 
   - Update database: status = FAILED, failedAt = now
   - Gửi SSE message: `{"status": "expired"}`
   - Close SSE connection
6. Client nhận SSE message và update UI

### Luồng Webhook (SePay callback)
1. User chuyển khoản thành công, SePay gửi webhook:
   ```
   POST https://api.unihub.edu.vn/payments/webhook
   body: {
     "transactionId": "TXN-12345",
     "amount": 500000,
     "status": "completed",
     "timestamp": 1632459200,
     "signature": "..."
   }
   ```
2. Payment Service verify signature (HMAC-SHA256) bằng webhook secret
3. Tìm transaction record theo transactionId
4. Update status = COMPLETED, completedAt = now
5. Publish event `payment.completed` tới RabbitMQ
6. Trả về HTTP 200 OK để SePay biết webhook đã nhận
7. Workshop Service listen event, update registration.status = CONFIRMED

### Luồng Hoàn tiền (Refund) - Nếu cần
1. Admin hoặc user request hoàn tiền
2. Payment Service verify quyền + điều kiện hoàn tiền
3. Gọi SePay refund API:
   ```
   POST https://api.sepay.vn/v3/payment_gw/refund
   body: {
     "transactionId": "TXN-12345",
     "amount": 500000
   }
   ```
4. Update transaction: status = REFUNDED, refundedAt, refundAmount
5. Publish event `payment.refunded`
6. Notification Service gửi email xác nhận hoàn tiền

## Kịch bản lỗi

### Timeout & Mất kết nối
- **SePay API timeout (> 10s)**: Retry 3 lần với 2s delay, nếu vẫn fail thì throw exception
- **Network error**: Retry exponential backoff (1s, 2s, 4s), tối đa 5 lần
- **SSE connection lost**: Client reconnect tự động, Payment Service lưu last_status_sent
- **Webhook timeout**: SePay tự động retry 5 lần, Payment Service idempotent (check nếu đã xử lý)

### Gateway không ổn định
- **SePay response invalid JSON**: Log error, không update DB, notify admin
- **SePay trả về error code**: Ghi log, trả về error message chi tiết, không charge
- **SePay signature verify fail**: Reject webhook, log warning (potential attack)
- **Transaction không tìm thấy**: Log warning, skip, không update database

### Chống gian lận (Anti-fraud)
- **Duplicate payment**: Check nếu payment với amount + user này đã tồn tại trong 1 phút, reject
- **Amount mismatch**: Webhook claim $500 nhưng transaction set $300 → reject, notify admin
- **Timeout payment**: Nếu PENDING > 30 phút → auto-fail, user phải tạo payment mới
- **Rate limit**: User tối đa 10 lần tạo payment per hour

### Exception handling
- **gRPC call timeout từ Workshop Service**: Payment Service trả về error response
- **Database connection error**: Retry transaction, nếu vẫn fail thì return 500 error
- **Redis cache miss**: Query database, cập nhật cache

## Ràng buộc
- **Payment amount**: Tối thiểu 10,000 VND, tối đa 50,000,000 VND
- **Timeout payment**: Nếu PENDING > 30 phút → auto-fail
- **SSE timeout**: Nếu không có update > 30 phút → auto-close connection
- **Webhook signature**: HMAC-SHA256 với secret key từ environment
- **Webhook retry**: SePay retry 5 lần, Payment Service phải idempotent
- **Rate limiting**: Tối đa 10 charges per minute per user
- **Concurrent payment**: 1 registration không thể có 2 payment PENDING cùng lúc
- **Transaction ID**: Unique per payment, không được reuse
- **Amount precision**: 2 decimal places (VND không cần decimal nhưng API có thể)
- **Audit log**: Mỗi transaction log: paymentId, amount, status, timestamp, sePay response

## Tiêu chí chấp nhận
1. User click "Thanh toán", nhận QR code trong 2 giây
2. QR code quét được bằng các ứng dụng banking
3. Sau khi chuyển khoản, trạng thái update ngay (SSE hoặc polling)
4. Notification email được gửi sau payment completed
5. Registration được confirm sau payment completed
6. Nếu timeout (> 30 phút), payment tự động fail, user phải thanh toán lại
7. Webhook fail không làm mất dữ liệu (idempotent)
8. Duplicate payment được prevent
9. Admin có thể xem transaction log chi tiết
10. Hoàn tiền (refund) được xử lý nếu user request
11. User không thể charge 2 lần cho cùng 1 registration
12. Signature verify fail được detect và log (bảo mật)