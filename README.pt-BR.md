# Asaas Payment Integration

🇺🇸 [Read in English](README.md)

API em Spring Boot para estudar a integração com o gateway de pagamentos [Asaas](https://www.asaas.com/): cadastro de usuários, autenticação com JWT e, nas próximas etapas, criação de clientes e assinaturas no sandbox do Asaas, com foco em resiliência (timeouts, retry, idempotência, webhooks).

> **Status:** em desenvolvimento. A base de usuários e autenticação está pronta. A integração com o Asaas ainda não foi implementada (veja o [Roadmap](#roadmap)).

---

## Sumário

- [Stack](#stack)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Endpoints](#endpoints)
- [Autenticação](#autenticação)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Problemas comuns](#problemas-comuns)
- [Roadmap](#roadmap)

---

## Stack

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | linguagem |
| Spring Boot | 4.1.2 | Web MVC, Validation, Data JPA, Security |
| Spring Cloud OpenFeign | 2025.1.4 | cliente HTTP para a API do Asaas (próximas etapas) |
| PostgreSQL | 18 | banco de dados (via Docker Compose) |
| JJWT | 0.12.6 | geração e validação dos tokens JWT |
| MapStruct | 1.6.3 | conversão entre DTOs e entidades |
| Lombok | — | redução de código repetitivo nas entidades |
| Bucket4j + Caffeine | 8.22.0 / gerenciada pelo Boot | limite de tentativas de login (em implementação) |

---

## Pré-requisitos

- **JDK 21** ou mais recente
- **Docker** com o Docker Compose (para o PostgreSQL)
- **OpenSSL** (para gerar a chave do JWT; já vem no macOS e na maioria das distribuições Linux)

O Maven não precisa estar instalado: o projeto usa o Maven Wrapper (`./mvnw`).

---

## Como rodar

**1. Clone o repositório**

```bash
git clone git@github.com:oliverigor27/asaas-payment-integration.git
cd asaas-payment-integration
```

**2. Crie o `.env` com a chave do JWT**

```bash
cp .env.example .env
```

Gere uma chave e cole no `SECRET_KEY=` do `.env`:

```bash
openssl rand -base64 32
```

O `.env` está no `.gitignore` e **nunca** deve ser commitado.

**3. Suba o PostgreSQL**

```bash
docker compose up -d
```

Isso cria o container `payment-postgres` na porta `5432`, com banco, usuário e senha `payment`.

**4. Suba a aplicação**

```bash
./mvnw spring-boot:run
```

Se o terminal estiver usando um JDK anterior ao 21, aponte o `JAVA_HOME` (exemplo no macOS):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21+) ./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. As tabelas são criadas automaticamente pelo Hibernate (`ddl-auto=update`).

**5. Teste**

```bash
# cadastro
curl -i -X POST http://localhost:8080/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","username":"ana","password":"senhaforte123"}'

# login
curl -s -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","password":"senhaforte123"}'
```

---

## Configuração

A aplicação lê o `.env` da raiz do projeto (`spring.config.import=optional:file:.env[.properties]`). Também aceita as mesmas variáveis exportadas no ambiente.

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `SECRET_KEY` | **sim** | — | chave do JWT, em **Base64**, com pelo menos **32 bytes** (256 bits) depois de decodificada. Gere com `openssl rand -base64 32` |
| `DB_HOST` | não | `localhost` | host do PostgreSQL |
| `DB_PORT` | não | `5432` | porta do PostgreSQL |
| `DB_NAME` | não | `payment` | nome do banco |
| `DB_USERNAME` | não | `payment` | usuário do banco |
| `DB_PASSWORD` | não | `payment` | senha do banco |

As variáveis `DB_*` são usadas tanto pelo `compose.yaml` quanto pela aplicação, então os dois ficam sempre em acordo.

> O caminho `file:.env` é relativo à pasta de onde a aplicação é iniciada. Rode os comandos a partir da raiz do projeto.

---

## Endpoints

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/v1/users` | pública | cadastra um usuário |
| `POST` | `/v1/auth/login` | pública | autentica e devolve um token JWT |

Qualquer outra rota exige um token válido no cabeçalho `Authorization`.

### `POST /v1/users`

**Corpo:**

```json
{
  "email": "ana@exemplo.com",
  "username": "ana",
  "password": "senhaforte123"
}
```

| Campo | Regras |
|---|---|
| `email` | obrigatório, formato de email, único |
| `username` | obrigatório, até 50 caracteres, único |
| `password` | obrigatório, de 8 a 100 caracteres; gravado com BCrypt |

**Respostas:**

| Status | Quando |
|---|---|
| `200` | usuário criado (corpo vazio) |
| `400` | dados inválidos |
| `500` | email ou username já cadastrado (ainda sem tratamento; veja o [Roadmap](#roadmap)) |

### `POST /v1/auth/login`

**Corpo:**

```json
{
  "email": "ana@exemplo.com",
  "password": "senhaforte123"
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

`expiresIn` é a validade do token em segundos (24 horas).

**`401 Unauthorized`** (email inexistente ou senha errada; a resposta é a mesma nos dois casos):

```json
{
  "message": "Usuário ou senha inválidos"
}
```

**`400 Bad Request`:** email ou senha ausentes, ou email em formato inválido.

---

## Autenticação

A API é **stateless**: não há sessão. Cada requisição a uma rota protegida precisa enviar o token recebido no login:

```bash
curl http://localhost:8080/v1/<rota> \
  -H "Authorization: Bearer <token>"
```

Como funciona:

```
requisição ─► JwtAuthenticationFilter ─► ... ─► AuthorizationFilter ─┬─► rota liberada ─► controller
              valida o token e registra                              │
              o usuário (email) no                                   └─► sem usuário ─► JwtAuthenticationEntryPoint ─► 401
              SecurityContext
```

| Situação | Resposta |
|---|---|
| sem o cabeçalho `Authorization` | `401` |
| token expirado, malformado ou assinado com outra chave | `401` |
| token válido | a requisição segue para o controller |

Detalhes de segurança:

- **Algoritmo:** HMAC-SHA (HS256 com uma chave de 32 bytes), assinado com a `SECRET_KEY`.
- **Conteúdo do token:** `sub` (email), `iat` (emissão) e `exp` (expiração).
- **Senhas:** armazenadas com BCrypt.
- **Proteção contra enumeração de usuários:** o login executa o BCrypt mesmo quando o email não existe, para que o tempo de resposta não revele quais emails estão cadastrados.
- **CSRF desabilitado:** o token viaja no cabeçalho `Authorization`, e não num cookie.

---

## Estrutura do projeto

```
src/main/java/com/payment/app/
├── PaymentApplication.java
├── config/                          configuração de segurança
│   ├── SecurityConfig.java              cadeia de filtros, rotas públicas, sessão stateless
│   ├── SecurityBeansConfig.java         PasswordEncoder (BCrypt)
│   ├── JwtAuthenticationFilter.java     lê e valida o token de cada requisição
│   └── JwtAuthenticationEntryPoint.java responde 401 quando falta autenticação
└── core/
    ├── presentation/                camada web
    │   ├── controllers/                 UserController, AuthController
    │   └── payloads/                    requests e responses (records)
    ├── application/                 regras de negócio
    │   ├── services/                    UserService, LoginService
    │   ├── mappers/                     UserMapper (MapStruct)
    │   └── exceptions/                  InvalidCredentialsException
    └── infrastructure/              acesso a dados e recursos técnicos
        ├── models/                      entidades JPA (BaseModel, User, CustomerModel)
        ├── repositories/                UserRepository
        ├── enums/                       GenderEnum
        └── security/                    JwtUtil
```

### Modelo de dados

```
users                              customer
├── id                             ├── id
├── email     (único)              ├── user_id   ──► users.id  (um-para-um)
├── username  (único)              ├── cpf       (único)
├── password  (BCrypt)             ├── name
├── created_at                     ├── phone_number (único)
├── updated_at                     ├── gender    (MEN | WOMEN)
└── is_active                      ├── created_at / updated_at / is_active
```

O `User` representa a **conta de acesso** (login). O `CustomerModel` representa **quem paga** (os dados que o Asaas exige) e ainda não tem endpoint.

---

## Testes

```bash
./mvnw test
```

O teste atual (`contextLoads`) sobe o contexto completo do Spring, então precisa do **PostgreSQL rodando** (`docker compose up -d`) e do **`.env`** com a `SECRET_KEY`.

---

## Problemas comuns

| Erro | Causa | Solução |
|---|---|---|
| `Could not resolve placeholder 'SECRET_KEY'` | o `.env` não existe ou a aplicação foi iniciada fora da raiz do projeto | criar o `.env` (passo 2) e rodar a partir da raiz |
| `WeakKeyException: ... is not secure enough` | a `SECRET_KEY` tem menos de 256 bits | gerar outra com `openssl rand -base64 32` |
| `DecodingException: Illegal base64 character` | a `SECRET_KEY` não é Base64 válido (aspas, espaços) | colar a chave sem aspas e sem espaços |
| `release version 21 not supported` | o Maven está usando um JDK anterior ao 21 | apontar o `JAVA_HOME` para o JDK 21 (passo 4) |
| `Connection refused` na porta 5432 | o PostgreSQL não está rodando | `docker compose up -d` |
| erro ao adicionar coluna `NOT NULL` ao subir | uma entidade ganhou um campo obrigatório e a tabela já tem linhas | apagar as linhas (ou a tabela) em desenvolvimento antes de subir |
| campo `"trace"` com o stack trace nas respostas de erro | o Spring Boot DevTools ativa esse comportamento em desenvolvimento | é esperado em desenvolvimento; em produção, `server.error.include-stacktrace=never` |

---

## Roadmap

**Autenticação**
- [x] Cadastro de usuário com senha em BCrypt
- [x] Login com JWT (`tokenType` e `expiresIn` na resposta)
- [x] Filtro JWT e resposta 401 para requisições sem autenticação
- [x] Proteção contra enumeração de usuários pelo tempo de resposta
- [ ] Limite de tentativas de login (Bucket4j): por IP e por conta, com `429` e `Retry-After`
- [ ] Normalizar o email (minúsculas) no cadastro e no login
- [ ] Responder `409` (em vez de `500`) para email ou username já cadastrado

**Integração com o Asaas**
- [ ] Cliente Feign com a chave da API do sandbox
- [ ] Cadastro dos dados de pagamento (`customer`: CPF, nome, telefone)
- [ ] Criação do customer no Asaas (`POST /customers`), reaproveitando o já existente
- [ ] Planos e assinaturas (`POST /subscriptions`)
- [ ] Endpoint `POST /v1/subscribe` para o usuário autenticado

**Resiliência**
- [ ] Timeouts e tratamento de erros da integração
- [ ] Idempotência, retry com backoff e circuit breaker
- [ ] Webhook do Asaas para acompanhar os pagamentos
- [ ] Reconciliação, Transactional Outbox e saga
