# Backend Conventions (Kotlin + Spring Boot)

> 이 문서는 실제 프로젝트 코드에서 추출한 패턴입니다. 새 기능 구현 시 이 패턴을 따르세요.

---

## 패키지 구조

```
com.otoki.internal/
├── controller/     # REST Controller
├── service/        # Business Logic
├── repository/     # JPA Repository
├── entity/         # JPA Entity + Enum
├── dto/
│   ├── request/    # Request DTO (validation 포함)
│   └── response/   # Response DTO
├── exception/      # BusinessException + 도메인별 예외
├── config/         # SecurityConfig, AppConfig 등
└── security/       # JWT, UserPrincipal
```

## Jackson 설정 (IMPORTANT)

```yaml
# application.yml
spring.jackson.property-naming-strategy: SNAKE_CASE
```

- **Kotlin DTO**: camelCase (`productCode`, `storeName`)
- **JSON 입출력**: snake_case (`product_code`, `store_name`) — 자동 변환
- **테스트 jsonPath**: snake_case 사용 → `jsonPath("$.data.product_code")`

---

## Controller

```kotlin
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService  // constructor injection (항상)
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
    }

    // 인증 필요 엔드포인트: @AuthenticationPrincipal UserPrincipal
    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        authService.changePassword(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 변경되었습니다"))
    }

    // 데이터 없는 응답: 204 No Content
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        val token = resolveToken(request)
        if (token != null) authService.logout(token)
        return ResponseEntity.noContent().build()
    }

    // 리소스 생성: 201 Created
    @PostMapping
    fun createShelfLife(...): ResponseEntity<ApiResponse<ShelfLifeItemResponse>> {
        val response = shelfLifeService.createShelfLife(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "유통기한이 등록되었습니다"))
    }

    // 쿼리 파라미터: @RequestParam
    @GetMapping
    fun getList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) storeId: Long?,  // 선택 파라미터
        @RequestParam fromDate: String,                    // 필수 파라미터
        @RequestParam toDate: String
    ): ResponseEntity<ApiResponse<ListResponse>> { ... }
}
```

**규칙**:
- 응답은 항상 `ApiResponse.success(data, message)` 래핑
- `@Valid @RequestBody`로 DTO validation 위임
- Controller에 비즈니스 로직 없음 — Service 위임만
- 리소스 생성: `ResponseEntity.status(HttpStatus.CREATED).body(...)`
- 삭제 with message: `ApiResponse.success(null as Any?, "삭제 메시지")`

---

## Service

```kotlin
// 패턴 A: 메서드별 @Transactional (기능이 적은 Service)
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {
    @Transactional(readOnly = true)  // 조회만: readOnly = true
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmployeeId(request.employeeId)
            .orElseThrow { InvalidCredentialsException() }
        // ...
    }

    @Transactional  // 변경: readOnly 생략
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        userRepository.save(user)
    }
}

// 패턴 B: 클래스 레벨 readOnly + 메서드 오버라이드 (CRUD Service)
@Service
@Transactional(readOnly = true)  // 기본: 조회
class ShelfLifeService(
    private val shelfLifeRepository: ShelfLifeRepository,
    private val userRepository: UserRepository
) {
    fun getList(...): ListResponse { ... }         // readOnly 상속
    fun getDetail(...): ItemResponse { ... }       // readOnly 상속

    @Transactional                                 // 변경: 오버라이드
    fun create(...): ItemResponse { ... }

    @Transactional
    fun delete(...) { ... }

    // Private helper methods
    private fun findById(id: Long): Entity =
        repository.findById(id).orElseThrow { NotFoundException() }

    private fun validateOwnership(entity: Entity, userId: Long) {
        if (entity.user.id != userId) throw ForbiddenException()
    }
}
```

**규칙**:
- 예외는 도메인별 BusinessException 서브클래스 throw
- 조회: `@Transactional(readOnly = true)`, 변경: `@Transactional`
- Optional 처리: `.orElseThrow { XxxException() }`
- CRUD Service는 클래스 레벨 readOnly + 변경 메서드만 `@Transactional` 오버라이드
- 반복 로직은 private helper 추출 (`findById`, `validateOwnership`)

---

## Exception

```kotlin
// 기본 클래스 (GlobalExceptionHandler.kt 하단에 정의)
open class BusinessException(
    val errorCode: String,
    override val message: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

// 도메인별 예외 (AuthExceptions.kt)
class InvalidCredentialsException : BusinessException(
    errorCode = "INVALID_CREDENTIALS",
    message = "사번 또는 비밀번호가 올바르지 않습니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

// 파라미터가 필요한 경우
class InvalidPasswordFormatException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_PASSWORD_FORMAT",
    message = detail ?: "비밀번호 형식이 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
```

**규칙**:
- 파일명: `<도메인>Exceptions.kt` (한 파일에 관련 예외 모음)
- errorCode: UPPER_SNAKE_CASE
- message: 한국어, 사용자 노출 가능한 문구
- GlobalExceptionHandler가 자동 처리 → Controller에서 try-catch 불필요

---

## Request DTO

```kotlin
data class LoginRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Pattern(regexp = "^\\d{8}$", message = "사번은 8자리 숫자여야 합니다")
    val employeeId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val password: String
)
```

**규칙**:
- `data class` + `@field:` validation 어노테이션 + 한국어 message
- 파일명: DTO 1개면 `<Domain>Request.kt`, 여러 개면 `<Domain>Requests.kt`

---

## Response DTO

```kotlin
// 단일 항목 Response (Entity → DTO 변환 포함)
data class ShelfLifeItemResponse(
    val id: Long, val productCode: String, val productName: String, ...
) {
    companion object {
        fun from(entity: ShelfLife): ShelfLifeItemResponse = from(entity, LocalDate.now())
        fun from(entity: ShelfLife, today: LocalDate): ShelfLifeItemResponse {  // 테스트용 오버로드
            val dDay = ChronoUnit.DAYS.between(today, entity.expiryDate).toInt()
            return ShelfLifeItemResponse(id = entity.id, ...)
        }
    }
}

// 목록 Response (래퍼)
data class ShelfLifeListResponse(
    val totalCount: Int,
    val expiredItems: List<ShelfLifeItemResponse>,
    val upcomingItems: List<ShelfLifeItemResponse>
)

// 작업 결과 Response
data class ShelfLifeBatchDeleteResponse(val deletedCount: Int)
```

**규칙**:
- Entity → DTO 변환은 DTO의 `companion object { fun from() }` 패턴
- 날짜 의존 계산은 `from(entity, today)` 오버로드로 테스트 가능하게
- 파일명: DTO 1개면 `<Domain>Response.kt`, 여러 개면 `<Domain>Responses.kt`

---

## ApiResponse (공통 응답 래퍼)

```kotlin
data class ApiResponse<T>(
    val success: Boolean, val data: T? = null,
    val error: ErrorDetail? = null, val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T>
        fun <T> error(code: String, message: String): ApiResponse<T>
    }
}
data class ErrorDetail(val code: String, val message: String)
```

---

## Entity

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "employee_id", nullable = false, unique = true, length = 8)
    val employeeId: String,
    var password: String,    // 변경 가능 필드: var
    val name: String,        // 불변 필드: val
    // ...
) {
    // 도메인 행위 메서드
    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.passwordChangeRequired = false
        this.updatedAt = LocalDateTime.now()
    }
}
```

**규칙**: 변경 가능 필드만 `var`, 나머지 `val`. 도메인 행위는 Entity 메서드로.

---

## Repository

```kotlin
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmployeeId(employeeId: String): Optional<User>
    fun existsByEmployeeId(employeeId: String): Boolean
    fun findByBranchName(branchName: String): List<User>
}
```

---

## Service Test (Mockito)

```kotlin
@ExtendWith(MockitoExtension::class)
@DisplayName("ShelfLifeService 테스트")
class ShelfLifeServiceTest {
    @Mock private lateinit var shelfLifeRepository: ShelfLifeRepository
    @Mock private lateinit var userRepository: UserRepository
    @InjectMocks private lateinit var shelfLifeService: ShelfLifeService

    // @Nested로 메서드별 테스트 그룹핑
    @Nested
    @DisplayName("createShelfLife - 유통기한 등록")
    inner class CreateShelfLifeTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> ShelfLife 생성 반환")
        fun createShelfLife_success() {
            // Given
            val request = ShelfLifeCreateRequest(storeId = 1025L, ...)
            whenever(storeRepository.findById(1025L)).thenReturn(Optional.of(store))
            whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.getArgument<ShelfLife>(0) }
            // When
            val result = shelfLifeService.createShelfLife(userId, request)
            // Then
            assertThat(result.productCode).isEqualTo("30310009")
        }

        @Test
        @DisplayName("거래처 없음 - 존재하지 않는 storeId -> StoreNotFoundException")
        fun createShelfLife_storeNotFound() {
            whenever(storeRepository.findById(9999L)).thenReturn(Optional.empty())
            assertThatThrownBy { shelfLifeService.createShelfLife(userId, request) }
                .isInstanceOf(ShelfLifeStoreNotFoundException::class.java)
        }
    }

    // Helper: default parameter로 유연한 테스트 데이터 생성
    private fun createShelfLife(
        id: Long = 1L, userId: Long = 1L, expiryDate: LocalDate = LocalDate.now().plusDays(5), ...
    ): ShelfLife { ... }
    private fun createUser(id: Long = 1L, employeeId: String = "12345678"): User { ... }
    private fun createStore(id: Long = 1025L): Store { ... }
}
```

**규칙**:
- `@Nested inner class`로 메서드별 테스트 그룹핑
- Given/When/Then + `@DisplayName` 한국어 (형식: "조건 - 입력 설명 -> 기대 결과")
- `whenever`/`assertThat`/`assertThatThrownBy` (mockito-kotlin + AssertJ)
- Helper 팩토리: default parameter로 테스트마다 필요한 값만 지정
- save mock: `whenever(repo.save(any<Entity>())).thenAnswer { it.getArgument<Entity>(0) }`

---

## Controller Test (WebMvcTest)

```kotlin
@WebMvcTest(ShelfLifeController::class)
@AutoConfigureMockMvc(addFilters = false)   // Security 필터 비활성화
@DisplayName("ShelfLifeController 테스트")
class ShelfLifeControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockitoBean private lateinit var shelfLifeService: ShelfLifeService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider       // 항상 필요
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter  // 항상 필요

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("POST /api/v1/shelf-life - 등록")
    inner class CreateShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 등록")
        fun create_success() {
            whenever(shelfLifeService.createShelfLife(eq(1L), any())).thenReturn(response)
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)                    // 201 for create
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_code").value("P001"))  // snake_case!
        }

        @Test
        @DisplayName("실패 - storeId 누락")
        fun create_missingStoreId() {
            val invalidJson = """{"store_id": null, "product_code": "P001", ...}"""
            mockMvc.perform(post("/api/v1/shelf-life")
                .contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 거래처 미존재")
        fun create_storeNotFound() {
            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(ShelfLifeStoreNotFoundException())
            mockMvc.perform(post(...).contentType(...).content(...))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"))
        }
    }
}
```

**규칙**:
- `@WebMvcTest` + `addFilters = false` + `@MockitoBean`
- `jwtTokenProvider` + `jwtAuthenticationFilter`는 항상 MockitoBean 등록
- `@Nested`로 HTTP 메서드 + URL별 그룹핑
- **jsonPath는 snake_case** (Jackson SNAKE_CASE 설정)
- validation 실패 테스트: raw JSON string 사용 (objectMapper 우회)
- 예외 테스트: Service mock이 예외 throw → HTTP status + error code 검증

---

## Repository Test (DataJpaTest)

```kotlin
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // H2 사용
@ActiveProfiles("test")
class ShelfLifeRepositoryTest {
    @Autowired private lateinit var shelfLifeRepository: ShelfLifeRepository
    @Autowired private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testStore: Store

    @BeforeEach
    fun setUp() {
        shelfLifeRepository.deleteAll()
        testEntityManager.clear()
        testUser = testEntityManager.persistAndFlush(User(employeeId = "20030117", ...))
        testStore = testEntityManager.persistAndFlush(Store(storeCode = "1025", ...))
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("findByUserIdAndExpiryDateBetween - 사용자별 기간 조회")
    inner class FindByUserIdAndExpiryDateBetweenTests {
        @Test
        @DisplayName("기간 내 데이터 조회")
        fun success() {
            // Given
            persistShelfLife(user = testUser, store = testStore, expiryDate = today.plusDays(3))
            // When
            val result = shelfLifeRepository.findByUserIdAndExpiryDateBetween(testUser.id, today, today.plusDays(7))
            // Then
            assertThat(result).hasSize(1)
        }
    }

    // Helper: persistAndFlush + clear 패턴
    private fun persistShelfLife(...): ShelfLife {
        val entity = ShelfLife(user = user, store = store, ...)
        val persisted = testEntityManager.persistAndFlush(entity)
        testEntityManager.clear()  // 1차 캐시 초기화 → 실제 DB 조회 보장
        return persisted
    }
}
```

**규칙**:
- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = ANY)` + `@ActiveProfiles("test")`
- `TestEntityManager`로 테스트 데이터 세팅 (Repository 자체가 아닌 EntityManager 사용)
- `persistAndFlush` → `clear` → 테스트 조회 (1차 캐시 우회)
- `@BeforeEach`에서 `deleteAll` + 기본 테스트 데이터 준비

---

## 환경 변수 패턴

### 규칙
- 환경별 값: `${ENV_VAR:default}` 형식 (application-dev.yml, application-prod.yml)
- 시크릿(비밀번호, API 키): `${ENV_VAR}` (default 없이, 누락 시 기동 실패)
- 새 환경 변수 추가 시 `docker-compose.yml` environment 섹션도 함께 수정

### Terraform 연계
- ECS Task Definition의 환경 변수: `infra/modules/ecs/main.tf`의 `environment` 블록
- Secrets Manager 시크릿: `infra/modules/secrets/main.tf` + ECS `secrets` 블록
- 새 환경 변수가 인프라 변경을 수반하면 별도 Infra Issue 필요
