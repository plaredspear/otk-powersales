package com.otoki.powersales.platform.common.service

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleService
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.dto.response.MyAccountInfo
import com.otoki.powersales.platform.common.dto.response.MyAccountListResponse
import com.otoki.powersales.platform.common.dto.response.MyAccountMeta
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.exception.AccountInvalidParameterException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepositoryCustom
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 내 거래처 서비스
 *
 * 레거시 거래처 조회 분기(권한 × 화면 유형)를 그대로 재현한다.
 *
 * | 권한 | 조회 기준 | 레거시 쿼리 |
 * |------|-----------|-------------|
 * | 부서장(AccountViewAll) + [MyAccountScope.SALES] | 일정 잡힌 전체 거래처 | `selectAllAccount` |
 * | 조장 (yang 예외 1인) | 팀장 기준 스케줄 거래처 | `selectMyAccount`(조장 분기) |
 * | 조장 (일반) | 지점코드 + 그룹 1000/1010 | `teamleaderAccList` |
 * | 여사원/그 외 | 본인 팀멤버스케줄 거래처 | `selectMyAccount`(여사원 분기) |
 *
 * 진열스케줄(displayWorkSchedule) union 과 주문가능 거래처유형(abctypecode) 필터는 레거시 주문 셀렉터
 * (`accountSelectList` with `order=order`)에만 존재한다. 따라서 주문 계열 scope([MyAccountScope.isOrder])
 * 일 때만 여사원/yang 예외 경로에 합치며, 매출/현장 화면(SALES/FIELD)에는 합치지 않는다.
 *
 * 주문서 **작성** 화면([MyAccountScope.ORDER_WRITE])은 여기서 한 번 더 갈린다 —
 * [FeatureFlag.ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY] 가 활성이면 팀멤버스케줄 전 기간을 쓰지 않고
 * **오늘 확정된 근무**(진열마스터 ∪ 확정 행사)의 거래처만 후보로 삼는다.
 */
@Service
@Transactional(readOnly = true)
class MyAccountService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepositoryCustom,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepositoryCustom,
    private val featureToggleService: FeatureToggleService
) {

    fun getMyAccounts(userId: Long, keyword: String?, scope: MyAccountScope = MyAccountScope.FIELD): MyAccountListResponse {
        if (keyword != null && keyword.length == 1) {
            throw AccountInvalidParameterException("검색 키워드는 2자 이상이어야 합니다")
        }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // C형(매출 계열) 부서장: 일정이 잡힌 전체 거래처 (레거시 selectAllAccount)
        val isAllScheduled = scope == MyAccountScope.SALES && employee.role == AppAuthority.ACCOUNT_VIEW_ALL
        // 조장 중 레거시 person-specific 예외(yang_sfid): 팀장 기준 스케줄 거래처 (레거시 selectMyAccount 조장 분기)
        // 예외는 특정 조장 1명(sfid) 한정이라 지점장으로 확장하지 않는다.
        val isLeaderScheduleException = employee.role == AppAuthority.LEADER && employee.sfid == LEGACY_SCHEDULE_LEADER_SFID
        // 조장·지점장: 지점코드 + 거래처 그룹 1000/1010 (레거시 teamleaderAccList).
        // 레거시는 `eq '조장'` 정확 일치였으나 지점장을 조장과 동일 처리하도록 확장 ([AppAuthority.isTeamManager]).
        val isLeader = AppAuthority.isTeamManager(employee.role) && !isLeaderScheduleException
        // 주문서 작성 화면 + 기능 토글 활성: 거래처 후보를 오늘 확정된 근무(진열마스터 ∪ 행사)로 한정한다.
        // 토글이 비활성이면 ORDER 와 동일한 이전 동작(팀멤버스케줄 ∪ 진열)으로 되돌아간다.
        val isDisplayScheduleOnly = scope == MyAccountScope.ORDER_WRITE &&
            featureToggleService.isEnabled(FeatureFlag.ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY, userId)

        val accounts = when {
            // 전사 거래처는 수천 건이라 keyword 필터 + 상한을 DB 레벨로 푸시다운 (레거시 검색+페이지네이션 정합).
            isAllScheduled -> getAllScheduledAccounts(keyword)

            // yang 예외: 본인이 팀장으로 배정된 스케줄 거래처
            isLeaderScheduleException -> getLeaderScheduleAccounts(employee.id, scope, isDisplayScheduleOnly)

            // 조장 일반. 레거시 주문 셀렉터에서도 abctype 필터가 주석 처리되어 있어 ORDER 여도 분기 동일.
            // 진열 일정 축이 없는 경로라 ORDER_WRITE 전환의 영향도 받지 않는다.
            isLeader -> getLeaderAccounts(employee.costCenterCode)

            // 여사원/그 외(yang 예외 제외): 본인 팀멤버스케줄 기반 (레거시 selectMyAccount 여사원 분기)
            else -> getEmployeeAccounts(employee.id, scope, isDisplayScheduleOnly)
        }

        val filteredList = if (!keyword.isNullOrBlank()) {
            val lowerKeyword = keyword.lowercase()
            accounts.filter { account ->
                account.accountName.lowercase().contains(lowerKeyword) ||
                    account.accountCode.lowercase().contains(lowerKeyword)
            }
        } else {
            accounts
        }

        val sortedList = filteredList.sortedBy { it.accountName }

        return MyAccountListResponse(
            accounts = sortedList,
            totalCount = sortedList.size,
            meta = buildMeta(
                isAllScheduled = isAllScheduled,
                isLeader = isLeader,
                scope = scope,
                isDisplayScheduleOnly = isDisplayScheduleOnly
            )
        )
    }

    /**
     * 거래처 표시 기준 안내 문구 생성.
     *
     * 실제 거래처 조회에 사용한 권한·scope 분기와 동일한 기준으로 사용자 문구를 만든다(모바일 하드코딩 분기 대체).
     * yang 예외([isLeaderScheduleException])는 본인 담당 스케줄 기반이라 여사원과 동일 문구로 안내한다.
     */
    private fun buildMeta(
        isAllScheduled: Boolean,
        isLeader: Boolean,
        scope: MyAccountScope,
        isDisplayScheduleOnly: Boolean
    ): MyAccountMeta = when {
        // 부서장 매출 전체조회: 전사 거래처를 DB 검색(최대 ALL_ACCOUNTS_LIMIT 건)으로 노출
        isAllScheduled -> MyAccountMeta(
            criteriaLines = listOf("일정이 등록된 전체 거래처가 표시됩니다"),
            searchHint = "거래처명·코드로 검색하세요 (최대 ${ALL_ACCOUNTS_LIMIT}건 표시)"
        )

        // 조장·지점장: 소속 지점 거래처 (기간·주문가능유형 필터 없음)
        isLeader -> MyAccountMeta(
            criteriaLines = listOf("소속 지점의 거래처가 표시됩니다"),
            searchHint = DEFAULT_SEARCH_HINT
        )

        // 여사원/yang 예외 + 주문서 작성(오늘 확정 근무 기준): 진열·행사 확정 거래처 중 주문 가능 유형만
        isDisplayScheduleOnly -> MyAccountMeta(
            criteriaLines = listOf(
                "오늘 진열·행사 근무가 확정된 거래처",
                "그중 주문 가능한 거래처 유형만 표시됩니다"
            ),
            searchHint = DEFAULT_SEARCH_HINT
        )

        // 여사원/yang 예외 + 주문 화면: 담당·진열 거래처 중 주문 가능 유형만
        scope.isOrder -> MyAccountMeta(
            criteriaLines = listOf(
                "이번 달(전월 25일~당월 말일) 본인이 담당·진열하는 거래처",
                "그중 주문 가능한 거래처 유형만 표시됩니다"
            ),
            searchHint = DEFAULT_SEARCH_HINT
        )

        // 여사원/yang 예외 + 매출/현장 화면: 담당 거래처
        else -> MyAccountMeta(
            criteriaLines = listOf("이번 달(전월 25일~당월 말일) 본인이 담당하는 거래처"),
            searchHint = DEFAULT_SEARCH_HINT
        )
    }

    /**
     * 조장 거래처 조회: 조장 소속 지점의 거래처 그룹 1000/1010인 전체 거래처 (레거시 teamleaderAccList)
     */
    private fun getLeaderAccounts(costCenterCode: String?): List<MyAccountInfo> {
        if (costCenterCode.isNullOrBlank()) return emptyList()

        return accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
            branchCode = costCenterCode,
            accountGroups = listOf("1000", "1010"),
            isDeleted = true
        ).map { MyAccountInfo.from(it) }
    }

    /**
     * 일반 사원 거래처 조회: 본인 팀멤버스케줄 기반 (레거시 selectMyAccount 여사원 분기).
     * 주문 계열 scope 면 본인 진열 일정(레거시 selectDisplayMyAccount) union + abctype 필터를 적용한다.
     * [isDisplayScheduleOnly] 면 기간 기반 팀멤버스케줄을 조회하지 않고 오늘 확정된 근무(진열 ∪ 행사)만 후보로 삼는다.
     */
    private fun getEmployeeAccounts(
        userId: Long,
        scope: MyAccountScope,
        isDisplayScheduleOnly: Boolean
    ): List<MyAccountInfo> {
        if (isDisplayScheduleOnly) {
            return toAccounts(todayConfirmedWorkAccountIds(userId), scope)
        }

        val (fromDate, toDateExclusive) = scheduleDateRange()

        val scheduleAccountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(userId, fromDate, toDateExclusive)

        val accountIds = unionDisplayAccountsIfOrder(scheduleAccountIds, userId, scope, fromDate, toDateExclusive)

        return toAccounts(accountIds, scope)
    }

    /**
     * 조장(yang 예외) 거래처 조회: 본인이 팀장으로 배정된 팀멤버스케줄 기반 (레거시 selectMyAccount 조장 분기).
     * [MyAccountScope.ORDER] 면 본인 진열 일정 union + abctype 필터를 적용한다.
     *
     * 레거시 주문 셀렉터에서 yang 예외(leader != null)는 selectDisplayMyAccount 의 `fullname__c` 필터가
     * 빠져 전체 진열 거래처를 노출하나, 이는 1인 하드코딩 예외의 의도치 않은 동작으로 판단되어
     * 신규에서는 본인 진열 일정 기준으로 한정한다(전사 진열 스캔 회피).
     */
    private fun getLeaderScheduleAccounts(
        leaderId: Long,
        scope: MyAccountScope,
        isDisplayScheduleOnly: Boolean
    ): List<MyAccountInfo> {
        if (isDisplayScheduleOnly) {
            return toAccounts(todayConfirmedWorkAccountIds(leaderId), scope)
        }

        val (fromDate, toDateExclusive) = scheduleDateRange()

        val scheduleAccountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByTeamLeaderIdAndDateRange(leaderId, fromDate, toDateExclusive)

        val accountIds = unionDisplayAccountsIfOrder(scheduleAccountIds, leaderId, scope, fromDate, toDateExclusive)

        return toAccounts(accountIds, scope)
    }

    /**
     * 주문(ORDER) 화면 한정: 팀멤버스케줄 거래처에 본인 진열 일정 거래처(레거시 selectDisplayMyAccount)를 합친다.
     * 진열 confirmed 조건은 레거시 selectDisplayMyAccount 원문에 없어 적용하지 않는다(날짜범위·삭제여부만).
     */
    private fun unionDisplayAccountsIfOrder(
        scheduleAccountIds: List<Long>,
        employeeId: Long,
        scope: MyAccountScope,
        fromDate: LocalDate,
        toDateExclusive: LocalDate
    ): List<Long> {
        if (!scope.isOrder) return scheduleAccountIds

        val displayAccountIds = displayWorkScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId, fromDate, toDateExclusive)

        return (scheduleAccountIds + displayAccountIds).distinct()
    }

    /**
     * 주문서 작성 전용: 오늘 근무가 확정된 거래처 id — 진열 축 ∪ 행사 축.
     *
     * - 진열 축: 확정(confirmed) + 오늘이 기간 안인 진열마스터의 거래처.
     * - 행사 축: 관리자 "행사 확정" 으로 생성된 오늘자 행사 파생 TMS 의 거래처
     *   ([TeamMemberScheduleRepositoryCustom.findConfirmedPromotionAccountIdsByEmployeeAndDate]).
     *
     * 진열 축에서 팀멤버스케줄(TMS)을 쓰지 않는 이유는 진열 TMS 가 **출근등록 시점**에 생성되는 실적
     * 기록이라 작성 시점의 근무 예정을 담지 못하기 때문이다. 반면 행사 TMS 는 생성 시점이 달라
     * (행사 확정 시점, 근무일 이전) 그 자체가 확정된 근무 예정이므로 행사 축의 정본으로 쓴다.
     */
    private fun todayConfirmedWorkAccountIds(employeeId: Long): List<Long> {
        val today = LocalDate.now()
        val displayAccountIds = displayWorkScheduleRepository
            .findConfirmedValidAccountIdsByEmployeeAndDate(employeeId, today)
        val promotionAccountIds = teamMemberScheduleRepository
            .findConfirmedPromotionAccountIdsByEmployeeAndDate(employeeId, today)
        return (displayAccountIds + promotionAccountIds).distinct()
    }

    /**
     * 부서장(매출 계열) 거래처 조회: 일정이 잡힌 거래처 (레거시 selectAllAccount — 본인/기간 필터 없음).
     * 전사 거래처가 수천 건이므로 keyword 필터 + 상한([ALL_ACCOUNTS_LIMIT])을 DB 레벨에서 적용한다.
     */
    private fun getAllScheduledAccounts(keyword: String?): List<MyAccountInfo> {
        return teamMemberScheduleRepository
            .findDistinctScheduledAccounts(keyword, ALL_ACCOUNTS_LIMIT)
            .map { MyAccountInfo.from(it) }
    }

    /**
     * accountId 목록 → 거래처 DTO. 주문 계열 scope 면 주문가능 거래처유형(abctypecode) 필터를 적용한다
     * (레거시 selectMyAccount/selectDisplayMyAccount 의 `order=order` 분기).
     */
    private fun toAccounts(accountIds: List<Long>, scope: MyAccountScope): List<MyAccountInfo> {
        if (accountIds.isEmpty()) return emptyList()
        val accounts = accountRepository.findByIdInAndIsDeletedNot(accountIds, true)
        val filtered = if (scope.isOrder) {
            accounts.filter { it.isOrderableType() }
        } else {
            accounts
        }
        return filtered.map { MyAccountInfo.from(it) }
    }

    /**
     * 레거시 selectMyAccount 조회 기간: 전월 25일 ~ 당월 말일(inclusive).
     * 레포가 `goe(from)`/`lt(to)` 반열림이라 상한은 당월 말일 다음날(다음달 1일)을 exclusive 로 전달한다.
     */
    private fun scheduleDateRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        val fromDate = now.minusMonths(1).withDayOfMonth(25)
        val toDateExclusive = now.plusMonths(1).withDayOfMonth(1)
        return fromDate to toDateExclusive
    }

    companion object {
        // 레거시 label.properties `yang_sfid` — 조장이지만 거래처 조회만 팀장 스케줄 기반(selectMyAccount)으로
        // 우회하는 person-specific 예외 1인. 레거시 PromotionController/ProductController 등 다수 화면 동일 처리.
        private const val LEGACY_SCHEDULE_LEADER_SFID = "a0c1y0000005452AAA"

        // 부서장 전체조회 결과 상한 — 모바일 드롭다운 과대 응답(broken pipe) 방지. keyword 검색과 함께 사용.
        private const val ALL_ACCOUNTS_LIMIT = 100

        // 기본 검색 안내 — 이미 표시된 목록 내에서 클라이언트 검색하는 경우(부서장 전체조회 제외 전 분기).
        private const val DEFAULT_SEARCH_HINT = "검색은 표시된 목록 안에서 이름·코드로 찾습니다."
    }
}
