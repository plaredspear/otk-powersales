package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.EffectiveBranchResult
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

    // 지점 판정/확장은 컨트롤러(BranchScopeGateway) 책임으로 옮겨졌다 — 서비스는 산출된 결과만 소비한다.
    private val allScope: EffectiveBranchResult = EffectiveBranchResult.All
    private fun branchScope(vararg codes: String): EffectiveBranchResult =
        EffectiveBranchResult.Filtered(codes.toList())

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀")

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "10012345")
        acc.name = "○○마트 강남점"
        acc.branchCode = "B0123"
        acc.accountType = "대형마트(3대)"
        return acc
    }

    private fun owner(): User =
        User(username = "admin01", employeeCode = "A0001", name = "관리자", password = "x")

    private fun schedule(withScheduleName: Boolean = true): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            name = if (withScheduleName) "TS00012345" else null,
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
        s.ownerUser = owner()
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetReport {

        @Test
        @DisplayName("점검 완료 일정을 24컬럼(스케줄번호 포함)으로 매핑한다")
        fun mapsRows() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.date).isEqualTo("2026-05-29")
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.ladyName).isEqualTo("홍길동")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.equipment1).isEqualTo("Y")
            // 마지막 컬럼 = 여사원일정 스케줄번호 (TeamMemberSchedule.name)
            assertThat(item.scheduleName).isEqualTo("TS00012345")
        }

        @Test
        @DisplayName("스케줄번호 부재 시 scheduleName = null")
        fun nullScheduleName() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule(withScheduleName = false))

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.items[0].scheduleName).isNull()
        }

        @Test
        @DisplayName("checkTime 은 startTime 을 보정 없이 그대로 반환한다")
        fun checkTimeNoOffset() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns listOf(schedule())

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.items[0].checkTime).isEqualTo("2026-05-29T09:05")
        }

        @Test
        @DisplayName("date 미지정 시 어제(KST) 로 조회한다")
        fun defaultsToYesterday() {
            val dateSlot = slot<LocalDate>()
            every { repository.findSafetyCheckReportRpa(capture(dateSlot), any()) } returns emptyList()

            val res = service.getReport(allScope, null)

            val expectedYesterday = LocalDate.now(TimeZones.SEOUL_ZONE).minusDays(1)
            assertThat(dateSlot.captured).isEqualTo(expectedYesterday)
            assertThat(res.date).isEqualTo(expectedYesterday.toString())
        }

        @Test
        @DisplayName("결과 0건이면 빈 items")
        fun emptyResult() {
            every { repository.findSafetyCheckReportRpa(any(), any()) } returns emptyList()

            val res = service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (컨트롤러 산출 결과 소비)")
    inner class Scope {

        @Test
        @DisplayName("All(전건) → 빈 리스트 전달")
        fun allBranchesNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("Filtered(단일) → 그 지점으로 좁힘")
        fun allBranchesWithSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("B999"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("B999")
        }

        @Test
        @DisplayName("Filtered(다중) → 그대로 전달")
        fun branchScopedNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A001", "A002"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("Filtered(선택 지점) → 그 지점으로 좁힘")
        fun branchScopedWithOwnSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReportRpa(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A002"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("A002")
        }

        @Test
        @DisplayName("차단(NoAccess) → 빈 결과, repository 미호출")
        fun branchScopedIdorBlocked() {
            // 권한 밖 지점 선택 판정은 컨트롤러(BranchScopeGateway)가 수행하고, 서비스는 그 결과만 소비한다.
            val res = service.getReport(EffectiveBranchResult.NoAccess, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
            verify(exactly = 0) { repository.findSafetyCheckReportRpa(any(), any()) }
        }

        @Test
        @DisplayName("권한 지점 없음 → NoAccess 빈 결과")
        fun noAccess() {
            val res = service.getReport(EffectiveBranchResult.NoAccess, LocalDate.of(2026, 5, 29))

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

            val result = service.exportReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(result.filename).isEqualTo("판매여사원안전점검RPA_2026-05-29.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
