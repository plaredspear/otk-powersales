package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.activity.schedule.service.AdminFemaleEmployeeWorkHistoryService
import com.otoki.powersales.domain.activity.schedule.service.InvalidParameterException
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AdminFemaleEmployeeWorkHistoryService 테스트")
class AdminFemaleEmployeeWorkHistoryServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = AdminFemaleEmployeeWorkHistoryService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(
        code: String = "20230016",
        name: String = "홍길동",
        birthDate: String? = "1985-01-01",
        role: String? = com.otoki.powersales.platform.auth.entity.AppAuthority.WOMAN,
    ): Employee = Employee(employeeCode = code, name = name, birthDate = birthDate, role = role)

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "10012345")
        acc.name = "○○마트 강남점"
        acc.branchCode = "B0123"
        acc.branchName = "강남지점"
        acc.accountType = "대형마트(3대)"
        return acc
    }

    private fun schedule(
        emp: Employee,
        acc: Account? = account(),
        workingDate: LocalDate = LocalDate.of(2026, 5, 12),
        withAttendanceLog: Boolean = true,
    ): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            name = "2026-05-12 진열",
            workingDate = workingDate,
            workingType = WorkingType.WORK,
            workingCategory1 = WorkingCategory1.DISPLAY,
        )
        s.employee = emp
        s.account = acc
        // 부근무유형은 레거시 formula 대로 출근로그 파생 (저장 컬럼 secondWorkType 아님)
        if (withAttendanceLog) {
            s.attendanceLog = AttendanceLog(
                attendanceDate = LocalDateTime.of(2026, 5, 12, 9, 0),
                secondWorkType = SecondWorkType.ROOM_TEMP,
            )
        }
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetWorkHistory {

        @Test
        @DisplayName("사번의 월간 근무내역을 15컬럼으로 매핑한다")
        fun mapsRows() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee()))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            assertThat(res.employeeCode).isEqualTo("20230016")
            assertThat(res.year).isEqualTo(2026)
            assertThat(res.month).isEqualTo(5)
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.scheduleName).isEqualTo("2026-05-12 진열")
            assertThat(item.name).isEqualTo("홍길동")
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.workingDate).isEqualTo("2026-05-12")
            assertThat(item.accountName).isEqualTo("○○마트 강남점")
            assertThat(item.accountSapCode).isEqualTo("10012345")
            assertThat(item.accountBranchName).isEqualTo("강남지점")
            assertThat(item.workingType).isEqualTo("근무")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.secondWorkType).isEqualTo("상온")
            assertThat(item.isWorkReport).isEqualTo("근무등록")
            assertThat(item.commuteDate).isEqualTo("2026-05-12T09:00")
        }

        @Test
        @DisplayName("출근로그가 없으면 부근무유형 null · 근무보고여부 빈값")
        fun noAttendanceLog() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee(), withAttendanceLog = false))

            val item = service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList()).items[0]

            assertThat(item.secondWorkType).isNull()
            assertThat(item.isWorkReport).isEmpty()
            assertThat(item.commuteDate).isNull()
        }

        @Test
        @DisplayName("repository 에 사번·월 1일~말일·trim 된 사번을 전달한다")
        fun passesParams() {
            val codeSlot = slot<String>()
            val fromSlot = slot<LocalDate>()
            val toSlot = slot<LocalDate>()
            every {
                repository.findWorkHistory(capture(codeSlot), capture(fromSlot), capture(toSlot), any())
            } returns emptyList()

            service.getWorkHistory(allScope, " 20230016 ", 2026, 2, emptyList())

            assertThat(codeSlot.captured).isEqualTo("20230016")
            assertThat(fromSlot.captured).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(toSlot.captured).isEqualTo(LocalDate.of(2026, 2, 28))
        }

        @Test
        @DisplayName("일정이 없으면 빈 items (예외 없음)")
        fun emptyResult() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns emptyList()

            val res = service.getWorkHistory(allScope, "99999999", 2026, 5, emptyList())

            assertThat(res.items).isEmpty()
        }

        @Test
        @DisplayName("나이는 SF Age__c formula 정합 — 기준 TODAY, 'N살' 문자열 (#839 와 동일 계산기)")
        fun calculatesAge() {
            val emp = employee(birthDate = "1985-01-01")
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns listOf(schedule(emp))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            // 기준일이 TODAY 이므로 동일 계산기의 오늘 값과 대조한다 (고정 기대값은 시간이 지나면 깨짐).
            assertThat(res.items[0].age).isEqualTo(emp.calculateAge(java.time.LocalDate.now(), womanOnly = true))
            assertThat(res.items[0].age).endsWith("살")
        }

        @Test
        @DisplayName("birthDate 가 null 이면 age 는 null")
        fun nullBirthDate() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee(birthDate = null)))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            assertThat(res.items[0].age).isNull()
        }

        @Test
        @DisplayName("여사원이 아닌 행(조장 등)의 나이는 SF 와 동일하게 null")
        fun nonWomanAgeNull() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee(role = com.otoki.powersales.platform.auth.entity.AppAuthority.LEADER)))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            assertThat(res.items[0].age).isNull()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (costCenterCode) — 배치 점검과 동일")
    inner class Scope {

        @Test
        @DisplayName("전사 권한자 + 선택 없음 → 빈 리스트(전건) 전달")
        fun allBranchesNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("전사 권한자 + 지점 선택 → 선택 지점 그대로 전달")
        fun allBranchesWithSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(allScope, "20230016", 2026, 5, listOf("B999"))

            assertThat(codesSlot.captured).containsExactly("B999")
        }

        @Test
        @DisplayName("지점 사용자 + 선택 없음 → 본인 지점 전체 전달")
        fun branchScopedNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(branchScope("A001", "A002"), "20230016", 2026, 5, emptyList())

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("지점 사용자 + 본인 지점 선택 → 교집합(선택 지점) 전달")
        fun branchScopedWithOwnSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(branchScope("A001", "A002"), "20230016", 2026, 5, listOf("A002"))

            assertThat(codesSlot.captured).containsExactly("A002")
        }

        @Test
        @DisplayName("지점 사용자 + 권한 밖 지점 선택 → 교집합 없음 → 403")
        fun branchScopedIdorBlocked() {
            assertThatThrownBy {
                service.getWorkHistory(branchScope("A001"), "20230016", 2026, 5, listOf("Z999"))
            }.isInstanceOf(AdminForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class Validation {

        @Test
        @DisplayName("employeeCode 공백이면 InvalidParameterException")
        fun blankEmployeeCode() {
            assertThatThrownBy { service.getWorkHistory(allScope, "  ", 2026, 5, emptyList()) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("year 범위 외면 InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy { service.getWorkHistory(allScope, "20230016", 1999, 5, emptyList()) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("month 범위 외면 InvalidParameterException")
        fun invalidMonth() {
            assertThatThrownBy { service.getWorkHistory(allScope, "20230016", 2026, 13, emptyList()) }
                .isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("15컬럼 xlsx 생성 + 파일명 사번_yyyyMM")
        fun exportsXlsx() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee()))

            val result = service.exportWorkHistory(allScope, "20230016", 2026, 5, emptyList())

            assertThat(result.filename).isEqualTo("여사원근무내역_20230016_202605.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
