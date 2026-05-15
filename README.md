# UniHub Workshop Backend

Backend cho hệ thống UniHub Workshop, phục vụ các luồng chính như đăng nhập, quản lý workshop, đăng ký vé, thanh toán, check-in, nhập dữ liệu và gửi email thông báo.

## Tech Stack

- Java 25
- Spring Boot 4.0.6
- Maven Wrapper (`mvnw` / `mvnw.cmd`) cho từng service
- Spring Cloud Gateway
- Spring Security
- Spring Data JPA
- Spring Data Redis
- RabbitMQ
- PostgreSQL
- gRPC
- JWT
- SePay payment integration
- Redis rate limiting trên gateway
- Amazon S3 SDK và Cloudflare R2
- Google Gemini API
- Spring Mail + Thymeleaf
- Apache Commons CSV
- PDFBox
- ZXing QR Code
- Lombok
- MapStruct
- Docker support qua `Dockerfile` ở từng service

## Requirements

Bạn nên cài sẵn:

- JDK 25
- Maven 3.9+ nếu không dùng Maven Wrapper
- Docker
- Docker Compose
- Redis
- RabbitMQ
- Ngrok
- PostgreSQL nếu muốn chạy local với DB riêng

### Ghi chú về database

Trong source hiện tại, một số service đang trỏ sẵn đến PostgreSQL Neon. Nếu bạn muốn chạy ổn định trên máy cá nhân hoặc thay DB khác, hãy chuẩn bị một PostgreSQL riêng và cập nhật lại cấu hình tương ứng.

## Clone & Installation

### 1. Clone repository/ Tải mã nguồn

```bash
git clone <repo-url>
cd unihub-workshop
```

### 2. Đi vào folder backend

Repo này là monorepo, mỗi service là một project Maven riêng. Bạn cần chạy từng service trong folder tương ứng:

- `api-gateway`
- `auth-service`
- `workshop-service`
- `payment-service`
- `data-import-service`
- `notification-service`

### 3. Setup environment variables

Project có dùng `dotenv-java`, nên bạn có thể tạo file `.env` trong từng service folder để override cấu hình trước khi chạy.

#### `api-gateway`

```env
JWT_SECRET=<YOUR_VALUE>
```

#### `auth-service`

`auth-service` hiện đang dùng một số giá trị hardcode trong `application.properties`. Nếu muốn đổi sang biến môi trường, bạn nên sửa trực tiếp file:

- `auth-service/src/main/resources/application.properties`

#### `workshop-service`

```env
APPLICATION_NAME=workshop-service
SERVER_PORT=4002
DB_URL=<YOUR_DB_URL>
DB_USERNAME=<YOUR_DB_USERNAME>
DB_PASSWORD=<YOUR_DB_PASSWORD>
DB_DRIVER=org.postgresql.Driver
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
RABBITMQ_VIRTUAL_HOST=/
RABBITMQ_IMPORT_QUEUE=data-import-queue
R2_ENDPOINT=<YOUR_VALUE>
R2_ACCESS_KEY=<YOUR_VALUE>
R2_SECRET_KEY=<YOUR_VALUE>
R2_BUCKET_NAME=<YOUR_VALUE>
GEMINI_API_KEY=<YOUR_VALUE>
```

#### `payment-service`

```env
ACCOUNT_NUMBER=<YOUR_VALUE>
BANK_NAME=<YOUR_VALUE>
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
RABBITMQ_VIRTUAL_HOST=/
```

Nếu dùng webhook SePay thật, bạn cũng cần sửa:

- `payment-service/src/main/resources/application.properties`

Đặc biệt là:

- `sepay.webhook.api-key`

#### `data-import-service`

```env
APPLICATION_NAME=data-import-service
SERVER_PORT=4005
DB_URL=<YOUR_DB_URL>
DB_USERNAME=<YOUR_DB_USERNAME>
DB_PASSWORD=<YOUR_DB_PASSWORD>
DB_DRIVER=org.postgresql.Driver
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
RABBITMQ_VHOST=/
RABBITMQ_IMPORT_QUEUE=data-import-queue
```

#### `notification-service`

`notification-service` hiện đang dùng trực tiếp cấu hình mail trong file properties. Nếu muốn đổi tài khoản gửi mail, sửa:

- `notification-service/src/main/resources/application.properties`

Các giá trị có thể tách ra:

```env
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
```

### 4. Start Redis bằng Docker

Repo hiện chưa có `docker-compose.yml`, nên cách đơn giản nhất là chạy Redis bằng Docker trực tiếp:

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis
```

Nếu bạn cần chạy RabbitMQ local để hệ thống hoạt động đầy đủ, có thể dùng thêm:

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

### 5. Chạy project

Chạy từng service trong từng terminal riêng. Với Windows, dùng `mvnw.cmd`. Với macOS/Linux, dùng `./mvnw`.

#### Thứ tự khuyến nghị

1. `auth-service`
2. `workshop-service`
3. `payment-service`
4. `data-import-service`
5. `notification-service`
6. `api-gateway`

#### Lệnh chạy

Ví dụ dưới đây dùng cú pháp macOS/Linux. Trên Windows, đổi `./mvnw` thành `mvnw.cmd`.

```bash
cd auth-service
./mvnw spring-boot:run

cd workshop-service
./mvnw spring-boot:run

cd payment-service
./mvnw spring-boot:run

cd data-import-service
./mvnw spring-boot:run

cd notification-service
./mvnw spring-boot:run

cd api-gateway
./mvnw spring-boot:run
```

## Running Redis With Docker

Redis được dùng cho:

- Gateway rate limiting
- Cache ở `workshop-service`
- Một số luồng xử lý ở `payment-service`

Chạy Redis:

```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis
```

Kiểm tra Redis:

```bash
docker ps
```

## Payment + Ngrok Setup

SePay hoặc các dịch vụ thanh toán bên ngoài không thể gọi vào `localhost` trên máy của bạn. Ngrok giúp expose port backend ra một URL HTTPS công khai để provider có thể gọi webhook.

### Cài ngrok

1. Tải và cài ngrok theo hướng dẫn chính thức.
2. Đăng nhập và cấu hình auth token nếu cần.
3. Đảm bảo `api-gateway` đang chạy ở port `4000`.

### Expose backend bằng ngrok

```bash
ngrok http 4000
```

Copy URL HTTPS dạng:

```text
https://<your-ngrok-domain>.ngrok-free.app
```

### Webhook endpoint cần dùng

Endpoint webhook của payment service đang đi qua gateway:

- Local: `http://localhost:4000/api/payments/webhook/sepay`
- Public qua ngrok: `https://<your-ngrok-domain>.ngrok-free.app/api/payments/webhook/sepay`

### Cần cập nhật ở đâu?

Nếu cấu hình provider đang trỏ cứng sang URL cũ, hãy sửa tại:

- `api-gateway/src/main/resources/application.yml`
- `payment-service/src/main/resources/application.properties`
- Cấu hình webhook/callback ở dashboard của nhà cung cấp thanh toán

### Header xác thực webhook

Webhook hiện tại yêu cầu header:

```http
Authorization: Apikey <sepay.webhook.api-key>
```

## Project Structure

Rút gọn theo module:

```text
unihub-workshop/
├── api-gateway/
├── auth-service/
├── workshop-service/
├── payment-service/
├── data-import-service/
├── notification-service/

```

Mỗi service có cấu trúc Java theo hướng phân lớp:

- `config/`
- `controllers/` hoặc `controller/`
- `services/`
- `repositories/`
- `models/` hoặc `entity/`
- `dto/`
- `exceptions/`
- Các thư mục đặc thù như `listeners/`, `clients/`, `cache/`, `templates/`

## Available Commands

Chạy trong từng folder service:

```bash
./mvnw clean
./mvnw test
./mvnw package
./mvnw spring-boot:run
```

Nếu dùng Windows:

```powershell
mvnw.cmd clean
mvnw.cmd test
mvnw.cmd package
mvnw.cmd spring-boot:run
```

## Troubleshooting

### 1. Redis connection failed

Kiểm tra Redis container:

```bash
docker ps
docker logs redis
```

Nếu Redis chưa chạy, khởi động lại:

```bash
docker start redis
```

### 2. Port already in use

Các port hay dùng trong project:

- `4000` gateway
- `4001` auth-service
- `4002` workshop-service
- `4003` payment-service
- `4005` data-import-service
- `6379` Redis
- `5672` RabbitMQ
- `9090` gRPC payment server

Nếu port bị chiếm, đổi trong file cấu hình tương ứng hoặc tắt process đang dùng port đó.

### 3. Docker container exited

Xem log:

```bash
docker logs <container-name>
```

Với RabbitMQ, nhớ mở cả port management `15672` nếu bạn cần giao diện quản trị.

### 4. Java version mismatch

Source hiện tại target `Java 25`. Nếu máy bạn đang dùng JDK thấp hơn, build có thể fail. Kiểm tra:

```bash
java -version
```

### 5. Ngrok forwarding failed

- Đảm bảo `api-gateway` đang chạy ở `4000`
- Chạy lại `ngrok http 4000`
- Copy đúng URL HTTPS, không dùng URL HTTP

### 6. Payment callback not working

- Kiểm tra URL webhook đã trỏ tới `.../api/payments/webhook/sepay`
- Kiểm tra `Authorization: Apikey <key>`
- Kiểm tra `sepay.webhook.api-key` trong `payment-service/src/main/resources/application.properties`
- Kiểm tra `api-gateway` đã chạy trước khi test webhook

### 7. CORS issues

Gateway có cấu hình CORS riêng. Nếu frontend hoặc tool test bị chặn, kiểm tra:

- `api-gateway/src/main/java/org/unihubworkshop/apigateway/configs/CorsConfig.java`

## Notes

- Project là monorepo, không có một lệnh duy nhất để chạy toàn bộ backend.
- `api-gateway` là entry point cho client, nên thường chạy sau cùng.
- `auth-service`, `payment-service`, `workshop-service` đang dùng PostgreSQL.
- `workshop-service` dùng Redis làm cache provider theo cấu hình mặc định.
- `payment-service` dùng gRPC server ở port `9090`.
- `data-import-service` và `notification-service` thiên về background processing, nên vẫn cần chạy nếu  muốn test đầy đủ RabbitMQ/email/import flow.
