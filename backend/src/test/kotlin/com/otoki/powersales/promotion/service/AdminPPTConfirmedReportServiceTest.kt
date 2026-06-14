package com.otoki.powersales.promotion.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AdminPPTConfirmedReportService 테스트 (Spec #846)")
class AdminPPTConfirmedReportServiceTest {

    private val repository: PPTMasterRepository = mockk()
    private val service = AdminPPTConfirmedReportService(repository)

    private fun employee(): Employee =
        Employee(employeeCode = "20230016", name = "홍길동", orgName = "영업1팀")

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "B0123")
        acc.name = "○○마트 강남점"
        acc.branchName = "서울지점"
        return acc
    }

    private fun master(): ProfessionalPromotionTeamMaster =
        ProfessionalPromotionTeamMaster(
            id = 1L,
            teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
            startDate = LocalDate.of(2026, 5, 1),
            isConfirmed = true,
            employee = employee(),
            account = account(),
        )

    @Nested
    @DisplayName("조회")
    inner class GetReport {

        @Test
        @DisplayName("확정 인원을 6컬럼으로 매핑한다")
        fun mapsRows() {
            every { repository.findConfirmedReport() } returns listOf(master())

            val res = service.getReport()

            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.branchName).isEqualTo("서울지점")
            assertThat(item.fullName).isEqualTo("홍길동")
            assertThat(item.employeeNumber).isEqualTo("20230016")
            assertThat(item.accountName).isEqualTo("○○마트 강남점")
            assertThat(item.accountCode).isEqualTo("B0123")
            assertThat(item.professionalPromotionTeam).isEqualTo("라면세일조")
        }

        @Test
        @DisplayName("결과 0건이면 빈 items")
        fun emptyResult() {
            every { repository.findConfirmedReport() } returns emptyList()

            val res = service.getReport()

            assertThat(res.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("6컬럼 xlsx + 고정 파일명")
        fun exportsXlsx() {
            every { repository.findConfirmedReport() } returns listOf(master())

            val result = service.exportReport()

            assertThat(result.filename).isEqualTo("전문행사조확정인원.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
