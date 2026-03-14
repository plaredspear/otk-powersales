package com.otoki.internal.admin.service

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.OrganizationRepository
import com.otoki.internal.sap.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminScheduleService 테스트")
class AdminScheduleServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var templateGenerator: ScheduleTemplateGenerator

    @InjectMocks
    private lateinit var adminScheduleService: AdminScheduleService

    @Nested
    @DisplayName("getBranches - 지점 목록 조회")
    inner class GetBranchesTests {

        @Test
        @DisplayName("정상 조회 - 지점 목록 반환")
        fun getBranches_success() {
            // Given
            val branches = listOf(
                BranchResponse("1234", "서울지점"),
                BranchResponse("5678", "부산지점")
            )
            whenever(userRepository.findDistinctBranches()).thenReturn(branches)

            // When
            val result = adminScheduleService.getBranches()

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].costCenterCode).isEqualTo("1234")
            assertThat(result[0].branchName).isEqualTo("서울지점")
            assertThat(result[1].costCenterCode).isEqualTo("5678")
            assertThat(result[1].branchName).isEqualTo("부산지점")
        }

        @Test
        @DisplayName("빈 지점 필터링 - branchCode 또는 branchName이 빈 문자열인 경우 제외")
        fun getBranches_filtersEmpty() {
            // Given
            val branches = listOf(
                BranchResponse("", "빈코드지점"),
                BranchResponse("1234", ""),
                BranchResponse("5678", "부산지점")
            )
            whenever(userRepository.findDistinctBranches()).thenReturn(branches)

            // When
            val result = adminScheduleService.getBranches()

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].costCenterCode).isEqualTo("5678")
        }
    }

    @Nested
    @DisplayName("generateTemplate - 양식 다운로드")
    inner class GenerateTemplateTests {

        @Test
        @DisplayName("정상 생성 - 사원이 있는 지점의 템플릿")
        fun generateTemplate_success() {
            // Given
            val costCenterCode = "1234"
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode)
            val employees = listOf(
                createUser(employeeId = "20030001", name = "홍길동", orgName = "A팀"),
                createUser(employeeId = "20030002", name = "김철수", orgName = "B팀")
            )

            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(org)
            whenever(
                userRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
                    costCenterCode, "재직"
                )
            ).thenReturn(employees)
            whenever(templateGenerator.generate(employees)).thenReturn(ByteArray(100))

            // When
            val result = adminScheduleService.generateTemplate(costCenterCode)

            // Then
            assertThat(result.bytes).hasSize(100)
            assertThat(result.filename).startsWith("진열스케줄_양식_1234_")
            assertThat(result.filename).endsWith(".xlsx")
        }

        @Test
        @DisplayName("사원 없음 - 헤더만 있는 템플릿 생성")
        fun generateTemplate_noEmployees() {
            // Given
            val costCenterCode = "9999"
            val org = Organization(id = 1, costCenterLevel5 = costCenterCode)

            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(org)
            whenever(
                userRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
                    costCenterCode, "재직"
                )
            ).thenReturn(emptyList())
            whenever(templateGenerator.generate(emptyList())).thenReturn(ByteArray(50))

            // When
            val result = adminScheduleService.generateTemplate(costCenterCode)

            // Then
            assertThat(result.bytes).hasSize(50)
            assertThat(result.filename).contains("9999")
        }

        @Test
        @DisplayName("존재하지 않는 지점 - OrganizationNotFoundException")
        fun generateTemplate_orgNotFound() {
            // Given
            val costCenterCode = "0000"
            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(null)
            whenever(organizationRepository.findFirstByCostCenterLevel4(costCenterCode)).thenReturn(null)

            // When/Then
            assertThatThrownBy { adminScheduleService.generateTemplate(costCenterCode) }
                .isInstanceOf(OrganizationNotFoundException::class.java)
        }

        @Test
        @DisplayName("Level4 매칭 - costCenterLevel5에 없지만 Level4에 있는 경우 성공")
        fun generateTemplate_level4Fallback() {
            // Given
            val costCenterCode = "5678"
            val org = Organization(id = 1, costCenterLevel4 = costCenterCode)

            whenever(organizationRepository.findFirstByCostCenterLevel5(costCenterCode)).thenReturn(null)
            whenever(organizationRepository.findFirstByCostCenterLevel4(costCenterCode)).thenReturn(org)
            whenever(
                userRepository.findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
                    costCenterCode, "재직"
                )
            ).thenReturn(emptyList())
            whenever(templateGenerator.generate(emptyList())).thenReturn(ByteArray(50))

            // When
            val result = adminScheduleService.generateTemplate(costCenterCode)

            // Then
            assertThat(result.filename).contains("5678")
        }
    }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030001",
        name: String = "테스트사원",
        costCenterCode: String = "1234",
        orgName: String = "테스트팀"
    ): User = User(
        id = id,
        employeeId = employeeId,
        name = name,
        costCenterCode = costCenterCode,
        orgName = orgName,
        appAuthority = null,
        appLoginActive = true,
        status = "재직"
    )
}
