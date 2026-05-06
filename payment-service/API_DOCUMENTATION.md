# Payment Service

Microservice xử lý thanh toán qua SePay với tích hợp RabbitMQ.

## Cấu trúc Thư mục

```
payment-service/
├── src/main/java/org/unihubworkshop/paymentservice/
│   ├── clients/              # SePay client integration
│   ├── config/               # RabbitMQ configuration
│   ├── controllers/          # REST endpoints
│   ├── dto/                  # Data transfer objects
│   ├── event/                # Event models for RabbitMQ
│   ├── exceptions/           # Custom exceptions & global handler
│   ├── mapper/               # MapStruct mappers
│   ├── models/               # JPA entities
│   ├── repositories/         # Spring Data JPA repositories
│   ├── services/             # Business logic
│   └── PaymentServiceApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql
└── pom.xml
```

## API Endpoints

### 1. Charge Payment (Tạo yêu cầu thanh toán)

**Endpoint:** `POST /api/payments/charge`

**Request:**
```json
{
  "registrationId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 500000
}
```

**Response:**
```json
{
  "success": true,
  "message": "QR code generated successfully",
  "data": {
    "paymentId": "550e8400-e29b-41d4-a716-446655440000",
    "qrCodeUrl": "https://qr.sepay.vn/img?acc=1234567890&bank=MBBANK&amount=500000&des=UNIHUB-550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**Status:** 201 Created

---

### 2. Get Payment by ID

**Endpoint:** `GET /api/payments/{paymentId}`

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "registrationId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 500000,
    "provider": "SEPAY",
    "gateway": null,
    "providerTransactionId": null,
    "bankReferenceCode": null,
    "actualContent": null,
    "status": "PENDING",
    "createdAt": "2026-05-05T10:30:00",
    "updatedAt": "2026-05-05T10:30:00"
  }
}
```

**Status:** 200 OK

---

### 3. Get Payment by Registration ID

**Endpoint:** `GET /api/payments/registration/{registrationId}`

**Response:** Same as above

**Status:** 200 OK

---

### 4. SePay Webhook (Callback)

**Endpoint:** `POST /api/payments/webhook/sepay`

**Request (from SePay):**
```json
{
  "transaction_id": "SEPAY-123456",
  "reference_code": "REF-789",
  "account_number": "0123456789",
  "amount": 500000,
  "content": "UNIHUB-550e8400-e29b-41d4-a716-446655440000",
  "transfer_type": "IN",
  "transfer_date": "2026-05-05T10:35:00",
  "description": "Payment confirmation"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": null
}
```

**Status:** 200 OK

---

## Configuration

### application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/unihub_payment
spring.datasource.username=postgres
spring.datasource.password=postgres

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# SePay
sepay.account-number=YOUR_ACCOUNT_NUMBER
sepay.bank=MBBANK
```

## Event Publishing

Khi thanh toán thành công, service sẽ publish event lên RabbitMQ:

```
Exchange: payment.exchange
Routing Key: payment.status.updated
Queue: payment.status.updated.queue
```

**Event Payload:**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440000",
  "registrationId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SUCCESS",
  "amount": 500000
}
```

Workshop Service hoặc các service khác có thể subscribe vào queue này để update trạng thái registration.

## Error Handling

### Status Codes

- **201 Created**: Thanh toán được tạo thành công
- **200 OK**: Request thành công
- **400 Bad Request**: Validation failed
- **404 Not Found**: Payment không tìm thấy
- **409 Conflict**: Payment trùng lặp (đã thanh toán)
- **500 Internal Server Error**: Lỗi server

### Error Response Format

```json
{
  "success": false,
  "message": "Payment not found with ID: xxx",
  "data": null
}
```

## Database Schema

Xem `schema.sql` để hiểu cấu trúc bảng payments.

### Constraints & Indexes
- `bank_reference_code` UNIQUE: Chống trùng lặp thanh toán
- `provider_transaction_id` UNIQUE: ID từ SePay
- Index trên `registration_id`, `bank_reference_code` để tối ưu query

## Dependencies

- Spring Boot 4.0.6
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- MapStruct
- Lombok
- PostgreSQL Driver

## Running the Service

```bash
cd payment-service
mvn spring-boot:run
```

Service sẽ chạy trên `http://localhost:4003`

## Testing

### Charge Payment
```bash
curl -X POST http://localhost:4003/api/payments/charge \
  -H "Content-Type: application/json" \
  -d '{
    "registrationId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 500000
  }'
```

### Get Payment
```bash
curl http://localhost:4003/api/payments/550e8400-e29b-41d4-a716-446655440000
```

### Webhook Test
```bash
curl -X POST http://localhost:4003/api/payments/webhook/sepay \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "SEPAY-123456",
    "reference_code": "REF-789",
    "account_number": "0123456789",
    "amount": 500000,
    "content": "UNIHUB-550e8400-e29b-41d4-a716-446655440000",
    "transfer_type": "IN",
    "transfer_date": "2026-05-05T10:35:00",
    "description": "Payment confirmation"
  }'
```

## Next Steps

1. Configure SePay account số và ngân hàng trong `application.properties`
2. Set up PostgreSQL database
3. Configure RabbitMQ connection
4. Thêm authentication/authorization nếu cần
5. Workshop Service cần subscribe vào RabbitMQ queue để handle payment status updates

