package com.otoki.internal.admin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.response.RowError
import com.otoki.internal.admin.dto.response.RowPreview
import com.otoki.internal.admin.exception.*
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.MonthlySalesHistory
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.sap.repository.OrganizationRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepository
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockMultipartFile
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
    private lateinit var excelParser: ScheduleExcelParser

    @Mock
    private lateinit var uploadValidator: ScheduleUploadValidator

    @Mock
    private lateinit var scheduleRepository: DisplayWorkScheduleRepository

    @Mock
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @Spy
    private var objectMapper: ObjectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }

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
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode)
            val employees = listOf(
                createEmployee(employeeNumber = "20030001", name = "홍길동", orgName = "A팀"),
                createEmployee(employeeNumber = "20030002", name = "김철수", orgName = "B팀")
            )

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(org)
            whenever(
                employeeRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
                    costCenterCode, "재직"
                )
            ).thenReturn(employees)
            whenever(templateGenerator.generate(employees)).thenReturn(ByteArray(100))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(100)
            assertThat(result.filename).startsWith("진열스케줄_양식_")
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
            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(null)
            whenever(organizationRepository.findFirstByCostCenterLevel4(costCenterCode)).thenReturn(null)

            assertThatThrownBy { adminScheduleService.generateTemplate(1L) }
                .isInstanceOf(OrganizationNotFoundException::class.java)
        }

        @Test
        @DisplayName("소속 사원 0명 - 빈 템플릿 반환")
        fun generateTemplate_noEmployees() {
            val userId = 1L
            val costCenterCode = "1234"
            val employee = createEmployee(id = userId, costCenterCode = costCenterCode)
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(org)
            whenever(
                employeeRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
                    costCenterCode, "재직"
                )
            ).thenReturn(emptyList())
            whenever(templateGenerator.generate(emptyList())).thenReturn(ByteArray(50))

            val result = adminScheduleService.generateTemplate(userId)

            assertThat(result.bytes).hasSize(50)
            assertThat(result.filename).startsWith("진열스케줄_양식_")
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
                ScheduleExcelParser.ParsedRow(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상시", "2026-04-01", null, LocalDate.of(2026, 4, 1), null)
            )
            val parseResult = ScheduleExcelParser.ParseResult(parsedRows, 1)
            val employee = createEmployee(employeeNumber = "20030001", name = "홍길동", sfid = "USR001")
            val account = createAccount(externalKey = "ACC001", sfid = "ACC_SFID_001", name = "이마트 강남점")

            whenever(excelParser.parse(any())).thenReturn(parseResult)
            whenever(employeeRepository.findByEmployeeNumberIn(listOf("20030001"))).thenReturn(listOf(employee))
            whenever(accountRepository.findByExternalKeyIn(listOf("ACC001"))).thenReturn(listOf(account))
            whenever(scheduleRepository.findByEmployeeIdInAndNotDeleted(listOf(1L))).thenReturn(emptyList())
            whenever(uploadValidator.validate(eq(parsedRows), any(), any(), any())).thenReturn(
                ScheduleUploadValidator.ValidationResult(
                    errors = emptyList(),
                    previews = listOf(
                        RowPreview(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상시", "2026-04-01", null)
                    ),
                    validRows = listOf(
                        ScheduleUploadValidator.ValidatedRow(1L, "20030001", 1, "고정", "상시", LocalDate.of(2026, 4, 1), null)
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
            val rows = (1..501).map { ScheduleExcelParser.ParsedRow(it + 3, "emp$it", "name$it", "acc$it", null, "고정", "상시", "2026-04-01", null) }
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
                    ScheduleUploadValidator.ValidatedRow(1L, "20030001", 1, "고정", "상시", LocalDate.of(2026, 4, 1), null)
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            // When
            val result = adminScheduleService.confirmUpload(uploadId)

            // Then
            assertThat(result.insertedCount).isEqualTo(1)
            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 &&
                    list[0].employeeId == 1L &&
                    list[0].accountId == 1 &&
                    list[0].typeOfWork1 == "진열" &&
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
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findByCostCenterCodeInAndAppAuthority(listOf("A10010"), "조장"))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), any()))
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
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)
            val manager = createEmployee(employeeNumber = "20030099", name = "조장사원", costCenterCode = "A10010", appAuthority = "조장")

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findByCostCenterCodeInAndAppAuthority(listOf("A10010"), "조장"))
                .thenReturn(listOf(manager))
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), any()))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerId == manager.id
            })
        }

        @Test
        @DisplayName("ownerId 자동 설정 - 조장 미존재 시 null")
        fun confirmUpload_ownerIdWithoutManager() {
            val uploadId = "test-no-manager"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findByCostCenterCodeInAndAppAuthority(listOf("A10010"), "조장"))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), any()))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerId == null
            })
        }

        @Test
        @DisplayName("lastMonthRevenue 자동 설정 - 전월 매출 존재 시 Long 변환 저장")
        fun confirmUpload_lastMonthRevenue() {
            val uploadId = "test-revenue"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)
            val salesHistory = MonthlySalesHistory(
                id = 1,
                accountExternalKey = "EXT001",
                lastMonthResults = 5000000.0
            )

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findByCostCenterCodeInAndAppAuthority(listOf("A10010"), "조장"))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), eq(listOf("EXT001"))))
                .thenReturn(listOf(salesHistory))
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].lastMonthRevenue == 5000000L
            })
        }

        @Test
        @DisplayName("lastMonthRevenue 자동 설정 - 매출 데이터 없으면 null")
        fun confirmUpload_lastMonthRevenueNull() {
            val uploadId = "test-no-revenue"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = "A10010", accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(employeeRepository.findByCostCenterCodeInAndAppAuthority(listOf("A10010"), "조장"))
                .thenReturn(emptyList())
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), eq(listOf("EXT001"))))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].lastMonthRevenue == null
            })
        }

        @Test
        @DisplayName("costCenterCode가 null인 사원 - ownerId null, costCenterCode null")
        fun confirmUpload_nullCostCenterCode() {
            val uploadId = "test-null-cc"
            val cacheData = AdminScheduleService.UploadCacheData(
                validRows = listOf(
                    ScheduleUploadValidator.ValidatedRow(
                        1L, "20030001", 1, "고정", "상시",
                        LocalDate.of(2026, 4, 1), null,
                        costCenterCode = null, accountExternalKey = "EXT001"
                    )
                ),
                errorCount = 0
            )
            val json = objectMapper.writeValueAsString(cacheData)

            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
            whenever(valueOperations.get("schedule:upload:$uploadId")).thenReturn(json)
            whenever(monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(any(), any(), eq(listOf("EXT001"))))
                .thenReturn(emptyList())
            whenever(scheduleRepository.saveAll(any<List<DisplayWorkSchedule>>())).thenAnswer { it.getArgument<List<DisplayWorkSchedule>>(0) }
            whenever(redisTemplate.delete(any<String>())).thenReturn(true)

            adminScheduleService.confirmUpload(uploadId)

            verify(scheduleRepository).saveAll(argThat<List<DisplayWorkSchedule>> { list ->
                list.size == 1 && list[0].ownerId == null && list[0].costCenterCode == null
            })
        }
    }

    @Nested
    @DisplayName("listSchedules - 스케줄 목록 조회")
    inner class ListSchedulesTests {

        @Test
        @DisplayName("정상 조회 - 필터 없이 전체 목록 반환")
        fun listSchedules_success() {
            val schedule = createSchedule(id = 1L, employeeId = 1L, accountId = 100, confirmed = false)
            val page = PageImpl(listOf(schedule), PageRequest.of(0, 20), 1)
            val employee = createEmployee(id = 1L, employeeNumber = "20030001", name = "홍길동")
            val account = createAccount(id = 100, externalKey = "SAP001", name = "이마트 성수점")

            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(page)
            whenever(employeeRepository.findAllById(listOf(1L))).thenReturn(listOf(employee))
            whenever(accountRepository.findByIdIn(listOf(100))).thenReturn(listOf(account))

            val result = adminScheduleService.listSchedules(0, 20, null, null, null, null, null, null)

            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content[0].employeeCode).isEqualTo("20030001")
            assertThat(result.content[0].employeeName).isEqualTo("홍길동")
            assertThat(result.content[0].accountCode).isEqualTo("SAP001")
            assertThat(result.content[0].accountName).isEqualTo("이마트 성수점")
        }

        @Test
        @DisplayName("빈 결과 - 매칭 없음")
        fun listSchedules_empty() {
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            val result = adminScheduleService.listSchedules(0, 20, null, null, null, null, null, null)

            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.content).isEmpty()
        }

        @Test
        @DisplayName("거래처명 필터 - 매칭 거래처 ID로 조회")
        fun listSchedules_accountNameFilter() {
            val account = createAccount(id = 100, name = "이마트 성수점")
            whenever(accountRepository.findByNameContainingIgnoreCase("이마트")).thenReturn(listOf(account))
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), eq(listOf(100)), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            adminScheduleService.listSchedules(0, 20, null, "이마트", null, null, null, null)

            verify(scheduleRepository).findScheduleList(isNull(), eq(listOf(100)), isNull(), isNull(), isNull(), isNull(), any())
        }

        @Test
        @DisplayName("페이지 크기 제한 - 100 초과 시 100으로 제한")
        fun listSchedules_pageSizeLimit() {
            val emptyPage = PageImpl<DisplayWorkSchedule>(emptyList(), PageRequest.of(0, 100), 0)
            whenever(scheduleRepository.findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(emptyPage)

            adminScheduleService.listSchedules(0, 200, null, null, null, null, null, null)

            verify(scheduleRepository).findScheduleList(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), argThat { pageSize == 100 })
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

    private fun createSchedule(
        id: Long = 1L,
        employeeId: Long = 1L,
        accountId: Int = 1,
        confirmed: Boolean? = false,
        isDeleted: Boolean? = null
    ): DisplayWorkSchedule = DisplayWorkSchedule(
        id = id,
        employeeId = employeeId,
        accountId = accountId,
        typeOfWork1 = "진열",
        typeOfWork3 = "고정",
        typeOfWork5 = "상시",
        startDate = LocalDate.of(2026, 4, 1),
        endDate = LocalDate.of(2026, 12, 31),
        confirmed = confirmed,
        isDeleted = isDeleted
    )

    private fun createEmployee(
        id: Long = 1L,
        employeeNumber: String = "20030001",
        name: String = "테스트사원",
        costCenterCode: String? = "1234",
        orgName: String = "테스트팀",
        sfid: String? = "USR_SFID_001",
        status: String = "재직",
        appAuthority: String? = null
    ): Employee = Employee(
        id = id,
        employeeNumber = employeeNumber,
        name = name,
        costCenterCode = costCenterCode,
        orgName = orgName,
        appAuthority = appAuthority,
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
}
