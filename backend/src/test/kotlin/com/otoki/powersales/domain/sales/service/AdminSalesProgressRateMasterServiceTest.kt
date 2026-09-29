package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
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
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("AdminSalesProgressRateMasterService 테스트")
class AdminSalesProgressRateMasterServiceTest {

    private val repository: SalesProgressRateMasterRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk(relaxed = true)

    // 당월/전월 실적은 조회 시점 산출이라 실제 resolver 를 물리고 원천(월매출이력)만 mock 한다 —
    // 저장 컬럼 폴백/산출값 우선 규칙까지 이 테스트가 함께 지킨다.
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()
    private val service = AdminSalesProgressRateMasterService(
        repository = repository,
        policyEvaluator = policyEvaluator,
        actualsResolver = SalesProgressRateMasterActualsResolver(monthlySalesHistoryGateway),
    )

    private val materializedAt: LocalDateTime = LocalDateTime.of(2026, 3, 20, 6, 12)

    private val scope: DataScope = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        // 가시 범위 단건 검증 기본 통과 — forbidden 케이스는 개별 override.
        every { repository.existsVisibleById(any(), any()) } returns true
        // 월매출이력 기본 stub — 산출 케이스는 개별 override (미적재 = 저장 컬럼 폴백).
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
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
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val response = service.getList(scope, null, null, null, null, 0, 20)

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
                it.accountType = "C.V.S"
            }
            val entity = createEntity(account = account)
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val item = service.getList(scope, null, null, null, null, 0, 20).content.single()

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
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val item = service.getList(scope, null, null, null, null, 0, 20).content.single()

            assertThat(item.targetSum).isEqualTo(0.0)
            assertThat(item.progressRate).isNull()
        }

        @Test
        @DisplayName("월매출이력이 있으면 저장 컬럼 대신 마감 합계로 실적/진도율을 산출하고 적재 시각을 함께 준다")
        fun derivesActualsFromMonthlySalesHistory() {
            // 저장 컬럼은 이관 스냅샷(500/450) 이지만, 월매출이력 row 가 있으면 그 값이 정본이다.
            val entity = createEntity()
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))
            every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(
                monthlySalesRow("202603", "800", materializedAt),
                monthlySalesRow("202602", "600", materializedAt.minusDays(30)),
            )

            val item = service.getList(scope, null, null, null, null, 0, 20).content.single()

            assertThat(item.currentMonthSalesAmount).isEqualTo(800.0)
            assertThat(item.previousMonthSalesAmount).isEqualTo(600.0)
            // 진도율도 산출값 기준 — 800 / 1000
            assertThat(item.progressRate).isEqualTo(0.8)
            assertThat(item.currentMonthSourceUpdatedAt).isEqualTo(materializedAt)
        }

        @Test
        @DisplayName("월매출이력 row 가 없으면 저장 컬럼(이관 스냅샷)으로 폴백하고 적재 시각은 null 이다")
        fun fallsBackToStoredColumnWhenNoMonthlyRow() {
            val entity = createEntity()
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(entity))

            val item = service.getList(scope, null, null, null, null, 0, 20).content.single()

            assertThat(item.currentMonthSalesAmount).isEqualTo(500.0)
            assertThat(item.previousMonthSalesAmount).isEqualTo(450.0)
            assertThat(item.currentMonthSourceUpdatedAt).isNull()
            assertThat(item.previousMonthSourceUpdatedAt).isNull()
        }

        @Test
        @DisplayName("키워드/목표년도/목표월/지점코드 필터를 repository 에 그대로 전달한다")
        fun passesFiltersToRepository() {
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.getList(scope, "역삼", "2026", "3", listOf("1100"), 0, 20)

            verify {
                repository.searchForAdmin(any(), "역삼", "2026", "3", listOf("1100"), any())
            }
        }

        @Test
        @DisplayName("확장 집합(호출부 산출)을 그대로 조회 조건으로 쓴다 (개편 전 코드 적재분 누락 방지)")
        fun expandsBranchCode() {
            // 확장은 컨트롤러(BranchScopeGateway) 가 판정 후 수행한다 — 서비스는 결과 목록만 소비.
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.getList(scope, null, null, null, listOf("5815", "5452"), 0, 20)

            verify {
                repository.searchForAdmin(
                    any(),
                    any(),
                    any(),
                    any(),
                    match<List<String>> { it.toSet() == setOf("5815", "5452") },
                    any()
                )
            }
        }

        @Test
        @DisplayName("지점코드 미지정 - 지점 조건 없음(null) 으로 조회한다")
        fun noBranchCode() {
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.getList(scope, null, null, null, null, 0, 20)

            verify { repository.searchForAdmin(any(), any(), any(), any(), null, any()) }
        }

        @Test
        @DisplayName("요청한 page/size 를 응답에 반영한다")
        fun reflectsPageAndSize() {
            every {
                repository.searchForAdmin(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            val response = service.getList(scope, null, null, null, null, 2, 50)

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

    /** createEntity 의 거래처(account id=100) + 지정 매출월 월매출이력 row. */
    private fun monthlySalesRow(salesDate: String, closingAmountSum: String, updatedAt: LocalDateTime) =
        MonthlySalesRow(
            sapAccountCode = "1025008",
            salesDate = salesDate,
            closingAmountSum = BigDecimal(closingAmountSum),
            accountId = 100L,
            updatedAt = updatedAt,
            abcClosingAmount1 = null,
        )
}
