package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Gender
import com.otoki.powersales.common.entity.SystemCodeMaster
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequestItem
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("SapEmployeeMasterService 테스트")
class SapEmployeeMasterServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapEmployeeMasterService

    private fun item(
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
    ): EmployeeMasterRequestItem = EmployeeMasterRequestItem(
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

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 직원 1건 - INSERT, success_count=1, EmployeeInfo cascade 자동 생성")
        fun upsert_insertNew() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item()))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.employeeInfo).isNotNull
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 직원 갱신 - 동일 PK 유지, name/homePhone 업데이트")
        fun upsert_updateExisting() {
            val existing = Employee(employeeCode = "100123", name = "기존이름")
            existing.homePhone = "old-phone"
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(listOf(existing))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(item(employeeName = "새이름", homePhone = "new-phone")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
            assertThat(saved.homePhone).isEqualTo("new-phone")
        }

        @Test
        @DisplayName("Status 변환 성공 - SystemCodeMaster 매칭 코드명 적용")
        fun upsert_statusResolved() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")))
                .thenReturn(listOf(statusCode("10", "재직")))

            service.upsert(listOf(item(status = "10")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("재직")
        }

        @Test
        @DisplayName("Status 매칭 실패 - 원본 코드 그대로 저장")
        fun upsert_statusUnmapped() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")))
                .thenReturn(listOf(statusCode("10", "재직")))

            service.upsert(listOf(item(status = "99")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("99")
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
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(listOf(other))

            service.upsert(listOf(item(status = "10")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("10")
        }

        @Test
        @DisplayName("Gender 변환 - '1' → MALE, '2' → FEMALE, 그 외 → null")
        fun upsert_sexConversion() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    item(employeeCode = "M01", gender = "1"),
                    item(employeeCode = "F01", gender = "2"),
                    item(employeeCode = "X01", gender = "9")
                )
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val byCode = captor.firstValue.associateBy { it.employeeCode }
            assertThat(byCode["M01"]?.gender).isEqualTo(Gender.MALE)
            assertThat(byCode["F01"]?.gender).isEqualTo(Gender.FEMALE)
            assertThat(byCode["X01"]?.gender).isNull()
        }

        @Test
        @DisplayName("StartDate / EndDate / Birthdate - YYYYMMDD 변환, 00000000 은 null")
        fun upsert_dateConversion() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    item(
                        startDate = "20200401",
                        endDate = "00000000",
                        birthdate = "19850315"
                    )
                )
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.startDate).isEqualTo(LocalDate.of(2020, 4, 1))
            assertThat(saved.endDate).isNull()
            assertThat(saved.birthDate).isEqualTo("19850315")
        }

        @Test
        @DisplayName("LockingFlag - Y → false, N → true, null → true")
        fun upsert_lockingFlag() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    item(employeeCode = "L01", lockingFlag = "Y"),
                    item(employeeCode = "L02", lockingFlag = "N"),
                    item(employeeCode = "L03", lockingFlag = null)
                )
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val byCode = captor.firstValue.associateBy { it.employeeCode }
            assertThat(byCode["L01"]?.appLoginActive).isFalse
            assertThat(byCode["L02"]?.appLoginActive).isTrue
            assertThat(byCode["L03"]?.appLoginActive).isTrue
        }

        @Test
        @DisplayName("WorkEmail / Email - 신규 컬럼 저장")
        fun upsert_emailColumns() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(item(workEmail = "work@otoki.com", email = "personal@otoki.com")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().workEmail).isEqualTo("work@otoki.com")
            assertThat(captor.firstValue.single().email).isEqualTo("personal@otoki.com")
        }

        @Test
        @DisplayName("OrgCode → costCenterCode 매핑")
        fun upsert_orgCodeMapping() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(item(orgCode = "11110")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().costCenterCode).isEqualTo("11110")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("EmployeeCode 누락 - failures 기록, emp_code null")
        fun upsert_missingEmployeeCode() {
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(employeeCode = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isNull()
            assertThat(detail.failures.single().reason).contains("EmployeeCode 필수")
            verify(employeeRepository, never()).saveAll(any<List<Employee>>())
        }

        @Test
        @DisplayName("EmployeeName 누락 - failures 기록, emp_code 보존")
        fun upsert_missingEmployeeName() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(employeeName = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isEqualTo("100123")
            assertThat(detail.failures.single().reason).contains("EmployeeName 필수")
        }

        @Test
        @DisplayName("StartDate 형식 오류 - failures 기록")
        fun upsert_invalidStartDate() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(startDate = "2020/04/01")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("StartDate 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123", "100124"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(
                    item(employeeCode = "100123", employeeName = "정상"),
                    item(employeeCode = "100124", employeeName = null)
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isEqualTo("100124")
            assertThat(detail.failures.single().reason).contains("EmployeeName 필수")
        }

        @Test
        @DisplayName("모든 행 실패 - successCount=0, saveAll 미호출")
        fun upsert_allFailures() {
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(
                    item(employeeCode = null, employeeName = "A"),
                    item(employeeCode = null, employeeName = "B")
                )
            )

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(2)
            verify(employeeRepository, never()).saveAll(any<List<Employee>>())
            verify(auditService).record(any<SapInboundAudit>())
        }
    }
}
