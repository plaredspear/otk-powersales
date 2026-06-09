package com.otoki.powersales.common.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.common.dto.response.MyAccountInfo
import com.otoki.powersales.common.dto.response.MyAccountListResponse
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.AccountInvalidParameterException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.powersales.employee.repository.EmployeeRepository
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
 * 진열스케줄(displayWorkSchedule) union 은 레거시 주문/범용 셀렉터(`accountSelectList`)에만 존재하며,
 * 본 endpoint 를 사용하는 매출/현장 화면(7종)에는 없으므로 합치지 않는다.
 */
@Service
@Transactional(readOnly = true)
class MyAccountService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepositoryCustom
) {

    fun getMyAccounts(userId: Long, keyword: String?, scope: MyAccountScope = MyAccountScope.FIELD): MyAccountListResponse {
        if (keyword != null && keyword.length == 1) {
            throw AccountInvalidParameterException("검색 키워드는 2자 이상이어야 합니다")
        }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val accounts = when {
            // C형(매출 계열) 부서장: 일정이 잡힌 전체 거래처 (레거시 selectAllAccount)
            // 전사 거래처는 수천 건이라 keyword 필터 + 상한을 DB 레벨로 푸시다운 (레거시 검색+페이지네이션 정합).
            scope == MyAccountScope.SALES && employee.role == AppAuthority.ACCOUNT_VIEW_ALL ->
                getAllScheduledAccounts(keyword)

            // 조장 중 레거시 person-specific 예외(yang_sfid): 팀장 기준 스케줄 거래처 (레거시 selectMyAccount 조장 분기)
            employee.role == AppAuthority.LEADER && employee.sfid == LEGACY_SCHEDULE_LEADER_SFID ->
                getLeaderScheduleAccounts(employee.id)

            // 조장 일반: 지점코드 + 거래처 그룹 1000/1010 (레거시 teamleaderAccList)
            employee.role == AppAuthority.LEADER ->
                getLeaderAccounts(employee.costCenterCode)

            // 여사원/그 외: 본인 팀멤버스케줄 기반 (레거시 selectMyAccount 여사원 분기)
            else ->
                getEmployeeAccounts(employee.id)
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
            stores = sortedList,
            totalCount = sortedList.size
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
     * 일반 사원 거래처 조회: 본인 팀멤버스케줄 기반 (레거시 selectMyAccount 여사원 분기 — 진열 union 없음)
     */
    private fun getEmployeeAccounts(userId: Long): List<MyAccountInfo> {
        val (fromDate, toDateExclusive) = scheduleDateRange()

        val accountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(userId, fromDate, toDateExclusive)

        return toAccounts(accountIds)
    }

    /**
     * 조장(yang 예외) 거래처 조회: 본인이 팀장으로 배정된 팀멤버스케줄 기반 (레거시 selectMyAccount 조장 분기)
     */
    private fun getLeaderScheduleAccounts(leaderId: Long): List<MyAccountInfo> {
        val (fromDate, toDateExclusive) = scheduleDateRange()

        val accountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByTeamLeaderIdAndDateRange(leaderId, fromDate, toDateExclusive)

        return toAccounts(accountIds)
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

    private fun toAccounts(accountIds: List<Long>): List<MyAccountInfo> {
        if (accountIds.isEmpty()) return emptyList()
        return accountRepository.findByIdInAndIsDeletedNot(accountIds, true)
            .map { MyAccountInfo.from(it) }
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
    }
}
