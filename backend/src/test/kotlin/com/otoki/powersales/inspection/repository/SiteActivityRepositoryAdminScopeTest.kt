package com.otoki.powersales.inspection.repository

import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityFilter
import com.otoki.powersales.inspection.entity.QSiteActivity.Companion.siteActivity
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.user.entity.User
import com.querydsl.core.types.Predicate
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

/**
 * SiteActivity admin 조회의 sharing-policy join 회귀 테스트.
 *
 * 버그: policyPredicate 가 owner/hierarchy 절로 `ownerUser` 를 참조할 때, repository 쿼리가
 * `ownerUser` 를 명시 leftJoin 하지 않으면 QueryDSL 이 암묵 INNER JOIN 을 생성한다. 그 결과
 * `owner_user_id IS NULL` 인 행(모바일 등록 건)이, OR 의 다른 절(cost_center_code 등)로
 * 통과해야 함에도 전부 누락된다. 본 테스트는 owner 경로를 참조하는 OR predicate 를 주입해
 * owner NULL 행이 branch 절로 정상 조회되는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("SiteActivityRepository admin scope join 테스트")
class SiteActivityRepositoryAdminScopeTest {

    @Autowired
    private lateinit var siteActivityRepository: SiteActivityRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val branchCode = "5815"

    @BeforeEach
    fun setUp() {
        siteActivityRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistActivity(
        owner: User?,
        costCenterCode: String,
        activityDate: LocalDate = LocalDate.of(2026, 6, 12),
    ): SiteActivity {
        val sa = SiteActivity(
            activityDate = activityDate,
            category = "본매대",
            productType = "자사",
            costCenterCode = costCenterCode,
            isDeleted = false,
            ownerUser = owner,
        )
        return testEntityManager.persistAndFlush(sa)
    }

    private fun persistUser(username: String, userRoleId: Long?): User {
        val u = User(
            username = username,
            employeeCode = username,
            password = "x",
            userRoleId = userRoleId,
        )
        return testEntityManager.persistAndFlush(u)
    }

    /**
     * 실제 SharingRulePolicyEvaluator 의 OR 합성 모사:
     * `ownerUser.userRoleId IN (...)` (hierarchy 절 — owner 경로 참조)
     * OR `costCenterCode IN (branchCodes)` (legacy branch 절).
     */
    private fun ownerHierarchyOrBranchPredicate(
        subordinateRoleIds: List<Long>,
        branchCodes: List<String>,
    ): Predicate =
        siteActivity.ownerUser.userRoleId.`in`(subordinateRoleIds)
            .or(siteActivity.costCenterCode.`in`(branchCodes))

    @Test
    @DisplayName("owner_user_id NULL 행도 branch 절(costCenterCode)로 조회된다 — 암묵 INNER JOIN 회귀")
    fun ownerNullRowVisibleViaBranchPredicate() {
        // 모바일 등록 건 모사: ownerUser=null, 조장 지점과 동일한 costCenterCode
        persistActivity(owner = null, costCenterCode = branchCode)

        val predicate = ownerHierarchyOrBranchPredicate(
            subordinateRoleIds = listOf(999L), // 매칭되는 owner 없음 — branch 절로만 통과해야 함
            branchCodes = listOf(branchCode),
        )
        val filter = AdminSiteActivityFilter(
            startDate = LocalDate.of(2026, 5, 14),
            endDate = LocalDate.of(2026, 6, 13),
            category = null, fieldType = null, employeeName = null, accountCode = null,
        )

        val page = siteActivityRepository.searchForAdmin(predicate, filter, PageRequest.of(0, 20))

        assertThat(page.totalElements).isEqualTo(1)
        assertThat(page.content[0].costCenterCode).isEqualTo(branchCode)
        assertThat(page.content[0].ownerUser).isNull()
    }

    @Test
    @DisplayName("owner 있는 행은 hierarchy 절로, owner NULL 행은 branch 절로 — 둘 다 조회된다")
    fun ownerRowAndNullRowBothVisible() {
        val roleId = 42L
        val owner = persistUser(username = "owner1", userRoleId = roleId)
        // 행 A: owner 보유, 다른 지점 — hierarchy 절로 통과
        persistActivity(owner = owner, costCenterCode = "9999")
        // 행 B: owner NULL, 조장 지점 — branch 절로 통과
        persistActivity(owner = null, costCenterCode = branchCode)

        val predicate = ownerHierarchyOrBranchPredicate(
            subordinateRoleIds = listOf(roleId),
            branchCodes = listOf(branchCode),
        )
        val filter = AdminSiteActivityFilter(
            startDate = LocalDate.of(2026, 5, 14),
            endDate = LocalDate.of(2026, 6, 13),
            category = null, fieldType = null, employeeName = null, accountCode = null,
        )

        val page = siteActivityRepository.searchForAdmin(predicate, filter, PageRequest.of(0, 20))

        assertThat(page.totalElements).isEqualTo(2)
    }
}
