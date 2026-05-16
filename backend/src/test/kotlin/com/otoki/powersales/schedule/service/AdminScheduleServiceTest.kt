package com.otoki.powersales.schedule.service

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.UserRole
import tools.jackson.databind.json.JsonMapper
import com.otoki.powersales.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.RowPreview
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.enums.SchedulePreset
import com.otoki.powersales.schedule.enums.SecondWorkType
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.enums.TypeOfWork3
import com.otoki.powersales.schedule.enums.TypeOfWork5
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminScheduleService 테스트")
class AdminScheduleServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var templateGenerator: ScheduleTemplateGenerator

    @Mock
    private lateinit var exportGenerator: ScheduleExportGenerator

    @Mock
    private lateinit var excelParser: ScheduleExcelParser

    @Mock
    private lateinit var uploadValidator: ScheduleUploadValidator

    @Mock
    private lateinit var scheduleRepository: DisplayWorkScheduleRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @Spy
    private var objectMapper: ObjectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @InjectMocks
    private lateinit var adminScheduleService: AdminScheduleService

    @Nested
    @DisplayName("generateTemplate - 양식 다운로드")
    inner class GenerateTemplateTests {

        @Test
        @DisplayName("정상 생성 - 사원이 있는 지점의 템플릿")
        fun generateTemplate_success() {
            val userId = 1L
            val costCenterCode = "1234"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode)
            val employees = listOf(
                createEmployee(employeeCode = "20030001", name = "홍길동", orgName = "A팀"),
                createEmployee(employeeCode = "20030002", name = "김철수", orgName = "B팀")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode))
                .thenReturn(orgCacheDto())
            whenever(
                employeeRepository.findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                    costCenterCode, UserRole.WOMAN, "재직"
                )
            ).thenReturn(employees)
            whenever(templateGenerator.generate(employees)).thenReturn(ByteArray(100))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(100)
            assertThat(result.filename).startsWith("진열마스터Template(신규작성용)_")
            assertThat(result.filename).doesNotContain("1234")
            assertThat(result.filename).endsWith(".xlsx")
        }

        @Test
        @DisplayName("사용자 미존재 - EmployeeNotFoundException")
        fun generateTemplate_userNotFound() {
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminScheduleService.generateTemplate(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("소속 지점 미설정 - MissingCostCenterException")
        fun generateTemplate_missingCostCenter() {
            val employee = createEmployee(costCenterCode = null)
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            assertThatThrownBy { adminScheduleService.generateTemplate(1L) }
                .isInstanceOf(MissingCostCenterException::class.java)
        }

        @Test
        @DisplayName("소속 지점 빈 문자열 - MissingCostCenterException")
        fun generateTemplate_emptyCostCenter() {
            val employee = createEmployee(costCenterCode = "")
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            assertThatThrownBy { adminScheduleService.generateTemplate(1L) }
                .isInstanceOf(MissingCostCenterException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 지점 - OrganizationNotFoundException")
        fun generateTemplate_orgNotFound() {
            val costCenterCode = "0000"
            val employee = createEmployee(costCenterCode = costCenterCode)
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            // cascade Level5→4 모두 miss → null. Service 는 OrganizationNotFoundException.
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode)).thenReturn(null)

            assertThatThrownBy { adminScheduleService.generateTemplate(1L) }
                .isInstanceOf(OrganizationNotFoundException::class.java)
        }

        @Test
        @DisplayName("소속 사원 0명 - 빈 템플릿 반환")
        fun generateTemplate_noEmployees() {
            val userId = 1L
            val costCenterCode = "1234"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode))
                .thenReturn(orgCacheDto())
            whenever(
                employeeRepository.findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                    costCenterCode, UserRole.WOMAN, "재직"
                )
            ).thenReturn(emptyList())
            whenever(templateGenerator.generate(emptyList())).thenReturn(ByteArray(50))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(50)
            assertThat(result.filename).startsWith("진열마스터Template(신규작성용)_")
        }

        @Test
        @DisplayName("영업지원실 사용자 - 다중 지점 여사원 조회")
        fun generateTemplate_salesSupport_multiBranch() {
            val userId = 1L
            val costCenterCode = "1111"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode, role = UserRole.SALES_SUPPORT)
            // cascade 결과 (DTO) — UC: SALES_SUPPORT 케이스에서 costCenterLevel3 를 expand 키로 사용
            val orgCache = orgCacheDto(costCenterLevel3 = "CC3")
            // findByCostCenterLevel3 결과는 Organization entity 리스트 (캐시 대상 아님)
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode, costCenterLevel3 = "CC3")
            val orgA = Organization(id = 2, costCenterLevel3 = "CC3", costCenterLevel5 = "2222")
            val orgB = Organization(id = 3, costCenterLevel3 = "CC3", costCenterLevel5 = "3333")

            val emp1 = createEmployee(employeeCode = "20030001", name = "김여사", orgName = "A지점", costCenterCode = "2222")
            val emp2 = createEmployee(employeeCode = "20030002", name = "이여사", orgName = "A지점", costCenterCode = "2222")
            val emp3 = createEmployee(employeeCode = "20030003", name = "박여사", orgName = "B지점", costCenterCode = "3333")

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode)).thenReturn(orgCache)
            whenever(organizationRepository.findByCostCenterLevel3("CC3")).thenReturn(listOf(org, orgA, orgB))
            whenever(
                employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                    listOf("1111", "2222", "3333"), UserRole.WOMAN, "재직"
                )
            ).thenReturn(listOf(emp1, emp2, emp3))
            whenever(templateGenerator.generate(any())).thenReturn(ByteArray(200))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(200)
            verify(employeeRepository).findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                listOf("1111", "2222", "3333"), UserRole.WOMAN, "재직"
            )
            verify(employeeRepository, never()).findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                any(), any(), any()
            )
        }

        @Test
        @DisplayName("조장 사용자 - 단일 지점 여사원 조회")
        fun generateTemplate_leader_singleBranch() {
            val userId = 1L
            val costCenterCode = "1234"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode, role = UserRole.LEADER)
            val employees = listOf(
                createEmployee(employeeCode = "20030001", name = "홍길동", orgName = "A팀"),
                createEmployee(employeeCode = "20030002", name = "김철수", orgName = "B팀")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode))
                .thenReturn(orgCacheDto())
            whenever(
                employeeRepository.findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
                    costCenterCode, UserRole.WOMAN, "재직"
                )
            ).thenReturn(employees)
            whenever(templateGenerator.generate(employees)).thenReturn(ByteArray(100))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(100)
            verify(employeeRepository, never()).findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                any(), any(), any()
            )
        }

        @Test
        @DisplayName("영업지원실이지만 코스트센터 없음 - MissingCostCenterException")
        fun generateTemplate_salesSupport_missingCostCenter() {
            val employee = createEmployee(costCenterCode = null, role = UserRole.SALES_SUPPORT)
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            assertThatThrownBy { adminScheduleService.generateTemplate(1L) }
                .isInstanceOf(MissingCostCenterException::class.java)
        }

        @Test
        @DisplayName("영업지원실 - Level3 하위에 여사원 0명 - 빈 템플릿 반환")
        fun generateTemplate_salesSupport_noEmployees() {
            val userId = 1L
            val costCenterCode = "1111"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode, role = UserRole.SALES_SUPPORT)
            val orgCache = orgCacheDto(costCenterLevel3 = "CC3")
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode, costCenterLevel3 = "CC3")
            val orgA = Organization(id = 2, costCenterLevel3 = "CC3", costCenterLevel5 = "2222")

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterCascade(costCenterCode)).thenReturn(orgCache)
            whenever(organizationRepository.findByCostCenterLevel3("CC3")).thenReturn(listOf(org, orgA))
            whenever(
                employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
                    listOf("1111", "2222"), UserRole.WOMAN, "재직"
                )
            ).thenReturn(emptyList())
            whenever(templateGenerator.generate(emptyList())).thenReturn(ByteArray(50))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(50)
        }
    }

    @Nested
    @DisplayName("exportSchedules - 선택 다운로드 (UC-08)")
    inner class ExportSchedulesTests {

        private val userId = 1L

        private fun mockAdminScope(): com.otoki.powersales.admin.dto.DataScope =
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 다운로드 - 선택 ID 순서 보존 + 파일명 패턴")
        fun exportSchedules_success() {
            val scope = mockAdminScope()
            val s1 = createSchedule(id = 11L)
            val s2 = createSchedule(id = 12L)
            whenever(scheduleRepository.findAllById(listOf(12L, 11L))).thenReturn(listOf(s1, s2))
            whenever(exportGenerator.generate(any())).thenReturn(ByteArray(500))

            val result = adminScheduleService.exportSchedules(scope, listOf(12L, 11L))

            assertThat(result.bytes).hasSize(500)
            assertThat(result.filename).startsWith("진열스케줄_").endsWith(".xlsx")
            // 입력 순서대로 (12, 11) entity 가 generator 에 전달되었는지 확인
            verify(exportGenerator).generate(argThat<List<DisplayWorkSchedule>> {
                this.size == 2 && this[0].id == 12L && this[1].id == 11L
            })
        }

        @Test
        @DisplayName("삭제된 레코드는 제외")
        fun exportSchedules_excludesDeleted() {
            val scope = mockAdminScope()
            val active = createSchedule(id = 11L)
            val deletedSchedule = createSchedule(id = 12L, isDeleted = true)
            whenever(scheduleRepository.findAllById(listOf(11L, 12L))).thenReturn(listOf(active, deletedSchedule))
            whenever(exportGenerator.generate(any())).thenReturn(ByteArray(200))

            adminScheduleService.exportSchedules(scope, listOf(11L, 12L))

            verify(exportGenerator).generate(argThat<List<DisplayWorkSchedule>> {
                this.size == 1 && this[0].id == 11L
            })
        }

        @Test
        @DisplayName("UC-12 scope 위반 - LEADER scope 밖 레코드는 조용히 제외")
        fun exportSchedules_leaderScopeFilter() {
            val scope = com.otoki.powersales.admin.dto.DataScope(branchCodes = listOf("A10010"), isAllBranches = false)
            val inScope = DisplayWorkSchedule(id = 11L, costCenterCode = "A10010")
            val outOfScope = DisplayWorkSchedule(id = 12L, costCenterCode = "B20020")
            whenever(scheduleRepository.findAllById(listOf(11L, 12L))).thenReturn(listOf(inScope, outOfScope))
            whenever(exportGenerator.generate(any())).thenReturn(ByteArray(200))

            adminScheduleService.exportSchedules(scope, listOf(11L, 12L))

            verify(exportGenerator).generate(argThat<List<DisplayWorkSchedule>> {
                this.size == 1 && this[0].id == 11L
            })
        }
    }

    @Nested
    @DisplayName("uploadAndValidate - Excel 업로드 검증")
    inner class UploadAndValidateTests {

        @Test
        @DisplayName("정상 업로드 - 검증 결과 반환")
        fun uploadAndValidate_success() {
            // Given
            val file = MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ByteArray(100))
            val parsedRows = listOf(
                ScheduleExcelParser.ParsedRow(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상온", "상시", "2026-04-01", null, LocalDate.of(2026, 4, 1), null)
            )
            val parseResult = ScheduleExcelParser.ParseResult(parsedRows, 1)
            val employee = createEmployee(employeeCode = "20030001", name = "홍길동", sfid = "USR001")
            val account = createAccount(externalKey = "ACC001", sfid = "ACC_SFID_001", name = "이마트 강남점")

            whenever(excelParser.parse(any())).thenReturn(parseResult)
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("20030001"))).thenReturn(listOf(employee))
            whenever(accountRepository.findByExternalKeyIn(listOf("ACC001"))).thenReturn(listOf(account))
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validate(eq(parsedRows), any(), any(), any())).thenReturn(
                ScheduleUploadValidator.ValidationResult(
                    errors = emptyList(),
                    previews = listOf(
                        RowPreview(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상온", "상시", "2026-04-01", null)
                    ),
                    validRows = listOf(
                        ScheduleUploadValidator.ValidatedRow(1L, "20030001", 1, "고정", "상온", "상시", LocalDate.of(2026, 4, 1), null)
                    )
                )
            )
            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)

            // When
            val result = adminScheduleService.uploadAndValidate(file)

            // Then
            assertThat(result.totalRows).isEqualTo(1)
            assertThat(result.successRows).isEqualTo(1)
            assertThat(result.errorRows).isEqualTo(0)
            assertThat(result.uploadId).isNotBlank()
            assertThat(result.previews).hasSize(1)
            verify(valueOperations).set(any(), any(), eq(30L), any())
        }

        @Test
        @DisplayName("빈 파일 - EMPTY_FILE 에러")
        fun uploadAndValidate_emptyFile() {
            val file = MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ByteArray(100))
            whenever(excelParser.parse(any())).thenReturn(ScheduleExcelParser.ParseResult(emptyList(), 0))

            assertThatThrownBy { adminScheduleService.uploadAndValidate(file) }
                .isInstanceOf(ScheduleEmptyFileException::class.java)
        }

        @Test
        @DisplayName("행 초과 - ROW_LIMIT_EXCEEDED 에러")
        fun uploadAndValidate_rowLimitExceeded() {
            val file = MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ByteArray(100))
            val rows = (1..501).map { ScheduleExcelParser.ParsedRow(it + 3, "emp$it", "name$it", "acc$it", null, "고정", "상온", "상시", "2026-04-01", null) }
            whenever(excelParser.parse(any())).thenReturn(ScheduleExcelParser.ParseResult(rows, 501))

            assertThatThrownBy { adminScheduleService.uploadAndValidate(file) }
                .isInstanceOf(ScheduleRowLimitExceededException::class.java)
        }

        @Test
        @DisplayName("잘못된 확장자 - INVALID_FILE_TYPE 에러")
        fun uploadAndValidate_invalidFileType() {
            val file = MockMultipartFile("file", "test.csv", "text/csv", ByteArray(100))

            assertThatThrownBy { adminScheduleService.uploadAndValidate(file) }
                .isInstanceOf(ScheduleInvalidFileTypeException::class.java)
        }

        @Test
        @DisplayName("파일 크기 초과 - FILE_TOO_LARGE 에러")
        fun uploadAndValidate_fileTooLarge() {
            val file = MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ByteArray(6 * 1024 * 1024))

            assertThatThrownBy { adminScheduleService.uploadAndValidate(file) }
                .isInstanceOf(ScheduleFileTooLargeException::class.java)
        }

        @Test
        @DisplayName("파일 미첨부 - FILE_REQUIRED 에러")
        fun uploadAndValidate_emptyUpload() {
            val file = MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ByteArray(0))

            assertThatThrownBy { adminScheduleService.uploadAndValidate(file) }
                .isInstanceOf(ScheduleFileRequiredException::class.java)
        }
    }

    @Nested
    @DisplayName("confirmUpload - 업로드 확정")
    inner class ConfirmUploadTests {

        @Test
        @DisplayName("정상 확정 - DB 저장 성공")
        fun confirmUpload_success() {
            // Given
            val uploadId = "test-upload-id"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(1L, "20030001", 1, "고정", "상온", "상시", LocalDate.of(2026, 4, 1), null)
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(createAccount(id = 1)))
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            // When
            val result = adminScheduleService.confirmUpload(uploadId)

            // Then
            assertThat(result.insertedCount).isEqualTo(1)
            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 &&
                    list[0].employee?.id == 1L &&
                    list[0].account?.id == 1 &&
                    list[0].typeOfWork1 == TypeOfWork1.DISPLAY &&
                    list[0].confirmed == false
            })
            verify(redisTemplate).delete("schedule:upload:$uploadId")
        }

        @Test
        @DisplayName("만료된 upload_id - UPLOAD_NOT_FOUND")
        fun confirmUpload_notFound() {
            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get(any())).thenReturn(null)

            assertThatThrownBy { adminScheduleService.confirmUpload("expired-id") }
                .isInstanceOf(ScheduleUploadNotFoundException::class.java)
        }

        @Test
        @DisplayName("에러 있는 상태 확정 - HAS_VALIDATION_ERRORS")
        fun confirmUpload_hasErrors() {
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = emptyList(),
                errorCount = 3
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get(any())).thenReturn(json)

            assertThatThrownBy { adminScheduleService.confirmUpload("error-upload-id") }
                .isInstanceOf(ScheduleHasValidationErrorsException::class.java)
        }

        @Test
        @DisplayName("costCenterCode 자동 설정 - 사원의 costCenterCode가 저장된다")
        fun confirmUpload_costCenterCode() {
            val uploadId = "test-cost-center"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(createAccount(id = 1)))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].costCenterCode == "A10010"
            })
        }

        @Test
        @DisplayName("ownerId 자동 설정 - 조장이 존재하면 조장의 User PK 저장")
        fun confirmUpload_ownerIdWithManager() {
            val uploadId = "test-owner"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)
            val manager = createEmployee(employeeCode = "20030099", name = "조장사원", costCenterCode = "A10010", role = UserRole.LEADER)
            val leaderUser = createUser(id = 1099L, employeeCode = "20030099")

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(createAccount(id = 1)))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(listOf(manager))
            whenever(userRepository.findByEmployeeCodeIn(listOf("20030099"))).thenReturn(listOf(leaderUser))
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerUser?.id == leaderUser.id
            })
        }

        @Test
        @DisplayName("ownerId 자동 설정 - 조장 미존재 시 null")
        fun confirmUpload_ownerIdWithoutManager() {
            val uploadId = "test-no-manager"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(createAccount(id = 1)))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerUser == null
            })
        }

        @Test
        @DisplayName("lastMonthRevenue 자동 설정 - 전월 매출 존재 시 BigDecimal 변환 저장")
        fun confirmUpload_lastMonthRevenue() {
            val uploadId = "test-revenue"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)
            val account = Account(id = 1, externalKey = "EXT001")
            val salesHistory = MonthlySalesHistory(
                id = 1,
                account = account,
                lastMonthResults = BigDecimal("5000000")
            )

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(account))
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(account))))
                .thenReturn(listOf(salesHistory))
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].lastMonthRevenue?.compareTo(BigDecimal("5000000")) == 0
            })
        }

        @Test
        @DisplayName("lastMonthRevenue 자동 설정 - 매출 데이터 없으면 null")
        fun confirmUpload_lastMonthRevenueNull() {
            val uploadId = "test-no-revenue"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].lastMonthRevenue == null
            })
        }

        @Test
        @DisplayName("costCenterCode가 null인 사원 - owner null, costCenterCode null")
        fun confirmUpload_nullCostCenterCode() {
            val uploadId = "test-null-cc"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상온", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = null, accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(createEmployee(id = 1L)))
            whenever(accountRepository.findByIdIn(listOf(1))).thenReturn(listOf(createAccount(id = 1)))
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerUser == null && list[0].costCenterCode == null
            })
        }
    }

    @Nested
    @DisplayName("listSchedules - 스케줄 목록 조회")
    inner class ListSchedulesTests {

        private val userId = 1L

        private fun mockAdminScope(): com.otoki.powersales.admin.dto.DataScope =
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 조회 - 필터 없이 전체 목록 반환")
        fun listSchedules_success() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = 1L, employeeCode = "20030001", name = "홍길동")
            val account = createAccount(id = 100, externalKey = "SAP001", name = "이마트 성수점")
            val schedule = createSchedule(id = 1L, confirmed = false, employee = employee, account = account)
            val page = PageImpl(listOf(schedule), PageRequest.of(0, 20), 1)

            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page)

            val result = adminScheduleService.listSchedules(scope, 0, 20, null, null, null, null, null, null, null, Sort.unsorted())

            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].employeeCode).isEqualTo("20030001")
            assertThat(result.content[0].employeeName).isEqualTo("홍길동")
            assertThat(result.content[0].accountCode).isEqualTo("SAP001")
            assertThat(result.content[0].accountName).isEqualTo("이마트 성수점")
        }

        @Test
        @DisplayName("빈 결과 - 매칭 없음")
        fun listSchedules_empty() {
            val scope = mockAdminScope()
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            val result = adminScheduleService.listSchedules(scope, 0, 20, null, null, null, null, null, null, null, Sort.unsorted())

            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("거래처명 필터 - 매칭 거래처 ID로 조회")
        fun listSchedules_accountNameFilter() {
            val scope = mockAdminScope()
            val account = createAccount(id = 100, name = "이마트 성수점")
            whenever(accountRepository.findByNameContainingIgnoreCase("이마트")).thenReturn(listOf(account))
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), eq(listOf(100)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            adminScheduleService.listSchedules(scope, 0, 20, null, "이마트", null, null, null, null, null, Sort.unsorted())

            verify(scheduleRepository).findScheduleList(isNull(), eq(listOf(100)), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any())
        }

        @Test
        @DisplayName("페이지 크기 제한 - 100 초과 시 100으로 제한")
        fun listSchedules_pageSizeLimit() {
            val scope = mockAdminScope()
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 100), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            adminScheduleService.listSchedules(scope, 0, 200, null, null, null, null, null, null, null, Sort.unsorted())

            verify(scheduleRepository).findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), argThat { pageSize == 100 }
            )
        }

        @Test
        @DisplayName("preset 필터 - 레거시 List View 매핑 (END) 가 Repository 에 전달됨")
        fun listSchedules_presetEnd() {
            val scope = mockAdminScope()
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(SchedulePreset.END), isNull(), any()
            )).thenReturn(emptyPage)

            adminScheduleService.listSchedules(scope, 0, 20, null, null, null, null, null, null, SchedulePreset.END, Sort.unsorted())

            verify(scheduleRepository).findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(SchedulePreset.END), isNull(), any()
            )
        }

        @Test
        @DisplayName("정렬 - Sort 가 Pageable 에 반영됨")
        fun listSchedules_sortApplied() {
            val scope = mockAdminScope()
            val sort = Sort.by(Sort.Direction.DESC, "endDate")
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20, sort), 0)
            whenever(scheduleRepository.findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()
            )).thenReturn(emptyPage)

            adminScheduleService.listSchedules(scope, 0, 20, null, null, null, null, null, null, null, sort)

            verify(scheduleRepository).findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                argThat { this.sort == sort }
            )
        }

        @Test
        @DisplayName("UC-12 LEADER 사용자 - costCenterCodes 필터가 Repository 에 전달됨")
        fun listSchedules_leaderScope() {
            val scope = com.otoki.powersales.admin.dto.DataScope(branchCodes = listOf("A10010"), isAllBranches = false)
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(listOf("A10010")), any()
            )).thenReturn(emptyPage)

            adminScheduleService.listSchedules(scope, 0, 20, null, null, null, null, null, null, null, Sort.unsorted())

            verify(scheduleRepository).findScheduleList(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(listOf("A10010")), any()
            )
        }
    }

    @Nested
    @DisplayName("batchConfirm - 일괄 확정")
    inner class BatchConfirmTests {

        @Test
        @DisplayName("정상 확정 - confirmed=false 3건 → updated_count: 3")
        fun batchConfirm_success() {
            val schedules = listOf(
                createSchedule(id = 1L, confirmed = false),
                createSchedule(id = 2L, confirmed = false),
                createSchedule(id = 3L, confirmed = false)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L, 3L))).thenReturn(schedules)

            val result = adminScheduleService.batchConfirm(listOf(1L, 2L, 3L))

            assertThat(result.updatedCount).isEqualTo(3)
            assertThat(schedules.all { it.confirmed == true }).isTrue()
        }

        @Test
        @DisplayName("이미 확정된 건 포함 - 2건 false + 1건 true → updated_count: 2")
        fun batchConfirm_alreadyConfirmed() {
            val schedules = listOf(
                createSchedule(id = 1L, confirmed = false),
                createSchedule(id = 2L, confirmed = false),
                createSchedule(id = 3L, confirmed = true)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L, 3L))).thenReturn(schedules)

            val result = adminScheduleService.batchConfirm(listOf(1L, 2L, 3L))

            assertThat(result.updatedCount).isEqualTo(2)
        }

        @Test
        @DisplayName("전체 이미 확정 - updated_count: 0")
        fun batchConfirm_allAlreadyConfirmed() {
            val schedules = listOf(
                createSchedule(id = 1L, confirmed = true),
                createSchedule(id = 2L, confirmed = true)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L))).thenReturn(schedules)

            val result = adminScheduleService.batchConfirm(listOf(1L, 2L))

            assertThat(result.updatedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("미존재 ID 포함 - ScheduleNotFoundException")
        fun batchConfirm_notFound() {
            whenever(scheduleRepository.findAllById(listOf(1L, 999L))).thenReturn(
                listOf(createSchedule(id = 1L))
            )

            assertThatThrownBy { adminScheduleService.batchConfirm(listOf(1L, 999L)) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 레코드 포함 - ScheduleNotFoundException")
        fun batchConfirm_deleted() {
            val schedules = listOf(
                createSchedule(id = 1L),
                createSchedule(id = 2L, isDeleted = true)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L))).thenReturn(schedules)

            assertThatThrownBy { adminScheduleService.batchConfirm(listOf(1L, 2L)) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("batchUnconfirm - 확정 해제")
    inner class BatchUnconfirmTests {

        @Test
        @DisplayName("정상 해제 - confirmed=true 2건 → updated_count: 2")
        fun batchUnconfirm_success() {
            val schedules = listOf(
                createSchedule(id = 1L, confirmed = true),
                createSchedule(id = 2L, confirmed = true)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L))).thenReturn(schedules)

            val result = adminScheduleService.batchUnconfirm(listOf(1L, 2L))

            assertThat(result.updatedCount).isEqualTo(2)
            assertThat(schedules.all { it.confirmed == false }).isTrue()
        }

        @Test
        @DisplayName("이미 미확정 포함 - 1건 true + 1건 false → updated_count: 1")
        fun batchUnconfirm_alreadyUnconfirmed() {
            val schedules = listOf(
                createSchedule(id = 1L, confirmed = true),
                createSchedule(id = 2L, confirmed = false)
            )
            whenever(scheduleRepository.findAllById(listOf(1L, 2L))).thenReturn(schedules)

            val result = adminScheduleService.batchUnconfirm(listOf(1L, 2L))

            assertThat(result.updatedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("미존재 ID 포함 - ScheduleNotFoundException")
        fun batchUnconfirm_notFound() {
            whenever(scheduleRepository.findAllById(listOf(1L, 999L))).thenReturn(
                listOf(createSchedule(id = 1L))
            )

            assertThatThrownBy { adminScheduleService.batchUnconfirm(listOf(1L, 999L)) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("updateSchedule - 단건 편집")
    inner class UpdateScheduleTests {

        private val userId = 1L
        private val scheduleId = 10L

        private fun mockAdminScope(): com.otoki.powersales.admin.dto.DataScope =
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        private val baseRequest = AdminScheduleUpdateRequest(
            employeeCode = "20030001",
            accountCode = "ACC001",
            typeOfWork3 = "고정",
            typeOfWork4 = "상온",
            typeOfWork5 = "상시",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 12, 31)
        )

        @Test
        @DisplayName("정상 편집 — validateSingle 통과 + 자동채움 재실행 + entity 갱신")
        fun updateSchedule_success() {
            val scope = mockAdminScope()
            val originalEmployee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val originalAccount = createAccount(id = 1, externalKey = "ACC001")
            val schedule = createSchedule(id = scheduleId, confirmed = false, employee = originalEmployee, account = originalAccount)
            val user = createEmployee(id = userId, role = UserRole.LEADER)
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = baseRequest.startDate, endDate = baseRequest.endDate,
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(originalEmployee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(originalAccount)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(listOf(schedule))
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(baseRequest.startDate), eq(baseRequest.endDate),
                eq(originalEmployee), eq(originalAccount), eq(listOf(schedule)), eq(scheduleId)
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(originalAccount))))
                .thenReturn(emptyList())

            val result = adminScheduleService.updateSchedule(scope, userId, scheduleId, baseRequest)

            assertThat(result.id).isEqualTo(scheduleId)
            assertThat(result.employeeCode).isEqualTo("20030001")
            assertThat(schedule.startDate).isEqualTo(baseRequest.startDate)
            assertThat(schedule.endDate).isEqualTo(baseRequest.endDate)
            assertThat(schedule.costCenterCode).isEqualTo("A10010")
        }

        @Test
        @DisplayName("UC-05 차단 — 확정 + LEADER + 거래처 변경 시 ScheduleEditBlockedAfterConfirmException")
        fun updateSchedule_blockedAfterConfirm_leaderChangesAccount() {
            val scope = mockAdminScope()
            val originalEmployee = createEmployee(id = 1L, employeeCode = "20030001")
            val originalAccount = createAccount(id = 1, externalKey = "ACC_ORIGINAL")
            val schedule = createSchedule(id = scheduleId, confirmed = true, employee = originalEmployee, account = originalAccount)
            val user = createEmployee(id = userId, role = UserRole.LEADER)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(user))

            assertThatThrownBy {
                adminScheduleService.updateSchedule(scope, userId, scheduleId, baseRequest.copy(accountCode = "ACC001"))
            }.isInstanceOf(ScheduleEditBlockedAfterConfirmException::class.java)

            verify(uploadValidator, never()).validateSingle(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }

        @Test
        @DisplayName("UC-05 차단 예외 — 확정 + SYSTEM_ADMIN 은 모든 필드 변경 가능")
        fun updateSchedule_systemAdminBypassesBlock() {
            val scope = mockAdminScope()
            val originalEmployee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val originalAccount = createAccount(id = 1, externalKey = "ACC001")
            val schedule = createSchedule(id = scheduleId, confirmed = true, employee = originalEmployee, account = originalAccount)
            val adminUser = createEmployee(id = userId, role = UserRole.SYSTEM_ADMIN)
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = baseRequest.startDate, endDate = baseRequest.endDate,
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(adminUser))
            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(originalEmployee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(originalAccount)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(listOf(schedule))
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(baseRequest.startDate), eq(baseRequest.endDate),
                eq(originalEmployee), eq(originalAccount), eq(listOf(schedule)), eq(scheduleId)
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(originalAccount))))
                .thenReturn(emptyList())

            adminScheduleService.updateSchedule(scope, userId, scheduleId, baseRequest.copy(typeOfWork3 = "고정"))

            assertThat(schedule.startDate).isEqualTo(baseRequest.startDate)
        }

        @Test
        @DisplayName("UC-05 통과 — 확정 + LEADER + 종료일만 변경은 차단되지 않음")
        fun updateSchedule_leaderChangesOnlyEndDate() {
            val scope = mockAdminScope()
            val originalEmployee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val originalAccount = createAccount(id = 1, externalKey = "ACC001")
            // 기존 시작일·종료일 (baseRequest 와 시작일 동일하게 설정)
            val schedule = DisplayWorkSchedule(
                id = scheduleId,
                employee = originalEmployee,
                account = originalAccount,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                typeOfWork3 = TypeOfWork3.FIXED,
                typeOfWork4 = SecondWorkType.ROOM_TEMP,
                typeOfWork5 = TypeOfWork5.REGULAR,
                startDate = baseRequest.startDate,
                endDate = LocalDate.of(2026, 6, 30),
                confirmed = true
            )
            val user = createEmployee(id = userId, role = UserRole.LEADER)
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = baseRequest.startDate, endDate = LocalDate.of(2026, 5, 31),
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(originalEmployee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(originalAccount)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(listOf(schedule))
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(baseRequest.startDate), eq(LocalDate.of(2026, 5, 31)),
                eq(originalEmployee), eq(originalAccount), eq(listOf(schedule)), eq(scheduleId)
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(originalAccount))))
                .thenReturn(emptyList())

            adminScheduleService.updateSchedule(
                scope, userId, scheduleId,
                baseRequest.copy(endDate = LocalDate.of(2026, 5, 31))
            )

            assertThat(schedule.endDate).isEqualTo(LocalDate.of(2026, 5, 31))
        }

        @Test
        @DisplayName("미존재 스케줄 — ScheduleNotFoundException")
        fun updateSchedule_notFound() {
            whenever(scheduleRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminScheduleService.updateSchedule(mockAdminScope(), userId, 999L, baseRequest) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("validator 실패 — ScheduleValidationException")
        fun updateSchedule_validationFailure() {
            val scope = mockAdminScope()
            val originalEmployee = createEmployee(id = 1L, employeeCode = "20030001")
            val originalAccount = createAccount(id = 1, externalKey = "ACC001")
            val schedule = createSchedule(id = scheduleId, confirmed = false, employee = originalEmployee, account = originalAccount)
            val user = createEmployee(id = userId, role = UserRole.LEADER)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(originalEmployee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(originalAccount)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(listOf(schedule))
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(baseRequest.startDate), eq(baseRequest.endDate),
                eq(originalEmployee), eq(originalAccount), eq(listOf(schedule)), eq(scheduleId)
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(
                listOf("시작일이 종료일보다 이후입니다"), null
            ))

            assertThatThrownBy { adminScheduleService.updateSchedule(scope, userId, scheduleId, baseRequest) }
                .isInstanceOf(ScheduleValidationException::class.java)
                .hasMessageContaining("시작일이 종료일보다 이후")
        }
    }

    @Nested
    @DisplayName("batchDelete - 일괄 삭제 (UC-07 partial success)")
    inner class BatchDeleteTests {

        private val userId = 1L

        private fun mockAdminScopeForUser(user: Employee): com.otoki.powersales.admin.dto.DataScope =
            // 본 batchDelete 테스트군은 user 인자에 의존하지 않고 항상 ADMIN scope 반환 — 기존
            // mockAdminScope 와 동등. 호출자 가독성을 위해 시그니처만 유지.
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("ADMIN_GRADE - 확정/연결 여부 무관 전체 삭제")
        fun batchDelete_adminAllSucceed() {
            val admin = createEmployee(id = userId, role = UserRole.SYSTEM_ADMIN)
            val scope = mockAdminScopeForUser(admin)
            val s1 = createSchedule(id = 11L, confirmed = true)
            val s2 = createSchedule(id = 12L, confirmed = false)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(admin))
            whenever(scheduleRepository.findAllById(listOf(11L, 12L))).thenReturn(listOf(s1, s2))

            val result = adminScheduleService.batchDelete(scope, userId, listOf(11L, 12L))

            assertThat(result.deletedCount).isEqualTo(2)
            assertThat(result.failedCount).isEqualTo(0)
            assertThat(result.failures).isEmpty()
            assertThat(s1.isDeleted).isTrue()
            assertThat(s2.isDeleted).isTrue()
            verify(teamMemberScheduleRepository, never()).existsByDisplayWorkSchedule(any())
        }

        @Test
        @DisplayName("LEADER - partial success: 확정+FK 연결 건만 차단, 나머지 삭제")
        fun batchDelete_leaderPartialSuccess() {
            val leader = createEmployee(id = userId, role = UserRole.LEADER)
            val scope = mockAdminScopeForUser(leader)
            val blocked = createSchedule(id = 21L, confirmed = true) // FK 연결 있음
            val confirmedNoLink = createSchedule(id = 22L, confirmed = true)
            val unconfirmed = createSchedule(id = 23L, confirmed = false)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(leader))
            whenever(scheduleRepository.findAllById(listOf(21L, 22L, 23L)))
                .thenReturn(listOf(blocked, confirmedNoLink, unconfirmed))
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(eq(blocked))).thenReturn(true)
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(eq(confirmedNoLink))).thenReturn(false)

            val result = adminScheduleService.batchDelete(scope, userId, listOf(21L, 22L, 23L))

            assertThat(result.deletedCount).isEqualTo(2)
            assertThat(result.failedCount).isEqualTo(1)
            assertThat(result.failures).hasSize(1)
            assertThat(result.failures[0].id).isEqualTo(21L)
            assertThat(result.failures[0].errorCode).isEqualTo("SCHEDULE_DELETE_CONSTRAINT")
            assertThat(blocked.isDeleted).isNotEqualTo(true)
            assertThat(confirmedNoLink.isDeleted).isEqualTo(true)
            assertThat(unconfirmed.isDeleted).isEqualTo(true)
        }

        @Test
        @DisplayName("BRANCH_MANAGER - 전체 거부 (ScheduleDeleteForbiddenException)")
        fun batchDelete_branchManagerRejected() {
            val branch = createEmployee(id = userId, role = UserRole.BRANCH_MANAGER)
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(branch))

            assertThatThrownBy { adminScheduleService.batchDelete(mockAdminScopeForUser(branch), userId, listOf(11L, 12L)) }
                .isInstanceOf(ScheduleDeleteForbiddenException::class.java)

            verify(scheduleRepository, never()).findAllById(any<List<Long>>())
        }

        @Test
        @DisplayName("미존재 / 이미 삭제된 ID 포함 - SCHEDULE_NOT_FOUND 로 실패 기록")
        fun batchDelete_missingIds() {
            val leader = createEmployee(id = userId, role = UserRole.LEADER)
            val scope = mockAdminScopeForUser(leader)
            val valid = createSchedule(id = 31L, confirmed = false)
            val deletedAlready = createSchedule(id = 32L, confirmed = false, isDeleted = true)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(leader))
            whenever(scheduleRepository.findAllById(listOf(31L, 32L, 99L)))
                .thenReturn(listOf(valid, deletedAlready))

            val result = adminScheduleService.batchDelete(scope, userId, listOf(31L, 32L, 99L))

            assertThat(result.deletedCount).isEqualTo(1)
            assertThat(result.failedCount).isEqualTo(2)
            assertThat(result.failures.map { it.id }).containsExactlyInAnyOrder(32L, 99L)
            assertThat(result.failures).allMatch { it.errorCode == "SCHEDULE_NOT_FOUND" }
        }

        @Test
        @DisplayName("LEADER - 전체 차단되는 케이스 (deletedCount=0)")
        fun batchDelete_leaderAllBlocked() {
            val leader = createEmployee(id = userId, role = UserRole.LEADER)
            val scope = mockAdminScopeForUser(leader)
            val blocked1 = createSchedule(id = 41L, confirmed = true)
            val blocked2 = createSchedule(id = 42L, confirmed = true)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(leader))
            whenever(scheduleRepository.findAllById(listOf(41L, 42L)))
                .thenReturn(listOf(blocked1, blocked2))
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(any())).thenReturn(true)

            val result = adminScheduleService.batchDelete(scope, userId, listOf(41L, 42L))

            assertThat(result.deletedCount).isEqualTo(0)
            assertThat(result.failedCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("deleteSchedule - 진열마스터 삭제")
    inner class DeleteScheduleTests {

        private val userId = 1L
        private val scheduleId = 10L

        private fun mockAdminScope(): com.otoki.powersales.admin.dto.DataScope =
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("시스템관리자 삭제 - 확정+여사원일정 존재해도 삭제 성공")
        fun deleteSchedule_systemAdmin_success() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = userId, role = UserRole.SYSTEM_ADMIN)
            val schedule = createSchedule(id = scheduleId, confirmed = true)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))

            adminScheduleService.deleteSchedule(scope, userId, scheduleId)

            assertThat(schedule.isDeleted).isTrue()
        }

        @Test
        @DisplayName("영업지원실 삭제 - 확정+여사원일정 존재해도 삭제 성공")
        fun deleteSchedule_salesSupport_success() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = userId, role = UserRole.SALES_SUPPORT)
            val schedule = createSchedule(id = scheduleId, confirmed = true)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))

            adminScheduleService.deleteSchedule(scope, userId, scheduleId)

            assertThat(schedule.isDeleted).isTrue()
        }

        @Test
        @DisplayName("일반 사용자 미확정 삭제 - 삭제 성공")
        fun deleteSchedule_normalUser_unconfirmed_success() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = userId, role = UserRole.LEADER)
            val schedule = createSchedule(id = scheduleId, confirmed = false)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))

            adminScheduleService.deleteSchedule(scope, userId, scheduleId)

            assertThat(schedule.isDeleted).isTrue()
        }

        @Test
        @DisplayName("일반 사용자 확정+FK 연결 없음 - 삭제 성공")
        fun deleteSchedule_normalUser_confirmedNoLinked_success() {
            val scope = mockAdminScope()
            val scheduleEmployee = createEmployee(id = 2L)
            val scheduleAccount = createAccount(id = 100)
            val schedule = createSchedule(
                id = scheduleId, confirmed = true,
                employee = scheduleEmployee, account = scheduleAccount
            )
            val employee = createEmployee(id = userId, role = UserRole.LEADER)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(eq(schedule))).thenReturn(false)

            adminScheduleService.deleteSchedule(scope, userId, scheduleId)

            assertThat(schedule.isDeleted).isTrue()
        }

        @Test
        @DisplayName("미존재 스케줄 - ScheduleNotFoundException")
        fun deleteSchedule_notFound() {
            whenever(scheduleRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { adminScheduleService.deleteSchedule(mockAdminScope(), userId, 999L) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("이미 삭제된 스케줄 - ScheduleNotFoundException")
        fun deleteSchedule_alreadyDeleted() {
            val schedule = createSchedule(id = scheduleId, isDeleted = true)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))

            assertThatThrownBy { adminScheduleService.deleteSchedule(mockAdminScope(), userId, scheduleId) }
                .isInstanceOf(ScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("지점장 삭제 시도 - ScheduleDeleteForbiddenException")
        fun deleteSchedule_branchManager_forbidden() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = userId, role = UserRole.BRANCH_MANAGER)
            val schedule = createSchedule(id = scheduleId)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))

            assertThatThrownBy { adminScheduleService.deleteSchedule(scope, userId, scheduleId) }
                .isInstanceOf(ScheduleDeleteForbiddenException::class.java)
        }

        @Test
        @DisplayName("확정+FK 연결 여사원일정 존재 - ScheduleDeleteConstraintException")
        fun deleteSchedule_confirmedWithLinked_constraint() {
            val scope = mockAdminScope()
            val scheduleEmployee = createEmployee(id = 2L)
            val scheduleAccount = createAccount(id = 100)
            val schedule = createSchedule(
                id = scheduleId, confirmed = true,
                employee = scheduleEmployee, account = scheduleAccount
            )
            val employee = createEmployee(id = userId, role = UserRole.LEADER)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(eq(schedule))).thenReturn(true)

            assertThatThrownBy { adminScheduleService.deleteSchedule(scope, userId, scheduleId) }
                .isInstanceOf(ScheduleDeleteConstraintException::class.java)
        }

        @Test
        @DisplayName("UC-12 scope 위반 - LEADER 가 본인 담당 사업소 외 레코드 삭제 시도 시 ScheduleForbiddenException")
        fun deleteSchedule_uc12LeaderForbidden() {
            val scope = com.otoki.powersales.admin.dto.DataScope(branchCodes = listOf("A10010"), isAllBranches = false)
            val user = createEmployee(id = userId, role = UserRole.LEADER, costCenterCode = "A10010")
            val schedule = DisplayWorkSchedule(id = scheduleId, costCenterCode = "B20020")
            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(user))

            assertThatThrownBy { adminScheduleService.deleteSchedule(scope, userId, scheduleId) }
                .isInstanceOf(ScheduleForbiddenException::class.java)

            assertThat(schedule.isDeleted).isNotEqualTo(true)
        }

        @Test
        @DisplayName("UC-06 FK null 일정만 존재 - 삭제 허용 (값 매칭 대비 false positive 회피)")
        fun deleteSchedule_fkNullLinkedOnly_allowsDelete() {
            // 시나리오: 동일 (사원, 거래처, 기간) 의 여사원일정이 존재하지만 FK 가 null 인 케이스
            // 레거시 SF 와 동등하게 진열마스터 FK 가 연결되지 않은 일정은 "연결 없음" 으로 간주.
            val scope = mockAdminScope()
            val scheduleEmployee = createEmployee(id = 2L)
            val scheduleAccount = createAccount(id = 100)
            val schedule = createSchedule(
                id = scheduleId, confirmed = true,
                employee = scheduleEmployee, account = scheduleAccount
            )
            val employee = createEmployee(id = userId, role = UserRole.LEADER)

            whenever(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule))
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            // FK 매칭 — FK null 인 일정은 매치되지 않으므로 false 반환
            whenever(teamMemberScheduleRepository.existsByDisplayWorkSchedule(eq(schedule))).thenReturn(false)

            adminScheduleService.deleteSchedule(scope, userId, scheduleId)

            assertThat(schedule.isDeleted).isTrue()
        }
    }

    @Nested
    @DisplayName("createSchedule - 단건 신규 등록")
    inner class CreateScheduleTests {

        private val userId = 1L
        private val baseRequest = AdminScheduleCreateRequest(
            employeeCode = "20030001",
            accountCode = "ACC001",
            typeOfWork3 = "고정",
            typeOfWork4 = "상온",
            typeOfWork5 = "상시",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = null
        )

        private fun mockAdminScope(): com.otoki.powersales.admin.dto.DataScope =
            com.otoki.powersales.admin.dto.DataScope(branchCodes = emptyList(), isAllBranches = true)

        @Test
        @DisplayName("정상 등록 - validator 통과 + 자동채움 적용 후 저장")
        fun createSchedule_success() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val account = createAccount(id = 1, externalKey = "ACC001", name = "이마트 강남점")
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1), endDate = null,
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(LocalDate.of(2026, 5, 1)), isNull(), eq(employee), eq(account), eq(emptyList()), isNull()
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(account))))
                .thenReturn(emptyList())
            whenever(scheduleRepository.save(any<DisplayWorkSchedule>())).thenAnswer { it.getArgument<DisplayWorkSchedule>(0) }

            val result = adminScheduleService.createSchedule(scope, userId, baseRequest)

            assertThat(result.employeeCode).isEqualTo("20030001")
            assertThat(result.accountCode).isEqualTo("ACC001")
            assertThat(result.typeOfWork3).isEqualTo("고정")
            assertThat(result.costCenterCode).isEqualTo("A10010")
            verify(scheduleRepository).save(argThat<DisplayWorkSchedule> {
                this.employee?.id == 1L &&
                    this.account?.id == 1 &&
                    this.typeOfWork1 == TypeOfWork1.DISPLAY &&
                    this.confirmed == false &&
                    this.costCenterCode == "A10010"
            })
        }

        @Test
        @DisplayName("검증 실패 - validator 메시지로 ScheduleValidationException")
        fun createSchedule_validationFailure() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = 1L, employeeCode = "20030001")
            val account = createAccount(id = 1, externalKey = "ACC001")

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(LocalDate.of(2026, 5, 1)), isNull(), eq(employee), eq(account), eq(emptyList()), isNull()
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(
                listOf("기간내에 동일한 거래처가 등록되어 있습니다"), null
            ))

            assertThatThrownBy { adminScheduleService.createSchedule(scope, userId, baseRequest) }
                .isInstanceOf(ScheduleValidationException::class.java)
                .hasMessageContaining("기간내에 동일한 거래처가 등록되어 있습니다")

            verify(scheduleRepository, never()).save(any<DisplayWorkSchedule>())
        }

        @Test
        @DisplayName("사원 미존재 - validator 가 결정 + 에러 메시지 포함")
        fun createSchedule_employeeNotFound() {
            // employee 가 null 이면 scope 검증 skip 되므로 mockAdminScope 호출 불필요
            val account = createAccount(id = 1, externalKey = "ACC001")
            whenever(employeeRepository.findByEmployeeCode("99999999")).thenReturn(Optional.empty())
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)
            whenever(uploadValidator.validateSingle(
                eq("99999999"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(LocalDate.of(2026, 5, 1)), isNull(), isNull(), eq(account), eq(emptyList()), isNull()
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(
                listOf("사원번호 99999999: 존재하지 않는 사원"), null
            ))

            assertThatThrownBy {
                adminScheduleService.createSchedule(mockAdminScope(), userId, baseRequest.copy(employeeCode = "99999999"))
            }.isInstanceOf(ScheduleValidationException::class.java)
                .hasMessageContaining("존재하지 않는 사원")
        }

        @Test
        @DisplayName("조장 매칭 - 조장 User 가 ownerUser 로 설정됨")
        fun createSchedule_ownerLeader() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val account = createAccount(id = 1, externalKey = "ACC001")
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1), endDate = null,
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )
            val leaderEmp = createEmployee(employeeCode = "20030099", costCenterCode = "A10010", role = UserRole.LEADER)
            val leaderUser = createUser(id = 1099L, employeeCode = "20030099")

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(LocalDate.of(2026, 5, 1)), isNull(), eq(employee), eq(account), eq(emptyList()), isNull()
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(listOf(leaderEmp))
            whenever(userRepository.findByEmployeeCodeIn(listOf("20030099"))).thenReturn(listOf(leaderUser))
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(account))))
                .thenReturn(emptyList())
            whenever(scheduleRepository.save(any<DisplayWorkSchedule>())).thenAnswer { it.getArgument<DisplayWorkSchedule>(0) }

            adminScheduleService.createSchedule(scope, userId, baseRequest)

            verify(scheduleRepository).save(argThat<DisplayWorkSchedule> {
                this.ownerUser?.id == 1099L
            })
        }

        @Test
        @DisplayName("UC-12 scope 위반 - LEADER 가 다른 사업소 사원 등록 시 ScheduleForbiddenException")
        fun createSchedule_leaderScopeViolation() {
            val scope = com.otoki.powersales.admin.dto.DataScope(branchCodes = listOf("A10010"), isAllBranches = false)
            val employee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "B20020")
            val account = createAccount(id = 1, externalKey = "ACC001")
            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)

            assertThatThrownBy { adminScheduleService.createSchedule(scope, userId, baseRequest) }
                .isInstanceOf(ScheduleForbiddenException::class.java)

            verify(uploadValidator, never()).validateSingle(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }

        @Test
        @DisplayName("전월 매출 자동채움 - lastMonthRevenue BigDecimal 매핑")
        fun createSchedule_lastMonthRevenue() {
            val scope = mockAdminScope()
            val employee = createEmployee(id = 1L, employeeCode = "20030001", costCenterCode = "A10010")
            val account = Account(id = 1, externalKey = "ACC001")
            val validatedRow = ScheduleUploadValidator.ValidatedRow(
                userId = 1L, userEmployeeCode = "20030001", accountId = 1,
                typeOfWork3 = "고정", typeOfWork4 = "상온", typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1), endDate = null,
                costCenterCode = "A10010", accountExternalKey = "ACC001"
            )
            val salesHistory = MonthlySalesHistory(
                id = 1, account = account, lastMonthResults = BigDecimal("3500000")
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(account)
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validateSingle(
                eq("20030001"), eq("ACC001"), eq("고정"), eq("상온"), eq("상시"),
                eq(LocalDate.of(2026, 5, 1)), isNull(), eq(employee), eq(account), eq(emptyList()), isNull()
            )).thenReturn(ScheduleUploadValidator.SingleValidationResult(emptyList(), validatedRow))
            whenever(employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("A10010"), UserRole.LEADER))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), eq(listOf(account))))
                .thenReturn(listOf(salesHistory))
            whenever(scheduleRepository.save(any<DisplayWorkSchedule>())).thenAnswer { it.getArgument<DisplayWorkSchedule>(0) }

            val result = adminScheduleService.createSchedule(scope, userId, baseRequest)

            assertThat(result.lastMonthRevenue).isEqualTo(3500000L)
            verify(scheduleRepository).save(argThat<DisplayWorkSchedule> {
                this.lastMonthRevenue?.compareTo(BigDecimal("3500000")) == 0
            })
        }
    }

    // cascade stub helper — generateTemplate 의 cascade hit 케이스. SALES_SUPPORT 분기에서만
    // costCenterLevel3 가 필요, 나머지 케이스는 cascade 결과 non-null 확인만 의도.
    private fun orgCacheDto(
        costCenterLevel3: String? = null,
        orgCodeLevel3: String? = null,
        orgNameLevel3: String? = null,
        orgNameLevel4: String? = null,
    ): com.otoki.powersales.organization.repository.dto.OrganizationCacheDto =
        com.otoki.powersales.organization.repository.dto.OrganizationCacheDto(
            orgCodeLevel3 = orgCodeLevel3,
            orgNameLevel3 = orgNameLevel3,
            orgNameLevel4 = orgNameLevel4,
            costCenterLevel3 = costCenterLevel3,
        )

    private fun createSchedule(
        id: Long = 1L,
        employeeId: Long = 1L,
        accountId: Int = 1,
        confirmed: Boolean? = false,
        isDeleted: Boolean? = null,
        employee: Employee? = null,
        account: Account? = null
    ): DisplayWorkSchedule = DisplayWorkSchedule(
        id = id,
        employee = employee ?: createEmployee(id = employeeId),
        account = account ?: createAccount(id = accountId),
        typeOfWork1 = TypeOfWork1.DISPLAY,
        typeOfWork3 = TypeOfWork3.FIXED,
        typeOfWork5 = TypeOfWork5.REGULAR,
        startDate = LocalDate.of(2026, 4, 1),
        endDate = LocalDate.of(2026, 12, 31),
        confirmed = confirmed,
        isDeleted = isDeleted
    )

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "20030001",
        name: String = "테스트사원",
        costCenterCode: String? = "1234",
        orgName: String = "테스트팀",
        sfid: String? = "USR_SFID_001",
        status: String = "재직",
        role: UserRole? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        costCenterCode = costCenterCode,
        orgName = orgName,
        role = role,
        appLoginActive = true,
        status = status,
        sfid = sfid
    )

    private fun createAccount(
        id: Int = 1,
        externalKey: String = "ACC001",
        sfid: String = "ACC_SFID_001",
        name: String = "테스트거래처"
    ): Account = Account(
        id = id,
        externalKey = externalKey,
        sfid = sfid,
        name = name
    )

    private fun createUser(
        id: Long = 1L,
        employeeCode: String = "20030001",
        username: String = "user-$employeeCode",
        password: String = "pwd"
    ): User = User(
        id = id,
        username = username,
        employeeCode = employeeCode,
        password = password
    )
}
