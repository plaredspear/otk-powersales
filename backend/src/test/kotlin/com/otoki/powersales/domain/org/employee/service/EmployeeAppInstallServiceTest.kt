package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffItem
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@DisplayName("EmployeeAppInstallService 테스트")
class EmployeeAppInstallServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val exporter: AppUninstalledFemaleStaffExcelExporter = mockk()
    private val service = EmployeeAppInstallService(employeeRepository, exporter)

    @Nested
    @DisplayName("getUninstalledFemaleStaffSummary - 미설치 추정 집계")
    inner class SummaryTests {

        @Test
        @DisplayName("정상 집계 - 미설치 인원은 명단 건수, 모수는 대상 count 를 그대로 노출한다")
        fun summary_success() {
            every { employeeRepository.findAppUninstalledFemaleStaff() } returns
                listOf(employee("10000001", "김여사"), employee("10000002", "이여사"))
            every { employeeRepository.countAppLoginTargetFemaleStaff() } returns 10L

            val result = service.getUninstalledFemaleStaffSummary()

            assertThat(result.uninstalledCount).isEqualTo(2)
            assertThat(result.targetCount).isEqualTo(10L)
        }

        @Test
        @DisplayName("미설치 0명 - 전원 앱을 사용 중이면 0 을 반환한다 (모수는 그대로)")
        fun summary_none() {
            every { employeeRepository.findAppUninstalledFemaleStaff() } returns emptyList()
            every { employeeRepository.countAppLoginTargetFemaleStaff() } returns 10L

            val result = service.getUninstalledFemaleStaffSummary()

            assertThat(result.uninstalledCount).isZero()
            assertThat(result.targetCount).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("exportUninstalledFemaleStaff - 명단 엑셀")
    inner class ExportTests {

        @Test
        @DisplayName("정상 export - 사번/이름/지점명 매핑 + 추출일 파일명으로 exporter 에 위임한다")
        fun export_success() {
            every { employeeRepository.findAppUninstalledFemaleStaff() } returns
                listOf(employee("10000001", "김여사", orgName = "부산1지점"))
            val itemsSlot = slot<List<AppUninstalledFemaleStaffItem>>()
            val filenameSlot = slot<String>()
            every { exporter.export(capture(itemsSlot), capture(filenameSlot)) } returns
                ExcelResult(ByteArray(0), "dummy.xlsx")

            service.exportUninstalledFemaleStaff()

            assertThat(itemsSlot.captured).containsExactly(
                AppUninstalledFemaleStaffItem("10000001", "김여사", "부산1지점"),
            )
            val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            assertThat(filenameSlot.captured).isEqualTo("앱미설치여사원_$today.xlsx")
        }

        @Test
        @DisplayName("지점명 없는 사원 - 조직명이 null 이어도 매핑에 실패하지 않는다")
        fun export_nullBranchName() {
            every { employeeRepository.findAppUninstalledFemaleStaff() } returns
                listOf(employee("10000001", "김여사", orgName = null))
            val itemsSlot = slot<List<AppUninstalledFemaleStaffItem>>()
            every { exporter.export(capture(itemsSlot), any()) } returns ExcelResult(ByteArray(0), "dummy.xlsx")

            service.exportUninstalledFemaleStaff()

            assertThat(itemsSlot.captured.single().branchName).isNull()
        }
    }

    private fun employee(
        employeeCode: String,
        name: String,
        orgName: String? = "부산1지점",
    ): Employee = Employee(
        employeeCode = employeeCode,
        name = name,
        orgName = orgName,
        status = "재직",
        role = "여사원",
        appLoginActive = true,
    )
}
