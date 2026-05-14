# Đặc tả: Tóm tắt AI từ PDF

## Mô tả
Tính năng này cho phép người dùng tải lên tài liệu PDF liên quan đến workshop (tài liệu hướng dẫn, tài liệu học tập, bài giảng, v.v.) và hệ thống sẽ sử dụng API AI (như Claude, GPT) để tạo ra một bản tóm tắt tự động bằng tiếng Việt. Bản tóm tắt này được hiển thị trên trang chi tiết workshop, giúp sinh viên nhanh chóng hiểu được nội dung chính của workshop mà không cần đọc toàn bộ tài liệu.

## Luồng chính
1. **Tải lên PDF**: Người quản lý workshop tải lên file PDF của workshop
2. **Lưu trữ**: File PDF được lưu trữ trên AWS S3 hoặc dịch vụ cloud tương tự
3. **Gửi yêu cầu tóm tắt**: Workshop Service gửi URL của PDF đến AI Service qua HTTP
4. **Xử lý AI**: AI Service đọc PDF, trích xuất text, và tạo bản tóm tắt bằng LLM API
5. **Lưu trữ kết quả**: Bản tóm tắt được lưu vào database (bảng Workshop.ai_summary)
6. **Hiển thị**: Khi người dùng xem chi tiết workshop, hệ thống lấy bản tóm tắt từ cache (Redis) hoặc database và hiển thị

## Kịch bản lỗi
- **PDF không hợp lệ hoặc hỏng**: Hệ thống ghi log lỗi, thông báo cho người quản lý, workshop hiển thị mà không có bản tóm tắt
- **AI Service timeout (> 30s)**: Hủy yêu cầu, ghi log, workshop hiển thị mà không có bản tóm tắt
- **Lỗi kết nối S3**: Ghi log lỗi, không thể truy cập PDF, thông báo lỗi cho người quản lý
- **API AI quá nhiều requests (Rate limit)**: Retry với exponential backoff (1s, 2s, 4s), nếu vẫn thất bại thì skip tóm tắt
- **LLM trả về text không hợp lệ**: Ghi log, lưu placeholder text, workshop vẫn hiển thị bình thường
- **Database error khi lưu**: Retry transaction, nếu thất bại thì cache trong Redis tạm thời

## Ràng buộc
- **Hiệu năng**: Quá trình tóm tắt không được chặn UI, phải chạy asynchronously (background job)
- **Chi phí**: Mỗi lần tóm tắt tốn chi phí API (tính theo token), nên cache kết quả lâu dài (TTL: 30 ngày)
- **Kích thước**: Chỉ hỗ trợ PDF dưới 10MB, giới hạn độ dài bản tóm tắt (tối đa 1000 ký tự)
- **Ngôn ngữ**: Bản tóm tắt phải bằng tiếng Việt, cần prompt rõ ràng cho AI
- **Độ chính xác**: Bản tóm tắt phải giữ lại những thông tin chính (80% overhead từ nội dung gốc)
- **Bảo mật**: PDF có thể chứa thông tin nhạy cảm, không được lưu logs chi tiết về nội dung

## Tiêu chí chấp nhận
1. Người quản lý có thể tải lên PDF và thấy thông báo "Đang xử lý bản tóm tắt"
2. Sau 2-5 giây, bản tóm tắt xuất hiện trên trang chi tiết workshop
3. Bản tóm tắt viết bằng tiếng Việt, ngắn gọn nhưng đủ thông tin
4. Khi reload trang, bản tóm tắt được load nhanh từ cache
5. Nếu tóm tắt thất bại (timeout, error), workshop vẫn hiển thị bình thường mà không bản tóm tắt
6. Không có bản tóm tắt cũ khi tải PDF mới lên (cache được làm mới)
7. PDF > 10MB bị từ chối với thông báo lỗi rõ ràng