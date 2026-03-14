package com.otoki.internal.schedule.service

import com.otoki.internal.common.dto.response.StoreInfo
import com.otoki.internal.common.dto.response.StoreListResponse
import com.otoki.internal.common.util.GeoUtils
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.schedule.dto.response.CommuteResponse
import com.otoki.internal.schedule.dto.response.CommuteStatusItem
import com.otoki.internal.schedule.dto.response.CommuteStatusResponse
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.exception.AlreadyRegisteredException
import com.otoki.internal.schedule.exception.DistanceExceededException
import com.otoki.internal.schedule.exception.TeamMemberScheduleNotFoundException
import com.otoki.internal.schedule.integration.OroraApiService
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val userRepository: UserRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val accountRepository: AccountRepository,
    private val ororaApiService: OroraApiService
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val DEFAULT_ALLOWED_DISTANCE_KM = 0.5

        /** 대리점 유형코드 면제 목록 — GPS 거리 검증 생략 */
        private val EXEMPT_STORE_TYPE_CODES = setOf(
            "1110", "1120", "1130", "1140",
            "1210", "1220",
            "1510", "1530",
            "1810", "1900"
        )
    }

    /**
     * 오늘 출근 거래처 목록 조회
     */
    fun getStoreList(userId: Long, keyword: String?): StoreListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()
        val userSfid = user.sfid ?: ""

        // 오늘 스케줄 조회
        val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userSfid, today)

        // Account 정보 batch fetch
        val accountSfids = teamMemberSchedules.mapNotNull { it.accountId }.distinct()
        val accountMap = if (accountSfids.isNotEmpty()) {
            accountRepository.findBySfidIn(accountSfids).associateBy { it.sfid }
        } else {
            emptyMap()
        }

        // DTO 변환 + 키워드 필터링
        val storeInfos = teamMemberSchedules.mapNotNull { teamMemberSchedule ->
            val account = teamMemberSchedule.accountId?.let { accountMap[it] }
            val storeName = account?.name ?: ""

            // 키워드 필터링
            if (!keyword.isNullOrBlank()) {
                val lowerKeyword = keyword.lowercase()
                if (!storeName.lowercase().contains(lowerKeyword)) return@mapNotNull null
            }

            StoreInfo(
                scheduleSfid = teamMemberSchedule.sfid ?: "",
                storeSfid = teamMemberSchedule.accountId,
                storeName = storeName,
                storeTypeCode = account?.abcTypeCode,
                workCategory = teamMemberSchedule.workingCategory1 ?: "",
                address = account?.address1,
                latitude = account?.latitude?.toDoubleOrNull(),
                longitude = account?.longitude?.toDoubleOrNull(),
                isRegistered = teamMemberSchedule.commuteLogId != null
            )
        }

        val registeredCount = storeInfos.count { it.isRegistered }

        return StoreListResponse(
            stores = storeInfos,
            totalCount = storeInfos.size,
            registeredCount = registeredCount,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * 출근 등록
     *
     * 1. 스케줄 조회 + 중복 검증
     * 2. GPS 거리 검증 (면제 코드 확인)
     * 3. Orora WorkReport 전송 (Mock)
     * 4. 응답 반환
     */
    @Transactional
    fun registerCommute(userId: Long, teamMemberScheduleSfid: String, latitude: Double, longitude: Double, workType: String?): CommuteResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        // 1. 스케줄 조회
        val teamMemberSchedule = teamMemberScheduleRepository.findBySfid(teamMemberScheduleSfid)
            ?: throw TeamMemberScheduleNotFoundException()

        // 2. 중복 등록 검증
        if (teamMemberSchedule.commuteLogId != null) {
            throw AlreadyRegisteredException()
        }

        // 3. 거래처 정보 조회 + GPS 거리 검증
        val account = teamMemberSchedule.accountId?.let { accountRepository.findBySfid(it) }
        val distanceKm = validateDistance(latitude, longitude, account)

        // 4. Orora WorkReport 전송
        ororaApiService.sendWorkReport(teamMemberScheduleSfid)

        // 5. 출근 현황 집계 (commuteLogId 업데이트 후)
        val userSfid = user.sfid ?: ""
        val today = LocalDate.now()
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userSfid, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.commuteLogId != null || it.sfid == teamMemberScheduleSfid }

        return CommuteResponse(
            teamMemberScheduleSfid = teamMemberScheduleSfid,
            storeName = account?.name ?: "",
            workType = workType ?: teamMemberSchedule.workingType,
            distanceKm = distanceKm,
            totalCount = totalCount,
            registeredCount = registeredCount
        )
    }

    /**
     * 출근 현황 조회
     */
    fun getCommuteStatus(userId: Long): CommuteStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()
        val userSfid = user.sfid ?: ""

        val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userSfid, today)

        // Account 정보 batch fetch
        val accountSfids = teamMemberSchedules.mapNotNull { it.accountId }.distinct()
        val accountMap = if (accountSfids.isNotEmpty()) {
            accountRepository.findBySfidIn(accountSfids).associateBy { it.sfid }
        } else {
            emptyMap()
        }

        val statusList = teamMemberSchedules.map { teamMemberSchedule ->
            val account = teamMemberSchedule.accountId?.let { accountMap[it] }
            CommuteStatusItem(
                scheduleSfid = teamMemberSchedule.sfid ?: "",
                storeName = account?.name ?: "",
                workCategory = teamMemberSchedule.workingCategory1 ?: "",
                status = if (teamMemberSchedule.commuteLogId != null) "REGISTERED" else "PENDING"
            )
        }

        val registeredCount = statusList.count { it.status == "REGISTERED" }

        return CommuteStatusResponse(
            totalCount = statusList.size,
            registeredCount = registeredCount,
            statusList = statusList,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * GPS 거리 검증
     * @return 계산된 거리 (km). 면제 시 0.0
     */
    private fun validateDistance(userLat: Double, userLon: Double, account: Account?): Double {
        // 면제 코드 확인
        val storeTypeCode = account?.abcTypeCode
        if (storeTypeCode != null && storeTypeCode in EXEMPT_STORE_TYPE_CODES) {
            return 0.0
        }

        // 거래처 위경도 확인
        val storeLat = account?.latitude?.toDoubleOrNull() ?: return 0.0
        val storeLon = account.longitude?.toDoubleOrNull() ?: return 0.0

        // Haversine 거리 계산
        val distance = GeoUtils.calculateDistance(userLat, userLon, storeLat, storeLon)

        // 허용 거리 비교 (commute_distance 테이블 미구현 → 기본값 사용)
        val allowedDistance = DEFAULT_ALLOWED_DISTANCE_KM

        if (distance > allowedDistance) {
            throw DistanceExceededException(distance)
        }

        return distance
    }
}
