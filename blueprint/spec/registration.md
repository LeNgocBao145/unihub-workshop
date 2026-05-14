# Đặc tả: Đăng ký Workshop và Quản lý Chỗ ngồi

## Mô tả
Tính năng này cho phép sinh viên đăng ký tham gia workshop với cơ chế quản lý chỗ ngồi (slots). Hệ thống phải xử lý tình huống nhiều sinh viên đăng ký cùng lúc (race condition), đảm bảo không vượt quá số chỗ trống, và cung cấp danh sách chờ (waitlist) nếu hết chỗ. Hệ thống tích hợp thanh toán, xác thực người dùng, và gửi email xác nhận.

## Luồng chính

### Luồng Đăng ký Cơ bản
1. User (sinh viên đã login) vào trang chi tiết workshop
2. Click nút "Đăng ký", hệ thống gửi POST request:
   ```
   POST /workshops/{workshopId}/register
   body: {
     "workshopId": "WS-001",
     "notes": "Tôi muốn tham gia workshop này"
   }
   ```
3. Workshop Service validate:
   - User đã authenticated (có JWT token)
   - Workshop còn hạn đăng ký (registrationDeadline > now)
   - User không đã đăng ký workshop này (check unique constraint)
   - Workshop còn chỗ trống (availableSlots > 0)
4. Nếu valid: 
   - Ghi lock row `Workshop` để chống race condition
   - Decrease availableSlots by 1
   - Tạo record `Registration` với status = PENDING_PAYMENT
   - Release lock
   - Publish event `registration.created`
   - Trả về HTTP 201 Created với registration ID
5. Notification Service gửi email xác nhận (chứa QR code, hướng dẫn)
6. User chuyển tới trang thanh toán

### Luồng Chờ (Waitlist)
1. Nếu availableSlots = 0 và workshop chưa full waitlist:
   - Check nếu `waitlist.size() < max_waitlist` (e.g., 50 người)
   - Tạo record `Waitlist` với status = WAITING, position = current_size + 1
   - Trả về HTTP 202 Accepted với thông báo "Bạn đã được thêm vào danh sách chờ"
   - Gửi email thông báo
2. Khi có người rút khỏi hoặc hoàn tiền:
   - Tìm người đầu tiên trong waitlist
   - Auto-add vào registration (hoặc gửi email "Có chỗ trống, click để confirm")
   - Update vị trí của các người tiếp theo
   - Publish event `waitlist.promoted`

### Luồng Thanh toán & Confirm
1. User click "Thanh toán"
2. Workshop Service gọi Payment Service: `chargePayment(registrationId, workshopPrice)`
3. User quét QR code, chuyển khoản
4. Payment Service verify webhook, publish `payment.completed`
5. Workshop Service nhận event:
   - Update registration.status = CONFIRMED
   - Update workshop.totalRegistered += 1 (hoặc just query để chính xác)
   - Publish event `registration.confirmed`
6. Notification Service gửi email xác nhận đăng ký thành công (kèm QR code, giờ check-in)

### Luồng Hoàn tiền & Rút khỏi
1. User hoặc admin request rút khỏi workshop
2. Kiểm tra điều kiện:
   - Registration status phải là CONFIRMED hoặc PENDING_PAYMENT
   - Nếu < 7 ngày trước workshop: không được rút (hoặc có penalty)
3. Nếu có thanh toán:
   - Gọi Payment Service: `refund(paymentId, amount)`
   - Xóa hoặc archive registration
4. Nếu chưa thanh toán:
   - Xóa registration
   - availableSlots += 1
5. Publish event `registration.cancelled`
6. Notification Service gửi email xác nhận hoàn tiền
7. Nếu có waitlist:
   - Promote người đầu tiên trong waitlist
   - Gửi email "Có chỗ trống cho bạn, click để confirm"

### Luồng Lấy thông tin Registrations
1. User GET `/my-registrations` để xem các workshop đã đăng ký
2. Workshop Service query database (có caching Redis):
   ```sql
   SELECT r.*, w.* FROM registrations r
   JOIN workshops w ON r.workshopId = w.id
   WHERE r.userId = :userId
   ORDER BY w.startDate DESC
   ```
3. Format response:
   ```json
   {
     "id": "REG-001",
     "workshop": {
       "id": "WS-001",
       "name": "Web Development Basics",
       "date": "2026-06-15",
       "time": "14:00-17:00",
       "location": "Room 101, Building A"
     },
     "status": "CONFIRMED",
     "qrCode": "...",
     "amount": 500000,
     "paymentStatus": "COMPLETED",
     "checkedIn": false
   }
   ```

## Kịch bản lỗi

### Race Condition & Concurrency
- **Nhiều user click đăng ký cùng lúc**: Database lock + transaction đảm bảo chỉ 1 người được, người khác receive "Workshop hết chỗ"
- **Decrease availableSlots < 0**: Database constraint (availableSlots >= 0), request fail
- **Xóa registration khi updating waitlist**: Transaction rollback tất cả, user được notify retry

### Tình trạng không hợp lệ
- **User chưa login**: Trả về 401 Unauthorized
- **Workshop không tồn tại**: Trả về 404 Not Found
- **Workshop hết hạn đăng ký**: Trả về 400 "Đã hết hạn đăng ký"
- **User đã đăng ký rồi**: Trả về 409 Conflict "Bạn đã đăng ký workshop này"
- **Waitlist full (>50)**: Trả về 503 "Danh sách chờ đầy, không thể đăng ký"

### Thanh toán & Refund
- **Payment timeout**: Registration vẫn PENDING_PAYMENT, user có 30 phút để thanh toán lại
- **Refund fail**: Log error, notify admin, user không thể rút khỏi tạm thời
- **Hoàn tiền sau khi đã check-in**: Không được phép (check registration.checkedIn flag)

### Data Inconsistency
- **Registration bị xóa nhưng Payment vẫn exist**: Một số database cleanup job chạy định kỳ để fix
- **availableSlots < 0**: Database constraint + monitoring alert
- **Waitlist position không sequential**: Data cleanup job reorder positions nightly

## Ràng buộc
- **Concurrency**: Database-level locking (SELECT ... FOR UPDATE) để prevent race condition
- **Deadlock prevention**: Luôn acquire locks theo thứ tự: Workshop → Registration → Waitlist
- **Timeout payment**: Nếu PENDING_PAYMENT > 30 phút → tự động cancel, release slot
- **Refund deadline**: Chỉ được refund trước 7 ngày workshop
- **Waitlist size**: Tối đa 50 người
- **Duplicate prevention**: Unique constraint (userId, workshopId) trên bảng Registration
- **Slot management**: availableSlots không được âm, database constraint CHECK (availableSlots >= 0)
- **User authentication**: Phải có JWT token hợp lệ
- **Rate limiting**: Tối đa 20 registration requests per minute per user
- **Audit log**: Mỗi registration/cancellation/refund được log với timestamp, userId, workshopId

## Tiêu chí chấp nhận
1. User có thể đăng ký workshop còn chỗ trống
2. Đăng ký thành công trả về HTTP 201 + registration ID
3. Email xác nhận được gửi trong 5 giây
4. Khi slot đầy, user được add vào waitlist thay vì lỗi
5. Waitlist có tối đa 50 người, vượt quá bị từ chối
6. Khi ai đó rút khỏi, người đầu tiên trong waitlist tự động được promote
7. Thanh toán xong, registration status = CONFIRMED
8. User không thể đăng ký 2 lần cùng 1 workshop
9. User có thể rút khỏi trước 7 ngày workshop
10. Hoàn tiền được xử lý trong 5-10 phút
11. Khi hoàn tiền, slot được release, người chờ được promote
12. Multi-thread scenario: race condition được handle (chỉ 1 người successful)
13. availableSlots không bao giờ âm
14. Hoàn tiền sau khi check-in được reject