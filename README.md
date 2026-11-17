# Asaas Payment Integration

🇧🇷 [Leia em português](README.pt-BR.md)

A Spring Boot API for learning how to integrate with the [Asaas](https://www.asaas.com/) payment gateway: user sign-up, JWT authentication and, in the next stages, creating customers and subscriptions in the Asaas sandbox, with a focus on resilience (timeouts, retries, idempotency, webhooks).

> **Status:** work in progress. Users and authentication are done. The Asaas integration has not been implemented yet (see the [Roadmap](#roadmap)).

---

## Contents

- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
- [Authentication](#authentication)
- [Project structure](#project-structure)
- [Tests](#tests)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)

---

## Tech stack

| Technology | Version | Used for |
|---|---|---|
| Java | 21 | language |
| Spring Boot | 4.1.2 | Web MVC, Validation, Data JPA, Security |
| Spring Cloud OpenFeign | 2025.1.4 | HTTP client for the Asaas API (next stages) |
| PostgreSQL | 18 | database (via Docker Compose) |
| JJWT | 0.12.6 | creating and validating JWTs |
| MapStruct | 1.6.3 | mapping between DTOs and entities |
| Lombok | — | less boilerplate in the entities |
| Bucket4j + Caffeine | 8.22.0 / managed by Spring Boot | login rate limiting (in progress) |

---

## Prerequisites

- **JDK 21** or later
- **Docker** with Docker Compose (for PostgreSQL)
- **OpenSSL** (to generate the JWT key; ships with macOS and most Linux distributions)

You don't need Maven installed: the project uses the Maven Wrapper (`./mvnw`).

---

## Getting started

**1. Clone the repository**

```bash
git clone git@github.com:oliverigor27/asaas-payment-integration.git
cd asaas-payment-integration
```

**2. Create the `.env` file with the JWT key**

```bash
cp .env.example .env
```

Generate a key and paste it after `SECRET_KEY=` in `.env`:

```bash
openssl rand -base64 32
```

`.env` is listed in `.gitignore` and must **never** be committed.

**3. Start PostgreSQL**

```bash
docker compose up -d
```

This starts the `payment-postgres` container on port `5432`, with database, user and password all set to `payment`.

**4. Start the application**

```bash
./mvnw spring-boot:run
```

If your terminal uses a JDK older than 21, point `JAVA_HOME` to JDK 21 (macOS example):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21+) ./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. Hibernate creates the tables automatically (`ddl-auto=update`).

**5. Try it out**

```bash
# sign up
curl -i -X POST http://localhost:8080/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","username":"ana","password":"strongpassword123"}'

# log in
curl -s -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"strongpassword123"}'
```

---

## Configuration

The application reads `.env` from the project root (`spring.config.import=optional:file:.env[.properties]`). The same variables can also be exported as environment variables.

| Variable | Required | Default | Description |
|---|---|---|---|
| `SECRET_KEY` | **yes** | — | JWT signing key, **Base64-encoded**, at least **32 bytes** (256 bits) once decoded. Generate it with `openssl rand -base64 32` |
| `DB_HOST` | no | `localhost` | PostgreSQL host |
| `DB_PORT` | no | `5432` | PostgreSQL port |
| `DB_NAME` | no | `payment` | database name |
| `DB_USERNAME` | no | `payment` | database user |
| `DB_PASSWORD` | no | `payment` | database password |

Both `compose.yaml` and the application use the `DB_*` variables, so they always stay in sync.

> The `file:.env` path is relative to the directory the application is started from. Run all commands from the project root.

---

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/v1/users` | public | creates a user |
| `POST` | `/v1/auth/login` | public | authenticates and returns a JWT |

Every other route requires a valid token in the `Authorization` header.

### `POST /v1/users`

**Request body:**

```json
{
  "email": "ana@example.com",
  "username": "ana",
  "password": "strongpassword123"
}
```

| Field | Rules |
|---|---|
| `email` | required, valid email format, unique |
| `username` | required, up to 50 characters, unique |
| `password` | required, 8 to 100 characters; stored with BCrypt |

**Responses:**

| Status | When |
|---|---|
| `200` | user created (empty body) |
| `400` | invalid input |
| `500` | email or username already taken (not handled yet; see the [Roadmap](#roadmap)) |

### `POST /v1/auth/login`

**Request body:**

```json
{
  "email": "ana@example.com",
  "password": "strongpassword123"
}
```

**`200 OK`:**

```json
{
  "message": "Successful Login!",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

`expiresIn` is the token lifetime in seconds (24 hours).

**`401 Unauthorized`** (unknown email or wrong password; the response is the same in both cases). The message is currently in Portuguese ("Invalid username or password"):

```json
{
  "message": "Usuário ou senha inválidos"
}
```

**`400 Bad Request`:** missing email or password, or an invalid email format.

---

## Authentication

The API is **stateless**: there is no session. Every request to a protected route must send the token returned by the login:

```bash
curl http://localhost:8080/v1/<route> \
  -H "Authorization: Bearer <token>"
```

How it works:

```
request ─► JwtAuthenticationFilter ─► ... ─► AuthorizationFilter ─┬─► allowed ─► controller
           validates the token and                                │
           puts the user (email) in                               └─► no user ─► JwtAuthenticationEntryPoint ─► 401
           the SecurityContext
```

| Case | Response |
|---|---|
| no `Authorization` header | `401` |
| expired, malformed, or signed with a different key | `401` |
| valid token | the request reaches the controller |

Security details:

- **Algorithm:** HMAC-SHA (HS256 with a 32-byte key), signed with `SECRET_KEY`.
- **Token claims:** `sub` (email), `iat` (issued at) and `exp` (expiration).
- **Passwords:** stored with BCrypt.
- **User enumeration protection:** the login runs BCrypt even when the email doesn't exist, so the response time doesn't reveal which emails are registered.
- **CSRF disabled:** the token travels in the `Authorization` header, not in a cookie.

---

## Project structure

```
src/main/java/com/payment/app/
├── PaymentApplication.java
├── config/                          security configuration
│   ├── SecurityConfig.java              filter chain, public routes, stateless session
│   ├── SecurityBeansConfig.java         PasswordEncoder (BCrypt)
│   ├── JwtAuthenticationFilter.java     reads and validates the token on every request
│   └── JwtAuthenticationEntryPoint.java returns 401 when authentication is missing
└── core/
    ├── presentation/                web layer
    │   ├── controllers/                 UserController, AuthController
    │   └── payloads/                    requests and responses (records)
    ├── application/                 business logic
    │   ├── services/                    UserService, LoginService
    │   ├── mappers/                     UserMapper (MapStruct)
    │   └── exceptions/                  InvalidCredentialsException
    └── infrastructure/              data access and technical concerns
        ├── models/                      JPA entities (BaseModel, User, CustomerModel)
        ├── repositories/                UserRepository
        ├── enums/                       GenderEnum
        └── security/                    JwtUtil
```

### Data model

```
users                              customer
├── id                             ├── id
├── email     (unique)             ├── user_id   ──► users.id  (one-to-one)
├── username  (unique)             ├── cpf       (unique)
├── password  (BCrypt)             ├── name
├── created_at                     ├── phone_number (unique)
├── updated_at                     ├── gender    (MALE | FEMALE)
└── is_active                      ├── created_at / updated_at / is_active
```

`User` is the **login account**. `CustomerModel` is **the payer** (the data Asaas requires, such as the CPF, the Brazilian taxpayer ID) and has no endpoint yet.

---

## Tests

```bash
./mvnw test
```

The current test (`contextLoads`) starts the full Spring context, so it needs **PostgreSQL running** (`docker compose up -d`) and a **`.env`** file with `SECRET_KEY`.

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `Could not resolve placeholder 'SECRET_KEY'` | `.env` is missing, or the app was started outside the project root | create `.env` (step 2) and run from the project root |
| `WeakKeyException: ... is not secure enough` | `SECRET_KEY` is shorter than 256 bits | generate a new one with `openssl rand -base64 32` |
| `DecodingException: Illegal base64 character` | `SECRET_KEY` isn't valid Base64 (quotes, spaces) | paste the key without quotes or spaces |
| `release version 21 not supported` | Maven is running on a JDK older than 21 | point `JAVA_HOME` to JDK 21 (step 4) |
| `Connection refused` on port 5432 | PostgreSQL isn't running | `docker compose up -d` |
| error adding a `NOT NULL` column on startup | an entity got a new required field and the table already has rows | in development, delete the rows (or the table) before starting |
| a `"trace"` field with the stack trace in error responses | Spring Boot DevTools enables this in development | expected in development; in production, set `server.error.include-stacktrace=never` |

---

## Roadmap

**Authentication**
- [x] User sign-up with BCrypt-hashed passwords
- [x] JWT login (`tokenType` and `expiresIn` in the response)
- [x] JWT filter and `401` response for unauthenticated requests
- [x] Protection against user enumeration via response timing
- [ ] Login rate limiting (Bucket4j): per IP and per account, with `429` and `Retry-After`
- [ ] Normalize email (lowercase) on sign-up and login
- [ ] Return `409` (instead of `500`) for an email or username that's already taken

**Asaas integration**
- [ ] Feign client with the sandbox API key
- [ ] Payment details sign-up (`customer`: CPF, name, phone number)
- [ ] Create the customer in Asaas (`POST /customers`), reusing an existing one
- [ ] Plans and subscriptions (`POST /subscriptions`)
- [ ] `POST /v1/subscribe` endpoint for the authenticated user

**Resilience**
- [ ] Timeouts and error handling for the integration
- [ ] Idempotency, retry with backoff, and circuit breaker
- [ ] Asaas webhook to track payments
- [ ] Reconciliation, Transactional Outbox, and saga
