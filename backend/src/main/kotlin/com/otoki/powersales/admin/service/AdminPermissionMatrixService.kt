package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.*
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPermissionMatrixService(
    private val employeeRepository: EmployeeRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val adminPermissionResolver: AdminPermissionResolver
) {

    private val permissionInfoMap: Map<AdminPermission, Pair<String, List<String>>> = mapOf(
        AdminPermission.DASHBOARD_READ to ("대시보드 조회" to listOf("대시보드")),
        AdminPermission.EMPLOYEE_READ to ("사원 조회" to listOf("인사/근무 > 여사원 현황")),
        AdminPermission.EMPLOYEE_WRITE to ("사원 등록/수정" to listOf("인사/근무 > 여사원 현황 (신규 등록 / 정보 수정)")),
        AdminPermission.EMPLOYEE_RESET_CREDENTIALS to ("사원 자격 정보 리셋" to listOf("인사/근무 > 여사원 현황 (비밀번호 초기화 / 기기 재설정)")),
        AdminPermission.ACCOUNT_READ to ("거래처 조회" to listOf("기준정보 > 거래처")),
        AdminPermission.ACCOUNT_WRITE to ("거래처 등록/수정" to listOf("기준정보 > 거래처 (신규 등록 / 수정)")),
        AdminPermission.ACCOUNT_DELETE to ("거래처 삭제" to listOf("기준정보 > 거래처 (삭제)")),
        AdminPermission.PROMOTION_READ to ("행사 조회" to listOf("행사/배치 > 행사마스터", "행사/배치 > 전문행사조")),
        AdminPermission.PROMOTION_WRITE to ("행사 생성/수정" to listOf("행사/배치 > 진열스케줄마스터", "행사 등록/수정/확정/삭제")),
        AdminPermission.SAFETY_CHECK_READ to ("안전점검 조회" to listOf("현장 점검/이슈 > 안전점검")),
        AdminPermission.SCHEDULE_READ to ("일정 조회" to listOf("여사원 일정 > 여사원 일정관리", "여사원 일정 > 월별여사원 통합일정", "여사원 일정 > 근무형태별 인원현황")),
        AdminPermission.SCHEDULE_WRITE to ("일정 생성/수정" to listOf("행사/배치 > 진열스케줄마스터 일정 확정")),
        AdminPermission.SALES_COMPARISON_READ to ("거래처별 진열사원 배치적합성 조회" to listOf("거래처별 진열사원 배치적합성")),
        AdminPermission.MONTHLY_INPUT_ADEQUACY_READ to ("월별 진열사원 투입적합성 조회" to listOf("여사원 일정 > 월별 진열사원 투입적합성")),
        AdminPermission.PRODUCT_EXPIRATION_READ to ("유통기한 조회" to listOf("현장 점검/이슈 > 유통기한 관리")),
        AdminPermission.PRODUCT_EXPIRATION_WRITE to ("유통기한 등록/수정" to listOf("현장 점검/이슈 > 유통기한 관리 (등록 / 수정 / 삭제)")),
        AdminPermission.NAVER_GEOCODE_TEST to ("Naver Geocode 변환 테스트" to listOf("운영 도구 > Naver Geocode 변환 테스트")),
        AdminPermission.SCHEDULED_JOB_READ to ("스케줄 잡 실행 이력 조회" to listOf("운영 도구 > 스케줄 잡 실행 이력")),
        AdminPermission.AGREEMENT_READ to ("동의 약관 조회" to listOf("시스템 > 동의 약관 등록")),
        AdminPermission.AGREEMENT_WRITE to ("동의 약관 등록" to listOf("시스템 > 동의 약관 등록 (등록 / 수정)")),
        AdminPermission.USER_READ to ("사용자 조회" to listOf("시스템 > 사용자 관리")),
        AdminPermission.USER_WRITE to ("사용자 관리 (비밀번호 리셋 / 활성화)" to listOf("시스템 > 사용자 관리 (비밀번호 초기화 / 활성-비활성 토글)")),
    )

    init {
        val missing: Set<AdminPermission> = AdminPermission.entries.toSet() - permissionInfoMap.keys
        require(missing.isEmpty()) {
            "AdminPermissionMatrixService.permissionInfoMap 누락 권한: $missing — 모든 AdminPermission 은 description/menus 메타를 가져야 한다"
        }
    }

    fun getMatrix(userId: Long): PermissionMatrixResponse {
        val permissions = AdminPermission.entries.map { perm ->
            val (description, menus) = permissionInfoMap.getValue(perm)
            PermissionDetail(
                code = perm.name,
                description = description,
                menus = menus
            )
        }

        val allRolePermissions = rolePermissionRepository.findAll()
            .groupBy { it.role }
            .mapNotNull { (roleName, perms) ->
                val role = parseRole(roleName) ?: return@mapNotNull null
                RolePermissions(
                    role = role.name,
                    roleLabel = role.toKorean(),
                    permissions = perms.map { it.permission }
                )
            }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val role = employee.role
        val userPermissions = adminPermissionResolver.resolve(employee)
            .map { it.name }
        val isSystemAdmin = role == UserRole.SYSTEM_ADMIN

        return PermissionMatrixResponse(
            permissions = permissions,
            roles = allRolePermissions,
            currentUser = CurrentUserPermission(
                role = role?.name ?: "",
                roleLabel = role?.toKorean() ?: "",
                permissions = userPermissions,
                canManagePermissions = isSystemAdmin
            )
        )
    }

    private fun parseRole(name: String): UserRole? = try {
        UserRole.valueOf(name)
    } catch (_: IllegalArgumentException) {
        null
    }
}
