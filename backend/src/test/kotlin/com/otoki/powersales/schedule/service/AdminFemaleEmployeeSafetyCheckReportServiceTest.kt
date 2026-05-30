package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.util.TimeZones
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AdminFemaleEmployeeSafetyCheckReportService 테스트")
class AdminFemaleEmployeeSafetyCheckReportServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = AdminFemaleEmployeeSafetyCheckReportService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀")

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "B0123")
        acc.name = "○○마트 강남점"
        acc.branchCode = "B0123"
        acc.accountType = AccountType.DISCOUNT_STORE
        return acc
    }

    private fun schedule(): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            workingDate = LocalDate.of(2026, 5, 29),
            workingCategory1 = WorkingCategory1.DISPLAY,
            traversalFlag = "O",
            yesChkCnt = 9.0,
            equipment1 = "Y",
            equipment2 = "N",
            precaution = "냉장고 온도 점검",
            precautionChk = 1.0,
            startTime = LocalDateTime.of(2026, 5, 29, 9, 5),
            hrCode = "3234",
            secondWorkType = "보조",
        )
        s.employee = employee()
        s.account = account()
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetReport {

        @Test
        @DisplayName("점검 완료 일정을 24컬럼으로 매핑한다")
        fun mapsRows() {
            every { repository.findSafetyCheckReport(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.date).isEqualTo("2026-05-29")
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.ladyName).isEqualTo("홍길동")
            assertThat(item.employeeOrgName).isEqualTo("영업1팀")
            assertThat(item.accountType).isEqualTo("할인점")
            assertThat(item.accountBranchCode).isEqualTo("B0123")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.equipment1).isEqualTo("Y")
            assertThat(item.equipment2).isEqualTo("N")
            assertThat(item.precaution).isEqualTo("냉장고 온도 점검")
            assertThat(item.precautionChk).isEqualTo(1.0)
            assertThat(item.hrCode).isEqualTo("3234")
            assertThat(item.secondWorkType).isEqualTo("보조")
        }

        @Test
        @DisplayName("checkTime 은 startTime 을 보정 없이 그대로 반환한다")
        fun checkTimeNoOffset() {
            every { repository.findSafetyCheckReport(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            // startTime 09:05 그대로 (레거시 -9h 보정 미적용)
            assertThat(res.items[0].checkTime).isEqualTo("2026-05-29T09:05")
        }

        @Test
        @DisplayName("date 미지정 시 어제(KST) 로 조회한다")
        fun defaultsToYesterday() {
            val dateSlot = slot<LocalDate>()
            every { repository.findSafetyCheckReport(capture(dateSlot), any()) } returns emptyList()

            val res = service.getReport(allScope, null)

            val expectedYesterday = LocalDate.now(TimeZones.SEOUL_ZONE).minusDays(1)
            assertThat(dateSlot.captured).isEqualTo(expectedYesterday)
            assertThat(res.date).isEqualTo(expectedYesterday.toString())
        }

        @Test
        @DisplayName("결과 0건이면 빈 items")
        fun emptyResult() {
            every { repository.findSafetyCheckReport(any(), any()) } returns emptyList()

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (costCenterCode)")
    inner class Scope {

        @Test
        @DisplayName("isAllBranches=true 면 빈 리스트(전사) 전달")
        fun allBranches() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("isAllBranches=false 면 scope.branchCodes 전달")
        fun branchScoped() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A001", "A002"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("24컬럼 xlsx + 파일명 yyyy-MM-dd")
        fun exportsXlsx() {
            every { repository.findSafetyCheckReport(any(), any()) } returns listOf(schedule())

            val result = service.exportReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(result.filename).isEqualTo("판매여사원안전점검_2026-05-29.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
