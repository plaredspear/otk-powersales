package com.otoki.powersales.schedule.service

import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.schedule.dto.response.TeamMemberCategoryResultItem
import com.otoki.powersales.schedule.dto.response.TeamMemberCategorySearchResult
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 팀멤버 카테고리 검색 서비스 (SF `CategorySearchByTeamMemberController.cls:165-251`).
 *
 * MFEIS 데이터를 진열(고정/격고/순회) + 행사(상온/라면/냉동/냉장/만두/냉동냉장) 9개 카테고리로 분류하여
 * 지점별 인원수 집계. 현월 + 전월 두 달치 데이터로 증감 표 산출.
 *
 * D3=(a) 결정: SF formula 미적재 필드 (BranchName, AccountBranchName 등) 는 MFEIS lazy join 으로 환원.
 *   - SF `BranchName__c = FullName__r.DKRetail__OrgName__c` → backend `mfeis.employee.orgName`
 */
@Service
@Transactional(readOnly = true)
class TeamMemberCategorySearchService(
    private val expander: BranchCodeExpander,
    private val queryFactory: JPAQueryFactory,
) {

    /**
     * SF `getCategory(year, month, orgValues)` 동등.
     * @param year 년 ("2026")
     * @param month 월 ("5" / "05" 모두 허용)
     * @param orgValues 사용자 선택 지점 코드 리스트 (LWC 의 orgMap.values() 동등)
     */
    fun search(year: String, month: String, orgValues: List<String>): TeamMemberCategorySearchResult {
        val normMonth = month.toInt().toString()
        val (lastYear, lastMonth) = previousYearMonth(year, normMonth)

        val expandedCodes = expander.expand(orgValues)
        if (expandedCodes.isEmpty()) {
            return TeamMemberCategorySearchResult(resultCode = "S", resultMsg = "검색결과가 없습니다.", result = emptyList())
        }

        val currentMonthRows = fetchMfeisByYearMonthAndCostCenterCodes(year, normMonth, expandedCodes)
        val lastMonthRows = fetchMfeisByYearMonthAndCostCenterCodes(lastYear, lastMonth, expandedCodes)

        val currentCountMap = aggregateCountMap(currentMonthRows, normMonth)
        val lastCountMap = aggregateCountMap(lastMonthRows, lastMonth)
        val countMap = currentCountMap + lastCountMap  // SF L97 putAll 동등

        val items = orgValues.map { orgValue ->
            buildResultItem(orgValue, normMonth, lastMonth, countMap)
        }

        return TeamMemberCategorySearchResult(
            resultCode = "S",
            resultMsg = if (items.isEmpty()) "검색결과가 없습니다." else null,
            result = items,
        )
    }

    /**
     * SF `getCountMap` 의 9 카테고리 분류 (cls:178-217) 1:1.
     * key = `"<orgBranchName>/<month>월<category>"`.
     */
    internal fun aggregateCountMap(
        schedules: List<MonthlyFemaleEmployeeIntegrationSchedule>,
        month: String,
    ): Map<String, BigDecimal> {
        val countMap = mutableMapOf<String, BigDecimal>()
        for (s in schedules) {
            val w1 = s.workingCategory1
            val w3 = s.workingCategory3
            val w4 = s.workingCategory4
            val w5 = s.workingCategory5

            val key: String? = when {
                w1 == "진열" && w3 == "고정" && w5?.contains("상시") == true -> "${month}월진열고정"
                w1 == "진열" && w3 == "격고" && w5?.contains("상시") == true -> "${month}월진열격고"
                w1 == "진열" && w3 == "순회" && w5?.contains("상시") == true -> "${month}월진열순회"
                w1 == "행사" && w4 == "상온" -> "${month}월행사상온"
                w1 == "행사" && w4 == "라면" -> "${month}월행사라면"
                w1 == "행사" && w4 == "냉동" -> "${month}월행사냉동"
                w1 == "행사" && w4 == "냉장" -> "${month}월행사냉장"
                w1 == "행사" && w4 == "만두" -> "${month}월행사만두"
                w1 == "행사" && w4 == "냉동/냉장" -> "${month}월행사냉장냉동"
                else -> null
            } ?: continue

            // D4=(a): SF BranchName__c = FullName__r.DKRetail__OrgName__c → backend mfeis.employee.orgName
            val branchName = s.employee?.orgName ?: continue
            val mapKey = "$branchName/$key"
            val headcount = s.convertedHeadcount?.setScale(3, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
            countMap.merge(mapKey, headcount) { a, b -> a + b }
        }
        return countMap
    }

    /**
     * SF `getItemList` (cls:78-163) 의 단일 지점 row 생성 + 현월/전월 비교.
     */
    internal fun buildResultItem(
        orgValue: String,
        month: String,
        lastMonth: String,
        countMap: Map<String, BigDecimal>,
    ): TeamMemberCategoryResultItem {
        // 현월 진열
        val fix = countMap.getOrZero("$orgValue/${month}월진열고정")
        val store = countMap.getOrZero("$orgValue/${month}월진열격고")
        val rotate = countMap.getOrZero("$orgValue/${month}월진열순회")
        val currentExhibition = fix + store + rotate

        // 전월 진열
        val lastFix = countMap.getOrZero("$orgValue/${lastMonth}월진열고정")
        val lastStore = countMap.getOrZero("$orgValue/${lastMonth}월진열격고")
        val lastRotate = countMap.getOrZero("$orgValue/${lastMonth}월진열순회")
        val lastExhibition = lastFix + lastStore + lastRotate

        val exhibitionIncrease = currentExhibition - lastExhibition

        // 현월 행사 — 상온 = 상온(라면 제외) + 라면
        val ambientExceptRamen = countMap.getOrZero("$orgValue/${month}월행사상온")
        val ramen = countMap.getOrZero("$orgValue/${month}월행사라면")
        val roomTemperature = ambientExceptRamen + ramen

        // 현월 행사 — 냉동/냉장 = 냉동 + 냉장 + 만두 + 냉동/냉장
        val freezing = countMap.getOrZero("$orgValue/${month}월행사냉동")
        val refrigeration = countMap.getOrZero("$orgValue/${month}월행사냉장")
        val dumpling = countMap.getOrZero("$orgValue/${month}월행사만두")
        val freezingAndRefrigeration = countMap.getOrZero("$orgValue/${month}월행사냉장냉동")
        val refrigerationAndFreezing = freezing + refrigeration + dumpling + freezingAndRefrigeration

        // 전월 행사 — 상온
        val lastAmbientExceptRamen = countMap.getOrZero("$orgValue/${lastMonth}월행사상온")
        val lastRamen = countMap.getOrZero("$orgValue/${lastMonth}월행사라면")
        val lastRoomTemperature = lastAmbientExceptRamen + lastRamen

        // 전월 행사 — 냉동/냉장
        val lastFreezing = countMap.getOrZero("$orgValue/${lastMonth}월행사냉동")
        val lastRefrigeration = countMap.getOrZero("$orgValue/${lastMonth}월행사냉장")
        val lastDumpling = countMap.getOrZero("$orgValue/${lastMonth}월행사만두")
        val lastFreezingAndRefrigeration = countMap.getOrZero("$orgValue/${lastMonth}월행사냉장냉동")
        val lastRefrigerationAndFreezing =
            lastRefrigeration + lastFreezing + lastDumpling + lastFreezingAndRefrigeration

        val currentEventTotal = roomTemperature + refrigerationAndFreezing
        val lastEventTotal = lastRoomTemperature + lastRefrigerationAndFreezing
        val eventIncrease = currentEventTotal - lastEventTotal

        val currentMonthTotal = (currentExhibition + currentEventTotal).setScale(1, RoundingMode.HALF_UP)
        val lastMonthTotal = (lastExhibition + lastEventTotal).setScale(1, RoundingMode.HALF_UP)
        val totalIncrease = (currentMonthTotal - lastMonthTotal).setScale(1, RoundingMode.HALF_UP)

        // SF setNull() (cls:341-363) — 양 월 모두 0 이면 모든 수치 필드 null 처리
        val allZero = currentMonthTotal.signum() == 0 && lastMonthTotal.signum() == 0
        return if (allZero) {
            TeamMemberCategoryResultItem(branchName = orgValue)
        } else {
            TeamMemberCategoryResultItem(
                branchName = orgValue,
                fix = fix,
                store = store,
                rotate = rotate,
                currentExhibitionTotal = currentExhibition,
                lastExhibitionTotal = lastExhibition,
                exhibitionIncrease = exhibitionIncrease,
                roomTemperature = roomTemperature,
                refrigerationAndFreezing = refrigerationAndFreezing,
                currentEventTotal = currentEventTotal,
                lastEventTotal = lastEventTotal,
                eventIncrease = eventIncrease,
                currentMonthTotal = currentMonthTotal,
                lastMonthTotal = lastMonthTotal,
                totalIncrease = totalIncrease,
            )
        }
    }

    /**
     * SF cls:86-92 — 1월 입력 시 전년 12월 반환.
     */
    internal fun previousYearMonth(year: String, month: String): Pair<String, String> {
        val m = month.toInt()
        return if (m == 1) {
            (year.toInt() - 1).toString() to "12"
        } else {
            year to (m - 1).toString()
        }
    }

    private fun Map<String, BigDecimal>.getOrZero(key: String): BigDecimal = this[key] ?: BigDecimal.ZERO

    /**
     * SF L169-174 SOQL 동등 — `WHERE Year__c = :year AND Month__c = :month AND CostCenterCode__c IN :codes`.
     */
    private fun fetchMfeisByYearMonthAndCostCenterCodes(
        year: String,
        month: String,
        costCenterCodes: Collection<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val q = monthlyFemaleEmployeeIntegrationSchedule
        return queryFactory
            .selectFrom(q)
            .where(
                q.year.eq(year)
                    .and(q.month.eq(month))
                    .and(q.costCenterCode.`in`(costCenterCodes))
            )
            .fetch()
    }
}
