# Đặc tả: Đồng bộ dữ liệu Sinh viên từ CSV

## Mô tả
Tính năng này cho phép nhà trường tải lên file CSV chứa danh sách sinh viên (kèm thông tin chuyên ngành, lớp, bộ môn) và hệ thống sẽ tự động phân tích, validate, và import dữ liệu vào cơ sở dữ liệu. Quá trình này là asynchronous, giảm tải cho API, và cung cấp báo cáo chi tiết về số sinh viên imported, số lỗi, v.v.

## Luồng chính

### Luồng Khởi tạo Import
1. Người quản lý (ADMIN/ORGANIZER) tải lên file CSV qua endpoint
2. API Gateway nhận file, upload lên AWS S3, lưu URL vào database
3. Tạo record DataImportRecord với status = PENDING
4. Publish event `data-import.requested` với URL của CSV lên RabbitMQ
5. Trả về HTTP 202 Accepted với import ID cho client
6. Client có thể poll hoặc subscribe để biết tiến độ

### Luồng Xử lý CSV (Data Import Service)
1. Data Import Service nhận message từ RabbitMQ
2. Download CSV từ URL (S3)
3. Parse CSV, kiểm tra header (cột bắt buộc: email, fullName, className, majorName, deptName)
4. Validate từng row:
   - Email phải hợp lệ
   - fullName không được trống
   - className, majorName, deptName phải tồn tại hoặc tạo mới
5. Kiểm tra duplicate: nếu email đã có trong DB, skip hoặc update (configurable)
6. Batch insert: insert 1000 rows một lần để hiệu năng
7. Ghi lại failed rows (row number, lý do)
8. Update DataImportRecord: status = COMPLETED, tổng rows, successful, failed
9. Publish event `data-import.completed` với kết quả
10. Notification Service có thể gửi email report (nếu cần)

### Cấu trúc CSV
```
email,fullName,className,majorName,deptName
student1@example.edu.vn,Nguyễn Văn A,CNTT-K45,Công Nghệ Thông Tin,Khoa CNTT
student2@example.edu.vn,Trần Thị B,CNTT-K45,Công Nghệ Thông Tin,Khoa CNTT
```

## Kịch bản lỗi
- **File CSV không hợp lệ** (encoding sai, format sai): Trả về error chi tiết, yêu cầu format lại
- **CSV quá lớn** (> 50MB): Từ chối với thông báo, yêu cầu chia nhỏ file
- **CSV rỗng hoặc chỉ có header**: Import 0 rows, status = COMPLETED với 0 successful
- **Cột bắt buộc bị thiếu**: Reject toàn bộ file, trả về lỗi chi tiết
- **Email không hợp lệ ở row nào đó**: Skip row đó, log reason, continue
- **className/majorName/deptName không tồn tại**: Auto-create nếu enabled, hoặc skip row
- **Duplicate email**: Có 3 tùy chọn (configurable):
  1. Skip (bỏ qua)
  2. Update (cập nhật thông tin cũ)
  3. Error (reject toàn bộ)
- **Database connection error**: Retry với exponential backoff, nếu vẫn fail sau 3 lần thì FAILED
- **Download CSV từ S3 timeout**: Retry 3 lần, nếu vẫn fail thì FAILED
- **RabbitMQ connection lost**: Message được retry tự động

## Ràng buộc
- **Kích thước file**: Tối đa 50MB, hỗ trợ UTF-8 encoding
- **Số lượng rows**: Tối đa 100,000 rows per file
- **Format date**: DD/MM/YYYY hoặc YYYY-MM-DD (configurable)
- **Batch size**: Insert 1000 rows một lần để avoid memory issue
- **Timeout**: Download CSV phải xong trong 30s, process phải xong trong 5 phút
- **Retention**: DataImportRecord lưu lâu dài (7 năm), CSV trên S3 xóa sau 7 ngày
- **Quyền**: Chỉ ADMIN/ORGANIZER có quyền import
- **Concurrent imports**: Tối đa 5 import cùng lúc per service
- **Notification**: Gửi email report sau mỗi import (ADMIN nhận)

## Tiêu chí chấp nhận
1. Upload file CSV, API trả về import ID ngay (202 Accepted)
2. Có thể polling endpoint để xem tiến độ (status: PENDING/PROCESSING/COMPLETED/FAILED)
3. Sau khi completed, số sinh viên mới xuất hiện trong database
4. Report import hiển thị: tổng rows, successful, failed, error details
5. Invalid rows được skip, không làm fail toàn bộ import
6. Duplicate emails được xử lý theo config (skip/update/error)
7. className, majorName, deptName được tự động tạo nếu không tồn tại
8. File CSV quá lớn (> 50MB) bị từ chối ngay
9. Email notification được gửi sau import hoàn thành
10. Import log lưu lâu dài, có thể tra cứu lịch sử
11. Dữ liệu invalid không được insert vào database
12. Concurrent imports xử lý tuần tự, không race condition