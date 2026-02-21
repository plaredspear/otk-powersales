# Backend Conventions (Kotlin + Spring Boot)

> 이 문서는 실제 프로젝트 코드에서 추출한 패턴입니다. 새 기능 구현 시 이 패턴을 따르세요.

---

## 패키지 구조

```
com.otoki.internal/
├── controller/     # REST Controller
├── service/        # Business Logic
├── repository/     # JPA Repository
├── entity/         # JPA Entity
├── dto/
│   ├── request/    # Request DTO (validation 포함)
│   └── response/   # Response DTO
├── exception/      # BusinessException + 도메인별 예외
├── config/         # SecurityConfig, AppConfig 등
└── security/       # JWT, UserPrincipal
```

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
}
```

**규칙**:
- 응답은 항상 `ApiResponse.success(data, message)` 래핑
- `@Valid @RequestBody`로 DTO validation 위임
- Controller에 비즈니스 로직 없음 — Service 위임만

---

## Service

```kotlin
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
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }
        // ... 토큰 생성 + LoginResponse 반환
    }

    @Transactional  // 변경: readOnly 생략
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        // ... 검증 + 변경
        userRepository.save(user)
    }
}
```

**규칙**:
- 예외는 도메인별 BusinessException 서브클래스 throw
- 조회: `@Transactional(readOnly = true)`, 변경: `@Transactional`
- Optional 처리: `.orElseThrow { XxxException() }`

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

**규칙**: `data class` + `@field:` validation 어노테이션 + 한국어 message

---

## Response DTO

```kotlin
data class LoginResponse(
    val user: UserInfo,
    val token: TokenInfo,
    val requiresPasswordChange: Boolean,
    val requiresGpsConsent: Boolean
)

// 중첩 DTO + Entity → DTO 변환
data class UserInfo(
    val id: Long, val employeeId: String, val name: String,
    val department: String, val branchName: String, val role: String
) {
    companion object {
        fun from(user: User): UserInfo = UserInfo(
            id = user.id, employeeId = user.employeeId, name = user.name,
            department = user.department, branchName = user.branchName,
            role = user.role.name
        )
    }
}
```

**규칙**: Entity → DTO 변환은 DTO의 `companion object { fun from() }` 패턴

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
@DisplayName("AuthService 테스트")
class AuthServiceTest {
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var passwordEncoder: PasswordEncoder
    @InjectMocks private lateinit var authService: AuthService

    @Test
    @DisplayName("로그인 성공 - 유효한 사번과 비밀번호로 로그인 시 LoginResponse 반환")
    fun login_success() {
        // Given
        val user = createTestUser(id = 1L, employeeId = "12345678")
        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", user.password)).thenReturn(true)
        // When
        val response = authService.login(LoginRequest("12345678", "password123"))
        // Then
        assertThat(response.user.employeeId).isEqualTo("12345678")
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사번으로 로그인 시 InvalidCredentialsException 발생")
    fun login_userNotFound() {
        whenever(userRepository.findByEmployeeId("99999999")).thenReturn(Optional.empty())
        assertThatThrownBy { authService.login(LoginRequest("99999999", "password123")) }
            .isInstanceOf(InvalidCredentialsException::class.java)
    }

    // Helper
    private fun createTestUser(id: Long = 1L, ...): User = User(id = id, ...)
}
```

**규칙**: Given/When/Then + `@DisplayName` 한국어 + `whenever`/`assertThat`/`assertThatThrownBy` (mockito-kotlin + AssertJ)

---

## Controller Test (WebMvcTest)

```kotlin
@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)   // Security 필터 비활성화
class AuthControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockitoBean private lateinit var authService: AuthService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        // SecurityContext에 인증된 사용자 설정
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Test
    @DisplayName("정상 로그인 - 200 OK, 사용자 정보 및 토큰 반환")
    fun login_success() {
        whenever(authService.login(any())).thenReturn(mockLoginResponse)
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.employee_id").value("12345678"))
    }
}
```

**규칙**: `@WebMvcTest` + `addFilters = false` + `@MockitoBean` + JSON body는 `objectMapper` 또는 raw string
