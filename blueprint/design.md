# UniHub Workshop — Technical Design

## Kiến trúc tổng thể

### 1. Mô hình kiến trúc và Lý do lựa chọn
Hệ thống UniHub Workshop được thiết kế theo mô hình kiến trúc Distributed Monolith. Quyết định này được đưa ra dựa trên các yếu tố thực tế của dự án:
* **Phù hợp với nguồn lực:** Đội ngũ phát triển nhỏ (3 thành viên) có thể dễ dàng quản lý, phát triển và triển khai trên một khối mã nguồn duy nhất.
* **Tốc độ phát triển:** Cho phép xây dựng và hoàn thiện các tính năng nhanh chóng để đáp ứng kịp thời hạn của dự án.
* **Quy mô hệ thống:** Với quy mô của một ứng dụng quản lý sự kiện cấp trường, kiến trúc phân tán đảm bảo hiệu năng tốt mà không gây ra sự phức tạp quá mức trong việc vận hành.

### 2. Các thành phần chính của hệ thống
Hệ thống được cấu thành từ 3 lớp chính với các công nghệ đề xuất cụ thể:

* **Lớp giao diện (Client-side):**
  * **Web App (React):** Cung cấp giao diện cho sinh viên xem lịch và đăng ký; giao diện quản trị cho Ban tổ chức và Dashboard thống kê và giao diện dành cho nhân sự thực hiện quét mã QR check-in tại cửa phòng.
  * **Mobile App (React Native):** Ứng dụng dành cho nhân sự thực hiện quét mã QR check-in tại cửa phòng, hỗ trợ cả chế độ online và offline.
* **Lớp xử lý (Server-side):** Backend được thiết kế theo mô hình phân tách thành các service độc lập (chạy cùng trên một khối cơ sở dữ liệu chung) để dễ quản lý và mở rộng mã nguồn:
  * **Auth Service:** Chuyên xử lý định danh và phân quyền. Đảm nhiệm việc đăng nhập, đăng xuất, cấp phát và xác thực JWT cho cả 3 nhóm người dùng (Sinh viên, Ban tổ chức, Nhân sự check-in).
  * **Workshop Service:** Là core service xử lý nghiệp vụ chính. Quản lý toàn bộ thông tin sự kiện (tạo, sửa, hủy), điều phối luồng đăng ký giữ chỗ (tính toán số lượng slot, chống tranh chấp bằng cơ chế khóa), và ghi nhận dữ liệu check-in từ nhân sự.
  * **Payment Service:** Service chuyên biệt xử lý luồng giao dịch. Chịu trách nhiệm khởi tạo thanh toán, giao tiếp với cổng thanh toán bên thứ ba, xử lý trạng thái giao dịch (timeout, retry) và đảm bảo tính Idempotency (chống trừ tiền hai lần).
  * **Background Worker:** Một tiến trình chạy ngầm tách biệt để xử lý các tác vụ tốn tài nguyên, bất đồng bộ hoặc chạy định kỳ (như import dữ liệu sinh viên từ file CSV ban đêm, gửi email thông báo, hoặc gọi mô hình AI để tóm tắt tài liệu PDF).
* **Lớp dữ liệu (Data Layer):**
  * **Primary Database (PostgreSQL):** Đảm bảo tính toàn vẹn dữ liệu và hỗ trợ tốt các giao dịch (ACID) cho việc đặt vé và thanh toán.
  * **Cache Engine (Redis):** Lưu trữ thông tin về số chỗ trống và quản lý hàng chờ, giúp giảm tải cho database chính khi có hàng nghìn yêu cầu truy cập cùng lúc.

### 3. Cơ chế giao tiếp
Hệ thống kết hợp linh hoạt giữa hai hình thức giao tiếp để tối ưu trải nghiệm người dùng:
* **Giao tiếp đồng bộ (Synchronous):** Sử dụng HTTP/RESTful API cho các thao tác cần phản hồi ngay lập tức giữa Web/Mobile App và Backend.
* **Giao tiếp bất đồng bộ (Asynchronous):** Sử dụng Message Broker (như RabbitMQ) để xử lý các luồng công việc chạy ngầm (ví dụ: sau khi sinh viên đăng ký thành công, hệ thống sẽ đẩy một sự kiện vào hàng chờ để gửi Email xác nhận mà không làm gián đoạn luồng đăng ký của người dùng).
## C4 Diagram

### Level 1 — System Context

![Link ảnh bị lỗi](https://i.postimg.cc/sfTFfsnD/system-context.png "System Context")

### Level 2 — Container
![Link ảnh bị lỗi](https://i.postimg.cc/brZWZ2Jw/container.png "Container")

## High-Level Architecture Diagram
![Link ảnh bị lỗi](https://i.postimg.cc/cLG1Nd92/High-Level-Architecture-Diagram.png "Level Architecture Diagram")

## Thiết kế cơ sở dữ liệu
### Quyết định kiến trúc
Hệ thống sử dụng hệ quản trị cơ sở dữ liệu quan hệ (RDBMS), cụ thể là **PostgreSQL**. Đây là lựa chọn tối ưu để đảm bảo tính nhất quán của dữ liệu trong một hệ thống có các giao dịch tài chính và yêu cầu về tính chính xác cao. Phù hợp để lưu trữ dữ liệu của các thực thể có mối quan hệ với nhau.

### Lý do lựa chọn 

* **Tính toàn vẹn dữ liệu (ACID) tuyệt đối:**
  Hệ thống UniHub có luồng đăng ký workshop thu phí và yêu cầu chống trừ tiền hai lần. PostgreSQL đảm bảo tính `ACID` nghiêm ngặt, giúp các giao dịch thanh toán (giao tiếp với cổng thanh toán, cập nhật trạng thái) không bị sai lệch dữ liệu hay mất mát khi xảy ra sự cố.

* **Giải quyết triệt để bài toán tranh chấp (Concurrency Control):**
  Đối với vấn đề hàng trăm sinh viên giành nhau những chỗ ngồi cuối cùng, hệ thống phải đảm bảo không có hai người nhận cùng một chỗ. PostgreSQL hỗ trợ cơ chế khóa dòng cực tốt (Row-level locking với `SELECT ... FOR UPDATE`), giúp các transaction đăng ký diễn ra tuần tự và an toàn trên cùng một bản ghi workshop.

* **Mô hình dữ liệu có tính cấu trúc và quan hệ cao:**
  Các thực thể trong hệ thống như Sinh viên, Workshop, Lịch trình, và Phòng học có mối quan hệ phụ thuộc rõ ràng với nhau. Mô hình quan hệ (Relational) là cách tiếp cận tự nhiên và tối ưu nhất để biểu diễn các liên kết này.

### Schema
![Link ảnh bị lỗi](https://i.postimg.cc/dVT09mmw/database.png "Level Architecture Diagram")

## Thiết kế kiểm soát truy cập
### Mô hình: Role-Based Access Control (RBAC)
Phù hợp với bài toán quản lý hội thảo có sự phân chia công việc rõ ràng, số lượng vai trò không quá nhiều, dễ dàng thu hồi/ cấp phát quyền khi có sự thay đổi nhân sự tổ chức.
### Định nghĩa các nhóm người dùng (Roles):
- Sinh viên (ATTENDEE): Người dùng cuối, tham gia nền tảng để tìm kiếm và đăng ký workshop.
- Nhân sự Check-in (STAFF) : Cộng tác viên được cấp quyền truy cập Mobile App/ Web app để phục vụ tại sự kiện.
- Ban tổ chức (HOST): Người quản lý hệ thống, tạo sự kiện, theo dõi lượng đăng ký

### Ma trận phân quyền
| Nhóm chức năng | API Endpoint (RESTful) | Ý nghĩa nghiệp vụ | Sinh viên | Nhân sự | Ban tổ chức |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **1. Xác thực (Auth)** | `POST /api/auth/login` | Đăng nhập hệ thống | ✅ | ✅ | ✅ |
| | `POST /api/auth/register` | Đăng ký tài khoản mới | ✅ | ❌ | ❌ |
| | `POST /api/auth/refresh-token` | Xin cấp lại access token | ✅ | ✅ | ✅ |
| | `POST /api/auth/logout` | Đăng xuất hệ thống | ✅ | ✅ | ✅ |
| | `GET /api/auth/users/me` | Lấy thông tin người dùng | ✅ | ✅ | ✅ |
| **2. Quản lý Sự kiện** | `GET /api/workshops` | Xem danh sách sự kiện | ✅ | ✅ | ✅ |
| | `GET /api/workshops/{id}` | Xem thông tin của 1 sự kiện | ✅ | ❌ | ✅ |
| | `POST, PUT, PATCH /api/workshops` | Tạo mới hoặc cập nhật thông tin sự kiện | ❌ | ❌ | ✅ |
| | `DELETE /api/workshops/{id}` | Hủy sự kiện | ❌ | ❌ | ✅ |
| | `POST /api/students/import-csv` | Import file csv thông tin sinh viên từ hệ thống cũ | ❌ | ❌ | ✅ |
| | `GET /api/students` | Lấy danh sách sinh viên | ❌ | ❌ | ✅ |
| | `POST /api/workshops/check-in` | Đẩy dữ liệu điểm danh lên Server | ❌ | ❌ | ✅ |
| | `GET /api/workshops/{whorkshopsId}/tickets/{registrationId}` | Lấy mã QR cho học sinh thanh toán | ✅ | ✅ | ✅ |
| | `GET /api/workshops/id/payment` | Lấy thông tin thanh toán của 1 sự kiện | ✅ | ✅ | ✅ |
| | `GET /api/workshops/statistics` | Lấy thống kê về số lượng sự kiện, đăng ký | ❌ | ❌ | ✅ |
| | `GET /api/workshops/reference-data` | Lấy thông tin về lớp, khoa, ngành của trường | ✅ | ✅ | ✅ |
| | `GET /api/tickets` | Lấy danh sách đăng ký | ❌ | ❌ | ✅ |
| **3. Đặt vé & Thanh toán** | `POST /api/workshops/{id}/tickets` | Đăng ký giữ chỗ | ✅ | ❌ | ❌ |
| | `POST /api/payments/charge` | Khởi tạo giao dịch thanh toán | ✅ | ❌ | ❌ |
| | `GET /api/payments/{registrationId}/status/stream` | Lấy trạng thái thanh toán của 1 giao dịch | ✅ | ✅ | ✅ |
### Cơ chế kiểm tra tại các điểm chạm
Để đảm bảo an toàn thông tin và tính toàn vẹn dữ liệu hệ thống sử dụng giao thức xác thực cốt lõi JSON Web Token (JWT). Sau khi xác thực thành công, hệ thống cấp phát một JWT chứa thông tin định danh và Vai trò (Role) của người dùng. Chuỗi Token này được đính kèm trong mọi yêu cầu (request) tiếp theo.
Hệ thống tiến hành kiểm soát truy cập và phân quyền (Authorization) độc lập tại 3 điểm chạm chính:
1. Tại Frontend - Trang Web (Web App - React) Việc kiểm soát quyền tại lớp Web App chủ yếu nhằm mục đích tối ưu hóa trải nghiệm người dùng (UX) và bảo vệ giao diện:
   Hiển thị giao diện theo điều kiện (Conditional UI Rendering): Hệ thống trích xuất thông tin Role để quyết định việc kết xuất (render) các thành phần giao diện.
   Ví dụ: Các nút chức năng như "Import CSV", "Xóa sự kiện" hoặc "Chỉnh sửa" sẽ bị ẩn hoàn toàn khỏi cây DOM nếu người dùng đăng nhập với vai trò ATTENDEE.
   Bảo vệ tuyến đường (Protected Routes): Đối với các trang dành riêng cho Ban tổ chức (Admin Dashboard), hệ thống sử dụng các thành phần bảo vệ cấp cao. Nếu một tài khoản không đủ thẩm quyền cố tình truy cập vào URL quản trị, Frontend sẽ ngay lập tức chặn lại và điều hướng (redirect) người dùng về trang chủ hoặc trang báo lỗi.
2. Tại Frontend - Ứng dụng di động (Mobile App - React Native) Ứng dụng di động được thiết kế chuyên biệt cho nghiệp vụ điểm danh ngoại tuyến (Offline Check-in). Cơ chế kiểm tra quyền tại đây tập trung vào việc định tuyến luồng công việc:
   Hiển thị giao diện theo điều kiện (Conditional UI Rendering): Hệ thống trích xuất thông tin Role để quyết định việc kết xuất (render) các thành phần giao diện.
   Nếu Role là STAFF(Nhân sự), ứng dụng sẽ tự động điều hướng vào màn hình "Camera Quét QR".
   Nếu phát hiện Role là ATTENDEE, hệ thống sẽ từ chối quyền truy cập vào màn hình chính của ứng dụng và hiển thị thông báo không đủ thẩm quyền.
3. Tại Backend - Các API Endpoint (Chốt chặn bảo mật tuyệt đối) Mặc dù Frontend đã có cơ chế ẩn giao diện, người dùng vẫn có thể sử dụng các công cụ bên thứ ba (như Postman) để gửi yêu cầu trực tiếp. Do đó, Backend là chốt chặn cuối cùng, thực hiện kiểm tra quyền hạn qua hai bước độc lập:
   - Tầng API Gateway (Xác thực - Authentication): API Gateway đóng vai trò cổng bảo vệ vòng ngoài. Mọi HTTP Request đều phải đi qua Gateway và được kiểm tra chữ ký Token (Signature Validation). Nếu Request không đính kèm Token, hoặc Token đã hết hạn/bị làm giả, Gateway sẽ từ chối kết nối và trả về mã lỗi 401 Unauthorized, nếu có thì hệ thống bóc tách Token, lấy ra Vai trò (Role) và đối chiếu với cấu hình quyền hạn của từng Endpoint . Nếu Request không đáp ứng đúng quyền hạn yêu cầu, hệ thống trả về mã lỗi 403 Forbidden, giúp giảm tải cho các dịch vụ bên trong.

## Thiết kế các cơ chế bảo vệ hệ thống

### Kiểm soát tải đột biến (12.000 sinh viên đăng ký cùng lúc)

**Giải pháp nhóm lựa chọn:**
Sử dụng **Rate Limiting** với thuật toán **Sliding Window** kết hợp với **Redis** để kiểm soát số lượng request, đồng thời dùng Redis để quản lý số lượng slot workshop.

**Cách hoạt động:**
- Mỗi request đăng ký sẽ được kiểm tra qua Rate Limiter trước khi vào backend.
- Với Sliding Window, hệ thống lưu lại timestamp của các request gần nhất trong Redis.
- **Khi có request mới:**
  - Đếm số request trong khoảng thời gian gần nhất (ví dụ: 1 giây / 10 giây).
  - Nếu vượt ngưỡng (ví dụ: 500 request/giây), request sẽ bị từ chối hoặc đưa vào hàng chờ.
- **Redis được dùng vì:**
  - Tốc độ đọc/ghi rất nhanh (in-memory).
  - Có thể dùng các cấu trúc như `Sorted Set` để lưu timestamp và xoá tự động các request cũ.
- **Song song đó:**
  - Số slot workshop cũng được lưu trong Redis.
  - Dùng atomic operation (`INCR`/`DECR` hoặc Lua script) để tránh race condition khi nhiều user đăng ký cùng lúc.

**Lý do phù hợp:**
- Sliding Window chính xác hơn Fixed Window, tránh tình trạng burst ở ranh giới thời gian.
- Redis đảm bảo hiệu năng cao và tính nhất quán tạm thời cho các thao tác đồng thời lớn.
- **Giải pháp giúp:**
  - Bảo vệ backend khỏi bị quá tải.
  - Phân phối request đều hơn theo thời gian.
  - Phù hợp với bài toán có lượng truy cập đột biến lớn trong thời gian ngắn.

---

### Xử lý cổng thanh toán không ổn định

**Giải pháp nhóm lựa chọn:**
Sử dụng **Circuit Breaker** (Closed / Open / Half-Open) kết hợp với **Graceful Degradation**.

**Cách hoạt động:**
- Mỗi request gọi đến cổng thanh toán sẽ đi qua Circuit Breaker.
- **Các trạng thái:**
  - **Closed:** Hoạt động bình thường, request được gửi đi.
  - **Open:** Khi số lỗi vượt ngưỡng, circuit chuyển sang Open. Tất cả request thanh toán sẽ bị chặn ngay lập tức (fail fast), không gọi sang service bên ngoài.
  - **Half-Open:** Sau một khoảng thời gian (cooldown), hệ thống cho phép một số request thử nghiệm đi qua.
    - Nếu thành công → quay về Closed.
    - Nếu vẫn lỗi → quay lại Open.
- **Kết hợp thêm:**
  - **Exponential Backoff** khi retry để tránh spam cổng thanh toán.
- **Graceful Degradation:**
  - Nếu thanh toán lỗi, hệ thống vẫn cho phép:
    - “Giữ chỗ” (reservation).
    - Trạng thái thanh toán: `Pending`.
    - UI thông báo: *"Hệ thống thanh toán đang bảo trì, chỗ của bạn được giữ trong 15 phút."*

**Lý do phù hợp:**
- Tránh việc request bị treo khi service bên thứ ba chậm → bảo vệ tài nguyên backend.
- **Circuit Breaker giúp:**
  - Fail fast thay vì chờ timeout.
  - Giảm tải hệ thống khi dependency bị lỗi.
- **Graceful Degradation đảm bảo:**
  - Core business (đăng ký workshop) vẫn hoạt động.
  - Trải nghiệm người dùng không bị gián đoạn hoàn toàn.
- Phù hợp với hệ thống phụ thuộc vào dịch vụ bên thứ ba không ổn định.

---

### Chống trừ tiền hai lần (Idempotency)

**Tổng quan (cập nhật theo schema `payments`) :**
Hệ thống ưu tiên dùng thông tin do cổng thanh toán cung cấp để dedupe ở mức database. Bảng `payments` có trường `bank_reference_code` (unique, indexed) — giá trị này chỉ được ghi vào khi webhook xác nhận giao dịch thành công. Khi SePay gọi webhook kèm `bank_reference_code`, service sẽ dùng giá trị này để phát hiện và bỏ qua các webhook trùng lặp.

**Luồng xử lý đề xuất:**
- Khi khởi tạo giao dịch: tạo một record `payments` với `status = PENDING`, chưa có `bank_reference_code` và (nếu có) lưu `provider_transaction_id` do cổng trả về.
- Khi webhook thành công từ SePay đến (body có `bank_reference_code`):
  1. Kiểm tra nhanh bằng index `bank_reference_code`:
     - Nếu tồn tại một `payments` với cùng `bank_reference_code` và `status = SUCCESS` → trả về 200, không xử lý lại.
  2. Nếu chưa tồn tại, trong một transaction cập nhật record liên quan (tìm theo `registration_id` hoặc `provider_transaction_id`): ghi `bank_reference_code`, `actual_content`, `provider_transaction_id` (nếu cần) và set `status = SUCCESS`.
  3. Nếu có race condition dẫn tới lỗi unique constraint trên `bank_reference_code`, bắt exception đó và tải lại record tồn tại để trả về (treat as duplicate).

**Ghi chú kỹ thuật:**
- `bank_reference_code` nên có `UNIQUE` constraint và index (đã có trong schema). Index cho phép kiểm tra trùng lặp cực nhanh khi webhook gọi tới.
- Không ghi `bank_reference_code` vào DB khi chỉ khởi tạo giao dịch; chỉ set khi webhook xác nhận thành công — điều này tránh ghi nhầm khi giao dịch chưa hoàn tất.
- `provider_transaction_id` cũng nên có `UNIQUE` nếu nhà cung cấp đảm bảo nó duy nhất, dùng làm cách tìm thay thế khi webhook thiếu `bank_reference_code`.
- DB-level constraint là hàng rào cuối cùng: ứng dụng cần kiểm tra trước bằng index, nhưng vẫn phải xử lý lỗi unique constraint defensively.

**Kết hợp với Idempotency Key (Redis):**
- Redis idempotency key vẫn hữu ích cho các trường hợp client retry trực tiếp (user nhấn nút nhiều lần) hoặc khi gọi API khởi tạo giao dịch — dùng để tránh tạo nhiều request khởi tạo đến cổng thanh toán.
- Tuy nhiên, dedupe cuối cùng khi nhận webhook thành công phải dựa vào `bank_reference_code` (và/hoặc `provider_transaction_id`) vì đó là nguồn tin cậy từ hệ thống thanh toán.

**TTL / Lưu trữ:**
- Giữ `bank_reference_code` vĩnh viễn (hoặc trong chu kỳ audit phù hợp) để hỗ trợ truy cứu và tránh duplicate lâu dài.
- Redis idempotency keys nên có TTL ngắn (ví dụ 15 phút) tương ứng vòng đời giao dịch.

**Tóm tắt:**
- Dùng `bank_reference_code` (DB unique + index) làm cơ chế dedupe chính khi xử lý webhook từ SePay.
- Dùng Redis idempotency key ở lớp khởi tạo giao dịch để tránh double-submit từ client.
- Kết hợp kiểm tra bằng index + xử lý lỗi unique constraint để bảo đảm an toàn trong mọi race condition.

## Các quyết định kỹ thuật quan trọng (ADR - Architecture Decision Records)

Phần này ghi nhận lại các quyết định thiết kế cốt lõi của hệ thống, bao gồm các phương án đã được xem xét, lý do lựa chọn và những sự đánh đổi (trade-offs) mà nhóm phải chấp nhận.

---

### ADR 001: Lựa chọn Hệ quản trị cơ sở dữ liệu (SQL vs NoSQL)

**1. Bối cảnh**
Hệ thống cần lưu trữ dữ liệu về Sinh viên, Sự kiện (Workshop), Vé đăng ký (Tickets) và Giao dịch thanh toán. Dữ liệu này có tính liên kết chặt chẽ với nhau và yêu cầu tính toàn vẹn rất cao (đặc biệt là dữ liệu thanh toán và số lượng vé).

**2. Quyết định**
Lựa chọn **Cơ sở dữ liệu quan hệ (SQL)** (ví dụ: PostgreSQL hoặc MySQL) thay vì NoSQL (như MongoDB).

**3. Lý do lựa chọn**
- **Đảm bảo tính ACID:** Giao dịch thanh toán và việc đăng ký giữ chỗ yêu cầu tính toàn vẹn dữ liệu tuyệt đối (Atomicity, Consistency, Isolation, Durability). SQL xử lý cực tốt bài toán lock dòng (row-level locking) để tránh việc cấp vé vượt quá số lượng (overselling) khi có hàng ngàn sinh viên truy cập cùng lúc.
- **Lược đồ dữ liệu rõ ràng (Structured Schema):** Các thực thể như Sinh viên, Sự kiện, Vé có cấu trúc cố định, ít biến đổi.
- **Ràng buộc toàn vẹn (Relational constraints):** Dễ dàng sử dụng Foreign Keys để đảm bảo một vé phải thuộc về một sinh viên hợp lệ và một sự kiện có thật.

**4. Sự đánh đổi (Trade-offs)**
- Việc mở rộng theo chiều ngang (Horizontal Scaling) khó khăn hơn so với NoSQL. Để giải quyết, nhóm kết hợp thêm Caching (Redis) cho các thao tác đọc nhiều (như xem danh sách sự kiện) nhằm giảm tải cho Database chính.
- Yêu cầu thiết kế schema chặt chẽ ngay từ đầu, thay đổi schema về sau (migration) sẽ mất thời gian hơn.

---

### ADR 002: Lựa chọn Cơ chế xác thực (JWT vs Session)

**1. Bối cảnh**
Hệ thống cung cấp RESTful APIs cho nhiều nền tảng (Web Client, Mobile App) và phục vụ nhiều đối tượng (Sinh viên, Nhân sự, Ban tổ chức). Cần một cơ chế xác thực an toàn, dễ mở rộng và độc lập với trạng thái của server.

**2. Quyết định**
Lựa chọn **JWT (JSON Web Token)** kết hợp Access Token & Refresh Token thay vì Session-based Authentication.

**3. Lý do lựa chọn**
- **Stateless (Phi trạng thái):** Server không cần lưu trữ trạng thái đăng nhập của người dùng. Điều này giúp backend dễ dàng scale theo chiều ngang (chạy nhiều instance độc lập) mà không lo vấn đề đồng bộ Session.
- **Hiệu năng cao:** JWT mang theo payload (như User ID, Role) ngay bên trong token, giúp hệ thống kiểm tra quyền hạn (Authorization) mà không cần query vào Database trong mỗi request.
- **Phù hợp với kiến trúc Microservices / RESTful:** Các service khác nhau có thể tự verify token thông qua Secret Key hoặc Public Key mà không cần gọi về Auth Service liên tục.

**4. Sự đánh đổi (Trade-offs)**
- **Khó thu hồi token (Revocation):** Không thể ép JWT hết hạn ngay lập tức khi user đăng xuất hoặc bị đổi mật khẩu. *Giải pháp khắc phục:* Sử dụng Access Token có vòng đời ngắn (ví dụ: 15 phút) và lưu danh sách Token bị chặn (Blacklist) vào Redis.
- Kích thước JWT lớn hơn Session ID, có thể làm tăng nhẹ dung lượng header của HTTP request.

---

### ADR 003: Lựa chọn Message Broker (RabbitMQ vs Kafka)

**1. Bối cảnh**
Hệ thống có nhiều tác vụ chạy ngầm cần xử lý bất đồng bộ (Asynchronous processing) để không làm block luồng chính của người dùng, ví dụ: gửi email xác nhận đăng ký, xử lý webhook từ cổng thanh toán, đồng bộ dữ liệu sinh viên.

**2. Quyết định**
Lựa chọn **RabbitMQ** thay vì Apache Kafka.

**3. Lý do lựa chọn**
- **Khả năng định tuyến linh hoạt (Smart Routing):** RabbitMQ hỗ trợ mô hình AMQP với các Exchanges (Direct, Topic, Fanout), giúp dễ dàng điều phối các loại message khác nhau (vd: message gửi email riêng, message xử lý thanh toán riêng) đến đúng worker một cách đơn giản.
- **Độ trễ thấp & Đảm bảo gửi nhận (Acknowledgment):** RabbitMQ đảm bảo message được xử lý ngay lập tức với độ trễ tính bằng mili-giây, rất phù hợp cho các task như gửi email xác nhận đăng ký. Cơ chế ACK/NACK giúp retry dễ dàng nếu worker gặp lỗi.
- **Dễ vận hành:** Setup và quản trị hệ thống RabbitMQ cho bài toán Task Queue đơn giản và tốn ít tài nguyên hơn so với một cụm Kafka.

**4. Sự đánh đổi (Trade-offs)**
- Khả năng lưu trữ (Retention): RabbitMQ không thiết kế để lưu trữ message lâu dài như log (giống Kafka). Message sẽ bị xóa sau khi được tiêu thụ thành công. Điều này là hoàn toàn chấp nhận được vì bài toán của nhóm là Task Queue, không phải là Event Streaming hay Data Analytics.
- Thông lượng (Throughput) tổng thể không thể cao bằng Kafka ở quy mô hàng triệu message/giây, nhưng với quy mô dự án (12.000 sinh viên), RabbitMQ hoàn toàn dư sức đáp ứng mà không gặp bất kỳ nút thắt cổ chai (bottleneck) nào.