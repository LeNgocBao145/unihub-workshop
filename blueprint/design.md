# UniHub Workshop — Technical Design

## 1. Kiến trúc tổng thể
<!-- Mô tả architectural style được chọn và lý do.
     Hệ thống gồm những thành phần nào? Chúng giao tiếp với nhau như thế nào? -->

## 2. C4 Diagram

### 2.1 Level 1 — System Context

![Link ảnh bị lỗi](https://i.postimg.cc/sfTFfsnD/system-context.png "System Context")

### 2.2 Level 2 — Container
![Link ảnh bị lỗi](https://i.postimg.cc/brZWZ2Jw/container.png "Container")

## 3. High-Level Architecture Diagram
![Link ảnh bị lỗi](https://i.postimg.cc/cLG1Nd92/High-Level-Architecture-Diagram.png "Level Architecture Diagram")

## 4. Thiết kế cơ sở dữ liệu
### 4.1. Quyết định kiến trúc
Hệ thống sử dụng hệ quản trị cơ sở dữ liệu quan hệ (RDBMS), cụ thể là **PostgreSQL**. Đây là lựa chọn tối ưu để đảm bảo tính nhất quán của dữ liệu trong một hệ thống có các giao dịch tài chính và yêu cầu về tính chính xác cao.

### 4.2. Lý do lựa chọn (Biện luận kỹ thuật)

* **Tính toàn vẹn dữ liệu (ACID) tuyệt đối:**
  Hệ thống UniHub có luồng đăng ký workshop thu phí và yêu cầu chống trừ tiền hai lần. PostgreSQL đảm bảo tính `ACID` nghiêm ngặt, giúp các giao dịch thanh toán (giao tiếp với cổng thanh toán, cập nhật trạng thái) không bị sai lệch dữ liệu hay mất mát khi xảy ra sự cố.

* **Giải quyết triệt để bài toán tranh chấp (Concurrency Control):**
  Đối với vấn đề hàng trăm sinh viên giành nhau những chỗ ngồi cuối cùng, hệ thống phải đảm bảo không có hai người nhận cùng một chỗ. PostgreSQL hỗ trợ cơ chế khóa dòng cực tốt (Row-level locking với `SELECT ... FOR UPDATE`), giúp các transaction đăng ký diễn ra tuần tự và an toàn trên cùng một bản ghi workshop.

* **Mô hình dữ liệu có tính cấu trúc và quan hệ cao:**
  Các thực thể trong hệ thống như Sinh viên, Workshop, Lịch trình, và Phòng học có mối quan hệ phụ thuộc rõ ràng với nhau. Mô hình quan hệ (Relational) là cách tiếp cận tự nhiên và tối ưu nhất để biểu diễn các liên kết này.

* **Sự linh hoạt với kiểu dữ liệu phi cấu trúc (JSONB):**
  Tuy là SQL, PostgreSQL hỗ trợ kiểu dữ liệu `JSONB` rất mạnh mẽ. Điều này đặc biệt hữu ích để lưu trữ bản tóm tắt workshop được trả về từ mô hình AI, hoặc để lưu trữ tạm thời các payload check-in offline có cấu trúc chưa đồng nhất trước khi chuẩn hóa.
## Thiết kế kiểm soát truy cập
<!-- Mô hình phân quyền, các nhóm người dùng, cách kiểm tra quyền tại từng điểm truy cập -->

## Thiết kế các cơ chế bảo vệ hệ thống

### Kiểm soát tải đột biến
<!-- Giải pháp, thuật toán, ngưỡng, hành vi khi vượt ngưỡng -->

### Xử lý cổng thanh toán không ổn định
<!-- Giải pháp, các trạng thái, ngưỡng kích hoạt, hành vi khi lỗi -->

### Chống trừ tiền hai lần
<!-- Cơ chế, nơi lưu trữ, TTL, luồng xử lý khi phát hiện trùng lặp -->

## Các quyết định kỹ thuật quan trọng (ADR)
<!-- Với mỗi quyết định lớn: lựa chọn gì, tại sao, đánh đổi gì.
     Ví dụ: SQL vs NoSQL, JWT vs Session, Kafka vs RabbitMQ, ... -->