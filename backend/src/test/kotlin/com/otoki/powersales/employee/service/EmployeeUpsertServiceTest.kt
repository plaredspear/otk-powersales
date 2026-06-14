package com.otoki.powersales.employee.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.enums.Gender
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.user.event.EmployeeCreatedEvent
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate

@DisplayName("EmployeeUpsertService 테스트")
class EmployeeUpsertServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

    private val service = EmployeeUpsertService(
        employeeRepository,
        systemCodeMasterRepository,
        eventPublisher,
        userRepository,
    )

    private fun command(
        employeeCode: String? = "100123",
        employeeName: String? = "홍길동",
        gender: String? = null,
        homePhone: String? = null,
        workPhone: String? = null,
        workEmail: String? = null,
        email: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null,
        birthdate: String? = null,
        orgCode: String? = null,
        lockingFlag: String? = null
    ): EmployeeUpsertCommand = EmployeeUpsertCommand(
        employeeCode = employeeCode,
        employeeName = employeeName,
        gender = gender,
        homePhone = homePhone,
        workPhone = workPhone,
        workEmail = workEmail,
        email = email,
        startDate = startDate,
        endDate = endDate,
        status = status,
        birthdate = birthdate,
        orgCode = orgCode,
        lockingFlag = lockingFlag
    )

    private fun statusCode(detailCode: String, detailCodeName: String): SystemCodeMaster =
        SystemCodeMaster(
            companyCode = "1000",
            groupCode = "H10010",
            detailCode = detailCode,
            detailCodeName = detailCodeName,
            externalKey = "1000_H10010_$detailCode"
        )

    private fun stubSaveAllCapture(): CapturingSlot<List<Employee>> {
        val slot = slot<List<Employee>>()
        every { employeeRepository.saveAll(capture(slot)) } answers { firstArg<List<Employee>>() }
        return slot
    }

    private fun stubEventCapture(): CapturingSlot<EmployeeCreatedEvent> {
        val slot = slot<EmployeeCreatedEvent>()
        every { eventPublisher.publishEvent(capture(slot)) } answers { }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 직원 1건 - INSERT, success_count=1, EmployeeInfo cascade 자동 생성")
        fun upsert_insertNew() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command()))

            val saved = savedSlot.captured.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.employeeInfo).isNotNull
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 직원 갱신 - 동일 PK 유지, name/homePhone 업데이트")
        fun upsert_updateExisting() {
            val existing = Employee(employeeCode = "100123", name = "기존이름")
            existing.homePhone = "old-phone"
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeName = "새이름", homePhone = "new-phone")))

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
            assertThat(saved.homePhone).isEqualTo("new-phone")
        }

        @Test
        @DisplayName("Status 변환 성공 - SystemCodeMaster 매칭 코드명 적용")
        fun upsert_statusResolved() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns listOf(statusCode("10", "재직"))
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(status = "10")))

            assertThat(savedSlot.captured.single().status).isEqualTo("재직")
        }

        @Test
        @DisplayName("Status 매칭 실패 - 원본 코드 그대로 저장")
        fun upsert_statusUnmapped() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns listOf(statusCode("10", "재직"))
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(status = "99")))

            assertThat(savedSlot.captured.single().status).isEqualTo("99")
        }

        @Test
        @DisplayName("companyCode 1000 외 행은 status 매핑에서 제외")
        fun upsert_statusFiltersByCompanyCode() {
            val other = SystemCodeMaster(
                companyCode = "2000",
                groupCode = "H10010",
                detailCode = "10",
                detailCodeName = "다른회사재직",
                externalKey = "2000_H10010_10"
            )
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns listOf(other)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(status = "10")))

            assertThat(savedSlot.captured.single().status).isEqualTo("10")
        }

        @Test
        @DisplayName("Gender 변환 - '1' → MALE, '2' → FEMALE, 그 외 → null")
        fun upsert_sexConversion() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(employeeCode = "M01", gender = "1"),
                    command(employeeCode = "F01", gender = "2"),
                    command(employeeCode = "X01", gender = "9")
                )
            )

            val byCode = savedSlot.captured.associateBy { it.employeeCode }
            assertThat(byCode["M01"]?.gender).isEqualTo(Gender.MALE)
            assertThat(byCode["F01"]?.gender).isEqualTo(Gender.FEMALE)
            assertThat(byCode["X01"]?.gender).isNull()
        }

        @Test
        @DisplayName("StartDate / EndDate / Birthdate - YYYYMMDD 변환, 00000000 은 null")
        fun upsert_dateConversion() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(command(startDate = "20200401", endDate = "00000000", birthdate = "19850315"))
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.startDate).isEqualTo(LocalDate.of(2020, 4, 1))
            assertThat(saved.endDate).isNull()
            assertThat(saved.birthDate).isEqualTo("19850315")
        }

        @Test
        @DisplayName("LockingFlag - Y → false, N → true, null → true")
        fun upsert_lockingFlag() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(employeeCode = "L01", lockingFlag = "Y"),
                    command(employeeCode = "L02", lockingFlag = "N"),
                    command(employeeCode = "L03", lockingFlag = null)
                )
            )

            val byCode = savedSlot.captured.associateBy { it.employeeCode }
            assertThat(byCode["L01"]?.appLoginActive).isFalse
            assertThat(byCode["L02"]?.appLoginActive).isTrue
            assertThat(byCode["L03"]?.appLoginActive).isTrue
        }

        @Test
        @DisplayName("WorkEmail / Email - 신규 컬럼 저장")
        fun upsert_emailColumns() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(workEmail = "work@otoki.com", email = "personal@otoki.com")))

            assertThat(savedSlot.captured.single().workEmail).isEqualTo("work@otoki.com")
            assertThat(savedSlot.captured.single().email).isEqualTo("personal@otoki.com")
        }

        @Test
        @DisplayName("OrgCode → costCenterCode 매핑")
        fun upsert_orgCodeMapping() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(orgCode = "11110")))

            assertThat(savedSlot.captured.single().costCenterCode).isEqualTo("11110")
        }
    }

    @Nested
    @DisplayName("upsert - Spec #579 origin=MANUAL 보호")
    inner class ManualOriginProtection {

        @Test
        @DisplayName("기존 origin=MANUAL - 갱신 스킵 + protectedManualCodes 누적, successCount 영향 없음")
        fun upsert_manualOriginProtected() {
            val manualEmp = Employee(employeeCode = "M0001", name = "수동등록", origin = EmployeeOrigin.MANUAL)
            val sapEmp = Employee(employeeCode = "100123", name = "SAP기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("M0001", "100123")) } returns listOf(manualEmp, sapEmp)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(
                listOf(
                    command(employeeCode = "M0001", employeeName = "갱신요청"),
                    command(employeeCode = "100123", employeeName = "SAP갱신")
                )
            )

            assertThat(result.successCount).isEqualTo(1) // SAP 직원만 갱신
            assertThat(result.protectedManualCodes).containsExactly("M0001")
            // SAP 직원만 saveAll 에 포함
            assertThat(savedSlot.captured.single().employeeCode).isEqualTo("100123")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("EmployeeCode 누락 - failures 기록, identifier null")
        fun upsert_missingEmployeeCode() {
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()

            val result = service.upsert(listOf(command(employeeCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("EmployeeCode 필수")
            verify(exactly = 0) { employeeRepository.saveAll(any<List<Employee>>()) }
        }

        @Test
        @DisplayName("EmployeeName 누락 - failures 기록, identifier 보존")
        fun upsert_missingEmployeeName() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }

            val result = service.upsert(listOf(command(employeeName = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100123")
            assertThat(result.failures.single().reason).contains("EmployeeName 필수")
        }

        @Test
        @DisplayName("StartDate 형식 오류 - failures 기록")
        fun upsert_invalidStartDate() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }

            val result = service.upsert(listOf(command(startDate = "2020/04/01")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("StartDate 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123", "100124")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }

            val result = service.upsert(
                listOf(
                    command(employeeCode = "100123", employeeName = "정상"),
                    command(employeeCode = "100124", employeeName = null)
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100124")
        }
    }

    @Nested
    @DisplayName("Employee 신규 생성 시 EmployeeCreatedEvent 발행 (SF @future 동등)")
    inner class EventPublishing {

        @Test
        @DisplayName("E1 신규 Employee 인입 - EmployeeCreatedEvent 1건 발행 (Employee 스냅샷 전달)")
        fun upsert_newEmployee_publishesEvent() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val eventSlot = stubEventCapture()

            val result = service.upsert(
                listOf(
                    command(
                        employeeCode = "100123",
                        employeeName = "홍길동",
                        workEmail = "hong@otokims.co.kr",
                        birthdate = "19900315"
                    )
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            val event = eventSlot.captured
            assertThat(event.employeeCode).isEqualTo("100123")
            assertThat(event.name).isEqualTo("홍길동")
            assertThat(event.workEmail).isEqualTo("hong@otokims.co.kr")
            assertThat(event.birthDate).isEqualTo("19900315")
            assertThat(event.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("E2 기존 Employee 갱신 - 이벤트 발행 안 함")
        fun upsert_existingEmployee_doesNotPublish() {
            val existing = Employee(employeeCode = "100123", name = "기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "갱신")))

            verify(exactly = 0) { eventPublisher.publishEvent(any<EmployeeCreatedEvent>()) }
        }

        @Test
        @DisplayName("E3 신규 + 기존 혼합 - 신규 행에만 이벤트 발행")
        fun upsert_partialNew_publishesOnlyForNew() {
            val existing = Employee(employeeCode = "100100", name = "기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100100", "100200")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val eventSlot = stubEventCapture()

            service.upsert(
                listOf(
                    command(employeeCode = "100100", employeeName = "기존갱신"),
                    command(employeeCode = "100200", employeeName = "신규", workEmail = "new@otoki.com")
                )
            )

            assertThat(eventSlot.captured.employeeCode).isEqualTo("100200")
        }

        @Test
        @DisplayName("E4 LockingFlag Y - 이벤트의 appLoginActive=false 로 전달")
        fun upsert_lockingFlagY_eventCarriesAppLoginActiveFalse() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val eventSlot = stubEventCapture()

            service.upsert(
                listOf(
                    command(
                        employeeCode = "100123",
                        employeeName = "홍길동",
                        workEmail = "hong@otoki.com",
                        lockingFlag = "Y"
                    )
                )
            )

            assertThat(eventSlot.captured.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("E5 origin=MANUAL 기존 Employee - 이벤트 발행 안 함")
        fun upsert_manualOriginExisting_doesNotPublish() {
            val existing = Employee(employeeCode = "100123", name = "기존").apply { origin = EmployeeOrigin.MANUAL }
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "변경시도")))

            verify(exactly = 0) { eventPublisher.publishEvent(any<EmployeeCreatedEvent>()) }
        }
    }
}
