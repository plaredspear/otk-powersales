package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.exception.SalesProgressRateMasterNotFoundException
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl

@DisplayName("AdminSalesProgressRateMasterService 테스트")
class AdminSalesProgressRateMasterServiceTest {

    private val repository: SalesProgressRateMasterRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk(relaxed = true)

    private val service = AdminSalesProgressRateMasterService(
        repository = repository,
        policyEvaluator = policyEvaluator,
    )

    private val scope: DataScope = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        // 가시 범위 단건 검증 기본 통과 — forbidden 케이스는 개별 override.
        every { repository.existsVisibleById(any(), any()) } returns true
    }

    @Nested
    @DisplayName("getList - 목록 조회")
    inner class GetListTests {

        @Test
        @DisplayName("4채널 목표 합산(targetSum)과 진도율(current/sum)을 산출한다")
        fun computesTargetSumAndProgressRate() {
            val entity = createEntity(
                rt = 100.0, fr = 200.0, rm = 300.0, fo = 400.0,
                currentMonthSalesAmount = 500.0,
            )
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val response = service.getList(scope, null, null, null, 0, 20)

            val item = response.content.single()
            assertThat(item.targetSum).isEqualTo(1000.0)
            // 500 / 1000 = 0.5
            assertThat(item.progressRate).isEqualTo(0.5)
            assertThat(response.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("거래처 lookup 값(이름/지점명/코드/유형)을 행에 매핑한다")
        fun mapsAccountLookupValues() {
            val account = Account(id = 7, name = "GS25 역삼점").also {
                it.branchName = "강남53지점"
                it.externalKey = "1025008"
                it.accountType = AccountType.CVS
            }
            val entity = createEntity(account = account)
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val item = service.getList(scope, null, null, null, 0, 20).content.single()

            assertThat(item.accountName).isEqualTo("GS25 역삼점")
            assertThat(item.accountBranchName).isEqualTo("강남53지점")
            assertThat(item.accountCode).isEqualTo("1025008")
            assertThat(item.accountType).isEqualTo("C.V.S")
        }

        @Test
        @DisplayName("targetSum 이 0 이면 진도율은 null 이다")
        fun nullProgressRateWhenTargetSumZero() {
            val entity = createEntity(rt = 0.0, fr = 0.0, rm = 0.0, fo = 0.0, currentMonthSalesAmount = 500.0)
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val item = service.getList(scope, null, null, null, 0, 20).content.single()

            assertThat(item.targetSum).isEqualTo(0.0)
            assertThat(item.progressRate).isNull()
        }

        @Test
        @DisplayName("키워드/목표년도/목표월 필터를 repository 에 그대로 전달한다")
        fun passesFiltersToRepository() {
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.getList(scope, "역삼", "2026", "3", 0, 20)

            verify {
                repository.searchForAdmin(any(), "역삼", "2026", "3", any())
            }
        }

        @Test
        @DisplayName("요청한 page/size 를 응답에 반영한다")
        fun reflectsPageAndSize() {
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            val response = service.getList(scope, null, null, null, 2, 50)

            assertThat(response.page).isEqualTo(2)
            assertThat(response.size).isEqualTo(50)
        }
    }

    @Nested
    @DisplayName("getDetail - 상세 조회")
    inner class GetDetailTests {

        @Test
        @DisplayName("가시 범위 안의 레코드를 상세로 반환한다")
        fun returnsVisibleDetail() {
            val entity = createEntity(id = 42L)
            every { repository.existsVisibleById(42L, any()) } returns true
            every { repository.findByIdWithRelations(42L) } returns entity

            val detail = service.getDetail(scope, 42L)

            assertThat(detail.id).isEqualTo(42L)
            verify { repository.findByIdWithRelations(42L) }
        }

        @Test
        @DisplayName("가시 범위 밖이면 404 예외 (findByIdWithRelations 미호출)")
        fun throwsWhenNotVisible() {
            every { repository.existsVisibleById(42L, any()) } returns false

            assertThatThrownBy { service.getDetail(scope, 42L) }
                .isInstanceOf(SalesProgressRateMasterNotFoundException::class.java)

            verify(exactly = 0) { repository.findByIdWithRelations(any()) }
        }

        @Test
        @DisplayName("가시 범위 안이지만 레코드 부재 시 404 예외")
        fun throwsWhenEntityMissing() {
            every { repository.existsVisibleById(42L, any()) } returns true
            every { repository.findByIdWithRelations(42L) } returns null

            assertThatThrownBy { service.getDetail(scope, 42L) }
                .isInstanceOf(SalesProgressRateMasterNotFoundException::class.java)
        }
    }

    private fun createEntity(
        id: Long = 1L,
        account: Account? = Account(id = 100, name = "GS25 역삼점"),
        rt: Double? = 100.0,
        fr: Double? = 200.0,
        rm: Double? = 300.0,
        fo: Double? = 400.0,
        currentMonthSalesAmount: Double? = 500.0,
    ) = SalesProgressRateMaster(
        id = id,
        name = "SPR-00000001",
        targetYear = "2026",
        targetMonth = "3",
        rtTargetAmount = rt,
        frTargetAmount = fr,
        rmTargetAmount = rm,
        foTargetAmount = fo,
        currentMonthSalesAmount = currentMonthSalesAmount,
        previousMonthSalesAmount = 450.0,
    ).also { it.account = account }
}
