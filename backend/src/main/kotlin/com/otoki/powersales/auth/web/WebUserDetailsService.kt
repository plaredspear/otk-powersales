package com.otoki.powersales.auth.web

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Web 로그인용 UserDetailsService (Spec #760).
 *
 * ## 레거시 매핑
 * - SF Apex: SF 표준 인증 (사용자가 Salesforce UI 에서 직접 로그인) — 신규 backend 는 SF org 외부 동작이라 자체 인증 모델 필요
 * - origin spec: #760
 *
 * ## 레거시 동작 요약
 * 1. 입력: SF Username (`DKRetail__Email__c`, Sandbox 시 접미사 처리)
 * 2. 비밀번호: `System.resetPassword(...)` (주석 처리됨) — 신규 User 는 비밀번호 미설정 상태
 * 3. SF 플랫폼 표준 로그인 화면 / 인증 처리
 * 4. 부수 효과: SF 로그인 이력 (LoginHistory) 자동 적재
 *
 * ## 신규 차이
 * - 인증 entity: SF 플랫폼 User → backend `user` 테이블 (BCrypt + DB self-managed). 참조: legacy-deviation.md §"인증·세션"
 * - 권한 산출: SF Profile + UserRole 직접 참조 → `ProfileType` + `is_sales_support` 캐시 컬럼 기반 (Spec #759). §2.3 매핑 표 적용
 */
@Service
class WebUserDetailsService(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val adminPermissionResolver: AdminPermissionResolver,
) : UserDetailsService {

    /**
     * Username 으로 User 조회 + UserDetails 빌드.
     *
     * 분기:
     * - User 부재 → `UsernameNotFoundException` (Spring Security 가 자동 변환 → 인증 실패)
     * - `is_active == false` → principal 반환 시 `isEnabled = false` 로 표시 (DaoAuthenticationProvider 가 자동 차단)
     *
     * 권한: `ProfileType` → `ROLE_*` 매핑 + `is_sales_support == true` → `ROLE_SALES_SUPPORT` 추가.
     *
     * Employee snapshot (`employeeId`, `role`, `costCenterCode`) 는 admin context 분기에 사용 — Employee 미존재
     * (예: ADMIN-* 부트스트랩) 시 null fallback.
     */
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): WebUserPrincipal {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $username")
        // employeeCode 부재 user (SF 시스템 user / 부서 공용 계정) 는 Employee 매칭 자체 skip.
        val employee = user.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        return toPrincipal(user, employee)
    }

    private fun toPrincipal(user: User, employee: com.otoki.powersales.employee.entity.Employee?): WebUserPrincipal {
        val permissions: Set<AdminPermission> = employee?.let { adminPermissionResolver.resolve(it) } ?: emptySet()
        return WebUserPrincipal(
            userId = user.id,
            usernameValue = user.username,
            employeeCode = user.employeeCode,
            employeeId = employee?.id,
            role = employee?.role,
            costCenterCode = employee?.costCenterCode,
            profileType = user.profileType,
            isSalesSupport = user.isSalesSupport ?: false,
            passwordChangeRequired = user.passwordChangeRequired ?: true,
            permissions = permissions,
            encodedPassword = user.password,
            grantedAuthorities = resolveAuthorities(user.profileType, user.isSalesSupport ?: false),
            active = user.isActive
        )
    }

    companion object {
        /**
         * Spec #760 §2.3 — ProfileType + is_sales_support 기반 GrantedAuthority 산출.
         *
         * 1차 매핑만 정의. 세부 화면/API 권한 분기는 후속 spec.
         */
        fun resolveAuthorities(profileType: ProfileType, isSalesSupport: Boolean): List<GrantedAuthority> {
            val role = profileType.toRoleAuthority()
            return buildList {
                add(SimpleGrantedAuthority(role))
                if (isSalesSupport) add(SimpleGrantedAuthority("ROLE_SALES_SUPPORT"))
            }
        }

        private fun ProfileType.toRoleAuthority(): String = when (this) {
            ProfileType.SYSTEM_ADMIN -> "ROLE_ADMIN"
            ProfileType.MARKETING -> "ROLE_MARKETING"
            ProfileType.STAFF -> "ROLE_STAFF"
            ProfileType.BUSINESS_DIRECTOR, ProfileType.DIVISION_HEAD -> "ROLE_DIRECTOR"
            ProfileType.BRANCH_MANAGER, ProfileType.SALES_MANAGER -> "ROLE_MANAGER"
            ProfileType.TEAM_LEADER, ProfileType.SALES_REP_LEADER -> "ROLE_LEADER"
            ProfileType.SALES_REP -> "ROLE_SALES_REP"
            ProfileType.FACTORY_STAFF -> "ROLE_FACTORY"
            ProfileType.OLS -> "ROLE_OLS"
        }
    }
}
