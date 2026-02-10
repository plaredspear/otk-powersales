package com.otoki.internal.service

import com.otoki.internal.dto.response.MyStoreInfo
import com.otoki.internal.dto.response.MyStoreListResponse
import com.otoki.internal.entity.Store
import com.otoki.internal.entity.StoreSchedule
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.repository.StoreRepository
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

/**
 * 내 거래처 서비스
 * 한 달 일정에 등록된 거래처 목록을 중복 제거하여 조회한다.
 */
@Service
class MyStoreService(
    private val userRepository: UserRepository,
    private val storeScheduleRepository: StoreScheduleRepository,
    private val storeRepository: StoreRepository
) {

    /**
     * 내 거래처 목록 조회
     *
     * 1. 현재 월(1일~말일) StoreSchedule에서 해당 사용자의 거래처 ID를 중복 제거하여 조회
     * 2. Store 마스터에서 대표자명, 전화번호 등 추가 정보 병합
     * 3. keyword가 있으면 거래처명/거래처코드로 필터링
     * 4. 거래처명 기준 오름차순 정렬
     */
    @Transactional(readOnly = true)
    fun getMyStores(userId: Long, keyword: String?): MyStoreListResponse {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException()
        }

        // 현재 월의 시작일과 종료일
        val now = LocalDate.now()
        val yearMonth = YearMonth.from(now)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 1. 월별 스케줄에서 중복 제거된 거래처 ID 조회
        val distinctStoreIds = storeScheduleRepository
            .findDistinctStoreIdsByUserIdAndScheduleDateBetween(userId, startDate, endDate)

        if (distinctStoreIds.isEmpty()) {
            return MyStoreListResponse(stores = emptyList(), totalCount = 0)
        }

        // 2. Store 마스터에서 추가 정보 조회
        val storeMap = storeRepository.findByIdIn(distinctStoreIds)
            .associateBy { it.id }

        // 3. StoreSchedule에서 기본 정보 조회 (Store 마스터에 없는 거래처용 fallback)
        val scheduleMap = storeScheduleRepository
            .findByUserIdAndScheduleDateBetween(userId, startDate, endDate)
            .distinctBy { it.storeId }
            .associateBy { it.storeId }

        // 4. Store 마스터 + StoreSchedule fallback 으로 정보 구성
        val storeInfoList = distinctStoreIds.mapNotNull { storeId ->
            buildMyStoreInfo(storeId, storeMap, scheduleMap)
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
     * Store 마스터가 있으면 대표자명/전화번호 포함,
     * 없으면 StoreSchedule의 기본 정보를 사용한다.
     */
    private fun buildMyStoreInfo(
        storeId: Long,
        storeMap: Map<Long, Store>,
        scheduleMap: Map<Long, StoreSchedule>
    ): MyStoreInfo? {
        val storeMaster = storeMap[storeId]
        val schedule = scheduleMap[storeId]

        return when {
            storeMaster != null -> MyStoreInfo(
                storeId = storeMaster.id,
                storeName = storeMaster.storeName,
                storeCode = storeMaster.storeCode,
                address = storeMaster.address,
                representativeName = storeMaster.representativeName,
                phoneNumber = storeMaster.phoneNumber
            )
            schedule != null -> MyStoreInfo(
                storeId = schedule.storeId,
                storeName = schedule.storeName,
                storeCode = schedule.storeCode,
                address = schedule.address,
                representativeName = null,
                phoneNumber = null
            )
            else -> null
        }
    }
}
