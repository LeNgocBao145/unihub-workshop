# Đặc tả: Quản lý Workshop

## Mô tả
Tính năng này cho phép người tổ chức (ORGANIZER) tạo, sửa, xóa và quản lý các workshop. Mỗi workshop có thông tin cơ bản (tên, mô tả, giờ, địa điểm, số chỗ), tích hợp AI tóm tắt PDF, quản lý danh sách sinh viên đã đăng ký, và cung cấp các thống kê (số lượng đăng ký, doanh thu, tỉ lệ check-in, v.v.).

## Luồng chính

### Luồng Tạo Workshop
1. ORGANIZER click "Tạo Workshop Mới", mở form
2. Nhập thông tin:
   - Tên workshop (required, max 200 ký tự)
   - Mô tả (required, max 2000 ký tự)
   - Ngày/giờ bắt đầu (required, must > now)
   - Ngày/giờ kết thúc (required, must > startDate)
   - Địa điểm (required, max 500 ký tự)
   - Số lượng chỗ (required, 10-10000)
   - Giá vé (optional, 0-50,000,000 VND)
   - Hạn đăng ký (required, must be before startDate)
   - PDF tài liệu (optional)
   - Mô tả thêm (optional)
3. Click "Tạo", gửi POST request:
   ```
   POST /workshops
   body: { ... }
   ```
4. Workshop Service validate dữ liệu
5. Nếu có PDF:
   - Upload lên AWS S3
   - Trigger AI tóm tắt (async)
6. Tạo record Workshop trong database:
   - Gán organizerId = currentUser.id
   - availableSlots = totalSlots
   - status = DRAFT (chưa public)
   - totalRegistered = 0
7. Trả về HTTP 201 + workshop ID
8. ORGANIZER được redirect tới trang chi tiết, có thể edit hoặc publish

### Luồng Publish Workshop
1. ORGANIZER click "Publish"
2. Validation:
   - Tất cả required fields có giá trị
   - Ngày/giờ valid
   - Ít nhất 1 chỗ còn trống
3. Update workshop.status = PUBLISHED, publishedAt = now
4. Workshop hiện trên danh sách công khai
5. Có thể unpublish sau đó để chỉnh sửa

### Luồng Sửa Workshop
1. ORGANIZER click "Sửa" trên trang chi tiết
2. Load form với dữ liệu hiện tại
3. Chỉnh sửa các field (một số field không được chỉnh nếu đã có đăng ký):
   - Có thể chỉnh: tên, mô tả, địa điểm, hạn đăng ký
   - KHÔNG được chỉnh: ngày/giờ, số lượng chỗ (nếu đã có đăng ký)
4. Click "Lưu"
5. Update database, invalidate cache (Redis)
6. Trả về HTTP 200

### Luồng Xóa Workshop
1. ORGANIZER click "Xóa"
2. Confirm dialog: "Xóa workshop sẽ xóa tất cả đăng ký và không thể khôi phục"
3. Kiểm tra:
   - Workshop phải status = DRAFT hoặc đã kết thúc (endDate < now)
   - Nếu có đăng ký: phải refund hết trước khi xóa
4. Nếu OK:
   - Soft delete: update deletedAt = now (keep for audit)
   - Hoặc hard delete (configurable)
   - Xóa file PDF trên S3
5. Trả về HTTP 204 No Content

### Luồng Lấy danh sách Workshop
1. User GET `/workshops` với optional filters:
   ```
   GET /workshops?status=PUBLISHED&sortBy=date&page=1&limit=20
   ```
2. Workshop Service query database:
   ```sql
   SELECT * FROM workshops
   WHERE status = 'PUBLISHED' AND deletedAt IS NULL
   ORDER BY startDate ASC
   LIMIT 20 OFFSET 0
   ```
3. Với mỗi workshop, include:
   - availableSlots
   - totalRegistered
   - aiSummary (từ cache hoặc DB)
   - registrationDeadline
4. Cache kết quả vào Redis (TTL: 5 phút)
5. Trả về HTTP 200 + list

### Luồng Lấy chi tiết Workshop
1. User GET `/workshops/{workshopId}`
2. Workshop Service query:
   ```sql
   SELECT w.*, COUNT(r.id) AS totalRegistered
   FROM workshops w
   LEFT JOIN registrations r ON r.workshopId = w.id AND r.status = 'CONFIRMED'
   WHERE w.id = :workshopId
   GROUP BY w.id
   ```
3. Check cache (Redis) trước
4. Response include:
   ```json
   {
     "id": "WS-001",
     "name": "Web Development Basics",
     "description": "Learn HTML, CSS, JavaScript",
     "startDate": "2026-06-15",
     "startTime": "14:00",
     "endDate": "2026-06-15",
     "endTime": "17:00",
     "location": "Room 101, Building A",
     "totalSlots": 50,
     "availableSlots": 25,
     "totalRegistered": 25,
     "price": 500000,
     "registrationDeadline": "2026-06-14",
     "aiSummary": "Workshop này dạy...",
     "organizer": {
       "id": "USR-001",
       "name": "Trần Văn A",
       "email": "a@example.com"
     },
     "status": "PUBLISHED",
     "canRegister": true,
     "registrationStatus": "PENDING" // for current user
   }
   ```
5. Cache kết quả TTL 10 phút

### Luồng Xem danh sách Registrations
1. ORGANIZER GET `/workshops/{workshopId}/registrations`
2. Query:
   ```sql
   SELECT r.*, u.email, u.name, p.status AS paymentStatus
   FROM registrations r
   JOIN users u ON r.userId = u.id
   LEFT JOIN payments p ON r.id = p.registrationId
   WHERE r.workshopId = :workshopId
   ORDER BY r.createdAt DESC
   ```
3. Filter options:
   - status: PENDING_PAYMENT, CONFIRMED, CHECKED_IN, CANCELLED
   - paymentStatus: PENDING, COMPLETED, FAILED
4. Response:
   ```json
   {
     "total": 45,
     "registrations": [
       {
         "id": "REG-001",
         "user": {...},
         "status": "CONFIRMED",
         "registeredAt": "2026-06-10",
         "paymentStatus": "COMPLETED",
         "checkedIn": false
       }
     ]
   }
   ```

### Luồng Xem Thống kê
1. ORGANIZER click "Thống kê" trên trang workshop
2. Workshop Service tính toán:
   - Tổng đăng ký: COUNT(registrations)
   - Đã thanh toán: COUNT(registrations WHERE paymentStatus = 'COMPLETED')
   - Chưa thanh toán: COUNT(registrations WHERE paymentStatus = 'PENDING')
   - Đã check-in: COUNT(registrations WHERE checkedIn = true)
   - Tỉ lệ check-in: checkedIn / totalRegistered
   - Doanh thu: SUM(payments.amount WHERE status = 'COMPLETED')
   - Danh sách chờ: COUNT(waitlist)
3. Return dashboard với biểu đồ:
   - Trend: số đăng ký theo ngày (line chart)
   - Revenue: doanh thu accumulative (bar chart)
   - Status distribution: pie chart (PENDING, CONFIRMED, CHECKED_IN)

## Kịch bản lỗi

### Validation Errors
- **Tên workshop trống**: Trả về 400 "Tên workshop bắt buộc"
- **Ngày kết thúc < ngày bắt đầu**: Trả về 400 "Ngày kết thúc phải sau ngày bắt đầu"
- **Ngày bắt đầu <= now**: Trả về 400 "Workshop phải trong tương lai"
- **Số lượng chỗ < 1**: Trả về 400 "Số lượng chỗ phải >= 1"
- **Hạn đăng ký >= ngày bắt đầu**: Trả về 400 "Hạn đăng ký phải trước ngày bắt đầu"
- **Giá <= 0 hoặc > 50,000,000**: Trả về 400 "Giá không hợp lệ"

### Quyền hạn
- **Người không phải ORGANIZER hoặc ADMIN**: Không thể tạo/sửa/xóa → 403 Forbidden
- **ORGANIZER sửa workshop của người khác**: 403 Forbidden
- **User không login**: 401 Unauthorized

### Xóa Workshop
- **Workshop có đăng ký chưa refund**: Trả về 400 "Phải hoàn tiền tất cả đăng ký trước khi xóa"
- **Workshop đang diễn ra**: Không được xóa (locked)

### AI Tóm tắt
- **PDF upload fail**: Log error, workshop vẫn được tạo nhưng không có summary
- **AI timeout**: Retry 3 lần, nếu vẫn fail thì skip summary
- **PDF invalid**: Ghi log, thông báo cho organizer, không block tạo workshop

### Cache Issues
- **Cache miss**: Query database, update cache
- **Cache invalidation fail**: Fallback to database

## Ràng buộc
- **Tên workshop**: 1-200 ký tự, không được trống
- **Mô tả**: 1-2000 ký tự
- **Địa điểm**: 1-500 ký tự
- **Số chỗ**: 10-10000
- **Giá vé**: 0-50,000,000 VND, không decimal
- **Ngày/giờ**: Phải > now + 1 giờ (để có thời gian chuẩn bị)
- **Hạn đăng ký**: < startDate, không được trong quá khứ
- **PDF**: Tối đa 10MB, format PDF
- **Quyền**: ORGANIZER/ADMIN có thể tạo, ORGANIZER chỉ quản lý workshop riêng
- **Cache TTL**: Danh sách 5 phút, chi tiết 10 phút, registrations 2 phút
- **Soft delete**: Workshop bị xóa được lưu lâu dài (audit trail)
- **Concurrent edits**: Workshop không được edit khi đang publish/unpublish

## Tiêu chí chấp nhận
1. ORGANIZER có thể tạo workshop mới với đầy đủ thông tin
2. Tạo thành công trả về HTTP 201 + workshop ID
3. Workshop mới có status = DRAFT, không public
4. ORGANIZER có thể publish workshop để public tìm thấy
5. ORGANIZER có thể sửa workshop (trừ các field không thay đổi được)
6. ORGANIZER có thể xóa workshop (nếu chưa có đăng ký)
7. User có thể xem danh sách workshop PUBLISHED
8. User có thể xem chi tiết workshop kèm AI summary
9. ORGANIZER có thể xem danh sách đăng ký của workshop riêng
10. ORGANIZER có thể xem thống kê (đăng ký, doanh thu, check-in, v.v.)
11. Validation error được trả về ngay với thông báo rõ ràng
12. Không được xóa workshop khi có đăng ký chưa refund
13. Cache được invalidate khi workshop được update
14. PDF > 10MB bị reject
15. AI summary được load nhanh từ cache