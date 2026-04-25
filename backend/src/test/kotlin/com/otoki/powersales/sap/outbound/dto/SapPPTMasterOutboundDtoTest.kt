package com.otoki.powersales.sap.outbound.dto

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("SapPPTMasterOutboundDto 매핑 테스트")
class SapPPTMasterOutboundDtoTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private fun createEmployee(
        name: String = "홍길동",
        employeeCode: String = "A12345",
        status: String? = "재직",
        jikwee: String? = "대리"
    ) = Employee(
        id = 1L,
        employeeCode = employeeCode,
        name = name
    ).also {
        it.status = status
        it.jikwee = jikwee
    }

    private fun createAccount(
        name: String? = "OO마트 강남점",
        externalKey: String? = "0001234567",
        accountStatusName: String? = "활동",
        accountType: String? = "일반"
    ) = Account(
        id = 1,
        name = name
    ).also {
        it.externalKey = externalKey
        it.accountStatusName = accountStatusName
        it.accountType = accountType
    }

    private fun createMaster(
        id: Long = 1234L,
        teamType: String = "라면세일조",
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = LocalDate.of(2026, 12, 31),
        isConfirmed: Boolean = true,
        branchCode: String? = "1000",
        branchName: String? = "서울지점",
        employee: Employee? = createEmployee(),
        account: Account? = createAccount()
    ) = ProfessionalPromotionTeamMaster(
        id = id,
        employeeId = employee?.id ?: 0,
        accountId = account?.id ?: 0,
        teamType = teamType,
        startDate = startDate,
        endDate = endDate,
        isConfirmed = isConfirmed,
        branchCode = branchCode,
        branchName = branchName,
        employee = employee,
        account = account
    )

    @Nested
    @DisplayName("from - 17개 필드 매핑")
    inner class FieldMapping {

        @Test
        @DisplayName("정상 매핑 - 모든 필드 채워진 케이스")
        fun mapAllFields() {
            val today = LocalDate.of(2026, 4, 16)
            val master = createMaster()

            val dto = SapPPTMasterOutboundDto.from(master, today)

            assertThat(dto.name).isEqualTo("1234")
            assertThat(dto.professionalPromotionTeam).isEqualTo("라면세일조")
            assertThat(dto.account).isEqualTo("OO마트 강남점")
            assertThat(dto.fullName).isEqualTo("홍길동")
            assertThat(dto.employeeNumber).isEqualTo("A12345")
            assertThat(dto.accountStatus).isEqualTo("활동")
            assertThat(dto.accountType).isEqualTo("일반")
            assertThat(dto.accountCode).isEqualTo("0001234567")
            assertThat(dto.startDate).isEqualTo("2026-01-01")
            assertThat(dto.endDate).isEqualTo("2026-12-31")
            assertThat(dto.validData).isEqualTo("유효")
            assertThat(dto.validConditionData).isEqualTo("재직")
            assertThat(dto.costCenterCode).isEqualTo("1000")
            assertThat(dto.branchName).isEqualTo("서울지점")
            assertThat(dto.title).isEqualTo("대리")
            assertThat(dto.confirmed).isEqualTo("true")
            assertThat(dto.yearMonth).isEqualTo("202604")
        }

        @Test
        @DisplayName("endDate null - EndDate 필드는 'null' 문자열로 매핑된다")
        fun endDate_null_renderedAsNullToken() {
            val today = LocalDate.of(2026, 4, 16)
            val master = createMaster(endDate = null)

            val dto = SapPPTMasterOutboundDto.from(master, today)

            assertThat(dto.endDate).isEqualTo("null")
        }

        @Test
        @DisplayName("연관 엔티티 null - 모든 lookup 필드가 빈 문자열로 매핑된다")
        fun nullRelations_renderedAsEmptyString() {
            val today = LocalDate.of(2026, 4, 16)
            val master = createMaster(employee = null, account = null)

            val dto = SapPPTMasterOutboundDto.from(master, today)

            assertThat(dto.account).isEmpty()
            assertThat(dto.fullName).isEmpty()
            assertThat(dto.employeeNumber).isEmpty()
            assertThat(dto.accountStatus).isEmpty()
            assertThat(dto.accountType).isEmpty()
            assertThat(dto.accountCode).isEmpty()
            assertThat(dto.validConditionData).isEmpty()
            assertThat(dto.title).isEmpty()
        }
    }

    @Nested
    @DisplayName("ValidData 계산")
    inner class ValidDataComputation {

        @Test
        @DisplayName("isConfirmed=true, endDate=null -> 유효")
        fun confirmed_noEndDate_valid() {
            val dto = SapPPTMasterOutboundDto.from(
                createMaster(isConfirmed = true, endDate = null),
                LocalDate.of(2026, 4, 16)
            )
            assertThat(dto.validData).isEqualTo("유효")
        }

        @Test
        @DisplayName("isConfirmed=true, endDate 미래 -> 유효")
        fun confirmed_futureEnd_valid() {
            val dto = SapPPTMasterOutboundDto.from(
                createMaster(isConfirmed = true, endDate = LocalDate.of(2026, 12, 31)),
                LocalDate.of(2026, 4, 16)
            )
            assertThat(dto.validData).isEqualTo("유효")
        }

        @Test
        @DisplayName("isConfirmed=true, endDate 과거 -> 만료")
        fun confirmed_pastEnd_expired() {
            val dto = SapPPTMasterOutboundDto.from(
                createMaster(isConfirmed = true, endDate = LocalDate.of(2026, 1, 1)),
                LocalDate.of(2026, 4, 16)
            )
            assertThat(dto.validData).isEqualTo("만료")
        }

        @Test
        @DisplayName("isConfirmed=false -> 만료 (endDate 무관)")
        fun unconfirmed_expired() {
            val dto = SapPPTMasterOutboundDto.from(
                createMaster(isConfirmed = false, endDate = LocalDate.of(2026, 12, 31)),
                LocalDate.of(2026, 4, 16)
            )
            assertThat(dto.validData).isEqualTo("만료")
        }
    }

    @Nested
    @DisplayName("JSON 직렬화")
    inner class Serialization {

        @Test
        @DisplayName("Apex 호환 키 - 17개 필드를 PascalCase 키로 직렬화한다")
        fun serializesWithApexCompatibleKeys() {
            val today = LocalDate.of(2026, 4, 16)
            val dto = SapPPTMasterOutboundDto.from(createMaster(), today)

            val json = objectMapper.writeValueAsString(dto)

            listOf(
                "Name", "ProfessionalPromotionTeam", "Account", "FullName",
                "EmployeeNumber", "AccountStatus", "AccountType", "AccountCode",
                "StartDate", "EndDate", "ValidData", "ValidConditionData",
                "CostCenterCode", "BranchName", "Title", "Confirmed", "YearMonth"
            ).forEach { key ->
                assertThat(json).contains("\"$key\"")
            }
            assertThat(json).doesNotContain("\"name\"")
            assertThat(json).doesNotContain("\"req_item_list\"")
        }

        @Test
        @DisplayName("SapOutboundRequest 래퍼 직렬화 - { \"REQUEST\": [...] } 구조이며 interfaceId는 노출되지 않는다")
        fun outboundRequestSerializesAsRequestArray() {
            val today = LocalDate.of(2026, 4, 16)
            val dto = SapPPTMasterOutboundDto.from(createMaster(), today)
            val request = SapOutboundRequest(interfaceId = "SD03300", reqItemList = listOf(dto))

            val json = objectMapper.writeValueAsString(request)

            assertThat(json).startsWith("{\"REQUEST\":[")
            assertThat(json).doesNotContain("interfaceId")
            assertThat(json).doesNotContain("interface_id")
        }
    }
}
