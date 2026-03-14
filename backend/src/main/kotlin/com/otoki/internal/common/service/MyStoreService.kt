package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.MyStoreInfo
import com.otoki.internal.common.dto.response.MyStoreListResponse
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
 *
 * Note: V1 리매핑으로 StoreSchedule의 userId(Long)→fullName(String sfid),
 * storeId(Long)→account(String sfid) 변경. storeName/storeCode/address 삭제됨.
 * Account 마스터 조회는 sfid 기반으로 전환 필요 (후속 스펙).
 */
@Service
class MyStoreService(
    private val userRepository: UserRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val accountRepository: AccountRepository
) {

    /**
     * 내 거래처 목록 조회
     *
     * 1. 현재 월(1일~말일) StoreSchedule에서 해당 사용자의 거래처(account sfid)를 중복 제거하여 조회
     * 2. Account 마스터에서 sfid로 추가 정보 병합
     * 3. keyword가 있으면 거래처명/거래처코드로 필터링
     * 4. 거래처명 기준 오름차순 정렬
     */
    @Transactional(readOnly = true)
    fun getMyStores(userId: Long, keyword: String?): MyStoreListResponse {
        // 사용자 존재 확인 및 sfid 조회
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        val userSfid = user.sfid ?: ""

        // 현재 월의 시작일과 종료일
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 1. 월별 스케줄에서 중복 제거된 거래처 account(sfid) 조회
        val distinctAccountSfids = displayWorkScheduleRepository
            .findDistinctAccountsByFullNameAndStartDateBetween(userSfid, startDate, endDate)

        if (distinctAccountSfids.isEmpty()) {
            return MyStoreListResponse(stores = emptyList(), totalCount = 0)
        }

        // 2. Account 마스터에서 sfid 기반 추가 정보 조회
        val accountMap = accountRepository.findAll()
            .filter { it.sfid != null && it.sfid in distinctAccountSfids }
            .associateBy { it.sfid }

        // 3. DisplayWorkSchedule에서 기본 정보 조회 (Account 마스터에 없는 거래처용 fallback)
        val scheduleMap = displayWorkScheduleRepository
            .findByFullNameAndStartDateBetween(userSfid, startDate, endDate)
            .distinctBy { it.account }
            .associateBy { it.account }

        // 4. Account 마스터 + DisplayWorkSchedule fallback 으로 정보 구성
        val storeInfoList = distinctAccountSfids.mapNotNull { accountSfid ->
            buildMyStoreInfo(accountSfid, accountMap, scheduleMap)
        }

        // 5. keyword 필터링 (대소문자 무시)
        val filteredList = if (!keyword.isNullOrBlank()) {
            val lowerKeyword = keyword.lowercase()
            storeInfoList.filter { store ->
                store.storeName.lowercase().contains(lowerKeyword) ||
                    store.storeCode.lowercase().contains(lowerKeyword)
            }
        } else {
            storeInfoList
        }

        // 6. 거래처명 기준 오름차순 정렬
        val sortedList = filteredList.sortedBy { it.storeName }

        return MyStoreListResponse(
            stores = sortedList,
            totalCount = sortedList.size
        )
    }

    /**
     * 개별 거래처 정보를 구성한다.
     * Account 마스터가 있으면 대표자명/전화번호 포함,
     * 없으면 StoreSchedule의 기본 정보를 사용한다.
     *
     * Note: V1 리매핑으로 StoreSchedule에서 storeName/storeCode/address 삭제됨.
     * Account 마스터를 sfid로 조회하여 정보 구성. fallback 시 빈 문자열.
     */
    private fun buildMyStoreInfo(
        accountSfid: String,
        accountMap: Map<String?, Account>,
        scheduleMap: Map<String?, DisplayWorkSchedule>
    ): MyStoreInfo? {
        val account = accountMap[accountSfid]
        val schedule = scheduleMap[accountSfid]

        return when {
            account != null -> MyStoreInfo(
                storeId = account.id.toLong(),
                storeName = account.name ?: "",
                storeCode = account.externalKey ?: "",
                address = account.address1,
                representativeName = account.representative,
                phoneNumber = account.phone
            )
            schedule != null -> MyStoreInfo(
                storeId = 0L,  // V1: account(String sfid)로 변경됨, DTO Long 타입 유지
                storeName = "",  // V1에서 storeName 삭제됨
                storeCode = "",  // V1에서 storeCode 삭제됨
                address = null,  // V1에서 address 삭제됨
                representativeName = null,
                phoneNumber = null
            )
            else -> null
        }
    }
}
