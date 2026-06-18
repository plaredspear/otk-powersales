package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.enums.Gender
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.EmployeeUpsertService
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.event.EmployeesCreatedEvent
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

    private fun stubEventCapture(): CapturingSlot<EmployeesCreatedEvent> {
        val slot = slot<EmployeesCreatedEvent>()
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
            // SF EmployeeTrigger.upsertPhoneNumber() 동등 — HomePhone → Phone 무조건 복사.
            assertThat(saved.phone).isEqualTo("new-phone")
        }

        @Test
        @DisplayName("phone 미러링 - HomePhone 값이 Phone 으로 무조건 복사 (SF upsertPhoneNumber 동등)")
        fun upsert_mirrorsHomePhoneToPhone() {
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(homePhone = "010-1234-5678")))

            assertThat(savedSlot.captured.single().phone).isEqualTo("010-1234-5678")
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
    @DisplayName("LockingFlag 예외 (SF lockingFlagException 동등 — 판촉/레이디/OSC 여사원·조장 보호)")
    inner class LockingFlagException {

        private fun protectedExisting(
            employeeCode: String = "100123",
            jobCode: String? = "판촉직",
            role: String? = "여사원",
            status: String? = "재직"
        ): Employee {
            val emp = Employee(employeeCode = employeeCode, name = "현장사원")
            emp.jobCode = jobCode
            emp.role = role
            emp.status = status
            return emp
        }

        @Test
        @DisplayName("판촉직 + 여사원 + 재직 + LockingFlag=Y → 보호 복원 (appLoginActive=true, lockingFlag=false)")
        fun lockingException_protectsFieldWorker() {
            val existing = protectedExisting()
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            // status="재직" 은 H10010 매칭 비어 있어 raw 보존되므로 entity.status="재직" 유지.
            service.upsert(listOf(command(employeeCode = "100123", status = "재직", lockingFlag = "Y")))

            val saved = savedSlot.captured.single()
            assertThat(saved.appLoginActive).isTrue
            assertThat(saved.lockingFlag).isFalse
        }

        @Test
        @DisplayName("OSC직 + 조장 + LockingFlag=Y → 보호 복원")
        fun lockingException_oscLeader() {
            val existing = protectedExisting(jobCode = "OSC직", role = "조장")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeCode = "100123", status = "재직", lockingFlag = "Y")))

            assertThat(savedSlot.captured.single().appLoginActive).isTrue
        }

        @Test
        @DisplayName("판촉직 + 여사원 + 퇴직 + LockingFlag=Y → 보호 미적용 (퇴직 제외 조건)")
        fun lockingException_retiredNotProtected() {
            val existing = protectedExisting(status = "퇴직")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeCode = "100123", status = "퇴직", lockingFlag = "Y")))

            val saved = savedSlot.captured.single()
            assertThat(saved.appLoginActive).isFalse
            assertThat(saved.lockingFlag).isTrue
        }

        @Test
        @DisplayName("사무직(보호 대상 jobCode 아님) + LockingFlag=Y → 보호 미적용")
        fun lockingException_nonFieldJobNotProtected() {
            val existing = protectedExisting(jobCode = "사무직", role = "여사원")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeCode = "100123", status = "재직", lockingFlag = "Y")))

            assertThat(savedSlot.captured.single().appLoginActive).isFalse
        }

        @Test
        @DisplayName("판촉직이나 권한이 지점장 → 보호 미적용 (여사원/조장 아님)")
        fun lockingException_branchManagerNotProtected() {
            val existing = protectedExisting(role = "지점장")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeCode = "100123", status = "재직", lockingFlag = "Y")))

            assertThat(savedSlot.captured.single().appLoginActive).isFalse
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
    @DisplayName("Employee 신규 생성 시 EmployeesCreatedEvent 발행 (SF @future bulk 동등)")
    inner class EventPublishing {

        @Test
        @DisplayName("E1 신규 Employee 인입 - EmployeesCreatedEvent 1건 발행 (스냅샷 1개 포함)")
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
            assertThat(event.employees).hasSize(1)
            val snapshot = event.employees.single()
            assertThat(snapshot.employeeCode).isEqualTo("100123")
            assertThat(snapshot.name).isEqualTo("홍길동")
            assertThat(snapshot.workEmail).isEqualTo("hong@otokims.co.kr")
            assertThat(snapshot.birthDate).isEqualTo("19900315")
            assertThat(snapshot.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("E2 기존 Employee + User 존재 - 이벤트 발행 안 함 (SF userMap.containsKey → updateCode)")
        fun upsert_existingEmployeeWithUser_doesNotPublish() {
            val existing = Employee(employeeCode = "100123", name = "기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            // User 가 이미 존재 → update 경로, 신규 생성 이벤트 발행 안 함.
            every { userRepository.findByEmployeeCodeIn(listOf("100123")) } returns
                listOf(User(username = "u@otoki.com", employeeCode = "100123", password = "x", name = "기존"))

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "갱신")))

            verify(exactly = 0) { eventPublisher.publishEvent(any<EmployeesCreatedEvent>()) }
        }

        @Test
        @DisplayName("E2b 기존 Employee 인데 User 부재 - 이벤트 발행 (SF userMap.containsKey=false → insertCode)")
        fun upsert_existingEmployeeWithoutUser_publishes() {
            val existing = Employee(employeeCode = "100123", name = "기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            // User 가 없음 → insert(provisioning) 경로. 기존 Employee 라도 발행 대상.
            every { userRepository.findByEmployeeCodeIn(listOf("100123")) } returns emptyList()
            val eventSlot = stubEventCapture()

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "갱신", email = "p@otoki.com")))

            assertThat(eventSlot.captured.employees.map { it.employeeCode }).containsExactly("100123")
        }

        @Test
        @DisplayName("E3 신규 + 기존(User 존재) 혼합 - 이벤트 1건에 User 없는 사원만 포함")
        fun upsert_partialNew_publishesOnlyForMissingUser() {
            val existing = Employee(employeeCode = "100100", name = "기존")
            every { employeeRepository.findByEmployeeCodeIn(listOf("100100", "100200")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            // 100100 은 기존 User 존재(update), 100200 은 신규(User 없음 → insert).
            every { userRepository.findByEmployeeCodeIn(listOf("100100", "100200")) } returns
                listOf(User(username = "u100@otoki.com", employeeCode = "100100", password = "x", name = "기존"))
            val eventSlot = stubEventCapture()

            service.upsert(
                listOf(
                    command(employeeCode = "100100", employeeName = "기존갱신"),
                    command(employeeCode = "100200", employeeName = "신규", email = "new@otoki.com")
                )
            )

            assertThat(eventSlot.captured.employees.map { it.employeeCode }).containsExactly("100200")
        }

        @Test
        @DisplayName("E4 LockingFlag Y - 스냅샷의 appLoginActive=false 로 전달")
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

            assertThat(eventSlot.captured.employees.single().appLoginActive).isFalse()
        }

        @Test
        @DisplayName("E5 origin=MANUAL 기존 Employee - 이벤트 발행 안 함")
        fun upsert_manualOriginExisting_doesNotPublish() {
            val existing = Employee(employeeCode = "100123", name = "기존").apply { origin = EmployeeOrigin.MANUAL }
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "변경시도")))

            verify(exactly = 0) { eventPublisher.publishEvent(any<EmployeesCreatedEvent>()) }
        }

        @Test
        @DisplayName("E6 신규 여러 명 - 단일 이벤트에 신규 스냅샷 전부 포함 (bulk)")
        fun upsert_multipleNew_singleEventAllSnapshots() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100301", "100302", "100303")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val eventSlot = stubEventCapture()

            service.upsert(
                listOf(
                    command(employeeCode = "100301", employeeName = "신규1", workEmail = "n1@otoki.com"),
                    command(employeeCode = "100302", employeeName = "신규2", workEmail = "n2@otoki.com"),
                    command(employeeCode = "100303", employeeName = "신규3", workEmail = "n3@otoki.com")
                )
            )

            verify(exactly = 1) { eventPublisher.publishEvent(any<EmployeesCreatedEvent>()) }
            assertThat(eventSlot.captured.employees.map { it.employeeCode })
                .containsExactly("100301", "100302", "100303")
        }

        @Test
        @DisplayName("E7 신규 퇴직자 - User 생성 이벤트 발행 안 함 (SF insert SOQL Status!=퇴직 동등)")
        fun upsert_newRetired_notProvisioned() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100301")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }

            service.upsert(
                listOf(command(employeeCode = "100301", employeeName = "퇴직신규", status = "퇴직", workEmail = "n1@otoki.com"))
            )

            // 퇴직자는 신규 User 생성 대상에서 제외 → 발행 대상이 없어 이벤트 미발행.
            verify(exactly = 0) { eventPublisher.publishEvent(any<EmployeesCreatedEvent>()) }
        }

        @Test
        @DisplayName("E8 신규 excHrCode 코스트센터 - User 생성 이벤트 대상에서 제외 (SF NOT IN excHrCode 동등)")
        fun upsert_newExcludedCostCenter_notProvisioned() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100301", "100302")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val eventSlot = stubEventCapture()

            service.upsert(
                listOf(
                    // 4606 = excHrCode → 제외, 11110 = 일반 → 포함.
                    command(employeeCode = "100301", employeeName = "본사", orgCode = "4606", workEmail = "h@otoki.com"),
                    command(employeeCode = "100302", employeeName = "현장", orgCode = "11110", workEmail = "f@otoki.com")
                )
            )

            assertThat(eventSlot.captured.employees.map { it.employeeCode }).containsExactly("100302")
        }

        @Test
        @DisplayName("E9 기존 User - 사원 퇴직 전환 시 비활성화 (SF cls:264-267 동등)")
        fun upsert_existingUserDeactivatedOnRetire() {
            val existing = Employee(employeeCode = "100123", name = "기존").apply { costCenterCode = "11110" }
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            val user = User(username = "u@otoki.com", employeeCode = "100123", password = "x", name = "기존").apply { isActive = true }
            every { userRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(user)

            service.upsert(listOf(command(employeeCode = "100123", status = "퇴직", orgCode = "11110")))

            assertThat(user.isActive).isFalse()
        }

        @Test
        @DisplayName("E10 기존 User - excHrCode 사원은 User 무변경 (SF update SOQL NOT IN excHrCode 동등)")
        fun upsert_existingUserUntouchedForExcludedCostCenter() {
            val existing = Employee(employeeCode = "100123", name = "본사").apply { costCenterCode = "4606" }
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(existing)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            every { employeeRepository.saveAll(any<List<Employee>>()) } answers { firstArg<List<Employee>>() }
            // costCenterCode 가 처음부터 4606 인 기존 User — excHrCode 라 isActive/costCenter 모두 무변경이어야 한다.
            val user = User(username = "u@otoki.com", employeeCode = "100123", password = "x", name = "본사").apply {
                isActive = true
                costCenterCode = "4606"
            }
            every { userRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(user)

            service.upsert(listOf(command(employeeCode = "100123", status = "퇴직", orgCode = "4606")))

            // excHrCode 사원은 update 루프 진입 전 skip → 퇴직이어도 비활성화하지 않는다.
            assertThat(user.isActive).isTrue()
        }
    }
}
