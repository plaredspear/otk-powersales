package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.schedule.service.AdminFemaleEmployeeSafetyCheckReportService
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
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
        )
        s.employee = employee()
        s.account = account()
        // 부근무유형은 레거시 formula 대로 출근로그 파생 (저장 컬럼 secondWorkType 아님)
        s.attendanceLog = AttendanceLog(
            attendanceDate = LocalDateTime.of(2026, 5, 29, 9, 0),
            secondWorkType = SecondWorkType.ROOM_TEMP,
        )
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
            assertThat(item.accountType).isEqualTo("대형마트(3대)")
            // SF AccCode__c = ExternalKey (SAP 거래처코드)
            assertThat(item.accountSapCode).isEqualTo("10012345")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.equipment1).isEqualTo("Y")
            assertThat(item.equipment2).isEqualTo("N")
            assertThat(item.precaution).isEqualTo("냉장고 온도 점검")
            assertThat(item.precautionChk).isEqualTo(1.0)
            assertThat(item.hrCode).isEqualTo("3234")
            // 출근로그 파생 (SF DKRetail__SecondWorkType__c formula 정합)
            assertThat(item.secondWorkType).isEqualTo("상온")
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
    @DisplayName("지점 스코프 (컨트롤러 산출 결과 소비)")
    inner class Scope {

        @Test
        @DisplayName("All(전건) → 빈 리스트 전달")
        fun allBranchesNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(allScope, LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("Filtered(단일) → 그 지점으로 좁힘")
        fun allBranchesWithSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("B999"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("B999")
        }

        @Test
        @DisplayName("Filtered(다중) → 그대로 전달")
        fun branchScopedNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A001", "A002"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("Filtered(선택 지점) → 그 지점으로 좁힘")
        fun branchScopedWithOwnSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findSafetyCheckReport(any(), capture(codesSlot)) } returns emptyList()

            service.getReport(branchScope("A002"), LocalDate.of(2026, 5, 29))

            assertThat(codesSlot.captured).containsExactly("A002")
        }

        @Test
        @DisplayName("차단(NoAccess) → 빈 결과, repository 미호출")
        fun branchScopedIdorBlocked() {
            // 권한 밖 지점 선택 판정은 컨트롤러(BranchScopeGateway)가 수행하고, 서비스는 그 결과만 소비한다.
            val res = service.getReport(EffectiveBranchResult.NoAccess, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
            io.mockk.verify(exactly = 0) { repository.findSafetyCheckReport(any(), any()) }
        }

        @Test
        @DisplayName("권한 지점 없음 → NoAccess 빈 결과")
        fun noAccess() {
            val res = service.getReport(EffectiveBranchResult.NoAccess, LocalDate.of(2026, 5, 29))

            assertThat(res.items).isEmpty()
            io.mockk.verify(exactly = 0) { repository.findSafetyCheckReport(any(), any()) }
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
