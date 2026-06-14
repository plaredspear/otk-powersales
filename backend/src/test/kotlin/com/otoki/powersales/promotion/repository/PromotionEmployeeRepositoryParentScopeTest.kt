package com.otoki.powersales.promotion.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.QPromotion.Companion.promotion
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
import org.springframework.test.context.ActiveProfiles

/**
 * PromotionEmployee 의 ControlledByParent 조회 join 회귀 테스트.
 *
 * `findAllAccessibleByParentPolicy` 는 부모 Promotion 기준 parentPolicyPredicate 를 받는다.
 * 그 predicate 가 owner/hierarchy 절로 `promotion.ownerUser` 를 참조할 때, 쿼리가 부모의
 * `ownerUser` 를 명시 leftJoin 하지 않으면 암묵 INNER JOIN 이 생겨 부모 owner_user_id NULL 인
 * Promotion 의 자식이 OR 의 다른 절(branch 등)로 통과해야 함에도 전부 누락된다. 본 테스트는
 * 부모 owner=NULL 인 PromotionEmployee 가 부모 branch 절로 조회되는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("PromotionEmployeeRepository parent scope join 테스트")
class PromotionEmployeeRepositoryParentScopeTest {

    @Autowired
    private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val branchCode = "5815"
    private var promotionNumberSeq = 0

    @BeforeEach
    fun setUp() {
        promotionEmployeeRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistPromotion(owner: User?, costCenterCode: String): Promotion {
        val p = Promotion(
            promotionNumber = "P-${promotionNumberSeq++}",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            costCenterCode = costCenterCode,
            ownerUser = owner,
            isDeleted = false,
        )
        return testEntityManager.persistAndFlush(p)
    }

    private fun persistPromotionEmployee(promotionId: Long): PromotionEmployee {
        val pe = PromotionEmployee(
            promotionId = promotionId,
            scheduleDate = LocalDate.of(2026, 6, 12),
        )
        return testEntityManager.persistAndFlush(pe)
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

    /** 부모 Promotion 기준 OR 합성 모사: ownerUser hierarchy 절 OR branch 절. */
    private fun parentOwnerHierarchyOrBranchPredicate(
        subordinateRoleIds: List<Long>,
        branchCodes: List<String>,
    ): Predicate =
        promotion.ownerUser.userRoleId.`in`(subordinateRoleIds)
            .or(promotion.costCenterCode.`in`(branchCodes))

    @Test
    @DisplayName("부모 Promotion owner_user_id NULL 이어도 자식은 부모 branch 절로 조회된다 — 암묵 INNER JOIN 회귀")
    fun childVisibleWhenParentOwnerNull() {
        // 부모 owner=null, 조장 지점과 동일 costCenterCode
        val parent = persistPromotion(owner = null, costCenterCode = branchCode)
        persistPromotionEmployee(parent.id)

        val predicate = parentOwnerHierarchyOrBranchPredicate(
            subordinateRoleIds = listOf(999L), // 매칭 owner 없음 — branch 절로만 통과
            branchCodes = listOf(branchCode),
        )

        val result = promotionEmployeeRepository.findAllAccessibleByParentPolicy(predicate)

        assertThat(result).hasSize(1)
        assertThat(result[0].promotionId).isEqualTo(parent.id)
    }

    @Test
    @DisplayName("부모 owner 보유 자식(hierarchy 절) + 부모 owner NULL 자식(branch 절) 둘 다 조회된다")
    fun bothParentOwnerRowAndNullRowVisible() {
        val roleId = 42L
        val owner = persistUser(username = "owner1", userRoleId = roleId)
        val parentWithOwner = persistPromotion(owner = owner, costCenterCode = "9999")
        val parentOwnerNull = persistPromotion(owner = null, costCenterCode = branchCode)
        persistPromotionEmployee(parentWithOwner.id)
        persistPromotionEmployee(parentOwnerNull.id)

        val predicate = parentOwnerHierarchyOrBranchPredicate(
            subordinateRoleIds = listOf(roleId),
            branchCodes = listOf(branchCode),
        )

        val result = promotionEmployeeRepository.findAllAccessibleByParentPolicy(predicate)

        assertThat(result).hasSize(2)
    }
}
