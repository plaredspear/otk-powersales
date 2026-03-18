package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.MyAccountInfo
import com.otoki.internal.common.dto.response.MyAccountListResponse
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

/**
 * 내 거래처 서비스
 * 한 달 일정에 등록된 거래처 목록을 중복 제거하여 조회한다.
 */
@Service
class MyAccountService(
    private val userRepository: UserRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * 내 거래처 목록 조회
     *
     * 1. 현재 월(1일~말일) 스케줄에서 해당 사용자의 거래처(accountId)를 중복 제거하여 조회
     * 2. Account 마스터에서 id로 추가 정보 병합
     * 3. keyword가 있으면 거래처명/거래처코드로 필터링
     * 4. 거래처명 기준 오름차순 정렬
     */
    @Transactional(readOnly = true)
    fun getMyAccounts(userId: Long, keyword: String?): MyAccountListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        val employeeId = user.employeeId

        // 현재 월의 시작일과 종료일
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 1. 월별 스케줄에서 중복 제거된 거래처 accountId 조회
        val distinctAccountIds = displayWorkScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndStartDateBetween(employeeId, startDate, endDate)

        if (distinctAccountIds.isEmpty()) {
            return MyAccountListResponse(stores = emptyList(), totalCount = 0)
        }

        // 2. Account 마스터에서 id 기반 추가 정보 조회
        val accountMap = accountRepository.findByIdIn(distinctAccountIds)
            .associateBy { it.id }

        // 3. DisplayWorkSchedule에서 기본 정보 조회 (Account 마스터에 없는 거래처용 fallback)
        val scheduleMap = displayWorkScheduleRepository
            .findByEmployeeIdAndStartDateBetween(employeeId, startDate, endDate)
            .distinctBy { it.accountId }
            .associateBy { it.accountId }

        // 4. Account 마스터 + DisplayWorkSchedule fallback 으로 정보 구성
        val accountInfoList = distinctAccountIds.mapNotNull { accountId ->
            buildMyAccountInfo(accountId, accountMap, scheduleMap)
        }

        // 5. keyword 필터링 (대소문자 무시)
        val filteredList = if (!keyword.isNullOrBlank()) {
            val lowerKeyword = keyword.lowercase()
            accountInfoList.filter { account ->
                account.accountName.lowercase().contains(lowerKeyword) ||
                    account.accountCode.lowercase().contains(lowerKeyword)
            }
        } else {
            accountInfoList
        }

        // 6. 거래처명 기준 오름차순 정렬
        val sortedList = filteredList.sortedBy { it.accountName }

        return MyAccountListResponse(
            stores = sortedList,
            totalCount = sortedList.size
        )
    }

    /**
     * 개별 거래처 정보를 구성한다.
     * Account 마스터가 있으면 대표자명/전화번호 포함,
     * 없으면 스케줄의 기본 정보를 사용한다.
     */
    private fun buildMyAccountInfo(
        accountId: Int,
        accountMap: Map<Int, Account>,
        scheduleMap: Map<Int?, DisplayWorkSchedule>
    ): MyAccountInfo? {
        val account = accountMap[accountId]
        val schedule = scheduleMap[accountId]

        return when {
            account != null -> MyAccountInfo(
                accountId = account.id.toLong(),
                accountName = account.name ?: "",
                accountCode = account.externalKey ?: "",
                address = account.address1,
                representativeName = account.representative,
                phoneNumber = account.phone
            )
            schedule != null -> MyAccountInfo(
                accountId = 0L,
                accountName = "",
                accountCode = "",
                address = null,
                representativeName = null,
                phoneNumber = null
            )
            else -> null
        }
    }
}
