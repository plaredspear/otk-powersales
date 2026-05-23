package com.otoki.powersales.auth.permission

import com.otoki.powersales.auth.entity.Profile
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * Spec #805 — 12종 Profile + ProfileFlags 부팅 sync.
 *
 * CLAUDE.md 의 reference data 정책 정합 (Flyway 가 아니라 Kotlin SoT + ApplicationRunner).
 *
 * ## 책임
 * - 12종 Profile (name 기준) 부재 시 INSERT
 * - 각 Profile 의 ProfileFlags row 부재 시 INSERT (DEFAULT 비트는 모두 false)
 * - **"시스템 관리자" Profile 의 ProfileFlags 5비트 모두 TRUE 보장** — 회수된 비트는 강제 복구
 * - 그 외 Profile 의 비트는 기존 값 보존 (운영자 web admin 변경 손대지 않음)
 *
 * ## SoT
 * - [SystemAdminProfilePolicy] 의 `REQUIRED_PROFILE_NAMES` 12종 + `SYSTEM_ADMIN_PROFILE_NAME` 1개
 * - 운영 정책 변경 시 본 SoT 만 갱신하면 모든 환경 부팅 시 자동 sync
 *
 * ## 실행 조건
 * - 모든 profile (local/dev/prod) 에서 동작 — test profile 은 Spring Boot 의 ApplicationRunner 자동 제외 정책 활용
 *
 * ## 위험
 * - 운영자가 web admin 으로 "시스템 관리자" Profile 의 비트를 FALSE 로 변경해도 다음 부팅에 자동 TRUE 복구.
 *   본 정책은 명시적 invariant — 운영자 가이드 의무.
 */
@Component
class ProfileBootstrapRunner(
    private val profileRepository: ProfileRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        transactionTemplate.executeWithoutResult {
            syncProfiles()
            syncSystemAdminFlags()
        }
    }

    private fun syncProfiles() {
        for (name in SystemAdminProfilePolicy.REQUIRED_PROFILE_NAMES) {
            val profile = profileRepository.findByName(name)
                ?: profileRepository.save(Profile(name = name))
            val flags = profileFlagsRepository.findByProfileId(profile.id)
            if (flags == null) {
                profileFlagsRepository.save(ProfileFlags(profileId = profile.id))
                log.info("[ProfileBootstrapRunner] ProfileFlags INSERT — profile.name='{}' id={}", name, profile.id)
            }
        }
    }

    private fun syncSystemAdminFlags() {
        val profile = profileRepository.findByName(SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
            ?: run {
                log.error("[ProfileBootstrapRunner] '시스템 관리자' Profile 부재 — sync skip (이 상태는 syncProfiles() 이후 발생 불가)")
                return
            }
        val flags = profileFlagsRepository.findByProfileId(profile.id) ?: ProfileFlags(profileId = profile.id)

        val needsUpdate = !flags.permissionsViewAllData || !flags.permissionsModifyAllData ||
            !flags.permissionsViewAllUsers || !flags.permissionsManageUsers || !flags.permissionsApiEnabled

        if (!needsUpdate) return

        flags.permissionsViewAllData = true
        flags.permissionsModifyAllData = true
        flags.permissionsViewAllUsers = true
        flags.permissionsManageUsers = true
        flags.permissionsApiEnabled = true
        profileFlagsRepository.save(flags)
        log.info(
            "[ProfileBootstrapRunner] '시스템 관리자' ProfileFlags 5 비트 모두 TRUE 보장 — profile.id={}",
            profile.id,
        )
    }
}

/**
 * 12종 Profile 의 SoT + UserRoleEnum → Profile.name 매핑.
 *
 * Profile.name (한글) 이 권한 모델 입력 SoT — ROLE_ 산출 / EmployeeProfileResolver / UserProvisioningService 가 본 매핑을 공유.
 */
object SystemAdminProfilePolicy {

    const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"

    /** 12종 Profile 의 SoT. 부팅 시 부재 시 자동 생성. */
    val REQUIRED_PROFILE_NAMES: List<String> = listOf(
        SYSTEM_ADMIN_PROFILE_NAME,
        "8.마케팅",
        "9. Staff",
        "6.조장",
        "4.지점장",
        "3.영업부장",
        "2.사업부장",
        "1.본부장",
        "5.영업사원",
        "7.영업사원 + 조장",
        "공장관계자",
        "OLS",
    )

    /**
     * UserRoleEnum → Profile.name 매핑 — Provisioning / Seed 시점 profileId 결정.
     */
    fun profileNameForRole(role: UserRoleEnum?): String = when (role) {
        UserRoleEnum.SYSTEM_ADMIN -> SYSTEM_ADMIN_PROFILE_NAME
        UserRoleEnum.LEADER -> "6.조장"
        UserRoleEnum.BRANCH_MANAGER -> "4.지점장"
        UserRoleEnum.SALES_MANAGER -> "3.영업부장"
        UserRoleEnum.BUSINESS_MANAGER -> "2.사업부장"
        UserRoleEnum.HEADQUARTERS_MANAGER -> "1.본부장"
        UserRoleEnum.SALES_SUPPORT -> "9. Staff"
        UserRoleEnum.WOMAN, UserRoleEnum.ACCOUNT_VIEW_ALL, UserRoleEnum.UNKNOWN, null -> "5.영업사원"
    }
}
