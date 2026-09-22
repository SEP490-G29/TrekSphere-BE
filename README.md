# TrekSphere Backend

TrekSphere là nền tảng chuyên cung cấp các tour du lịch trekking và trải nghiệm khám phá thiên nhiên tại Việt Nam. Dự án Backend được xây dựng bằng ngôn ngữ Java với Framework Spring Boot, áp dụng các tiêu chuẩn thiết kế hiện đại và kiến trúc phân lớp theo module (module-based layered architecture) để đảm bảo tính mở rộng, hiệu năng cao và dễ bảo trì.

---

## 🛠️ Công nghệ & Thư viện Sử dụng

| Công nghệ / Thư viện     | Phiên bản   | Mô tả                                                                          |
| :------------------------ | :---------- | :------------------------------------------------------------------------------ |
| **Java**                  | 21          | Sử dụng các tính năng mới của Java LTS (Record, Pattern Matching, Virtual Threads ready). |
| **Spring Boot**           | 3.5.x       | Framework phát triển ứng dụng Java Web nhanh chóng và mạnh mẽ.                  |
| **PostgreSQL**            | 15 (Alpine) | Cơ sở dữ liệu quan hệ chính, tối ưu hiệu năng và độ ổn định cao.                |
| **Redis**                 | Alpine      | Cache / lưu trạng thái tạm thời (rate limit, token blacklist...).               |
| **Flyway**                | -           | Quản lý và versioning schema cơ sở dữ liệu.                                     |
| **Spring Security + JWT** | -           | Xác thực & phân quyền bằng JWT (thư viện `jjwt`), hỗ trợ Google OAuth2 login.   |
| **MapStruct**              | -           | Sinh code ánh xạ (mapping) giữa Entity và DTO tại compile-time.                 |
| **Spring WebSocket**       | -           | Giao tiếp realtime (chat, thông báo...).                                        |
| **Thymeleaf**              | -           | Render template email (xác thực tài khoản, đặt lại mật khẩu...).                |
| **SendGrid**                | -           | Gửi email giao dịch (transactional email).                                     |
| **Cloudinary**              | -           | Lưu trữ & xử lý media (ảnh, tài liệu đính kèm).                                 |
| **springdoc-openapi**       | -           | Sinh tài liệu API chuẩn OpenAPI / Swagger UI.                                    |

---

## 📂 Cấu trúc Thư mục Dự án

Mã nguồn được tổ chức theo **module nghiệp vụ**, mỗi module tự đóng gói đầy đủ các tầng xử lý riêng (Controller → Service → Repository/Entity), giúp tách biệt rõ ràng giữa các domain và dễ mở rộng/bảo trì độc lập. Cấu trúc phân lớp bên trong mỗi module tuân theo mẫu chung dưới đây (một số module có thêm `mapper/`, `event/`, `service/impl/` tùy theo nhu cầu thực tế của domain đó):

```text
treksphere-be/
├── .env                         # File lưu trữ biến môi trường (không commit vào git)
├── docker-compose.yml           # Định nghĩa các dịch vụ Docker (PostgreSQL, pgAdmin, Redis, RedisInsight)
├── pom.xml                      # Cấu hình dự án Maven và quản lý dependencies
├── src/main/
│   ├── java/com/sep/treksphere/
│   │   ├── TreksphereBeApplication.java  # Class khởi chạy ứng dụng
│   │   │
│   │   ├── common/               # Thành phần dùng chung toàn hệ thống
│   │   │   ├── config/            # Cấu hình hệ thống (Security, CORS, WebSocket, Swagger, Redis...)
│   │   │   ├── constant/          # Hằng số dùng chung (message constant...)
│   │   │   ├── dto/               # DTO dùng chung (pagination response...)
│   │   │   ├── entity/            # Base entity (audit fields...)
│   │   │   ├── exception/         # Global Exception Handler, ErrorCode, AppException
│   │   │   ├── security/          # JWT filter, CustomUserDetails, OAuth2 handler
│   │   │   └── util/              # Tiện ích dùng chung
│   │   │
│   │   ├── auth/                  # Đăng ký / đăng nhập / JWT / quên mật khẩu / OAuth2
│   │   ├── user/                  # Quản lý người dùng, vai trò, quyền hạn (User, Role, Permission)
│   │   ├── vendor/                 # Quản lý nhà cung cấp tour (đăng ký, hồ sơ, nhân sự, thống kê)
│   │   ├── tour/                   # Quản lý tour, lịch trình, checkpoint, đề xuất, hành vi người dùng
│   │   ├── matching/                # Ghép nhóm đi trekking, hành trình tự tạo, bài đăng nhóm, đánh giá thành viên
│   │   ├── blog/                    # Bài viết, bình luận
│   │   ├── chat/                    # Nhắn tin realtime giữa người dùng
│   │   ├── notification/            # Thông báo hệ thống (email, in-app)
│   │   ├── report/                  # Báo cáo vi phạm nội dung/người dùng
│   │   ├── file/                    # Upload & quản lý file đính kèm (Cloudinary)
│   │   │
│   │   └── <mỗi module trên đều theo mẫu>/
│   │       ├── controller/          # Tầng tiếp nhận request RESTful API và gửi trả response
│   │       ├── dto/
│   │       │   ├── request/         # Đối tượng nhận dữ liệu đầu vào
│   │       │   └── response/        # Đối tượng trả dữ liệu đầu ra
│   │       ├── entity/              # Entity JPA tương ứng bảng trong Database
│   │       ├── enums/                # Enum riêng của domain (status, type...)
│   │       ├── event/                # Application Event (nếu domain phát/nghe sự kiện nội bộ)
│   │       ├── mapper/                # Interface MapStruct ánh xạ Entity <-> DTO
│   │       ├── repository/            # Tầng giao tiếp cơ sở dữ liệu (Spring Data JPA)
│   │       └── service/                # Logic nghiệp vụ (một số domain có thêm service/impl/ khi tách interface)
│   │
│   └── resources/
│       ├── application.yml         # Cấu hình ứng dụng Spring Boot
│       ├── db/migration/           # Flyway migration (V1__init_schema.sql - schema hợp nhất, không seed data)
│       ├── templates/              # Template email (Thymeleaf)
│       ├── email/                  # Tài nguyên tĩnh dùng trong email (logo...)
│       └── static/                 # Tài nguyên tĩnh (nếu có)
```

> Lưu ý: hai domain đã lược bỏ hoàn toàn khỏi hệ thống là **booking** và **payment** — không còn tồn tại code, endpoint, bảng dữ liệu hay cấu hình liên quan.

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy Nhanh

### 1. Yêu cầu Hệ thống

Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:

- **Java Development Kit (JDK) 21** trở lên.
- **Docker** và **Docker Compose**.
- **Maven** (có thể dùng Maven Wrapper `mvnw` đi kèm dự án, không cần cài riêng).

### 2. Thiết lập Biến Môi trường (`.env`)

Tạo một file đặt tên là `.env` tại thư mục gốc của dự án `treksphere-be` (nếu chưa có) và cấu hình các thông số phù hợp:

```env
# Server
PORT=8080

# Database
POSTGRES_DB=treksphere_db
POSTGRES_USER=admin
POSTGRES_PASSWORD=your_secure_password
DB_HOST=localhost
DB_PORT=5432

# pgAdmin
PGADMIN_DEFAULT_EMAIL=admin@treksphere.com
PGADMIN_DEFAULT_PASSWORD=admin_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# JWT
JWT_SECRET_KEY=your_jwt_secret_key
JWT_EXPIRATION_TIME=86400000
JWT_REFRESH_EXPIRATION_TIME=604800000
JWT_RESET_PASSWORD_EXPIRATION_TIME=300000

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Frontend
FRONTEND_URL=http://localhost:3000

# SendGrid (gửi email)
SENDGRID_API_KEY=your_sendgrid_api_key
MAIL_FROM_ADDRESS=noreply@example.com

# Cloudinary (lưu trữ media)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret
```

> ⚠️ File `.env` đã được đưa vào `.gitignore`, **không commit** file này lên git vì chứa thông tin nhạy cảm.

### 3. Khởi chạy Hạ tầng (Docker)

Tại thư mục gốc của dự án `treksphere-be` (nơi chứa file `docker-compose.yml`), khởi chạy các container ở chế độ chạy ngầm (detached mode):

```bash
docker-compose up -d
```

Docker Compose sẽ khởi chạy 4 service:

| Service         | Cổng mặc định | Mô tả                          |
| :--------------- | :------------- | :------------------------------- |
| `db`             | `5432`          | PostgreSQL - cơ sở dữ liệu chính |
| `pgadmin`        | `5050`          | Giao diện quản trị PostgreSQL    |
| `redis`          | `6379`          | Cache / rate limit / blacklist   |
| `redisinsight`   | `8001`          | Giao diện quản trị Redis         |

Để kiểm tra trạng thái hoạt động của các container:

```bash
docker-compose ps
```

### 4. Chạy Ứng dụng Backend Spring Boot

Bạn có thể import dự án vào các IDE phổ biến như IntelliJ IDEA, Eclipse hoặc chạy trực tiếp bằng dòng lệnh:

- **Sử dụng Maven Wrapper trên Windows (PowerShell / CMD):**
  ```powershell
  .\mvnw spring-boot:run
  ```
- **Sử dụng Maven Wrapper trên Linux/macOS:**
  ```bash
  ./mvnw spring-boot:run
  ```

Ứng dụng mặc định sẽ khởi chạy trên cổng cấu hình bởi biến `PORT` (mặc định `8080`).

### 5. Database Migration (Flyway)

Toàn bộ schema được gộp thành **một file migration duy nhất**: [`src/main/resources/db/migration/V1__init_schema.sql`](src/main/resources/db/migration/V1__init_schema.sql) (chỉ chứa DDL, không có seed data).

Việc chạy migration được điều khiển bởi cấu hình `spring.flyway.enabled` trong [`application.yml`](src/main/resources/application.yml):

```yaml
spring:
  flyway:
    enabled: true   # đặt false nếu muốn bỏ qua bước chạy migration khi start app
```

- `enabled: true` (mặc định khi deploy) — Flyway tự động chạy `V1__init_schema.sql` để tạo schema trên database trống.
- `enabled: false` — bỏ qua bước migration, dùng khi chạy thử ứng dụng trên một database **đã có sẵn schema** từ trước.

> Lưu ý: khi tắt Flyway, `spring.jpa.hibernate.ddl-auto` vẫn đang ở chế độ `validate` — Hibernate sẽ đối chiếu entity với schema hiện có và **báo lỗi khi khởi động** nếu database đang trống hoặc thiếu bảng. Chỉ tắt Flyway khi chắc chắn database đích đã có đủ schema tương ứng.

---
