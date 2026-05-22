package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.dto.ProfileFlagsSnapshot
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Profile system 권한 비트 평가 Service (spec #782 P2-B).
 *
 * User.profile_id → ProfileFlags 의 5 비트 (`viewAllData` / `modifyAllData` / `viewAllUsers` /
 * `manageUsers` / `apiEnabled`) 조회.
 *
 * Profile entity 는 #780 산출 — system 권한 비트는 별도 ProfileFlags 테이블로 외부화 (Profile
 * entity 의 사실상 immutable 성격 유지 + 운영 정책 변경 시 ProfileFlags 만 갱신).
 *
 * sharing policy evaluator 의 권한 매트릭스 최우선 분기에 사용 — Hierarchy / sharingRule 평가 우회.
 */
@Service
class ProfileFlagsEvaluator(
    private val userRepository: UserRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
) {

    @Cacheable(value = ["profileFlags"], key = "#userId")
    fun getProfileFlags(userId: Long): ProfileFlagsSnapshot {
        val user = userRepository.findById(userId).orElse(null) ?: return ProfileFlagsSnapshot.NONE
        val profileId = user.profileId ?: return ProfileFlagsSnapshot.NONE
        val flags = profileFlagsRepository.findByProfileId(profileId) ?: return ProfileFlagsSnapshot.NONE
        return ProfileFlagsSnapshot(
            viewAllData = flags.permissionsViewAllData,
            modifyAllData = flags.permissionsModifyAllData,
            viewAllUsers = flags.permissionsViewAllUsers,
            manageUsers = flags.permissionsManageUsers,
            apiEnabled = flags.permissionsApiEnabled,
        )
    }

    fun hasViewAllData(userId: Long): Boolean = getProfileFlags(userId).viewAllData

    fun hasModifyAllData(userId: Long): Boolean = getProfileFlags(userId).modifyAllData
}
