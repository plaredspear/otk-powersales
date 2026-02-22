# Backend Architecture

> 현재 Backend 구현 상태 요약. 구현 시 참조용.
> 코딩 컨벤션은 `.claude/guides/backend-conventions.md` 참조.

---

## Tech Stack

| 항목 | 버전 |
|------|------|
| Kotlin | 1.9.25 |
| Spring Boot | 3.5.0 |
| Java | 17 |
| PostgreSQL | 16.x (Schema: `salesforce2`) |
| JWT | jjwt 0.12.6 (HS256) |
| Swagger | springdoc 2.8.4 |

---

## 패키지 구조

```
com.otoki.internal/
├── config/         SecurityConfig, AppConfig, OpenApiConfig
├── controller/     22개 REST Controller
├── service/        24개 Service
├── repository/     40개 JPA Repository
├── entity/         50개 Entity + Enum
├── dto/
│   ├── request/    12개 Request DTO
│   └── response/   50+개 Response DTO
├── exception/      BusinessException 계층 (16개 도메인별 예외)
├── security/       JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
└── integration/    SapOrderClient (Stub, 비활성)
```

---

## 인증/보안

- **JWT Stateless**: Access Token + Refresh Token
- **토큰 블랙리스트**: `ConcurrentHashMap` 인메모리 (Phase 2에서 Redis로 교체 예정)
- **역할**: USER, LEADER, ADMIN (`UserRole` enum)
- **필터 체인**: JwtAuthenticationFilter → SecurityConfig
- **Public 엔드포인트**: `/health`, `/auth/login`, `/auth/refresh`, `/swagger-ui/**`, `/v3/api-docs/**`

### JWT 설정

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:3600000}         # Access Token (기본 1시간)
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # Refresh Token (기본 7일)
```

---

## 외부 시스템 연동

| 시스템 | 상태 | 파일 |
|--------|------|------|
| SAP 주문 전송 | Stub (비활성) | `integration/SapOrderClient.kt`, `StubSapOrderClient.kt` |
| Orora 영업 | 미구현 | - |
| 대형마트 EDI | 미구현 | - |
| Redis (ElastiCache) | **미연결** (인프라만 준비됨) | - |

---

## 환경별 설정

| 항목 | local | dev | prod |
|------|-------|-----|------|
| DB | localhost PostgreSQL | `${DATABASE_URL}` (RDS) | `${DATABASE_URL}` (RDS) |
| DB Schema | salesforce2 | salesforce2 | salesforce2 |
| DDL | validate | none | none |
| Redis | 미설정 | 미설정 (ECS에 `REDIS_HOST` 전달 중) | 미설정 |
| Flyway | disabled | disabled | - |
| JWT Secret | 하드코딩 (개발용) | `${JWT_SECRET}` | `${JWT_SECRET}` |

---

## API 엔드포인트 목록

| Controller | Base Path | 주요 기능 |
|-----------|-----------|----------|
| AuthController | `/api/v1/auth` | 로그인, 로그아웃, 토큰갱신, 비밀번호변경, GPS동의 |
| HealthController | `/api/v1/health` | 헬스체크 |
| HomeController | `/api/v1/home` | 홈화면 통합 데이터 |
| ProductController | `/api/v1/products` | 제품 검색 (텍스트/바코드) |
| StoreController | `/api/v1/stores` | 거래처 목록 |
| OrderController | `/api/v1/me/orders` | 내 주문 조회/취소/재전송 |
| OrderDraftController | `/api/v1/me/drafts` | 주문 임시저장 CRUD |
| OrderQueryController | `/api/v1/queries/orders` | 주문 조회 |
| ClientOrderController | `/api/v1/clients/orders` | 거래처 주문 조회/생성 |
| ClaimController | `/api/v1/claims` | 클레임 CRUD |
| NoticeController | `/api/v1/notices` | 공지사항 목록/상세 |
| EducationController | `/api/v1/educations` | 교육 게시물 |
| EventController | `/api/v1/events` | 행사 정보 |
| DailySalesController | `/api/v1/daily-sales` | 일 매출 |
| MonthlySalesController | `/api/v1/monthly-sales` | 월 매출 |
| AttendanceController | `/api/v1/attendances` | 출근등록 |
| MyScheduleController | `/api/v1/me/schedules` | 내 일정 |
| InspectionController | `/api/v1/inspections` | 점검 CRUD |
| SafetyCheckController | `/api/v1/safety-checks` | 안전점검 |
| SuggestionController | `/api/v1/suggestions` | 건의사항 |
| ShelfLifeController | `/api/v1/shelf-life` | 유통기한 관리 |
| FavoriteProductController | `/api/v1/favorites` | 즐겨찾기 제품 |

---

## 구현 상태

### 활성 (Phase 1 완료)

- 인증 (로그인/로그아웃/토큰갱신/비밀번호변경)
- 홈 대시보드
- 제품 검색 / 거래처 조회
- 클레임 관리
- 공지사항 / 교육 / 행사
- 일 매출 / 월 매출
- 안전점검 / 건의사항
- 유통기한 관리 / 즐겨찾기

### 비활성 (코드 존재하나 commented out 또는 Phase 2 대기)

- 주문 전체 흐름 (Order full cycle)
- 출근등록 (Attendance)
- 행사 상세 (Event detail)
- SAP 연동 (Stub 상태)
- 유통기한 임박 제품 동기화 (ExpiryProduct)

### 미구현

- Redis 연결 (`S63-Redis-연결설정` 스펙 작성됨)
- 토큰 블랙리스트 Redis 마이그레이션
- 외부 시스템 동기화 (Orora, SAP, EDI)
- 파일 업로드 인프라
- Rate Limiting

---

## 테스트

- **Controller 테스트**: 21개 (`@WebMvcTest` + `addFilters = false`)
- **Service 테스트**: 23개 (`@ExtendWith(MockitoExtension::class)`)
- **Repository 테스트**: 17개 (`@DataJpaTest` + H2)
- **DTO/기타 테스트**: 3개
- **테스트 DB**: H2 in-memory (`application-test.yml`)
- **테스트 시 Security mock**: `jwtTokenProvider` + `jwtAuthenticationFilter`는 항상 `@MockitoBean`
