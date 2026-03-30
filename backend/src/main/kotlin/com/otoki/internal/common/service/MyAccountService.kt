package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.MyAccountInfo
import com.otoki.internal.common.dto.response.MyAccountListResponse
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.common.exception.AccountInvalidParameterException
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepositoryCustom
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.internal.sap.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 내 거래처 서비스
 * 사원 권한(일반사원/조장)에 따라 다른 조회 로직을 적용한다.
 */
@Service
@Transactional(readOnly = true)
class MyAccountService(
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepositoryCustom,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepositoryCustom
) {

    fun getMyAccounts(userId: Long, keyword: String?): MyAccountListResponse {
        if (keyword != null && keyword.length == 1) {
            throw AccountInvalidParameterException("검색 키워드는 2자 이상이어야 합니다")
        }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val accounts = if (employee.appAuthority == "조장") {
            getLeaderAccounts(employee.costCenterCode)
        } else {
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
     * 조장 거래처 조회: 조장 소속 지점의 거래처 그룹 1000/1010인 전체 거래처
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
     * 일반 사원 거래처 조회: 팀멤버스케줄 + 진열스케줄 기반 중복 제거
     */
    private fun getEmployeeAccounts(userId: Long): List<MyAccountInfo> {
        val now = LocalDate.now()
        val fromDate = now.minusMonths(1).withDayOfMonth(25)
        val toDate = now.plusMonths(1).withDayOfMonth(now.plusMonths(1).lengthOfMonth())

        // 팀멤버스케줄 기반 거래처 ID (userId PK로 조회)
        val teamAccountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(userId, fromDate, toDate)

        // 진열스케줄 기반 거래처 ID (userId PK로 조회)
        val displayAccountIds = displayWorkScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(userId, fromDate, toDate)

        // 합집합 (중복 제거)
        val mergedAccountIds = (teamAccountIds + displayAccountIds).distinct()

        if (mergedAccountIds.isEmpty()) return emptyList()

        return accountRepository.findByIdInAndIsDeletedNot(mergedAccountIds, true)
            .map { MyAccountInfo.from(it) }
    }
}
