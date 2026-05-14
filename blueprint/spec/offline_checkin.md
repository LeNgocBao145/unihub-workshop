# Đặc tả: Check-in offline

## Mô tả
Tính năng cho phép STAFF quét mã QR để check-in người tham dự ngay cả khi thiết bị mất mạng. Nếu không có kết nối, registrationId sẽ được lưu tạm vào hàng đợi offline bằng AsyncStorage và hệ thống sẽ tự đồng bộ lại khi mạng được khôi phục.

## Luồng chính
1. Người dùng mở màn hình Check-in và cấp quyền camera nếu cần.
2. Ứng dụng quét QR để lấy registrationId.
3. Hook check-in kiểm tra trạng thái mạng bằng NetInfo.
4. Nếu đang online, ứng dụng gọi API check-in trực tiếp.
5. Nếu đang offline hoặc phát sinh lỗi mạng không có response, registrationId được thêm vào offline queue trong AsyncStorage.
6. Khi có mạng trở lại hoặc khi app khởi động, sync service đọc queue và gửi lần lượt các check-in còn tồn đọng lên server.
7. Các phần tử đã đồng bộ thành công được loại khỏi queue, phần còn lỗi sẽ được giữ lại để thử lại sau.

## Kịch bản lỗi
- Mất mạng khi quét QR: dữ liệu vẫn được lưu offline thay vì thất bại ngay.
- Lỗi mạng trong lúc gọi API: fallback sang lưu queue offline.
- Dữ liệu QR trùng với phần tử đã có trong queue: không thêm bản ghi trùng lặp.
- Server trả lỗi nghiệp vụ hoặc lỗi không liên quan đến mạng: hiển thị toast thất bại và không tự chuyển sang offline.
- Đồng bộ lại khi mạng chưa sẵn sàng: sync service dừng và chờ lần sau.

## Ràng buộc
- Chỉ hỗ trợ quét QR cho check-in, không áp dụng cho loại barcode khác.
- Queue offline được lưu cục bộ bằng AsyncStorage, nên dữ liệu phụ thuộc vào bộ nhớ thiết bị.
- Chỉ đồng bộ khi NetInfo xác nhận có kết nối mạng.
- Cần giữ tính nhất quán giữa server và queue: mục nào sync lỗi phải được giữ lại để thử lại.
- Không được tạo bản ghi trùng trong queue cho cùng một registrationId.

## Tiêu chí chấp nhận
- Khi online, quét QR tạo check-in thành công trên server và hiển thị thông báo thành công.
- Khi offline, quét QR vẫn ghi nhận được vào queue và hiển thị thông báo đã lưu offline.
- Khi mạng quay lại, các check-in đã lưu offline được tự động đồng bộ lên hệ thống.
- Sau đồng bộ thành công, queue offline giảm đúng số lượng item đã xử lý.
- Không phát sinh bản ghi trùng khi quét lại cùng một QR đã nằm trong queue.