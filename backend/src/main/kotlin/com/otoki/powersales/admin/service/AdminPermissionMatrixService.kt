package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.*
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
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
        AdminPermission.EMPLOYEE_READ to ("사원 조회" to listOf("사원 > 여사원 현황")),
        AdminPermission.ACCOUNT_READ to ("거래처 조회" to listOf("SAP 데이터 > 거래처")),
        AdminPermission.PROMOTION_READ to ("행사 조회" to listOf("여사원 배치 > 행사마스터", "전문행사조")),
        AdminPermission.PROMOTION_WRITE to ("행사 생성/수정" to listOf("여사원 배치 > 진열스케줄마스터", "행사 등록/수정/확정/삭제")),
        AdminPermission.SAFETY_CHECK_READ to ("안전점검 조회" to listOf("안전점검")),
        AdminPermission.SCHEDULE_READ to ("일정 조회" to listOf("여사원 일정관리", "여사원관리 > 월별 통합일정", "여사원관리 > 근무형태별 인원현황")),
        AdminPermission.SCHEDULE_WRITE to ("일정 생성/수정" to listOf("여사원 배치 > 진열스케줄마스터 일정 확정"))
    )

    fun getMatrix(userId: Long): PermissionMatrixResponse {
        val permissions = AdminPermission.entries.map { perm ->
            val (description, menus) = permissionInfoMap[perm] ?: ("(미정의)" to emptyList())
            PermissionDetail(
                code = perm.name,
                description = description,
                menus = menus
            )
        }

        val allRolePermissions = rolePermissionRepository.findAll()
            .groupBy { it.role }
            .map { (role, perms) ->
                RolePermissions(
                    role = role,
                    permissions = perms.map { it.permission }
                )
            }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val userRole = employee.appAuthority ?: ""
        val userPermissions = adminPermissionResolver.resolve(employee)
            .map { it.name }
        val isSystemAdmin = employee.appAuthority == "시스템관리자"

        return PermissionMatrixResponse(
            permissions = permissions,
            roles = allRolePermissions,
            currentUser = CurrentUserPermission(
                role = userRole,
                permissions = userPermissions,
                canManagePermissions = isSystemAdmin
            )
        )
    }
}
