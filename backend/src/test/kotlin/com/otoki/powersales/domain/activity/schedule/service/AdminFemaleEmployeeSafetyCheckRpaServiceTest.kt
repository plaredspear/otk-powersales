package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.AdminFemaleEmployeeSafetyCheckRpaService
import com.otoki.powersales.user.entity.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AdminFemaleEmployeeSafetyCheckRpaService 테스트 (Spec #842)")
class AdminFemaleEmployeeSafetyCheckRpaServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = AdminFemaleEmployeeSafetyCheckRpaService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀")

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "B0123")
        acc.name = "○○마트 강남점"
        acc.branchCode = "B0123"
        acc.accountType = "대형마트(3대)"
        return acc
    }

    private fun owner(): User =
        User(username = "admin01", employeeCode = "A0001", name = "관리자", password = "x")

    private fun schedule(withOwner: Boolean = true): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            workingDate = LocalDate.of(2026, 5, 29),
            workingCategory1 = WorkingCategory1.DISPLAY,
            traversalFlag = "O",
            yesChkCnt = 9.0,
            equipment1 = "Y",
            precaution = "냉장고 온도 점검",
            precautionChk = 1.0,
            startTime = LocalDateTime.of(2026, 5, 29, 9, 5),
            hrCode = "3234",
        )
        s.employee = employee()
        s.account = account()
        if (withOwner) s.ownerUser = owner()
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetReport {

        @Test
        @DisplayName("점검 완료 일정을 24컬럼(CUST_NAME 포함)으로 매핑한다")
        fun mapsRows() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(res.date).isEqualTo("2026-05-29")
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.ladyName).isEqualTo("홍길동")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.equipment1).isEqualTo("Y")
            // CUST_NAME = owner User 이름
            assertThat(item.custName).isEqualTo("관리자")
        }

        @Test
        @DisplayName("owner 부재 시 custName = null")
        fun nullOwner() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule(withOwner = false))

            val res = service.getReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(res.items[0].custName).isNull()
        }

        @Test
        @DisplayName("checkTime 은 startTime 을 보정 없이 그대로 반환한다")
        fun checkTimeNoOffset() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(res.items[0].checkTime).isEqualTo("2026-05-29T09:05")
        }

        @Test
        @DisplayName("date 미지정 시 어제(KST) 로 조회한다")
        fun defaultsToYesterday() {
            val dateSlot = slot<LocalDate>()
            every { repository.findSafetyCheckReportRpa(capture(dateSlot), any()) } returns emptyList()

            val res = service.getReport(allScope, null, null)

            val expectedYesterday = LocalDate.now(TimeZones.SEOUL_ZONE).minusDays(1)
            assertThat(dateSlot.captured).isEqualTo(expectedYesterday)
            assertThat(res.date).isEqualTo(expectedYesterday.toString())
        }

        @Test
        @DisplayName("결과 0건이면 빈 items")
        fun emptyResult() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns emptyList()

            val res = service.getReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (costCenterCode)")
    inner class Scope {

        @Test
        @DisplayName("전사 권한자 + 선택 없음 → 빈 리스트(전건) 전달")
        fun allBranchesNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("전사 권한자 + 지점 선택 → 그 지점으로 좁힘")
        fun allBranchesWithSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(allScope, "B999", LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("B999")
        }

        @Test
        @DisplayName("지점 사용자 + 선택 없음 → 본인 지점 전체 전달")
        fun branchScopedNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A001", "A002"), null, LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("지점 사용자 + 본인 지점 선택 → 그 지점으로 좁힘")
        fun branchScopedWithOwnSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A001", "A002"), "A002", LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("A002")
        }

        @Test
        @DisplayName("지점 사용자 + 권한 밖 지점 선택 → IDOR 차단(빈 결과, repository 미호출)")
        fun branchScopedIdorBlocked() {
            val res = service.getReport(branchScope("A001"), "Z999", LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
            verify(exactly = 0) { repository.findSafetyCheckReportRpa(any(), any()) }
        }

        @Test
        @DisplayName("권한 지점 없음(빈 scope) → NoAccess 빈 결과")
        fun noAccess() {
            val res = service.getReport(branchScope(), null, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
            verify(exactly = 0) { repository.findSafetyCheckReportRpa(any(), any()) }
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("24컬럼 xlsx + 파일명 (RPA)")
        fun exportsXlsx() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule())

            val result = service.exportReport(allScope, null, LocalDate.of(2026, 5, 29))

            assertThat(result.filename).isEqualTo("판매여사원안전점검RPA_2026-05-29.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
