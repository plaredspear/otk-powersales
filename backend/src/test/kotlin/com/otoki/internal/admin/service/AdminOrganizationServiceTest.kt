package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminOrganizationService 테스트")
class AdminOrganizationServiceTest {

    @Mock
    private lateinit var dataScopeHolder: DataScopeHolder

    @Mock
    private lateinit var orgRepository: OrgRepository

    @InjectMocks
    private lateinit var adminOrganizationService: AdminOrganizationService

    @Nested
    @DisplayName("getOrganizations - 조직마스터 목록 조회")
    inner class GetOrganizationsTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 조직마스터 반환")
        fun allBranches_noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val orgs = listOf(createOrg(id = 1L, orgNameLevel4 = "강남지점"))
            whenever(orgRepository.searchForAdmin(isNull(), isNull(), isNull())).thenReturn(orgs)

            val result = adminOrganizationService.getOrganizations(null, null)

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(1L)
            assertThat(result[0].orgNameLevel4).isEqualTo("강남지점")
        }

        @Test
        @DisplayName("전체 권한 + 키워드 -> 키워드 전달")
        fun allBranches_withKeyword() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val orgs = listOf(createOrg(orgNameLevel4 = "강남지점"))
            whenever(orgRepository.searchForAdmin(eq("강남"), isNull(), isNull())).thenReturn(orgs)

            val result = adminOrganizationService.getOrganizations("강남", null)

            assertThat(result).hasSize(1)
            assertThat(result[0].orgNameLevel4).isEqualTo("강남지점")
        }

        @Test
        @DisplayName("전체 권한 + 레벨 필터 -> 레벨 전달")
        fun allBranches_withLevel() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val orgs = listOf(createOrg(orgNameLevel5 = "강남1조"))
            whenever(orgRepository.searchForAdmin(isNull(), eq("L5"), isNull())).thenReturn(orgs)

            val result = adminOrganizationService.getOrganizations(null, "L5")

            assertThat(result).hasSize(1)
            assertThat(result[0].orgNameLevel5).isEqualTo("강남1조")
        }

        @Test
        @DisplayName("지점 권한 - 필터 없이 조회 -> 본인 CC코드 매칭 조직만 반환")
        fun branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("1101"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val orgs = listOf(createOrg(costCenterLevel4 = "1101", orgNameLevel4 = "강남지점"))
            whenever(orgRepository.searchForAdmin(isNull(), isNull(), eq(listOf("1101")))).thenReturn(orgs)

            val result = adminOrganizationService.getOrganizations(null, null)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + branchCodes 비어있음 -> 빈 결과 (NoAccess)")
        fun branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminOrganizationService.getOrganizations(null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("빈 결과 -> 빈 리스트 반환")
        fun emptyResult() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            whenever(orgRepository.searchForAdmin(eq("존재하지않는조직"), isNull(), isNull())).thenReturn(emptyList())

            val result = adminOrganizationService.getOrganizations("존재하지않는조직", null)

            assertThat(result).isEmpty()
        }
    }

    private fun createOrg(
        id: Long = 1L,
        costCenterLevel2: String? = "1000",
        orgCodeLevel2: String? = "A100",
        orgNameLevel2: String? = "영업본부",
        costCenterLevel3: String? = "1100",
        orgCodeLevel3: String? = "A110",
        orgNameLevel3: String? = "수도권사업부",
        costCenterLevel4: String? = "1101",
        orgCodeLevel4: String? = "A111",
        orgNameLevel4: String? = "강남지점",
        costCenterLevel5: String? = null,
        orgCodeLevel5: String? = null,
        orgNameLevel5: String? = null,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 1, 15, 9, 0)
    ): Org = Org(
        id = id,
        costCenterLevel2 = costCenterLevel2,
        orgCodeLevel2 = orgCodeLevel2,
        orgNameLevel2 = orgNameLevel2,
        costCenterLevel3 = costCenterLevel3,
        orgCodeLevel3 = orgCodeLevel3,
        orgNameLevel3 = orgNameLevel3,
        costCenterLevel4 = costCenterLevel4,
        orgCodeLevel4 = orgCodeLevel4,
        orgNameLevel4 = orgNameLevel4,
        costCenterLevel5 = costCenterLevel5,
        orgCodeLevel5 = orgCodeLevel5,
        orgNameLevel5 = orgNameLevel5,
        createdAt = createdAt
    )
}
