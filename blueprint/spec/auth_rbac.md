# Đặc tả: Tính năng xác thực và phân quyền (RBAC)

## Mô tả
Tính năng này cung cấp cơ chế xác thực (authentication) người dùng và phân quyền truy cập (authorization) dựa trên vai trò (role-based access control). Hệ thống hỗ trợ đăng ký, đăng nhập, quản lý session, và kiểm soát quyền truy cập vào các endpoint/tính năng dựa trên vai trò của người dùng.

## Luồng chính

### Luồng Đăng ký (Registration)
1. Người dùng nhập email, password, thông tin cá nhân
2. Auth Service validate dữ liệu (email format, password strength)
3. Kiểm tra email đã tồn tại: nếu có, trả về lỗi
4. Hash password bằng bcrypt
5. Tạo record User và Account trong database
6. Gán vai trò mặc định (STUDENT)
7. Publish event `registration.confirmed` để Notification Service gửi email xác nhận
8. Trả về success, yêu cầu người dùng kiểm tra email

### Luồng Đăng nhập (Login)
1. Người dùng gửi email + password
2. Auth Service tìm User theo email
3. Kiểm tra password: nếu sai, trả về lỗi 401 và không logging lý do (bảo mật)
4. Nếu đúng: Tạo JWT token (7 ngày expiry) + Refresh token (90 ngày)
5. Lưu Refresh token vào database với HttpOnly cookie
6. Trả về JWT token trong response body + Refresh token trong secure cookie
7. Client sử dụng JWT token cho các request tiếp theo

### Luồng Refresh Token
1. JWT token hết hạn, client gửi refresh token
2. Auth Service validate refresh token từ cookie
3. Kiểm tra xem token có tồn tại trong database, chưa expire
4. Nếu hợp lệ: Tạo JWT token mới, trả về
5. Nếu không hợp lệ: Trả về 401, yêu cầu login lại

### Luồng Phân quyền (Authorization)
1. Client gửi request với JWT token
2. API Gateway verify JWT signature + expiry
3. Trích xuất `role` từ JWT payload
4. Kiểm tra endpoint có yêu cầu role nào: nếu có, so sánh với role của user
5. Nếu user có quyền: Cho phép request tiếp tục
6. Nếu user không có quyền: Trả về 403 Forbidden

### Các vai trò (Roles)
- **ADMIN**: Quản lý toàn bộ hệ thống, có thể tạo/sửa/xóa workshop
- **ORGANIZER**: Tạo và quản lý workshop riêng của họ
- **STUDENT**: Đăng ký và tham gia workshop
- **STAFF**: Hỗ trợ check-in, kiểm tra QR code

## Kịch bản lỗi
- **Email đã tồn tại khi đăng ký**: Trả về 400 Bad Request với thông báo "Email đã được sử dụng"
- **Password quá yếu** (< 8 ký tự, không có chữ và số): Trả về 400 với hướng dẫn yêu cầu
- **JWT token bị giả mạo**: Verify fail, trả về 401 Unauthorized
- **JWT token hết hạn**: Trả về 401, client phải refresh token
- **Refresh token hết hạn**: Trả về 401, user phải login lại
- **Login quá nhiều lần sai (brute force)**: Khóa account sau 5 lần fail trong 15 phút, gửi email cảnh báo
- **Session khác đăng nhập cùng tài khoản**: Session cũ bị invalidate, user ở phiên cũ bị logout
- **User bị xóa sau khi login**: JWT vẫn valid đến khi expire, lần access tiếp theo được reject

## Ràng buộc
- **Bảo mật mật khẩu**: Phải mã hóa bcrypt với salt, không được lưu plaintext
- **Token expiry**: JWT 7 ngày, Refresh token 90 ngày (không configurable runtime)
- **Độ dài password**: Tối thiểu 8 ký tự, phải có chữ và số
- **Email validation**: Phải là email hợp lệ, có thể là edu.vn hoặc domain khác
- **Brute force protection**: Khóa account sau 5 lần sai trong 15 phút
- **Rate limiting**: Tối đa 10 request login/refresh per minute per IP
- **HTTPS**: Tất cả giao tiếp phải dùng HTTPS, không được HTTP
- **CORS**: Chỉ cho phép domain của client (whitelist trong config)
- **Logout**: Xóa refresh token khỏi database, không thể dùng được nữa

## Tiêu chí chấp nhận
1. User có thể đăng ký với email mới và nhận email xác nhận
2. User có thể đăng nhập với email/password đúng và nhận JWT token
3. JWT token có thể dùng để truy cập endpoint protected
4. JWT token hết hạn sau 7 ngày, refresh token cấp token mới
5. User không thể truy cập endpoint không có quyền (403)
6. Password quá yếu bị từ chối
7. Đăng nhập sai 5 lần, account bị khóa 15 phút
8. Logout xóa refresh token, không thể refresh nữa
9. Token giả mạo bị reject
10. Hai session không thể chạy đồng thời với cùng tài khoản