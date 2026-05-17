package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.UpdateAuthorityRequest
import com.otoki.powersales.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.powersales.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.powersales.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.powersales.admin.dto.response.RolePermissionsUpdateResponse
import com.otoki.powersales.admin.dto.response.UpdateAuthorityResponse
import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.admin.entity.UserPermission
import com.otoki.powersales.admin.exception.*
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.repository.deleteByRole
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminEmployeePermissionService(
    private val employeeRepository: EmployeeRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val userPermissionRepository: UserPermissionRepository,
    private val adminPermissionResolver: AdminPermissionResolver,
    private val userRepository: UserRepository,
) {

    /**
     * @param principal 인증된 web admin 사용자 — role / employeeId snapshot 만 사용.
     *                  SYSTEM_ADMIN 외 role 이면 [AdminForbiddenException].
     */
    fun getEmployeePermissions(principal: WebUserPrincipal, employeeId: Long): EmployeePermissionDetailResponse {
        requireSystemAdmin(principal)
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }
        val result = adminPermissionResolver.resolveWithDetails(employee)
        return EmployeePermissionDetailResponse.from(employee, result)
    }

    @Transactional
    fun updateUserPermissions(principal: WebUserPrincipal, employeeId: Long, request: UpdateUserPermissionsRequest): EmployeePermissionDetailResponse {
        requireSystemAdmin(principal)
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }

        if (principal.employeeId == employee.id) {
            throw CannotModifyOwnPermissionException()
        }

        validatePermissions(request.permissions)

        val user = userRepository.findByEmployeeCode(employee.employeeCode)
            ?: throw AdminUserNotFoundException(employee.id)

        userPermissionRepository.deleteByUserId(user.id)
        request.permissions.forEach { perm ->
            userPermissionRepository.save(
                UserPermission(
                    userId = user.id,
                    permission = perm
                )
            )
        }

        val result = adminPermissionResolver.resolveWithDetails(employee)
        return EmployeePermissionDetailResponse.from(employee, result)
    }

    @Transactional
    fun updateAuthority(principal: WebUserPrincipal, employeeId: Long, request: UpdateAuthorityRequest): UpdateAuthorityResponse {
        requireSystemAdmin(principal)
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException() }

        if (principal.employeeId == employee.id) {
            throw CannotModifyOwnAuthorityException()
        }

        if (request.role !in UserRole.ALLOWED_FOR_ADMIN_LOGIN) {
            throw InvalidAuthorityException(request.role.name)
        }

        val previousRole = employee.role
        employee.role = request.role
        employeeRepository.save(employee)

        val effectivePermissions = adminPermissionResolver.resolve(employee).map { it.name }

        return UpdateAuthorityResponse(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            previousRole = previousRole?.name,
            previousRoleLabel = previousRole?.toKorean(),
            newRole = request.role.name,
            newRoleLabel = request.role.toKorean(),
            effectivePermissions = effectivePermissions
        )
    }

    @Transactional
    fun updateRolePermissions(principal: WebUserPrincipal, role: UserRole, request: UpdateRolePermissionsRequest): RolePermissionsUpdateResponse {
        requireSystemAdmin(principal)

        if (role !in UserRole.ALLOWED_FOR_ADMIN_LOGIN) {
            throw InvalidAuthorityException(role.name)
        }

        validatePermissions(request.permissions)

        rolePermissionRepository.deleteByRole(role)
        rolePermissionRepository.flush()
        request.permissions.forEach { perm ->
            rolePermissionRepository.save(
                RolePermission(
                    role = role.name,
                    permission = perm
                )
            )
        }

        return RolePermissionsUpdateResponse.of(role, request.permissions)
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (principal.role != UserRole.SYSTEM_ADMIN) {
            throw AdminForbiddenException()
        }
    }

    private fun validatePermissions(permissions: List<String>) {
        val seen = mutableSetOf<String>()
        for (perm in permissions) {
            try {
                AdminPermission.valueOf(perm)
            } catch (_: IllegalArgumentException) {
                throw InvalidPermissionException(perm)
            }
            if (!seen.add(perm)) {
                throw DuplicatePermissionException(perm)
            }
        }
    }
}
