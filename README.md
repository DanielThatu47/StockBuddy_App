# StockBuddy — Spring Boot Backend

A complete 1-to-1 port of the original Node.js/Express backend to **Spring Boot 3** with **MongoDB Atlas**.

---

## Tech Stack

| Layer         | Technology                           |
|---------------|--------------------------------------|
| Framework     | Spring Boot 3.2                      |
| Language      | Java 17                              |
| Database      | MongoDB Atlas (Spring Data MongoDB)  |
| Auth          | JWT (jjwt 0.12)                      |
| Password Hash | BCrypt (strength 10)                 |
| Image Upload  | Cloudinary SDK                       |
| Validation    | Spring Validation / Bean Validation  |
| Build         | Maven                                |

---

## Project Structure

```
src/main/java/com/stockbuddy/
├── StockBuddyApplication.java          # Entry point
│
├── config/
│   ├── SecurityConfig.java             # Spring Security + JWT filter wiring
│   ├── CorsConfig.java                 # CORS — allows all origins (mirrors Express)
│   └── CloudinaryConfig.java           # Cloudinary bean
│
├── controller/
│   ├── RootController.java             # GET /  → "API is running"
│   ├── AuthController.java             # /api/register, /login, /change-password, /admin/delete-user
│   ├── CaptchaController.java          # /api/captcha, /api/verify-captcha
│   ├── ProfileController.java          # /api/profile  (CRUD + picture upload/delete)
│   ├── PredictionController.java       # /api/predictions  (full CRUD + status/stop)
│   └── DemoTradingController.java      # /api/demotrading  (account/trade/history/reset)
│
├── model/
│   ├── User.java
│   ├── Prediction.java
│   ├── PredictionPoint.java
│   ├── Sentiment.java
│   ├── SentimentTotals.java
│   ├── DemoTradingAccount.java
│   ├── Holding.java
│   └── Transaction.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PredictionRepository.java
│   └── DemoTradingAccountRepository.java
│
├── security/
│   ├── JwtUtil.java                    # Token generation + validation
│   └── JwtAuthFilter.java              # Reads Bearer token, sets SecurityContext
│
├── service/
│   ├── UserService.java                # User → UserDto mapping
│   ├── CaptchaService.java             # CAPTCHA generate + verify (session-backed)
│   ├── CloudinaryService.java          # Upload + delete images
│   └── PredictionService.java          # Calls external model API
│
└── dto/
    ├── RegisterRequest.java
    ├── LoginRequest.java
    ├── ChangePasswordRequest.java
    ├── AdminDeleteUserRequest.java
    ├── UserDto.java
    ├── AuthResponse.java
    ├── UpdateProfileRequest.java
    ├── DeleteAccountRequest.java
    ├── VerifyCaptchaRequest.java
    ├── PredictionRequest.java
    ├── DeleteMultiplePredictionsRequest.java
    ├── TradeRequest.java
    ├── HoldingUpdateItem.java
    └── UpdateHoldingsRequest.java
```

---

## API Endpoints

### Auth — `/api`

| Method | Path                    | Auth | Description                          |
|--------|-------------------------|------|--------------------------------------|
| POST   | `/api/register`         | ✗    | Register new user                    |
| POST   | `/api/login`            | ✗    | Login, receive JWT                   |
| POST   | `/api/change-password`  | ✓    | Change authenticated user's password |
| DELETE | `/api/admin/delete-user`| ✓    | Admin: delete any user by email/name |

### Captcha — `/api`

| Method | Path                    | Auth | Description                     |
|--------|-------------------------|------|---------------------------------|
| GET    | `/api/captcha`          | ✗    | Generate CAPTCHA text           |
| POST   | `/api/verify-captcha`   | ✗    | Verify CAPTCHA answer           |

### Profile — `/api/profile`

| Method | Path                            | Auth | Description                    |
|--------|---------------------------------|------|--------------------------------|
| GET    | `/api/profile`                  | ✓    | Get current user profile       |
| PUT    | `/api/profile`                  | ✓    | Update profile fields          |
| POST   | `/api/profile/upload-picture`   | ✓    | Upload profile pic (multipart) |
| DELETE | `/api/profile/profile-picture`  | ✓    | Remove profile picture         |
| DELETE | `/api/profile`                  | ✓    | Delete own account             |

### Predictions — `/api/predictions`

| Method | Path                                | Auth | Description                           |
|--------|-------------------------------------|------|---------------------------------------|
| GET    | `/api/predictions`                  | ✓    | All predictions for current user      |
| GET    | `/api/predictions/{id}`             | ✓    | Single prediction by ID               |
| POST   | `/api/predictions`                  | ✓    | Start new prediction (calls model API)|
| GET    | `/api/predictions/status/{taskId}`  | ✓    | Poll prediction status from model API |
| POST   | `/api/predictions/stop/{taskId}`    | ✓    | Stop a running prediction             |
| DELETE | `/api/predictions/{id}`             | ✓    | Delete single prediction              |
| POST   | `/api/predictions/delete-multiple`  | ✓    | Delete many predictions by IDs        |

### Demo Trading — `/api/demotrading`

| Method | Path                               | Auth | Description                          |
|--------|------------------------------------|------|--------------------------------------|
| GET    | `/api/demotrading/account`         | ✓    | Get/create trading account           |
| POST   | `/api/demotrading/trade`           | ✓    | Execute BUY or SELL                  |
| GET    | `/api/demotrading/transactions`    | ✓    | Transaction history (newest first)   |
| PUT    | `/api/demotrading/holdings/update` | ✓    | Update current prices of holdings    |
| POST   | `/api/demotrading/reset`           | ✓    | Reset account to $100,000            |
| GET    | `/api/demotrading/portfolio-history`| ✓   | Full history + performance metrics   |

---

## Setup

### 1. Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB Atlas account
- Cloudinary account

### 2. Configure Environment

Copy `.env.example` and fill in your values, **or** edit `src/main/resources/application.properties` directly:

```properties
spring.data.mongodb.uri=mongodb+srv://<user>:<pass>@cluster0.mongodb.net/stockbuddy
jwt.secret=your-long-random-secret
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret
model.api.url=http://your-model-api-host
admin.emails=admin@stockbuddy.com
```

### 3. Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/stockbuddy-backend-1.0.0.jar

# Or directly via Maven
mvn spring-boot:run
```

Server starts on **port 5000** (same as the original Node.js server).

### 4. Authentication

All protected routes require the header:
```
Authorization: Bearer <token>
```

The token is returned from `/api/register` and `/api/login`.

---

## Key Design Decisions

| Node.js Behaviour                          | Spring Boot Equivalent                                |
|--------------------------------------------|-------------------------------------------------------|
| `bcrypt.genSalt(10)` / `bcrypt.hash()`     | `BCryptPasswordEncoder(10)`                           |
| `jwt.sign({ userId }, secret, { expiresIn: '7d' })` | `JwtUtil.generateToken()` with 7-day expiry  |
| `req.userId` set by auth middleware        | `Authentication.getPrincipal()` returns userId string |
| Mongoose pre-save hook (recalculate equity)| `DemoTradingAccount.recalculate()` called before save |
| Express session for CAPTCHA                | `HttpSession` via Spring Session                      |
| Multer `memoryStorage` + Cloudinary stream | `MultipartFile.getBytes()` → Cloudinary upload        |
| `deleteFromCloudinary(url)` via public_id  | `CloudinaryService.deleteByUrl(url)` extracts ID      |
| `node-fetch` POST to model API             | `RestTemplate` in `PredictionService`                 |
| CORS `origin: '*'`                         | `CorsConfig` with `addAllowedOriginPattern("*")`      |
