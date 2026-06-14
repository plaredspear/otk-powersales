package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("TeamMemberCategorySearchService (Spec 813)")
class TeamMemberCategorySearchServiceTest {

    private lateinit var expander: BranchCodeExpander
    private lateinit var queryFactory: JPAQueryFactory
    private lateinit var service: TeamMemberCategorySearchService

    @BeforeEach
    fun setUp() {
        expander = mockk()
        queryFactory = mockk()
        service = TeamMemberCategorySearchService(expander, queryFactory)
    }

    @Nested
    @DisplayName("previousYearMonth — SF cls:86-92 동등")
    inner class PreviousYearMonth {
        @Test
        @DisplayName("1월 → 전년 12월")
        fun januaryRollsBack() {
            assertThat(service.previousYearMonth("2026", "1")).isEqualTo("2025" to "12")
        }

        @Test
        @DisplayName("5월 → 같은 해 4월")
        fun midYear() {
            assertThat(service.previousYearMonth("2026", "5")).isEqualTo("2026" to "4")
        }

        @Test
        @DisplayName("12월 → 같은 해 11월")
        fun december() {
            assertThat(service.previousYearMonth("2026", "12")).isEqualTo("2026" to "11")
        }
    }

    @Nested
    @DisplayName("aggregateCountMap — SF getCountMap (cls:178-217) 9 카테고리 분류")
    inner class AggregateCountMap {

        @Test
        @DisplayName("진열 + 고정 + 상시 포함 → 진열고정 카테고리 + ConvertedHeadcount 합산")
        fun displayFixedWithStandby() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "고정", wc5 = "상시", headcount = "1.500"),
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "고정", wc5 = "상시", headcount = "0.500"),
            )
            val result = service.aggregateCountMap(rows, "5")
            assertThat(result).containsEntry("한밭1지점/5월진열고정", BigDecimal("2.000"))
        }

        @Test
        @DisplayName("진열 + 격고 + 상시 → 진열격고")
        fun displaySplitFixedWithStandby() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "격고", wc5 = "상시", headcount = "1.000"),
            )
            val result = service.aggregateCountMap(rows, "5")
            assertThat(result).containsEntry("한밭1지점/5월진열격고", BigDecimal("1.000"))
        }

        @Test
        @DisplayName("진열 + 순회 + 상시 → 진열순회")
        fun displayRotate() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "순회", wc5 = "상시", headcount = "1.000"),
            )
            assertThat(service.aggregateCountMap(rows, "5"))
                .containsEntry("한밭1지점/5월진열순회", BigDecimal("1.000"))
        }

        @Test
        @DisplayName("행사 6 카테고리 (상온/라면/냉동/냉장/만두/냉동/냉장)")
        fun eventCategories() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "상온", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "라면", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "냉동", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "냉장", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "만두", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "행사", wc4 = "냉동/냉장", headcount = "1.000"),
            )
            val result = service.aggregateCountMap(rows, "5")
            assertThat(result).containsKeys(
                "한밭1지점/5월행사상온",
                "한밭1지점/5월행사라면",
                "한밭1지점/5월행사냉동",
                "한밭1지점/5월행사냉장",
                "한밭1지점/5월행사만두",
                "한밭1지점/5월행사냉장냉동",
            )
        }

        @Test
        @DisplayName("진열 + WorkingCategory5 가 '상시' 토큰 미포함 (예: '기타') → 분류 제외 (SF L182 contains '상시' 와 동일)")
        fun displayWithoutStandbyExcluded() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "고정", wc5 = "기타", headcount = "1.000"),
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "고정", wc5 = null, headcount = "1.000"),
            )
            assertThat(service.aggregateCountMap(rows, "5")).isEmpty()
        }

        @Test
        @DisplayName("D4=(a) employee.orgName lazy join — orgName null 이면 분류 제외 (=branchName key 생성 불가)")
        fun excludeWhenOrgNameNull() {
            val rows = listOf(
                mfeisRow(orgName = null, wc1 = "진열", wc3 = "고정", wc5 = "상시", headcount = "1.000"),
            )
            assertThat(service.aggregateCountMap(rows, "5")).isEmpty()
        }

        @Test
        @DisplayName("ConvertedHeadcount null → BigDecimal.ZERO 적용")
        fun nullHeadcountAsZero() {
            val rows = listOf(
                mfeisRow(orgName = "한밭1지점", wc1 = "진열", wc3 = "고정", wc5 = "상시", headcount = null),
            )
            val result = service.aggregateCountMap(rows, "5")
            assertThat(result).containsEntry("한밭1지점/5월진열고정", BigDecimal.ZERO)
        }
    }

    @Nested
    @DisplayName("buildResultItem — SF getItemList (cls:78-163) 현월/전월 비교")
    inner class BuildResultItem {

        @Test
        @DisplayName("현월/전월 모두 0 → setNull (모든 수치 필드 null)")
        fun allZeroAppliesSetNull() {
            val item = service.buildResultItem("한밭1지점", "5", "4", emptyMap())
            assertThat(item.branchName).isEqualTo("한밭1지점")
            assertThat(item.fix).isNull()
            assertThat(item.currentMonthTotal).isNull()
            assertThat(item.totalIncrease).isNull()
        }

        @Test
        @DisplayName("현월에 진열고정 1.0 → currentExhibitionTotal=1.0, totalIncrease=1.0 (전월=0)")
        fun currentOnlyDisplay() {
            val countMap = mapOf(
                "한밭1지점/5월진열고정" to BigDecimal("1.000"),
            )
            val item = service.buildResultItem("한밭1지점", "5", "4", countMap)
            assertThat(item.fix).isEqualByComparingTo(BigDecimal("1.000"))
            assertThat(item.currentExhibitionTotal).isEqualByComparingTo(BigDecimal("1.000"))
            assertThat(item.currentMonthTotal).isEqualByComparingTo(BigDecimal("1.0"))
            assertThat(item.lastMonthTotal).isEqualByComparingTo(BigDecimal("0.0"))
            assertThat(item.totalIncrease).isEqualByComparingTo(BigDecimal("1.0"))
        }

        @Test
        @DisplayName("행사 상온 = 상온(라면 제외) + 라면 합산")
        fun roomTemperatureCombination() {
            val countMap = mapOf(
                "한밭1지점/5월행사상온" to BigDecimal("0.500"),
                "한밭1지점/5월행사라면" to BigDecimal("0.500"),
            )
            val item = service.buildResultItem("한밭1지점", "5", "4", countMap)
            assertThat(item.roomTemperature).isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        @DisplayName("행사 냉동/냉장 = 냉동 + 냉장 + 만두 + 냉동/냉장 합산")
        fun refrigerationFreezingCombination() {
            val countMap = mapOf(
                "한밭1지점/5월행사냉동" to BigDecimal("1"),
                "한밭1지점/5월행사냉장" to BigDecimal("1"),
                "한밭1지점/5월행사만두" to BigDecimal("1"),
                "한밭1지점/5월행사냉장냉동" to BigDecimal("1"),
            )
            val item = service.buildResultItem("한밭1지점", "5", "4", countMap)
            assertThat(item.refrigerationAndFreezing).isEqualByComparingTo(BigDecimal("4"))
        }

        @Test
        @DisplayName("buildResultItem 의 첫 인자는 한글 지점명 차원 — 코드값 (예: '5832') 으로 호출 시 countMap 의 한글 key 와 매칭 불가하여 setNull")
        fun keyDimensionMustMatchCountMap() {
            // countMap 은 aggregateCountMap 이 사용하는 차원 = employee.orgName (= 한글 지점명, SF BranchName__c 동등)
            val countMap = mapOf(
                "원주1지점/4월진열고정" to BigDecimal("10.623"),
                "원주1지점/4월진열격고" to BigDecimal("3.807"),
                "원주1지점/4월진열순회" to BigDecimal("10.431"),
            )
            // 코드값 ("5832") 으로 호출하면 countMap 의 한글 key 와 매칭 불가 → setNull
            val itemByCode = service.buildResultItem("5832", "4", "3", countMap)
            assertThat(itemByCode.fix).isNull()
            assertThat(itemByCode.currentMonthTotal).isNull()

            // 한글 지점명 ("원주1지점") 으로 호출해야 정상 매칭
            val itemByName = service.buildResultItem("원주1지점", "4", "3", countMap)
            assertThat(itemByName.fix).isEqualByComparingTo(BigDecimal("10.623"))
            assertThat(itemByName.store).isEqualByComparingTo(BigDecimal("3.807"))
            assertThat(itemByName.rotate).isEqualByComparingTo(BigDecimal("10.431"))
            assertThat(itemByName.currentExhibitionTotal).isEqualByComparingTo(BigDecimal("24.861"))
        }
    }

    private fun mfeisRow(
        orgName: String?,
        wc1: String? = null,
        wc3: String? = null,
        wc4: String? = null,
        wc5: String? = null,
        headcount: String?,
    ): MonthlyFemaleEmployeeIntegrationSchedule {
        val emp = orgName?.let { name -> Employee(employeeCode = "E${name.hashCode()}", name = "임직원", orgName = name) }
        return MonthlyFemaleEmployeeIntegrationSchedule(
            workingCategory1 = wc1,
            workingCategory3 = wc3,
            workingCategory4 = wc4,
            workingCategory5 = wc5,
            convertedHeadcount = headcount?.let { BigDecimal(it) },
            employee = emp,
        )
    }
}
